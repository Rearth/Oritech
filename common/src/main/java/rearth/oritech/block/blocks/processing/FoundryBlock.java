package rearth.oritech.block.blocks.processing;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.processing.FoundryBlockEntity;

public class FoundryBlock extends MultiblockMachine implements BlockEntityProvider {
    
    public FoundryBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return FoundryBlockEntity.class;
    }
}
