package rearth.oritech.client.ui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Common interface for machine screen handlers, allowing compat layers (EMI, REI, JEI)
 */
public interface MachineMenuHandler {
    BlockEntity getBlockEntity();
    int getMachineInvStartSlot(ItemStack stack);
    int getMachineInvEndSlot(ItemStack stack);
}
