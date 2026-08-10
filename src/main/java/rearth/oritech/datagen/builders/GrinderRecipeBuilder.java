package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class GrinderRecipeBuilder extends OritechRecipeBuilder {

    public GrinderRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.FRAGMENT_FORGE, "grinder", registryAccess);
        this.time = 40;
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if ((inputs == null || inputs.isEmpty()) || (results == null || results.isEmpty()))
            throw new IllegalStateException("inputs and results required for recipe " + id + " (type " + type + ")");
    }

}
