package rearth.oritech.client.ui;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class BasicMachineOutputSlot extends ResourceHandlerSlot {

    // An output-only slot. This could be expanded to give XP to the player when items are removed, similar to the FurnaceOutputSlot.

    public BasicMachineOutputSlot(StacksResourceHandler<ItemStack, ItemResource> inventory, int index, int x, int y) {
        super(inventory, inventory::set, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
