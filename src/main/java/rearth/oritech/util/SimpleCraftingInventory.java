package rearth.oritech.util;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;

public class SimpleCraftingInventory extends SimpleInventory implements RecipeInput {
    
    public SimpleCraftingInventory(int size) {
        super(size);
    }
    
    public SimpleCraftingInventory(ItemStack ... items) {
        super(items);
    }
    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.getStack(slot);
    }
    
    @Override
    public int getSize() {
        return this.size();
    }
}
