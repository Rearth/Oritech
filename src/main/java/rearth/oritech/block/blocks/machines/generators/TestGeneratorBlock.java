package rearth.oritech.block.blocks.machines.generators;

import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.UpgradableMachineBlock;
import rearth.oritech.block.entity.machines.generators.TestGeneratorEntity;

public class TestGeneratorBlock extends UpgradableMachineBlock {
    public TestGeneratorBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return TestGeneratorEntity.class;
    }
}
