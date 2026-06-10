package rearth.oritech.init.compat.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.init.recipes.OritechRecipe;

import java.util.List;

// holds the recipe data, which is then utilized by the different categories
public record OritechDisplay(RecipeHolder<OritechRecipe> entry) implements Display {

    @Override
    public List<EntryIngredient> getInputEntries() {
        return CollectionUtils.map(entry.value().getInputs(), EntryIngredients::ofIngredient);
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return CollectionUtils.map(entry.value().getResults(), EntryIngredients::of);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIdentifier.of(entry.value().getOriType().getIdentifier());
    }
}
