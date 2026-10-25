package rearth.oritech.spaceage.block.cubesat;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.SpaceAgeConfig;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.CubeSatData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketRecovery;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Real-world shrinking, cube block contents and launch ramp firing.
 */
public final class CubeSatGameTests {

    private CubeSatGameTests() {
    }

    // An engine with a charged battery and an antenna on top, standing on a 3x3 pad.
    private static BlockPos buildRocket(ServerLevel level, BlockPos pad) {

        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(pad.offset(dx, 0, dz), SpaceAgeBlocks.ROCKET_PAD.get().defaultBlockState());
        var engine = pad.above();
        level.setBlockAndUpdate(engine, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState());
        level.setBlockAndUpdate(engine.above(), BlockContent.PORTABLE_ENERGY_STORAGE.get().defaultBlockState());
        level.setBlockAndUpdate(engine.above(2), SpaceAgeBlocks.ANTENNA.get().defaultBlockState());
        var energy = level.getCapability(Capabilities.Energy.BLOCK, engine.above(), null);
        try (var transaction = Transaction.openRoot()) {
            energy.insert(100_000, transaction);
            transaction.commit();
        }
        return engine;
    }

    // A light chemical cube which can climb against Earth's gravity.
    private static CubeSatData cube(boolean scanner) {

        var id = UUID.randomUUID();
        var blocks = new HashSet<StaticRocketSegment.BlockData>();
        blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()));
        blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), (scanner ? SpaceAgeBlocks.SPACE_SCANNER : SpaceAgeBlocks.ANTENNA).get().defaultBlockState()));
        var segment = new StaticRocketSegment(id, blocks, Map.of(), 2, 1, 0, 40_000);
        return CubeSatData.of(new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(40_000, 0, 1, Set.of()))));
    }

    public static void shrink(GameTestHelper test) {

        var level = test.getLevel();
        var pad = test.absolutePos(new BlockPos(2, 1, 2));
        var engine = buildRocket(level, pad);
        var stored = level.getCapability(Capabilities.Energy.BLOCK, engine.above(), null).getAmountAsLong();
        test.assertTrue(CubeSatSplicerBehavior.operation(level, pad, 2).getBlockingReason().isPresent(), "A 3 block tall rocket needs core quality 3");
        var operation = CubeSatSplicerBehavior.operation(level, pad, 3);
        test.assertTrue(operation.getBlockingReason().isEmpty(), "Core quality 3 shrinks a 3 block tall rocket");
        test.assertTrue(operation.getEnergyCost() == 3 * SpaceAgeConfig.cubeSatRfPerBlock(), "Shrinking costs RF per block");

        var otherPad = pad.east(4);
        var otherEngine = buildRocket(level, otherPad);
        var other = CubeSatBlock.data(CubeSatSplicerBehavior.operation(level, otherPad, 3).getPreviewResult());

        var data = CubeSatBlock.data(operation.execute(level));
        test.assertTrue(data != null && data.blockCount() == 3, "The cube holds the whole rocket");
        var rocket = data.unfold(UUID.randomUUID(), null);
        test.assertTrue(rocket.getDynamicSegments().values().stream().mapToLong(segment -> segment.availableRF).sum() == stored && stored > 0,
                "Battery RF is captured");
        test.assertTrue(level.getBlockState(engine).isAir() && level.getBlockState(engine.above(2)).isAir(), "The rocket blocks are removed");
        test.assertTrue(level.getBlockState(pad).is(SpaceAgeBlocks.ROCKET_PAD), "The pad stays");
        test.assertTrue(other != null && other.fingerprint() == data.fingerprint(), "Identical rockets on other pads share a layout");

        level.setBlockAndUpdate(otherEngine.above(3), SpaceAgeBlocks.CUBE_SAT.get().defaultBlockState());
        test.assertTrue(CubeSatSplicerBehavior.operation(level, otherPad, 4).getBlockingReason().isPresent(), "Cube-sats can't contain cube-sats");
        test.succeed();
    }

    public static void contents(GameTestHelper test) {

        var level = test.getLevel();
        var data = cube(false).withName("Probe");
        var pos = test.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, SpaceAgeBlocks.CUBE_SAT.get().defaultBlockState());
        ((CubeSatBlockEntity) level.getBlockEntity(pos)).setData(data);
        var drops = Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos));
        test.assertTrue(drops.size() == 1 && data.equals(CubeSatBlock.data(drops.getFirst())), "Broken cubes drop with their contents and name");
        level.removeBlock(pos, false);

        // A host landing with an undeployed cube brings the cube back intact.
        var hostId = UUID.randomUUID();
        var host = new StaticRocketSegment(hostId, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.CUBE_SAT.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(1, 0, 0), Blocks.IRON_BLOCK.defaultBlockState())), Map.of(), 4, 0);
        var resources = new DynamicRocketSegment(0, 0, 0, Set.of());
        resources.payloads.put(BlockPos.ZERO, data);
        test.assertTrue(RocketRecovery.restore(level, new ActiveRocketData(Map.of(hostId, host), Map.of(hostId, resources)), pos), "Host recovers");
        test.assertTrue(level.getBlockEntity(pos) instanceof CubeSatBlockEntity cube && data.equals(cube.data()), "The carried cube keeps its data");
        test.succeed();
    }

    public static void launchRamp(GameTestHelper test) {

        var level = test.getLevel();
        var missions = MissionSavedData.get(level.getServer());
        var rampPos = test.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(rampPos, SpaceAgeBlocks.LAUNCH_RAMP.get().defaultBlockState());
        var ramp = (LaunchRampBlockEntity) level.getBlockEntity(rampPos);
        var pattern = cube(false);
        ramp.pattern.setItem(0, CubeSatBlock.createStack(pattern));
        var orbit = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.LOW);
        var plan = FlightPlan.empty();
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(orbit))));
        ramp.savePlan(FakePlayerFactory.getMinecraft(level), ramp.createPreview().getRocketId(), plan);

        var previewId = ramp.createPreview().getRocketId();
        ramp.pattern.setItem(0, CubeSatBlock.createStack(cube(true)));
        ramp.savePlan(FakePlayerFactory.getMinecraft(level), previewId, plan);
        test.assertTrue(!ramp.planForPattern(), "Saving a stale preview cannot attach its plan to a different pattern");
        ramp.pattern.setItem(0, CubeSatBlock.createStack(pattern));
        test.assertTrue(ramp.planForPattern(), "Rejecting a stale preview preserves the original plan fingerprint");

        var known = new HashSet<>(missions.craft.keySet());
        ramp.launch(cube(false), 10_000, new Vec3(1, 0, 0));
        var launched = missions.craft.values().stream().filter(state -> !known.contains(state.rocket.getRocketId())).toList();
        test.assertTrue(launched.size() == 1 && launched.getFirst().plan.root().actions().getFirst().id().equals(orbit.id()),
                "An identical cube follows the ramp's plan");
        test.assertTrue(ramp.lastOutcome() == LaunchRampBlockEntity.Outcome.PLAN && ramp.planForPattern(), "The ramp reports the planned launch");
        test.assertTrue(Math.hypot(launched.getFirst().position.vx(), launched.getFirst().position.vy()) > 0, "The particle speed is the starting velocity");
        known.addAll(missions.craft.keySet());

        ramp.launch(cube(true), 10_000, new Vec3(1, 0, 0));
        var coasting = missions.craft.values().stream().filter(state -> !known.contains(state.rocket.getRocketId())).toList();
        test.assertTrue(coasting.size() == 1 && coasting.getFirst().plan.root().actions().isEmpty()
                && coasting.getFirst().position.orbit() == OrbitBand.LOW, "A different cube coasts into low Earth orbit");
        test.assertTrue(ramp.lastOutcome() == LaunchRampBlockEntity.Outcome.ORBIT, "The ramp reports the orbit");
        known.addAll(missions.craft.keySet());

        ramp.launch(cube(true), 10, new Vec3(1, 0, 0));
        test.assertTrue(known.containsAll(missions.craft.keySet()), "A slow cube creates no craft");
        var landed = false;
        for (var candidate : BlockPos.betweenClosed(rampPos.offset(1, -3, -3), rampPos.offset(7, 6, 3)))
            landed |= level.getBlockState(candidate).is(SpaceAgeBlocks.SPACE_SCANNER);
        test.assertTrue(landed && ramp.lastOutcome() == LaunchRampBlockEntity.Outcome.FELL_BACK, "A slow cube falls back next to the ramp as blocks");

        launched.forEach(state -> missions.craft.remove(state.rocket.getRocketId()));
        coasting.forEach(state -> missions.craft.remove(state.rocket.getRocketId()));
        test.succeed();
    }
}
