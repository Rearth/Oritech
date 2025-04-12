package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class FuelGeneratorRecipeBuilder extends OritechRecipeBuilder {
    private FuelGeneratorRecipeBuilder() {
        super(RecipeContent.FUEL_GENERATOR, "fuelgen");
    }

    public static OritechRecipeBuilder build() {
        return new FuelGeneratorRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (fluidInput == null || fluidInput.isEmpty())
            throw new IllegalStateException("fluid input required for recipe " + id + " (type " + type + ")");
    }
}
