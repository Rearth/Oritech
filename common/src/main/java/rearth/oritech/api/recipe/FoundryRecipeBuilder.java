package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class FoundryRecipeBuilder extends OritechRecipeBuilder {
    private static final String resourcePath = "foundry/alloy";
    
    private FoundryRecipeBuilder() {
        super(RecipeContent.FOUNDRY, resourcePath);
        this.time = 120;
    }

    public static OritechRecipeBuilder build() {
        return new FoundryRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if ((inputs == null || inputs.size() < 2) || (results == null || results.isEmpty()))
            throw new IllegalStateException("wrong number of inputs and results for recipe " + id + " (type " + type + ")");
    }
}
