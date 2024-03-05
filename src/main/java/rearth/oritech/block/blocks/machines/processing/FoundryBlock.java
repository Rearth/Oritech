package rearth.oritech.block.blocks.machines.processing;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.machines.processing.AssemblerBlockEntity;
import rearth.oritech.block.entity.machines.processing.FoundryBlockEntity;

public class FoundryBlock extends MultiblockMachine implements BlockEntityProvider {
    
    public FoundryBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return FoundryBlockEntity.class;
    }
}
