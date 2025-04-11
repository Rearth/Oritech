package rearth.oritech.api.recipe;

import rearth.oritech.init.recipes.RecipeContent;

public class CentrifugeFluidRecipeBuilder extends OritechRecipeBuilder {

    protected CentrifugeFluidRecipeBuilder() {
        super(RecipeContent.CENTRIFUGE_FLUID, "centrifuge/fluid");
    }

    public static OritechRecipeBuilder build() {
        return new CentrifugeFluidRecipeBuilder();
    }

    @Override
    public void validate() throws IllegalStateException {
        if (this.inputs != null && this.inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe type " + this.type);
        if ((this.fluidInput == null || this.fluidInput.isEmpty()) && (this.fluidOutput == null || this.fluidOutput.isEmpty()))
            throw new IllegalStateException("fluid input or output required for recipe type " + this.type);
    }
    
}
