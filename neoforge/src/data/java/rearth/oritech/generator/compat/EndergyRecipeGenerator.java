package rearth.oritech.generator.compat;

import static rearth.oritech.util.TagUtils.cItemTag;

import com.enderio.endergy.common.EnderIOEndergy;
import com.enderio.endergy.common.init.EndergyItems;
import com.enderio.enderio.init.EIOItems;

import appeng.recipes.handlers.ChargerRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.init.ItemContent;

public class EndergyRecipeGenerator {
    private static final String PATH = "compat/" + EnderIOEndergy.MOD_ID + "/";

    public static void generateRecipes(RecipeOutput exporter) {
        FoundryRecipeBuilder.build().input(EIOItems.CONDUIT_BINDER_COMPOSITE.get()).input(Tags.Items.COBBLESTONES_NORMAL).result(EndergyItems.CRUDE_STEEL_INGOT.get()).time(180).export(exporter, PATH + "crudesteel");
        FoundryRecipeBuilder.build().input(EIOItems.PULSATING_POWDER.get()).input(Tags.Items.INGOTS_GOLD).result(EndergyItems.CRYSTALLINE_ALLOY_INGOT.get()).time(360).export(exporter, PATH + "crystalline");
        FoundryRecipeBuilder.build().input(EIOItems.END_STEEL_INGOT.get()).input(Items.POPPED_CHORUS_FRUIT).result(EndergyItems.MELODIC_ALLOY_INGOT.get()).time(720).export(exporter, PATH + "melodic");
    }
}
