package rearth.oritech.spaceage.block.cubesat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.SpaceAgeConfig;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampBlockEntity;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ServiceSettings;
import rearth.oritech.spaceage.simulation.flight.LaunchSpeedRules;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.SpacecraftModel;
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
            for (int dz = -1; dz <= 1; dz++)
                level.setBlockAndUpdate(pad.offset(dx, 0, dz), SpaceAgeBlocks.ROCKET_PAD.get().defaultBlockState());
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
        for (var includeData : List.of(false, true)) {
            var picked = SpaceAgeBlocks.CUBE_SAT.get().getCloneItemStack(level, pos, level.getBlockState(pos), includeData,
                    FakePlayerFactory.getMinecraft(level));
            test.assertTrue(data.equals(CubeSatBlock.data(picked)), "Pick-block preserves the whole cube with either data modifier");
            var placed = new CubeSatBlockEntity(pos, level.getBlockState(pos));
            placed.applyComponentsFromItemStack(picked);
            test.assertTrue(data.equals(placed.data()), "A picked cube places with the original contents and name");
        }
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
        level.setBlockAndUpdate(rampPos, SpaceAgeBlocks.LAUNCH_RAMP.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        var ramp = (LaunchRampBlockEntity) level.getBlockEntity(rampPos);
        ramp.launch(cube(false), 10_000, new Vec3(1, 0, 0));
        test.assertTrue(ramp.lastShot == -1, "An unassembled launcher cannot fire");
        level.setBlockAndUpdate(rampPos.above(), BlockContent.MACHINE_CORE_1.get().defaultBlockState());
        test.assertTrue(!ramp.initMultiblock(ramp.getBlockState()), "One core is insufficient");
        level.setBlockAndUpdate(rampPos.above().east(), BlockContent.MACHINE_CORE_1.get().defaultBlockState());
        test.assertTrue(ramp.initMultiblock(ramp.getBlockState()), "Above and above-forward cores assemble the launcher");
        test.assertTrue(ramp.getConnectedCores().size() == 2, "Exactly two cores belong to the launcher");
        var player = FakePlayerFactory.getMinecraft(level);
        test.assertTrue(ramp.createMenu(1, player.getInventory(), player) instanceof LaunchRampMenu,
                "The standalone launcher provides its planner menu");
        test.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, rampPos, null) == null,
                "The launcher exposes no energy storage");
        for (var core : ramp.getConnectedCores())
            test.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, core, null) == null,
                    "Launcher cores expose no inherited energy storage");
        var pattern = cube(false);
        ramp.pattern.setItem(0, CubeSatBlock.createStack(pattern));
        test.assertTrue(ramp.createPreview().spacecraftModel() == SpacecraftModel.ALTERNATE,
                "The automatic model for a cube without solar panels is the alternate");
        ramp.setModelChoice(SpacecraftModel.ORIGINAL);
        var orbit = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.LOW);
        var plan = FlightPlan.empty();
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(orbit))));
        ramp.savePlan(FakePlayerFactory.getMinecraft(level), ramp.createPreview().getRocketId(), plan);
        var savedSnapshot = ramp.createFlightPlannerSnapshot(player, ramp.createPreview().getRocketId());
        test.assertTrue(savedSnapshot.plan().root().actions().getFirst().id().equals(orbit.id()), "The editor shows the matching saved plan");
        var replacement = new LaunchRampBlockEntity(rampPos, ramp.getBlockState());
        replacement.setLevel(level);
        replacement.pattern.setItem(0, CubeSatBlock.createStack(pattern));
        test.assertTrue(replacement.createFlightPlannerSnapshot(player, replacement.createPreview().getRocketId()).plan().root().actions().isEmpty(),
                "A fresh launcher does not inherit an old plan from the same world position");

        var previewId = ramp.createPreview().getRocketId();
        ramp.pattern.setItem(0, CubeSatBlock.createStack(cube(true)));
        ramp.savePlan(FakePlayerFactory.getMinecraft(level), previewId, plan);
        test.assertTrue(!ramp.planForPattern(), "Editing the saved design cannot attach it to a different inserted pattern");
        test.assertTrue(ramp.createFlightPlannerSnapshot(player, ramp.createPreview().getRocketId()).plan().root().actions().isEmpty(),
                "A mismatched pattern does not display another cube's saved plan as its own");
        ramp.pattern.setItem(0, CubeSatBlock.createStack(pattern));
        test.assertTrue(ramp.planForPattern(), "Rejecting a stale preview preserves the original plan fingerprint");

        ramp.pattern.removeItemNoUpdate(0);
        test.assertTrue(ramp.createPreview() != null && ramp.savedPattern().equals(pattern), "Removing the item retains a stored design preview");
        test.assertTrue(ramp.createFlightPlannerSnapshot(player, ramp.createPreview().getRocketId()).plan().equals(ramp.savedPlan()),
                "The plan stays inspectable without an inserted item");
        var savedWithoutItem = new LaunchRampBlockEntity(rampPos, ramp.getBlockState());
        savedWithoutItem.setLevel(level);
        savedWithoutItem.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
                ramp.saveWithoutMetadata(level.registryAccess())));
        test.assertTrue(savedWithoutItem.pattern.isEmpty() && savedWithoutItem.savedPattern().equals(pattern)
                && savedWithoutItem.savedPlan().equals(ramp.savedPlan()), "The full program and design survive reload with an empty slot");
        test.assertTrue(savedWithoutItem.createPreview().modelChoice() == SpacecraftModel.ORIGINAL,
                "The manual model survives reloading the launcher");

        var known = new HashSet<>(missions.craft.keySet());
        ramp.launch(cube(false), 10_000, new Vec3(1, 0, 0));
        var launched = missions.craft.values().stream().filter(state -> !known.contains(state.rocket.getRocketId())).toList();
        test.assertTrue(launched.size() == 1 && launched.getFirst().plan.root().actions().getFirst().id().equals(orbit.id()),
                "An identical cube follows the ramp's plan");
        test.assertTrue(launched.getFirst().rocket.spacecraftModel() == SpacecraftModel.ORIGINAL
                && launched.getFirst().summary(0).rocket().spacecraftModel() == SpacecraftModel.ORIGINAL,
                "The launch and Mission Control summary both carry the selected model");
        test.assertTrue(ramp.lastOutcome() == LaunchRampBlockEntity.Outcome.PLAN && ramp.pattern.isEmpty(), "The ramp reports the planned launch");
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

        test.assertTrue(ramp.lastShot == level.getGameTime(), "Launching synchronizes the firing time");
        var restored = new LaunchRampBlockEntity(rampPos, ramp.getBlockState());
        restored.setLevel(level);
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
                ramp.saveWithoutMetadata(level.registryAccess())));
        test.assertTrue(restored.getConnectedCores().equals(ramp.getConnectedCores()), "Core links survive save and reload");
        test.assertTrue(restored.savedPlan().equals(ramp.savedPlan()) && restored.pattern.isEmpty() && restored.lastSpeed() == ramp.lastSpeed()
                && restored.lastOutcome() == ramp.lastOutcome(), "The pattern, plan ownership and launch status survive reload");
        // A flyable nearest-point preview must not authorize an unreachable assigned slot.
        var ionId = UUID.randomUUID();
        var ionStructure = new StaticRocketSegment(ionId, Set.of(
                new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ANTENNA.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(2), SpaceAgeBlocks.NAVIGATION_COMPUTER.get().defaultBlockState())),
                Map.of(), 5, 1, 20_000, 0);
        var ionCube = CubeSatData.of(new ActiveRocketData(Map.of(ionId, ionStructure),
                Map.of(ionId, new DynamicRocketSegment(0, 20_000, 0, Set.of()))));
        // Fix the launch longitude so this regression is independent of the test runner's random world origin.
        var slotRamp = new LaunchRampBlockEntity(rampPos, ramp.getBlockState()) {

            @Override
            public BlockPos launchPosition() {

                return new BlockPos(15, 65, 7);
            }
        };
        slotRamp.setLevel(level);
        slotRamp.pattern.setItem(0, CubeSatBlock.createStack(ionCube));
        slotRamp.savePlan(player, slotRamp.createPreview().getRocketId(), plan);
        var system = SpaceSimulationSavedData.getForPlayer(player);
        var ionPreview = slotRamp.createPreview();
        var blockedSlot = -1;
        for (var slot = 0; slot < SpaceBalance.slots(OrbitBand.LOW); slot++) {
            var destination = orbit.withService(new ServiceSettings(0, 0, slot));
            var assigned = plan.withBranches(List.of(plan.root().withActions(List.of(destination))));
            if (!LaunchSpeedRules.valid(assigned, ionPreview, system.createObjectData(), 7_000, List.of(), List.of())) {
                blockedSlot = slot;
                break;
            }
        }
        test.assertTrue(blockedSlot >= 0 && LaunchSpeedRules.valid(plan, ionPreview, system.createObjectData(), 7_000, List.of(), List.of()),
                "The fixture has a feasible Auto route and an unreachable explicit slot");
        var testMissions = new HashSet<UUID>();
        for (var slot = 0; slot < SpaceBalance.slots(OrbitBand.LOW); slot++) {
            if (slot == blockedSlot) continue;
            var satellite = ionCube.unfold(UUID.randomUUID(), null);
            var position = MissionState.Position.surface().placed(SpaceObjects.EARTH_X, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.LOW, slot);
            missions.craft.put(satellite.getRocketId(), new MissionState(player.getUUID(), satellite, FlightPlan.empty(), position,
                    system.earthKnowledge.copy(), ramp.launchPosition()));
            testMissions.add(satellite.getRocketId());
        }
        var beforeResolvedLaunch = new HashSet<>(missions.craft.keySet());
        slotRamp.launch(ionCube, 7_000, Vec3.ZERO);
        var fallback = missions.craft.values().stream().filter(state -> !beforeResolvedLaunch.contains(state.rocket.getRocketId())).toList();
        test.assertTrue(slotRamp.lastOutcome() == LaunchRampBlockEntity.Outcome.ORBIT && fallback.size() == 1
                        && fallback.getFirst().plan.root().actions().isEmpty(),
                "An unreachable allocated slot uses fallback orbit instead of creating a blocked planned rocket");
        fallback.forEach(state -> testMissions.add(state.rocket.getRocketId()));
        testMissions.forEach(missions.craft::remove);

        var brokenCore = rampPos.above().east();
        var coreState = level.getBlockState(brokenCore);
        coreState.getBlock().playerWillDestroy(level, brokenCore, coreState, FakePlayerFactory.getMinecraft(level));
        level.destroyBlock(brokenCore, false);
        test.assertTrue(!ramp.isAssembled(ramp.getBlockState()), "Breaking a core disassembles the launcher");
        test.assertTrue(!level.getBlockState(rampPos.above()).getValue(MachineCoreBlock.USED), "The other core is released");
        level.setBlockAndUpdate(brokenCore, BlockContent.MACHINE_CORE_1.get().defaultBlockState());
        test.assertTrue(ramp.initMultiblock(ramp.getBlockState()), "Replacing the core allows reassembly");
        ramp.pattern.setItem(0, CubeSatBlock.createStack(pattern));
        level.destroyBlock(rampPos, false);
        test.assertTrue(!level.getBlockState(rampPos.above()).getValue(MachineCoreBlock.USED)
                && !level.getBlockState(brokenCore).getValue(MachineCoreBlock.USED), "Removing the launcher releases both cores");
        test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(rampPos).inflate(2)).stream()
                .filter(item -> CubeSatBlock.data(item.getItem()) != null).count() == 1, "Removing the launcher drops its pattern once");
        launched.forEach(state -> missions.craft.remove(state.rocket.getRocketId()));
        coasting.forEach(state -> missions.craft.remove(state.rocket.getRocketId()));
        test.succeed();
    }
}
