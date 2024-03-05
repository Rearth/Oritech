package rearth.oritech.block.blocks.machines.interaction;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockFrameInteractionBlock;
import rearth.oritech.block.entity.machines.interaction.DestroyerBlockEntity;

public class DestroyerBlock extends MultiblockFrameInteractionBlock {
    public DestroyerBlock(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DestroyerBlockEntity(pos, state);
    }
}
