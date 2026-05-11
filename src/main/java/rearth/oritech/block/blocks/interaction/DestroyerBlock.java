package rearth.oritech.block.blocks.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockFrameInteractionBlock;
import rearth.oritech.block.entity.interaction.DestroyerBlockEntity;

public class DestroyerBlock extends MultiblockFrameInteractionBlock {
    public DestroyerBlock(Properties settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DestroyerBlockEntity(pos, state);
    }
}
