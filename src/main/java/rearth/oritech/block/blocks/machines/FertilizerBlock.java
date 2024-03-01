package rearth.oritech.block.blocks.machines;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.FrameInteractionBlock;
import rearth.oritech.block.entity.machines.FertilizerBlockEntity;

public class FertilizerBlock extends FrameInteractionBlock {
    public FertilizerBlock(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FertilizerBlockEntity(pos, state);
    }
}
