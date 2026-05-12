package rearth.oritech.init.recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record OritechRecipeInput(@NotNull List<ItemStack> itemStacks, @NotNull FluidStack fluidStack) implements RecipeInput {
    
    @Override
    public ItemStack getItem(int slot) {
        if (itemStacks.isEmpty()) return ItemStack.EMPTY;
        return itemStacks.get(slot);
    }
    
    public boolean itemsEmpty() {
        return itemStacks.isEmpty() || itemStacks.stream().allMatch(ItemStack::isEmpty);
    }
    
    public boolean fluidEmpty() {
        return fluidStack.isEmpty();
    }
    
    public FluidStack getFluid() {
        return fluidStack;
    }
    
    @Override
    public int size() {
        return itemStacks().size() + (fluidEmpty() ? 0 : 1);
    }
    
    @Override
    public boolean isEmpty() {
        return itemsEmpty() && fluidEmpty();
    }
}
