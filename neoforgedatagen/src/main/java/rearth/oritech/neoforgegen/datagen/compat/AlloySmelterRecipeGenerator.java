package rearth.oritech.neoforgegen.datagen.compat;

import rearth.oritech.Oritech;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

import sk.alloy_smelter.recipe.SmeltingRecipe;
import sk.alloy_smelter.recipe.SmeltingRecipe.Material;

import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.common.Tags;

import static rearth.oritech.util.datagen.RecipeGeneratorUtil.of;

public class AlloySmelterRecipeGenerator {
    public static void generateRecipes(RecipeOutput exporter) {
        offerAlloyRecipe(exporter, of(TagContent.NICKEL_INGOTS), of(Tags.Items.GEMS_DIAMOND), ItemContent.ADAMANT_INGOT, 10, 1, "adamant");
        offerAlloyRecipe(exporter, of(Tags.Items.INGOTS_IRON), of(ItemContent.RAW_BIOPOLYMER.asItem()), ItemContent.BIOSTEEL_INGOT.asItem(), 10, 1, "biosteel");
        offerAlloyRecipe(exporter, of(TagContent.PLATINUM_INGOTS), of(Tags.Items.INGOTS_NETHERITE), ItemContent.DURATIUM_INGOT.asItem(), 20, 1, "duratium");
        offerAlloyRecipe(exporter, of(Tags.Items.INGOTS_GOLD), of(Tags.Items.DUSTS_REDSTONE), ItemContent.ELECTRUM_INGOT.asItem(), 10, 1, "electrum");
        offerAlloyRecipe(exporter, of(TagContent.NICKEL_INGOTS), of(ItemContent.FLUXITE.asItem()), ItemContent.ENERGITE_INGOT.asItem(), 10, 1, "energite");
        offerAlloyRecipe(exporter, of(Tags.Items.INGOTS_IRON), of(TagContent.COAL_DUSTS), ItemContent.STEEL_INGOT.asItem(), 5, 1, "steel");
    }

    private static void offerAlloyRecipe(RecipeOutput exporter, Ingredient A, Ingredient B, Item output, int fuelPerTick, int requiredTier, String suffix) {
        exporter.accept(Oritech.id("compat/alloysmelter/" + suffix), new SmeltingRecipe(NonNullList.of(Material.of(A, 1), Material.of(B, 1)), new ItemStack(output), 200, fuelPerTick, requiredTier), null);
    }
}
