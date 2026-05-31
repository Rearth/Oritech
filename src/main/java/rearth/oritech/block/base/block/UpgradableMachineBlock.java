package rearth.oritech.block.base.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import rearth.oritech.util.MachineAddonController;

import java.util.function.BiConsumer;

public abstract class UpgradableMachineBlock extends MachineBlock {

    public UpgradableMachineBlock(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {

            var entity = level.getBlockEntity(pos);
            if (!(entity instanceof MachineAddonController machineEntity)) {
                return InteractionResult.SUCCESS;
            }

            machineEntity.initAddons();

        }

        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {

        if (!level.isClientSide()) {
            var entity = level.getBlockEntity(pos);
            if (entity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {

        if (!level.isClientSide()) {
            var entity = level.getBlockEntity(pos);
            if (entity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
        }

        super.onExplosionHit(state, level, pos, explosion, stackMerger);
    }
}
