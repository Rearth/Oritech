package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class ReactorGeneratorRecipeBuilder extends OritechRecipeBuilder {

    public ReactorGeneratorRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.REACTOR, "reactorgen", registryAccess);
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs == null || inputs.isEmpty())
            throw new IllegalStateException("Input required for recipe " + id + " (type " + type + ")");
    }
}
