package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class CentrifugeFluidRecipeBuilder extends OritechRecipeBuilder {

    protected CentrifugeFluidRecipeBuilder() {
        super(RecipeContent.CENTRIFUGE_FLUID, "centrifuge/fluid");
        this.time = 100;
    }

    public static OritechRecipeBuilder build() {
        return new CentrifugeFluidRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if (inputs != null && inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe " + id + " (type " + type + ")");
        if ((fluidInput == null || fluidInput.isEmpty()))
            throw new IllegalStateException("fluid input or output required for recipe " + id + " (type " + type + ")");
    }
    
}
