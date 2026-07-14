package rearth.oritech.block.blocks.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.processing.TaintedRefineryBlockEntity;

public class TaintedRefineryBlock extends MultiblockMachine implements EntityBlock {

    public TaintedRefineryBlock(Properties settings) {
        super(settings);
    }

    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return TaintedRefineryBlockEntity.class;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, placer, itemStack);

        if (!level.isClientSide() && placer instanceof Player player && player.isCreative() && level.getBlockEntity(pos) instanceof TaintedRefineryBlockEntity refinery) {
            refinery.afterCreation();
        }
    }
}
