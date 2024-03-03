package rearth.oritech.block.blocks.machines.processing;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.machines.processing.GrinderBlockEntity;

public class GrinderBlock extends MultiblockMachine implements BlockEntityProvider {
    
    public GrinderBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return GrinderBlockEntity.class;
    }
}
