package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class SteamGeneratorRecipeBuilder extends OritechRecipeBuilder {

    protected SteamGeneratorRecipeBuilder() {
        super(RecipeContent.STEAM_ENGINE, "steamgen");
    }

    public static OritechRecipeBuilder build() {
        return new SteamGeneratorRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if (fluidInput == null || fluidInput.isEmpty())
            throw new IllegalStateException("fluid input required for recipe " + id + " (type " + type + ")");
    }
}
