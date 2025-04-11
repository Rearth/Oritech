package rearth.oritech.neoforgegen.datagen.compat;

import rearth.oritech.api.recipe.FuelGeneratorRecipeBuilder;

import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.generators.common.registries.GeneratorsFluids;

import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.data.recipes.RecipeOutput;

public class MekanismGeneratorsRecipeGenerator {
    private static String PATH = "compat/mekanism/";
    public static void generateRecipes(RecipeOutput exporter) {
        FuelGeneratorRecipeBuilder.build()
            .fluidInput(GeneratorsFluids.BIOETHANOL.getFluidStack(100).getFluid(), 0.1f)
            .timeInSeconds(3)
            .export(exporter, PATH + "bioethanol");
    }    
}
