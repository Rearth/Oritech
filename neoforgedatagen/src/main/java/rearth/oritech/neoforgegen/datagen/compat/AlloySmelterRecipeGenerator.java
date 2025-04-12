package rearth.oritech.neoforgegen.datagen.compat;

import rearth.oritech.Oritech;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

import sk.alloy_smelter.recipe.SmeltingRecipe;
import sk.alloy_smelter.recipe.SmeltingRecipe.Material;

import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.common.Tags;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;

public class AlloySmelterRecipeGenerator {
    public static void generateRecipes(RecipeOutput exporter) {
        offerAlloyRecipe(exporter, of(TagContent.NICKEL_INGOTS), of(Tags.Items.GEMS_DIAMOND), new ItemStack(ItemContent.ADAMANT_INGOT), 10, 1, "adamant");
        offerAlloyRecipe(exporter, of(Tags.Items.INGOTS_IRON), of(ItemContent.RAW_BIOPOLYMER.asItem()), new ItemStack(ItemContent.BIOSTEEL_INGOT.asItem()), 10, 1, "biosteel");
        offerAlloyRecipe(exporter, of(TagContent.PLATINUM_INGOTS), of(Tags.Items.INGOTS_NETHERITE), new ItemStack(ItemContent.DURATIUM_INGOT.asItem()), 20, 3, "duratium");
        offerAlloyRecipe(exporter, of(Tags.Items.INGOTS_GOLD), of(Tags.Items.DUSTS_REDSTONE), new ItemStack(ItemContent.ELECTRUM_INGOT.asItem()), 10, 1, "electrum");
        offerAlloyRecipe(exporter, of(TagContent.NICKEL_INGOTS), of(ItemContent.FLUXITE.asItem()), new ItemStack(ItemContent.ENERGITE_INGOT.asItem()), 10, 2, "energite");
        offerAlloyRecipe(exporter, of(Tags.Items.INGOTS_IRON), of(TagContent.COAL_DUSTS), new ItemStack(ItemContent.STEEL_INGOT.asItem()), 5, 1, "steel");

        offerAlloyRecipe(exporter, of(ItemContent.COPPER_GEM), of(ItemContent.COPPER_GEM), new ItemStack(Items.COPPER_INGOT, 3), 5, 2, "coppergem");
        offerAlloyRecipe(exporter, of(ItemContent.IRON_GEM), of(ItemContent.IRON_GEM), new ItemStack(Items.COPPER_INGOT, 3), 5, 2, "irongem");
        offerAlloyRecipe(exporter, of(ItemContent.GOLD_GEM), of(ItemContent.GOLD_GEM), new ItemStack(Items.GOLD_INGOT, 3), 5, 2, "goldgem");
        offerAlloyRecipe(exporter, of(ItemContent.NICKEL_GEM), of(ItemContent.NICKEL_GEM), new ItemStack(ItemContent.NICKEL_INGOT), 5, 2, "platinumgem");
    }

    private static void offerAlloyRecipe(RecipeOutput exporter, Ingredient A, Ingredient B, ItemStack output, int fuelPerTick, int requiredTier, String suffix) {
        exporter.accept(Oritech.id("compat/alloysmelter/" + suffix), new SmeltingRecipe(NonNullList.of(Material.of(A, 1), Material.of(B, 1)), output, 200, fuelPerTick, requiredTier), null);
    }
}
