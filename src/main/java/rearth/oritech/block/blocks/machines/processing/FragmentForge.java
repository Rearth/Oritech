package rearth.oritech.block.blocks.machines.processing;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.machines.processing.FragmentForgeBlockEntity;

public class FragmentForge extends MultiblockMachine implements BlockEntityProvider {
    
    public FragmentForge(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return FragmentForgeBlockEntity.class;
    }
}
