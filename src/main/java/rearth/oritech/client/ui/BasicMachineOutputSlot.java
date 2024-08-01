package rearth.oritech.client.ui;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class BasicMachineOutputSlot extends Slot {

   // An output-only slot. This could be expanded to give XP to the player when items are removed, similar to the FurnaceOutputSlot.

   public BasicMachineOutputSlot(Inventory inventory, int index, int x, int y) {
      super(inventory, index, x, y);
   }

   public boolean canInsert(ItemStack stack) {
      return false;
   }
}
