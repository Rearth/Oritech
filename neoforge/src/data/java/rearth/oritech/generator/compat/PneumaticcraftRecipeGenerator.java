package rearth.oritech.generator.compat;

import me.desht.pneumaticcraft.common.registry.ModFluids;
import me.desht.pneumaticcraft.common.registry.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.CoolerRecipeBuilder;
import rearth.oritech.api.recipe.FuelGeneratorRecipeBuilder;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;

public class PneumaticcraftRecipeGenerator {
    public static final String PATH = "compat/pneumaticcraft/";
    public static void generateRecipes(RecipeOutput exporter) {
        CentrifugeFluidRecipeBuilder.build().input(Tags.Items.MUSHROOMS).fluidInput(Fluids.WATER).fluidOutput(ModFluids.YEAST_CULTURE.get(), 0.25f).export(exporter, PATH + "yeast");
        CentrifugeFluidRecipeBuilder.build().input(ModItems.WHEAT_FLOUR.get()).result(ModItems.SOURDOUGH.get()).fluidInput(ModFluids.YEAST_CULTURE.get()).export(exporter, PATH + "dough");
        
        CoolerRecipeBuilder.build().fluidInput(ModFluids.PLASTIC.get()).result(ItemContent.PLASTIC_SHEET).export(exporter, PATH + "plastic");

        // fuels
        CentrifugeFluidRecipeBuilder.build().input(ItemContent.FLUXITE).fluidInput(ModFluids.OIL.get()).fluidOutput(FluidContent.STILL_FUEL.get()).export(exporter, PATH + "fuel");
        FuelGeneratorRecipeBuilder.build().fluidInput(ModFluids.OIL.get()).timeInSeconds(3).export(exporter, PATH + "oil");
        FuelGeneratorRecipeBuilder.build().fluidInput(ModFluids.DIESEL.get()).timeInSeconds(12).export(exporter, PATH + "diesel");
        FuelGeneratorRecipeBuilder.build().fluidInput(ModFluids.BIODIESEL.get()).timeInSeconds(12).export(exporter, PATH + "biodiesel");
    }
}
