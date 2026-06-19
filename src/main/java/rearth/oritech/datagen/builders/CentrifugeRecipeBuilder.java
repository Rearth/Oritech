package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class CentrifugeRecipeBuilder extends OritechRecipeBuilder {

    public CentrifugeRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.CENTRIFUGE, "centrifuge", registryAccess);
        this.time = 100;
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if ((inputs == null || inputs.isEmpty()) || (results == null || results.isEmpty()))
            throw new IllegalStateException("inputs and results are required for recipe " + id + " (type " + type + ")");
        if (inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe " + id + " (type " + type + ")");
    }
}
