package rearth.oritech.block.blocks.machines.accelerator;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorMotorBlockEntity;

public class AcceleratorMotorBlock extends AcceleratorPassthroughBlock implements BlockEntityProvider {
    
    public AcceleratorMotorBlock(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AcceleratorMotorBlockEntity(pos, state);
    }
}
