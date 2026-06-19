package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class FuelGeneratorRecipeBuilder extends OritechRecipeBuilder {
    public FuelGeneratorRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.FUEL_GENERATOR, "fuelgen", registryAccess);
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (fluidInput == null)
            throw new IllegalStateException("fluid input required for recipe " + id + " (type " + type + ")");
    }
}
