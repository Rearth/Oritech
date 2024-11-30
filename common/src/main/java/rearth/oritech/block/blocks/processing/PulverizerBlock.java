package rearth.oritech.block.blocks.processing;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.UpgradableMachineBlock;
import rearth.oritech.block.entity.processing.PulverizerBlockEntity;

public class PulverizerBlock extends UpgradableMachineBlock implements BlockEntityProvider {
    
    public PulverizerBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return PulverizerBlockEntity.class;
    }
}
