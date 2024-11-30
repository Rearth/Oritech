package rearth.oritech.block.blocks.generators;

import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.generators.BioGeneratorEntity;

public class BioGeneratorBlock extends MultiblockMachine {
    public BioGeneratorBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return BioGeneratorEntity.class;
    }
}
