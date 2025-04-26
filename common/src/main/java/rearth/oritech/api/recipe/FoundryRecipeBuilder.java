package rearth.oritech.api.recipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;

public class FoundryRecipeBuilder extends OritechRecipeBuilder {
    private static final String resourcePath = "foundry/alloy";
    
    private FoundryRecipeBuilder() {
        super(RecipeContent.FOUNDRY, resourcePath);
    }

    public static OritechRecipeBuilder build() {
        return new FoundryRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if ((inputs == null || inputs.size() < 2) || (results == null || results.isEmpty()))
            throw new IllegalStateException("wrong number of inputs and results for recipe " + id + " (type " + type + ")");
    }
}
