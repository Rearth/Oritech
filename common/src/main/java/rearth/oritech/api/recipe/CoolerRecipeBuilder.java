package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class CoolerRecipeBuilder extends OritechRecipeBuilder {

    protected CoolerRecipeBuilder() {
        super(RecipeContent.COOLER, "cooler");
    }

    public static OritechRecipeBuilder build() {
        return new CoolerRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (results == null || results.isEmpty())
            throw new IllegalStateException("Results required for recipe " + id + " (type " + type + ")");
        if (fluidInput == null || fluidInput.isEmpty())
            throw new IllegalStateException("Fluid input required for recipe " + id + " (type " + type + ")");
    }
    
}
