package rearth.oritech.block.blocks.machines.generators;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.machines.generators.FuelGeneratorEntity;

import java.util.Objects;

public class FuelGeneratorBlock extends MultiblockMachine {
    public FuelGeneratorBlock(Settings settings) {
        super(settings);
    }
    
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // because the model is inverted, we dont do the opposite here
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing());
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return FuelGeneratorEntity.class;
    }
}
