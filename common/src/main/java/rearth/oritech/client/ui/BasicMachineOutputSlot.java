package rearth.oritech.client.ui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BasicMachineOutputSlot extends Slot {

   // An output-only slot. This could be expanded to give XP to the player when items are removed, similar to the FurnaceOutputSlot.

   public BasicMachineOutputSlot(Container inventory, int index, int x, int y) {
      super(inventory, index, x, y);
   }

   public boolean mayPlace(ItemStack stack) {
      return false;
   }
}
