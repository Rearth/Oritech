package rearth.oritech.generator.compat;

import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.generators.common.registries.GeneratorsFluids;
import net.minecraft.data.recipes.RecipeOutput;
import rearth.oritech.api.recipe.FuelGeneratorRecipeBuilder;

public class MekanismGeneratorsRecipeGenerator {
    private static String PATH = "compat/mekanism/";
    public static void generateRecipes(RecipeOutput exporter) {
        FuelGeneratorRecipeBuilder.build()
            .fluidInput(GeneratorsFluids.BIOETHANOL.asStack(100).getFluid(), 0.1f)
            .timeInSeconds(3)
            .export(exporter, PATH + "bioethanol");
    }    
}
