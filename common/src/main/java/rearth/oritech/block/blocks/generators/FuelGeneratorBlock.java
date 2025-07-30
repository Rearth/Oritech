package rearth.oritech.block.blocks.generators;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.generators.FuelGeneratorEntity;

import java.util.Objects;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class FuelGeneratorBlock extends MultiblockMachine {
    public FuelGeneratorBlock(Properties settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // because the model is inverted, we dont do the opposite here
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection());
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return FuelGeneratorEntity.class;
    }
}
