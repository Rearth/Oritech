package rearth.oritech.client.ui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Common interface for machine screen handlers, allowing compat layers (EMI, REI, JEI)
 * to work with both old (BasicMachineScreenHandler) and new (OritechScreenHandler) handler types.
 */
public interface MachineMenuHandler {
    BlockEntity getBlockEntity();
    int getMachineInvStartSlot(ItemStack stack);
    int getMachineInvEndSlot(ItemStack stack);
}
