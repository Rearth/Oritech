package rearth.oritech.block.custom.machines;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MachineBlock;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.machines.GrinderBlockEntity;

public class GrinderBlock extends MultiblockMachine implements BlockEntityProvider {
    
    public GrinderBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return GrinderBlockEntity.class;
    }
}
