package rearth.oritech.init.datagen.compat;

import java.util.List;

import me.jddev0.ep.item.ModItems;
import me.jddev0.ep.recipe.AlloyFurnaceRecipe;
import me.jddev0.ep.recipe.AlloyFurnaceRecipe.IngredientWithCount;
import me.jddev0.ep.recipe.AlloyFurnaceRecipe.OutputItemStackWithPercentages;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.datagen.RecipeGenerator;
import rearth.oritech.init.datagen.data.TagContent;

public class EnergizedPowerRecipeGenerator {

    public static void generateRecipes(RecipeExporter exporter) {
        addOritechAlloys(exporter);
        addEnergizedPowerPulverizerRecipes(exporter);
        addEnergizedPowerFragmentRecipes(exporter);
    }

    public static void addOritechAlloys(RecipeExporter exporter) {
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{new IngredientWithCount(Ingredient.fromTag(TagContent.NICKEL_INGOTS), 1), new IngredientWithCount(Ingredient.ofItems(Items.DIAMOND), 1)}, new ItemStack(ItemContent.ADAMANT_INGOT), 800, "adamant");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{new IngredientWithCount(Ingredient.ofItems(Items.IRON_INGOT), 1), new IngredientWithCount(Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER.asItem()), 1)}, new ItemStack(ItemContent.BIOSTEEL_INGOT.asItem()), 500, "biosteel");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{new IngredientWithCount(Ingredient.fromTag(TagContent.PLATINUM_INGOTS), 1), new IngredientWithCount(Ingredient.ofItems(Items.NETHERITE_INGOT), 1)}, new ItemStack(ItemContent.DURATIUM_INGOT), 1000, "duratium");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{new IngredientWithCount(Ingredient.ofItems(Items.GOLD_INGOT), 1), new IngredientWithCount(Ingredient.ofItems(Items.REDSTONE), 1)}, new ItemStack(ItemContent.ELECTRUM_INGOT.asItem()), 500, "oritech_electrum");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{new IngredientWithCount(Ingredient.fromTag(TagContent.NICKEL_INGOTS), 1), new IngredientWithCount(Ingredient.ofItems(ItemContent.FLUXITE.asItem()), 1)}, new ItemStack(ItemContent.ENERGITE_INGOT.asItem()), 500, "energite");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{new IngredientWithCount(Ingredient.ofItems(ItemContent.COPPER_GEM.asItem()), 1), new IngredientWithCount(Ingredient.ofItems(ItemContent.COPPER_GEM.asItem()), 1)}, new ItemStack(Items.COPPER_INGOT, 4), 800, "copper_gems");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{new IngredientWithCount(Ingredient.ofItems(ItemContent.IRON_GEM.asItem()), 1), new IngredientWithCount(Ingredient.ofItems(ItemContent.IRON_GEM.asItem()), 1)}, new ItemStack(Items.IRON_INGOT, 4), 800, "iron_gems");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{new IngredientWithCount(Ingredient.ofItems(ItemContent.NICKEL_GEM.asItem()), 1), new IngredientWithCount(Ingredient.ofItems(ItemContent.NICKEL_GEM.asItem()), 1)}, new ItemStack(ItemContent.NICKEL_INGOT, 4), 800, "nickel_gems");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{new IngredientWithCount(Ingredient.ofItems(ItemContent.PLATINUM_GEM.asItem()), 1), new IngredientWithCount(Ingredient.ofItems(ItemContent.PLATINUM_GEM.asItem()), 1)}, new ItemStack(ItemContent.PLATINUM_INGOT, 4), 800, "platinum_gems");
    }

    public static void addEnergizedPowerPulverizerRecipes(RecipeExporter exporter) {
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/tin"))), ModItems.RAW_TIN, 2, "compat/energizedpower/tin_ore");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/tin"))), ModItems.TIN_DUST, 2, "compat/energizedpower/raw_tin");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/tin"))), ModItems.TIN_DUST, "compat/energizedpower/tin_ingot");
    }

    public static void addEnergizedPowerFragmentRecipes(RecipeExporter exporter) {
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/tin"))), List.of(new ItemStack(ModItems.TIN_DUST, 4)), "compat/energizedpower/tin_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/tin"))), List.of(new ItemStack(ModItems.TIN_DUST, 2)), "compat/energizedpower/raw_tin");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/tin"))), List.of(new ItemStack(ModItems.RAW_TIN, 1)), "compat/energizedpower/tin_ingot");
    }

    private static void offerEPAlloyFurnaceRecipe(RecipeExporter exporter, IngredientWithCount[] inputs, ItemStack output, int ticks, String suffix) {
        var secondary = new OutputItemStackWithPercentages(new ItemStack(Items.DIAMOND_BLOCK), new double[0]);
        var recipe = new AlloyFurnaceRecipe(output, secondary, inputs, ticks);
        exporter.accept(Oritech.id("compat/energizedpower/" + suffix), recipe, null);
    }
    
}
