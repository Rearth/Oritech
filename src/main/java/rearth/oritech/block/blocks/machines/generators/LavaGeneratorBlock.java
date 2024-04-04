package rearth.oritech.block.blocks.machines.generators;

import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.machines.generators.LavaGeneratorEntity;

public class LavaGeneratorBlock extends MultiblockMachine {
    public LavaGeneratorBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return LavaGeneratorEntity.class;
    }
}
