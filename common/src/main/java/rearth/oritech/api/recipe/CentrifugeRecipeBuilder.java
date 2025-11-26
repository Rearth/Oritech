package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class CentrifugeRecipeBuilder extends OritechRecipeBuilder {

    protected CentrifugeRecipeBuilder() {
        super(RecipeContent.CENTRIFUGE, "centrifuge");
        this.time = 100;
    }

    public static OritechRecipeBuilder build() {
        return new CentrifugeRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if ((inputs == null || inputs.isEmpty()) || (results == null || results.isEmpty()))
            throw new IllegalStateException("inputs and results are required for recipe " + id + " (type " + type + ")");
        if (inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe " + id + " (type " + type + ")");
    }
}