package rearth.oritech.block.blocks.machines.accelerator;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorSensorBlockEntity;
import rearth.oritech.block.entity.machines.storage.SmallFluidTankEntity;

public class AcceleratorSensorBlock extends AcceleratorPassthroughBlock implements BlockEntityProvider {
    
    public AcceleratorSensorBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }
    
    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ((AcceleratorSensorBlockEntity) world.getBlockEntity(pos)).getComparatorOutput();
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AcceleratorSensorBlockEntity(pos, state);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
}
