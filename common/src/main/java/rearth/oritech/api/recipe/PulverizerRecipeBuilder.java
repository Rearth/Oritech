package rearth.oritech.api.recipe;

import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class PulverizerRecipeBuilder extends OritechRecipeBuilder {

    protected PulverizerRecipeBuilder() {
        super(RecipeContent.PULVERIZER, "pulverizer");
    }

    public static PulverizerRecipeBuilder build() {
        return new PulverizerRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if ((inputs == null || inputs.isEmpty()) || (results == null || results.isEmpty()))
            throw new IllegalStateException("inputs and results required for recipe " + id + " (type " + type + ")");
    }

    @Override
    public void export(RecipeExporter exporter, String suffix) {
        super.export(exporter, suffix);

        if (addToGrinder)
            GrinderRecipeBuilder.build().input(inputs).result(results).export(exporter, suffix);
    }
}
