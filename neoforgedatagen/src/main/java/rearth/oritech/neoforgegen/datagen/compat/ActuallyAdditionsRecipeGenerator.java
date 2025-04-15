package rearth.oritech.neoforgegen.datagen.compat;

import de.ellpeck.actuallyadditions.mod.crafting.LiquidFuelRecipe;
import de.ellpeck.actuallyadditions.mod.crafting.PressingRecipe;
import de.ellpeck.actuallyadditions.mod.fluids.InitFluids;
import de.ellpeck.actuallyadditions.mod.items.ActuallyItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.FuelGeneratorRecipeBuilder;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.TagContent;

public class ActuallyAdditionsRecipeGenerator {
    private static final String PATH = "compat/actuallyadditions/";
    public static void generateRecipes(RecipeOutput exporter) {
        // centrifuge to produce oil
        CentrifugeFluidRecipeBuilder.build()
            .input(ActuallyItems.CRYSTALLIZED_CANOLA_SEED.get())
            .fluidInput(InitFluids.REFINED_CANOLA_OIL.get())
            .fluidOutput(InitFluids.CRYSTALLIZED_OIL.get())
            .export(exporter, PATH + "crystallizedoil");
        CentrifugeFluidRecipeBuilder.build()
            .input(ActuallyItems.EMPOWERED_CANOLA_SEED.get())
            .fluidInput(InitFluids.CRYSTALLIZED_OIL.get())
            .fluidOutput(InitFluids.EMPOWERED_OIL.get())
            .export(exporter, PATH + "empoweredoil");

        // generator fuel recipes
        FuelGeneratorRecipeBuilder.build()
            .fluidInput(InitFluids.CRYSTALLIZED_OIL.get(), 0.1f)
            .timeInSeconds(3)
            .export(exporter, PATH + "crystallizedoil");
        FuelGeneratorRecipeBuilder.build()
            .fluidInput(InitFluids.EMPOWERED_OIL.get(), 0.1f)
            .timeInSeconds(12)
            .export(exporter, PATH + "empoweredoil");
        
        // AA fuel recipes
        exporter.accept(Oritech.id(PATH + "fuel/oil"), new LiquidFuelRecipe(new FluidStack(FluidContent.STILL_OIL.get(), 50), 120, 9_600), null);
        exporter.accept(Oritech.id(PATH + "fuel/biofuel"), new LiquidFuelRecipe(new FluidStack(FluidContent.STILL_BIOFUEL.get(), 50), 120, 9_600), null);
        exporter.accept(Oritech.id(PATH + "fuel/fuel"), new LiquidFuelRecipe(new FluidStack(FluidContent.STILL_FUEL.get(), 50), 400, 48_000), null);

        // turn biomass into fluid biofuel
        exporter.accept(Oritech.id(PATH + "pressing/biomass"), new PressingRecipe(Ingredient.of(TagContent.BIOMASS), new FluidStack(FluidContent.STILL_BIOFUEL.get(), 80)), null);

    }
}
