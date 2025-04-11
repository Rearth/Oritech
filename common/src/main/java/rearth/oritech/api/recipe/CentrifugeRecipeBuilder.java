package rearth.oritech.api.recipe;

import rearth.oritech.init.recipes.RecipeContent;

public class CentrifugeRecipeBuilder extends OritechRecipeBuilder {

    protected CentrifugeRecipeBuilder() {
        super(RecipeContent.CENTRIFUGE, "centrifuge");
    }

    public static OritechRecipeBuilder build() {
        return new CentrifugeRecipeBuilder();
    }

    @Override
    public void validate() throws IllegalStateException {
        if ((this.inputs == null || this.inputs.isEmpty()) || (this.results == null || this.results.isEmpty()))
            throw new IllegalStateException("inputs and results are required for recipe type " + this.type);
        if (this.inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe type " + this.type);
    }
}