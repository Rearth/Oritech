package rearth.oritech.block.blocks.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.reactor.ReactorAbsorberPortEntity;

public class ReactorAbsorberPortBlock extends BaseReactorBlock implements EntityBlock {
    public ReactorAbsorberPortBlock(Properties settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorAbsorberPortEntity(pos, state);
    }

    @Override
    public boolean validForWalls() {
        return true;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ReactorAbsorberPortEntity) {
            player.openMenu((MenuProvider) level.getBlockEntity(pos), pos);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {

        if (!level.isClientSide()) {
            var entity = (ReactorAbsorberPortEntity) level.getBlockEntity(pos);
            var stacks = entity.inventory.heldStacks;
            for (var stack : stacks) {
                if (!stack.isEmpty()) {
                    var itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    level.addFreshEntity(itemEntity);
                }
            }

            entity.inventory.heldStacks.clear();
            entity.inventory.setChanged();
        }

        return super.playerWillDestroy(level, pos, state, player);
    }
}
