package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class CoolerRecipeBuilder extends OritechRecipeBuilder {

    public CoolerRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.COOLER, "cooler", registryAccess);
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (results == null || results.isEmpty())
            throw new IllegalStateException("Results required for recipe " + id + " (type " + type + ")");
        if (fluidInput == null)
            throw new IllegalStateException("Fluid input required for recipe " + id + " (type " + type + ")");
    }

}
