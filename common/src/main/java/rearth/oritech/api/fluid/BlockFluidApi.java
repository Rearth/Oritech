package rearth.oritech.api.fluid;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface BlockFluidApi {
    
    void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier);
    
    FluidApi.FluidStorage find(World world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction);
    
    FluidApi.FluidStorage find(World world, BlockPos pos, @Nullable Direction direction);
}
