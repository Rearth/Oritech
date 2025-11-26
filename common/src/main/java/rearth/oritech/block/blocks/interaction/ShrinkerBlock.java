package rearth.oritech.block.blocks.interaction;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;

public class ShrinkerBlock extends MultiblockMachine {
    
    public ShrinkerBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return ShrinkerBlockEntity.class;
    }
    
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }
}
