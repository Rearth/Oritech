package rearth.oritech.block.custom;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.MachineBlock;
import rearth.oritech.block.base.MultiblockMachine;
import rearth.oritech.block.entity.AssemblerBlockEntity;

public class AssemblerBlock extends MultiblockMachine implements BlockEntityProvider {
    
    public AssemblerBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return AssemblerBlockEntity.class;
    }
}
