package rearth.oritech.init.recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

// common bridge to avoid duplicate recipe matching code for augment and oritech recipe
public interface OritechRecipeItemInput extends RecipeInput {
    
    List<ItemStack> getStacks();
    
}
