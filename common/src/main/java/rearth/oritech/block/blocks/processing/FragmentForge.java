package rearth.oritech.block.blocks.processing;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.processing.FragmentForgeBlockEntity;

public class FragmentForge extends MultiblockMachine implements EntityBlock {
    
    public FragmentForge(Properties settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return FragmentForgeBlockEntity.class;
    }
}
