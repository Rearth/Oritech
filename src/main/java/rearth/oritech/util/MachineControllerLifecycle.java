package rearth.oritech.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import rearth.oritech.block.base.block.MultiblockMachine;

public interface MachineControllerLifecycle extends IBlockEntityExtension {

    @Override
    default void onLoad() {
        IBlockEntityExtension.super.onLoad();

        if (!(this instanceof BlockEntity blockEntity) || !(blockEntity.getLevel() instanceof ServerLevel serverLevel))
            return;

        serverLevel.getServer().execute(() -> {
            if (blockEntity.isRemoved() || blockEntity.getLevel() != serverLevel)
                return;

            // Multiblocks provide addon slots, so restore them before scanning addons.
            if (this instanceof MultiblockMachineController multiblockController)
                multiblockController.rescanMultiblock();
            if (this instanceof MachineAddonController addonController) {
                var state = blockEntity.getBlockState();
                if (!state.hasProperty(MultiblockMachine.ASSEMBLED) || state.getValue(MultiblockMachine.ASSEMBLED))
                    addonController.initAddons();
                else
                    addonController.resetAddons();
            }

            blockEntity.setChanged();
        });
    }
}
