package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class RefineryRecipeBuilder extends OritechRecipeBuilder {

    protected RefineryRecipeBuilder() {
        super(RecipeContent.REFINERY, "refinery");
    }

    public static OritechRecipeBuilder build() {
        return new RefineryRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs != null && inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe " + id + " (type " + type + ")");
        if (results != null && results.size() > 1)
            throw new IllegalStateException("too many outputs for recipe " + id + " (type " + type + ")");
    }
    
}
