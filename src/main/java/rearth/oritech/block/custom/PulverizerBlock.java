package rearth.oritech.block.custom;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MachineBlock;
import rearth.oritech.block.entity.PulverizerBlockEntity;

public class PulverizerBlock extends MachineBlock implements BlockEntityProvider {
    
    public PulverizerBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return PulverizerBlockEntity.class;
    }
}
