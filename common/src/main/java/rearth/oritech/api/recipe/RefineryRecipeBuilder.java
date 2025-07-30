package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class RefineryRecipeBuilder extends OritechRecipeBuilder {

    protected RefineryRecipeBuilder() {
        super(RecipeContent.REFINERY, "refinery");
        this.time = 80;
    }

    public static OritechRecipeBuilder build() {
        return new RefineryRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if (inputs != null && inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe " + id + " (type " + type + ")");
        if (results != null && results.size() > 1)
            throw new IllegalStateException("too many outputs for recipe " + id + " (type " + type + ")");
    }
    
}
