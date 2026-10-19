package rearth.oritech.spaceage.robot;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.robot.RobotProgram.Action;
import rearth.oritech.spaceage.robot.RobotProgram.Kind;
import rearth.oritech.spaceage.robot.RobotProgram.StepSignal;

import java.util.List;
import java.util.Optional;

/** Exercises real placement, harvesting, interaction, and an in-flight save/reload. */
public final class RobotGameTests {

    private RobotGameTests() {}

    public static void playback(GameTestHelper test) {

        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 3, 2));
        var state = SpaceAgeBlocks.ROBOT_ARM.get().defaultBlockState().setValue(MultiblockMachine.ASSEMBLED, true);
        level.setBlock(pos, state, 3);
        var arm = (RobotArmBlockEntity) level.getBlockEntity(pos);
        var range = RobotArmBlockEntity.RANGE;
        test.assertTrue(arm.inRange(pos.above().east(range)), "Work area reaches its calculated horizontal radius");
        test.assertTrue(arm.inRange(pos.above().below(range)), "Work area is centred one block above the controller");
        test.assertTrue(!arm.inRange(pos.above().east(range + 1)), "Work area excludes positions beyond maximum reach");
        var target = pos.offset(3, 0, 0);
        var lever = pos.offset(3, 0, 1);
        level.setBlock(target.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(lever.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR), 3);
        arm.program = new RobotProgram(Optional.of(GlobalPos.of(level.dimension(), pos)), List.of(
                action(Kind.PLACE, target, new ItemStack(Items.DIRT)),
                action(Kind.DESTROY, target, ItemStack.EMPTY),
                action(Kind.INTERACT, lever, ItemStack.EMPTY)));
        arm.inventory.set(1, ItemResource.of(Items.DIRT), 1);
        arm.energyStorage.set(100_000);
        arm.running = true;
        tick(arm, 20);
        test.assertTrue(arm.visual.phase() == 2 && arm.inventory.getItem(1).isEmpty(), "Placement reserves exactly one item");
        var saved = arm.saveWithFullMetadata(level.registryAccess());
        var restored = (RobotArmBlockEntity) BlockEntity.loadStatic(pos, state, saved, level.registryAccess());
        test.assertTrue(restored != null, "Robot state loads");
        restored.setLevel(level);
        level.setBlockEntity(restored);
        arm = restored;
        tick(arm, 2);
        test.assertTrue(level.getBlockState(target).is(Blocks.DIRT), "Reloaded arm completes placement");
        test.assertTrue(!arm.visual.returnToWarp(), "Placement stays at the target before harvesting");
        arm.running = false;
        var before = arm.energyStorage.getAmountAsLong();
        tick(arm, 10);
        test.assertTrue(before == arm.energyStorage.getAmountAsLong(), "Paused arm spends no energy");
        arm.running = true;
        tick(arm, 2);
        test.assertTrue(arm.visual.start().equals(target), "Next action starts at the preceding target");
        tick(arm, 140);
        test.assertTrue(!arm.running && level.getBlockState(target).isAir(), "Playbook finishes and harvests target");
        test.assertTrue(arm.inventory.getStacks().stream().filter(stack -> stack.is(Items.DIRT)).mapToInt(ItemStack::getCount).sum() == 1,
                "Harvest returns exactly one item");
        test.assertTrue(level.getBlockState(lever).getValue(BlockStateProperties.POWERED), "Generic interaction toggles lever");

        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        var codec = NetworkManager.getAutoCodec(RobotArmBlockEntity.Visual.class);
        codec.encode(buffer, arm.visual);
        test.assertTrue(((RobotArmBlockEntity.Visual) codec.decode(buffer)).target().equals(arm.visual.target()), "Visual data has a valid network codec");
        buffer.release();

        arm.program = arm.program.withActions(List.of(action(Kind.INTERACT, lever, ItemStack.EMPTY).withSignal(StepSignal.WAIT_FOR_PULSE)));
        arm.redstoneMode = 3;
        arm.restart();
        tick(arm, 10);
        test.assertTrue(arm.status.equals("waiting_pulse") && level.getBlockState(lever).getValue(BlockStateProperties.POWERED),
                "Custom step waits without using the lever");
        level.setBlock(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        tick(arm, 1);
        level.setBlock(pos.west(), Blocks.AIR.defaultBlockState(), 3);
        tick(arm, 65);
        test.assertTrue(!arm.running && !level.getBlockState(lever).getValue(BlockStateProperties.POWERED), "Pulse releases one waiting action");

        arm.redstoneMode = 2;
        arm.repeat = true;
        level.setBlock(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        tick(arm, 130);
        test.assertTrue(!arm.running && level.getBlockState(lever).getValue(BlockStateProperties.POWERED),
                "Cycle-on-pulse runs once even with repeat enabled and a sustained signal");
        level.setBlock(pos.west(), Blocks.AIR.defaultBlockState(), 3);
        tick(arm, 1);

        arm.redstoneMode = 3;
        arm.repeat = false;
        arm.program = arm.program.withActions(List.of(action(Kind.INTERACT, lever, ItemStack.EMPTY).withSignal(StepSignal.EMIT_PULSE)));
        arm.restart();
        tick(arm, 1);
        test.assertTrue(arm.pulseTicks == 4 && state.getSignal(level, pos, Direction.WEST) == 15, "Custom step emits a redstone pulse");
        tick(arm, 65);

        arm.redstoneMode = 0;
        arm.program = arm.program.withActions(List.of(action(Kind.DESTROY, target, ItemStack.EMPTY)));
        level.setBlock(target, Blocks.DIRT.defaultBlockState(), 3);
        for (int slot = 1; slot < arm.getInventorySize(); slot++) arm.inventory.set(slot, ItemResource.of(Items.COBBLESTONE), 64);
        arm.restart();
        tick(arm, 65);
        test.assertTrue(arm.status.equals("inventory_full") && level.getBlockState(target).isAir() && arm.visual.held().is(Items.DIRT),
                "Harvest remains in the gripper when storage is full");
        arm.inventory.set(1, ItemResource.EMPTY, 0);
        tick(arm, 1);
        test.assertTrue(arm.inventory.getItem(1).is(Items.DIRT) && !arm.running, "Freeing storage deposits the pending harvest once");

        arm.inventory.set(2, ItemResource.EMPTY, 0);
        level.setBlock(target, Blocks.STONE.defaultBlockState(), 3);
        arm.restart();
        tick(arm, 25);
        test.assertTrue(arm.visual.phase() == 2 && arm.visual.duration() == 24,
                "Stone hardness lengthens only the harvesting phase");
        tick(arm, 23);
        test.assertTrue(level.getBlockState(target).is(Blocks.STONE), "Harder block survives until harvesting completes");
        tick(arm, 21);
        test.assertTrue(level.getBlockState(target).isAir() && !arm.running, "Hardness-scaled harvest completes and deposits");

        arm.program = arm.program.withActions(List.of(action(Kind.PLACE, target, new ItemStack(Items.DIRT))));
        level.setBlock(target, Blocks.STONE.defaultBlockState(), 3);
        arm.restart();
        tick(arm, 65);
        test.assertTrue(arm.status.equals("occupied") && arm.inventory.getItem(1).getCount() == 1,
                "Blocked placement waits without consuming its item");
        arm.skipBlocked = true;
        tick(arm, 1);
        test.assertTrue(!arm.running && arm.inventory.getItem(1).getCount() == 1, "Skip finishes an impossible action without consuming items");
        test.succeed();
    }

    private static Action action(Kind kind, BlockPos pos, ItemStack item) {

        return new Action(kind, pos, Blocks.DIRT.defaultBlockState(), Direction.UP, new Vec3(.5, .5, .5), item,
                0, 0, false, StepSignal.IGNORED);
    }

    private static void tick(RobotArmBlockEntity arm, int count) {

        for (int i = 0; i < count; i++) arm.serverTick((ServerLevel) arm.getLevel(), arm.getBlockPos(), arm.getBlockState(), arm);
    }
}
