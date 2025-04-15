package rearth.oritech.neoforgegen.datagen.compat;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;

import com.buuz135.industrial.module.ModuleCore;
import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.RecipeOutput;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.FuelGeneratorRecipeBuilder;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;

public class IndustrialForegoingRecipeGenerator {
    private static final String PATH = "compat/industrialforegoing/";

    public static void generateRecipes(RecipeOutput exporter) {
        FuelGeneratorRecipeBuilder.build()
            .fluidInput(ModuleCore.BIOFUEL.getSourceFluid().get(), 0.1f)
            .timeInSeconds(3)
            .export(exporter, PATH + "biofuel");

        CentrifugeFluidRecipeBuilder.build()
            .input(ItemContent.FLUXITE)
            .fluidInput(ModuleCore.BIOFUEL.getSourceFluid().get())
            .fluidOutput(FluidContent.STILL_FUEL.get())
            .export(exporter, PATH + "turbofuel");
    }    
}
