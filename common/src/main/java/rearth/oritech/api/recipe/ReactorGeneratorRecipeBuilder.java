package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class ReactorGeneratorRecipeBuilder extends OritechRecipeBuilder {

    protected ReactorGeneratorRecipeBuilder() {
        super(RecipeContent.REACTOR, "reactorgen");
    }

    public static OritechRecipeBuilder build() {
        return new ReactorGeneratorRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs == null || inputs.isEmpty())
            throw new IllegalStateException("Input required for recipe " + id + " (type " + type + ")");
    }
}
