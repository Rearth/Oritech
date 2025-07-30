package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class LavaGeneratorRecipeBuilder extends OritechRecipeBuilder {

    protected LavaGeneratorRecipeBuilder() {
        super(RecipeContent.LAVA_GENERATOR, "lavagen");
    }

    public static OritechRecipeBuilder build() {
        return new LavaGeneratorRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if (fluidInput == null || fluidInput.isEmpty())
            throw new IllegalStateException("fluid input required for recipe " + id + " (type " + type + ")");
    }
}
