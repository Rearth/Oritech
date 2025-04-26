package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class BioGeneratorRecipeBuilder extends OritechRecipeBuilder {

    protected BioGeneratorRecipeBuilder() {
        super(RecipeContent.BIO_GENERATOR, "biogen");
    }

    public static OritechRecipeBuilder build() {
        return new BioGeneratorRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs == null || inputs.isEmpty())
            throw new IllegalStateException("inputs required for recipe " + id + " (type " + type + ")");
    }
}
