package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class RefineryRecipeBuilder extends OritechRecipeBuilder {

    public RefineryRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.REFINERY, "refinery", registryAccess);
        this.time = 80;
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs != null && inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe " + id + " (type " + type + ")");
        if (results != null && results.size() > 1)
            throw new IllegalStateException("too many outputs for recipe " + id + " (type " + type + ")");
    }

}
