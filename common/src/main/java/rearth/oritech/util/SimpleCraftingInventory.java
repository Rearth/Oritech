package rearth.oritech.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public class SimpleCraftingInventory implements RecipeInput {
    
    private final int size;
    private final NonNullList<ItemStack> items;

    public SimpleCraftingInventory(ItemStack ... items) {
        this.size = items.length;
        this.items = NonNullList.of(ItemStack.EMPTY, items);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return size;
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }
}
