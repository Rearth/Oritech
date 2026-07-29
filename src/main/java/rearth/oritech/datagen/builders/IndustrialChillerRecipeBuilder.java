package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class IndustrialChillerRecipeBuilder extends OritechRecipeBuilder {

    public IndustrialChillerRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.INDUSTRIAL_CHILLER, "industrial_chiller", registryAccess);
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (results == null || results.isEmpty())
            throw new IllegalStateException("Results required for recipe " + id + " (type " + type + ")");
        if (fluidInput == null)
            throw new IllegalStateException("Fluid input required for recipe " + id + " (type " + type + ")");
    }

}
