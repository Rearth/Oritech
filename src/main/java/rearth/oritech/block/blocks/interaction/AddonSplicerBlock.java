package rearth.oritech.block.blocks.interaction;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.interaction.AddonSplicerBlockEntity;

public class AddonSplicerBlock extends MultiblockMachine {

    public AddonSplicerBlock(Properties settings) {
        super(settings);
    }

    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return AddonSplicerBlockEntity.class;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }
}
