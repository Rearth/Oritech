package rearth.oritech.block.blocks.machines.generators;

import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.UpgradableMachineBlock;
import rearth.oritech.block.entity.machines.generators.BasicGeneratorEntity;

public class BasicGeneratorBlock extends UpgradableMachineBlock {
    public BasicGeneratorBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return BasicGeneratorEntity.class;
    }
}
