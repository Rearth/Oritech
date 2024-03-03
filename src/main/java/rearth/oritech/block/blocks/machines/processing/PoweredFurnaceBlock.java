package rearth.oritech.block.blocks.machines.processing;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.machines.processing.PoweredFurnaceBlockEntity;

public class PoweredFurnaceBlock extends MultiblockMachine implements BlockEntityProvider {
    
    public PoweredFurnaceBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return PoweredFurnaceBlockEntity.class;
    }
}
