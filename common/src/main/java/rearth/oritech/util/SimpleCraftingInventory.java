package rearth.oritech.util;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public class SimpleCraftingInventory extends SimpleContainer implements RecipeInput {
    
    public SimpleCraftingInventory(ItemStack ... items) {
        super(items);
    }
    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.getItems().size() ? this.getItems().get(slot) : ItemStack.EMPTY;
    }
    
    @Override
    public int size() {
        return this.getContainerSize();
    }
}
