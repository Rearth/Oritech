package rearth.oritech.api.recipe;

import rearth.oritech.init.recipes.RecipeContent;

public class FuelGeneratorRecipeBuilder extends OritechRecipeBuilder {
    private FuelGeneratorRecipeBuilder() {
        super(RecipeContent.FUEL_GENERATOR, "fuelgen");
    }

    public static OritechRecipeBuilder build() {
        return new FuelGeneratorRecipeBuilder();
    }

    @Override
    public void validate() throws IllegalStateException {
        if (this.fluidInput == null || this.fluidInput.isEmpty())
            throw new IllegalStateException("fluid input required for recipe type " + this.type);
    }
}
