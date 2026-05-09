package rearth.oritech.init.recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AugmentDataRecipeInput(@NotNull List<ItemStack> itemStacks, @NotNull MatchMode mode) implements RecipeInput {
    
    @Override
    public ItemStack getItem(int slot) {
        if (itemStacks == null || itemStacks.isEmpty()) return ItemStack.EMPTY;
        return itemStacks.get(slot);
    }
    
    @Override
    public int size() {
        return itemStacks().size();
    }
    
    @Override
    public boolean isEmpty() {
        return itemStacks.isEmpty();
    }
    
    public enum MatchMode {
        RESEARCH,
        APPLY
    }
}