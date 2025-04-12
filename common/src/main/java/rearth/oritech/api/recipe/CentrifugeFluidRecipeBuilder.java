package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class CentrifugeFluidRecipeBuilder extends OritechRecipeBuilder {

    protected CentrifugeFluidRecipeBuilder() {
        super(RecipeContent.CENTRIFUGE_FLUID, "centrifuge/fluid");
    }

    public static OritechRecipeBuilder build() {
        return new CentrifugeFluidRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs != null && inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe " + id + " (type " + type + ")");
        if ((fluidInput == null || fluidInput.isEmpty()) && (fluidOutput == null || fluidOutput.isEmpty()))
            throw new IllegalStateException("fluid input or output required for recipe " + id + " (type " + type + ")");
    }
    
}
