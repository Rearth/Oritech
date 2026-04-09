package rearth.oritech.api.recipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class PulverizerRecipeBuilder extends OritechRecipeBuilder {

    protected PulverizerRecipeBuilder() {
        super(RecipeContent.PULVERIZER, "pulverizer");
        this.time = 100;
    }

    public static PulverizerRecipeBuilder build() {
        return new PulverizerRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if ((inputs == null || inputs.isEmpty()) || (results == null || results.isEmpty()))
            throw new IllegalStateException("inputs and results required for recipe " + id + " (type " + type + ")");
    }

    @Override
    public void export(RecipeOutput exporter, String suffix) {
        super.export(exporter, suffix);

        if (addToGrinder)
            // Grinder defaults to 20% more time (rounded to the nearest half second) than pulverizer, but can have more addons and often gets better results
            // To have a different time, build the grinder recipe separately instead of using the .addToGrinder() method on the pulverizer recipe builder
            GrinderRecipeBuilder.build().input(inputs).result(results).time((int)(Math.round(this.time * 1.2 / 10.0) * 10)).export(exporter, suffix);
    }
}
