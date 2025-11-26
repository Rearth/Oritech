package rearth.oritech.block.blocks.processing;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;

public class CentrifugeBlock extends MultiblockMachine implements EntityBlock {
    
    public CentrifugeBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return CentrifugeBlockEntity.class;
    }
}
