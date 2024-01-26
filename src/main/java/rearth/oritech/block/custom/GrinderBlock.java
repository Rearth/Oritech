package rearth.oritech.block.custom;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.MachineBlock;
import rearth.oritech.block.entity.GrinderBlockEntity;
import rearth.oritech.block.entity.PulverizerBlockEntity;

public class GrinderBlock extends MachineBlock implements BlockEntityProvider {
    
    public GrinderBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return GrinderBlockEntity.class;
    }
}
