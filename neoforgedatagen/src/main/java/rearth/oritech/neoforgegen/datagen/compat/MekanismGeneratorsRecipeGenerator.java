package rearth.oritech.neoforgegen.datagen.compat;

import rearth.oritech.util.datagen.RecipeGeneratorUtil;

import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.generators.common.registries.GeneratorsFluids;

import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.data.recipes.RecipeOutput;

public class MekanismGeneratorsRecipeGenerator {
    public static void generateRecipes(RecipeOutput exporter) {
        RecipeGeneratorUtil.addFuelGenRecipe(exporter, FluidStackHooksForge.fromForge(GeneratorsFluids.BIOETHANOL.getFluidStack(100)), 3, "compat/mekanism/bioethanol");
    }    
}
