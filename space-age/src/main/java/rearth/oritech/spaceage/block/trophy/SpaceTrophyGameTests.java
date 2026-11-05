package rearth.oritech.spaceage.block.trophy;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.simulation.SpaceAgeAdvancements;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;

import java.util.Map;
import java.util.UUID;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

public final class SpaceTrophyGameTests {

    private SpaceTrophyGameTests() {
    }

    public static void trophy(GameTestHelper test) {

        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        var block = SpaceAgeBlocks.SPACE_TROPHY.get();
        var inscription = new TrophyInscription("Orbit Builder", OrbitBand.HIGH);
        var reward = new ItemStack(block);
        reward.set(SpaceAgeComponents.TROPHY_INSCRIPTION.get(), inscription);
        var player = FakePlayerFactory.getMinecraft(level);
        level.setBlockAndUpdate(pos.above(2), Blocks.STONE.defaultBlockState());
        var context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, reward,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        test.assertTrue(block.getStateForPlacement(context) != null, "A ceiling two blocks above does not prevent placement");
        level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        test.assertTrue(block.getStateForPlacement(context) == null, "The support block still needs free space");
        level.removeBlock(pos.above(), false);
        for (var facing : Direction.Plane.HORIZONTAL) {
            var state = block.defaultBlockState().setValue(SpaceTrophyBlock.FACING, facing);
            level.setBlockAndUpdate(pos, state);
            var trophy = (SpaceTrophyBlockEntity) level.getBlockEntity(pos);
            trophy.applyComponentsFromItemStack(reward);
            block.setPlacedBy(level, pos, state, player, reward);
            test.assertTrue(level.getBlockState(pos).getValue(ASSEMBLED) && trophy.getConnectedCores().size() == 1,
                    "Placement assembles the two-block trophy with one support core");
            test.assertTrue(level.getBlockState(pos.above()).getValue(MachineCoreBlock.USED), "The support core belongs to the trophy");
            test.assertTrue(level.getBlockState(pos.above(2)).is(Blocks.STONE), "Assembly leaves the ceiling untouched");
            var saved = trophy.saveWithFullMetadata(level.registryAccess());
            var restored = (SpaceTrophyBlockEntity) BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
            test.assertTrue(inscription.equals(restored.data()), "The inscription survives a world save");
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, trophy);
            test.assertTrue(drops.size() == 1 && inscription.equals(drops.getFirst().get(SpaceAgeComponents.TROPHY_INSCRIPTION.get())),
                    "Trophy loot retains the player and completed orbit");
            reward = drops.getFirst();
            level.destroyBlock(pos, false);
            test.assertTrue(level.isEmptyBlock(pos.above()) && level.getBlockState(pos.above(2)).is(Blocks.STONE), "Breaking the trophy removes its support and preserves the ceiling");
        }
        var state = block.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        var trophy = (SpaceTrophyBlockEntity) level.getBlockEntity(pos);
        trophy.applyComponentsFromItemStack(reward);
        block.setPlacedBy(level, pos, state, player, reward);
        var core = pos.above();
        var coreState = level.getBlockState(core);
        coreState.getBlock().playerWillDestroy(level, core, coreState, player);
        level.destroyBlock(core, true);
        test.assertTrue(level.isEmptyBlock(pos) && level.getBlockState(pos.above(2)).is(Blocks.STONE), "Breaking a support drops and removes the whole trophy");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3),
                item -> item.getItem().is(block.asItem()) && inscription.equals(item.getItem().get(SpaceAgeComponents.TROPHY_INSCRIPTION.get())));
        test.assertTrue(drops.size() == 1, "Breaking a support drops one inscribed trophy");
        test.assertTrue(SpaceAgeBlocks.SPACECRAFT_ICON.get().asItem() == Items.AIR, "The rendering block has no item");
        test.assertTrue(level.getServer().getAdvancements().get(SpaceAgeAdvancements.FULL_ORBIT) != null, "The generated advancement is loaded");
        test.succeed();
    }

    public static void advancement(GameTestHelper test) {

        var server = test.getLevel().getServer();
        var player = rewardPlayer(test.getLevel());
        var missions = MissionSavedData.get(server);
        try {
            var advancement = server.getAdvancements().get(SpaceAgeAdvancements.FULL_ORBIT);
            test.assertTrue(advancement != null, "The generated advancement is loaded");
            for (int slot = 0; slot < SpaceBalance.LOW_ORBIT_SLOTS; slot++) {
                var mission = new MissionState(player.getUUID(), new ActiveRocketData(Map.of(), Map.of()), FlightPlan.empty(),
                        new MissionState.Position(0, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.LOW, slot, 1,
                                FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO)), new SurveyKnowledge(), BlockPos.ZERO);
                missions.craft.put(mission.rocket.getRocketId(), mission);
            }
            SpaceAgeAdvancements.recordCompletedOrbits(missions, SpaceSimulationSavedData.get(server));
            // The saved completion can be awarded after the fleet has left, including after an offline completion.
            missions.craft.values().removeIf(craft -> craft.owner.equals(player.getUUID()));
            SpaceAgeAdvancements.checkPlayer(player);
            test.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(), "A full orbit awards the advancement");
            var inscription = new TrophyInscription(player.getGameProfile().name(), OrbitBand.LOW);
            var trophies = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                var stack = player.getInventory().getItem(slot);
                if (stack.is(SpaceAgeBlocks.SPACE_TROPHY.asItem())) {
                    trophies += stack.getCount();
                    test.assertTrue(inscription.equals(stack.get(SpaceAgeComponents.TROPHY_INSCRIPTION.get())),
                            "The reward bears the player's name and completed orbit");
                }
            }
            test.assertTrue(trophies == 1, "The player receives one trophy");
            SpaceAgeAdvancements.checkPlayer(player);
            var count = player.getInventory().countItem(SpaceAgeBlocks.SPACE_TROPHY.asItem());
            test.assertTrue(count == 1, "An already completed advancement does not award another trophy");
        } finally {
            missions.craft.values().removeIf(craft -> craft.owner.equals(player.getUUID()));
            player.getTextFilter().leave();
        }
        test.succeed();
    }

    private static ServerPlayer rewardPlayer(ServerLevel level) {

        // Match the robot tests: a real player without an unnegotiated modded-client login.
        var profile = new GameProfile(UUID.randomUUID(), "orbit-test-player");
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(profile, false)) {
            @Override
            public void send(Packet<?> packet) {
            }

            @Override
            public void send(Packet<?> packet, ChannelFutureListener listener) {
            }
        };
        return player;
    }

}
