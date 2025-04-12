package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class CentrifugeRecipeBuilder extends OritechRecipeBuilder {

    protected CentrifugeRecipeBuilder() {
        super(RecipeContent.CENTRIFUGE, "centrifuge");
    }

    public static OritechRecipeBuilder build() {
        return new CentrifugeRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if ((inputs == null || inputs.isEmpty()) || (results == null || results.isEmpty()))
            throw new IllegalStateException("inputs and results are required for recipe " + id + " (type " + type + ")");
        if (inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe " + id + " (type " + type + ")");
    }
}