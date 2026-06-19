package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class BioGeneratorRecipeBuilder extends OritechRecipeBuilder {

    public BioGeneratorRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.BIO_GENERATOR, "biogen", registryAccess);
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs == null || inputs.isEmpty())
            throw new IllegalStateException("inputs required for recipe " + id + " (type " + type + ")");
    }
}
