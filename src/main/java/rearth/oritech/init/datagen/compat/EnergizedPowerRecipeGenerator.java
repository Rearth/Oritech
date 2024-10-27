package rearth.oritech.init.datagen.compat;

import me.jddev0.ep.item.ModItems;
import me.jddev0.ep.recipe.AlloyFurnaceRecipe;
import me.jddev0.ep.recipe.AssemblingMachineRecipe;
import me.jddev0.ep.recipe.FiltrationPlantRecipe;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.datagen.RecipeGenerator;
import rearth.oritech.init.datagen.data.TagContent;

import java.util.List;

public class EnergizedPowerRecipeGenerator {

    public static void generateRecipes(RecipeExporter exporter) {
        addOritechAlloys(exporter);
        addEPPulverizerRecipes(exporter);
        addEPFragmentRecipes(exporter);
        addOritechAssemblerRecipes(exporter);
        // not adding EP assembling recipes to Oritech because EP uses multiple ingredients from each slot and Oritech only supports single ingredients
        addOritechOreFiltrationRecipes(exporter);
        // possible future TODO: add nickel plate/wire items and recipes (machine and hand tools) if Energized Power mod is loaded
    }

    public static void addOritechAlloys(RecipeExporter exporter) {
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.NICKEL_INGOTS), 1), new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(Items.DIAMOND), 1)}, new ItemStack(ItemContent.ADAMANT_INGOT), 800, "adamant");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(Items.IRON_INGOT), 1), new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER.asItem()), 1)}, new ItemStack(ItemContent.BIOSTEEL_INGOT.asItem()), 500, "biosteel");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.PLATINUM_INGOTS), 1), new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(Items.NETHERITE_INGOT), 1)}, new ItemStack(ItemContent.DURATIUM_INGOT), 1000, "duratium");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(Items.GOLD_INGOT), 1), new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(Items.REDSTONE), 1)}, new ItemStack(ItemContent.ELECTRUM_INGOT.asItem()), 500, "oritech_electrum");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.NICKEL_INGOTS), 1), new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.FLUXITE.asItem()), 1)}, new ItemStack(ItemContent.ENERGITE_INGOT.asItem()), 500, "energite");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.COPPER_GEM.asItem()), 1), new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.COPPER_GEM.asItem()), 1)}, new ItemStack(Items.COPPER_INGOT, 4), 800, "copper_gems");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.IRON_GEM.asItem()), 1), new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.IRON_GEM.asItem()), 1)}, new ItemStack(Items.IRON_INGOT, 4), 800, "iron_gems");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.NICKEL_GEM.asItem()), 1), new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.NICKEL_GEM.asItem()), 1)}, new ItemStack(ItemContent.NICKEL_INGOT, 4), 800, "nickel_gems");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.PLATINUM_GEM.asItem()), 1), new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.PLATINUM_GEM.asItem()), 1)}, new ItemStack(ItemContent.PLATINUM_INGOT, 4), 800, "platinum_gems");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.ofItems(Items.IRON_INGOT), 1),new AlloyFurnaceRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.COAL_DUSTS), 1)}, new ItemStack(ItemContent.STEEL_INGOT), 500, "steel_with_dust");
    }

    public static void addEPPulverizerRecipes(RecipeExporter exporter) {
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/tin"))), ModItems.RAW_TIN, 2, "compat/energizedpower/tin_ore");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/tin"))), ModItems.TIN_DUST, 2, "compat/energizedpower/raw_tin");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/tin"))), ModItems.TIN_DUST, "compat/energizedpower/tin_ingot");
    }

    public static void addEPFragmentRecipes(RecipeExporter exporter) {
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/tin"))), List.of(new ItemStack(ModItems.TIN_DUST, 4)), "compat/energizedpower/tin_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/tin"))), List.of(new ItemStack(ModItems.TIN_DUST, 2)), "compat/energizedpower/raw_tin");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/tin"))), List.of(new ItemStack(ModItems.RAW_TIN, 1)), "compat/energizedpower/tin_ingot");
    }

    public static void addOritechAssemblerRecipes(RecipeExporter exporter) {
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.HONEYCOMB), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.BIOMASS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.BIOMASS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.BIOMASS), 1)}, 
            new ItemStack(Items.SLIME_BALL), "slime");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.GUNPOWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.BLAZE_POWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(ItemTags.COALS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(ItemTags.COALS), 1)},
            new ItemStack(Items.FIRE_CHARGE), "fireball");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.BLAZE_POWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.BLAZE_POWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.BLAZE_POWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.BLAZE_POWDER), 1)},
            new ItemStack(Items.BLAZE_ROD), "blazerod");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.AMETHYST_SHARD), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.AMETHYST_SHARD), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), 1)},
            new ItemStack(Items.BUDDING_AMETHYST), "amethystbud");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.PAPER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.PAPER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.PAPER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.LEATHER), 1)},
            new ItemStack(Items.BOOK, 2), "book");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.BIOMASS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.BIOMASS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.BIOMASS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(ItemTags.PLANKS), 1)},
            new ItemStack(ItemContent.SOLID_BIOFUEL), "solidbiofuel");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.COPPER_INGOT), 1)},
            new ItemStack(ItemContent.INSULATED_WIRE, 12), "fwire");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.WIRES), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.WIRES), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.WIRES), 1)},
            new ItemStack(ItemContent.MAGNETIC_COIL, 2), "magnet");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.MAGNETIC_COIL), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.MAGNETIC_COIL), 1)},
            new ItemStack(ItemContent.MOTOR, 2), "motor");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.COPPER_INGOT), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.PLASTIC_SHEET), 1)},
            new ItemStack(BlockContent.MACHINE_PLATING_BLOCK.asItem(), 8), "plating");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.IRON_INGOT), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.PLASTIC_SHEET), 1)},
            new ItemStack(BlockContent.IRON_PLATING_BLOCK.asItem(), 8), "platingiron");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.PLASTIC_SHEET), 1)},
            new ItemStack(BlockContent.NICKEL_PLATING_BLOCK.asItem(), 8), "platingnickel");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.PLASTIC_SHEET), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1)},
            new ItemStack(ItemContent.BASIC_BATTERY), "battery");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.PLASTIC_SHEET), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.FLUXITE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.FLUXITE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.STEEL_INGOTS), 1)},
            new ItemStack(ItemContent.BASIC_BATTERY, 2), "batterybetter");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.PLASTIC_SHEET), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.CARBON_FIBRE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(Items.REDSTONE), 1)},
            new ItemStack(ItemContent.PROCESSING_UNIT), "processingunit");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.ADAMANT_INGOT), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.CARBON_FIBRE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), 1)},
            new ItemStack(ItemContent.ENDERIC_LENS), "enderlens");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.PROCESSING_UNIT), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.FLUXITE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.FLUXITE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.PLATINUM_INGOTS), 1)},
            new ItemStack(ItemContent.FLUX_GATE), "fluxgate");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.FLUX_GATE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.fromTag(TagContent.WIRES), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.DUBIOS_CONTAINER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(Ingredient.ofItems(ItemContent.ENERGITE_INGOT), 1)},
            new ItemStack(BlockContent.SUPERCONDUCTOR.asItem()), "superconductor");
    }

    public static void addOritechOreFiltrationRecipes(RecipeExporter exporter) {
        offerEPOreFiltrationRecipe(exporter, new FiltrationPlantRecipe.OutputItemStackWithPercentages(new ItemStack(ModItems.STONE_PEBBLE), new double[]{0.33}), new FiltrationPlantRecipe.OutputItemStackWithPercentages(new ItemStack(ItemContent.RAW_NICKEL), new double[]{0.05}), "nickel");
        offerEPOreFiltrationRecipe(exporter, new FiltrationPlantRecipe.OutputItemStackWithPercentages(new ItemStack(ModItems.STONE_PEBBLE), new double[]{0.33}), new FiltrationPlantRecipe.OutputItemStackWithPercentages(new ItemStack(ItemContent.RAW_PLATINUM), new double[]{0.005}), "platinum");
    }

    private static void offerEPAlloyFurnaceRecipe(RecipeExporter exporter, AlloyFurnaceRecipe.IngredientWithCount[] inputs, ItemStack output, int ticks, String suffix) {
        // Items.EMPTY would be better, but exporter is rejecting that. 0% chance of dropping iron ingot should be fine.
        var secondary = new AlloyFurnaceRecipe.OutputItemStackWithPercentages(new ItemStack(Items.IRON_INGOT), new double[0]);
        var recipe = new AlloyFurnaceRecipe(output, secondary, inputs, ticks);
        exporter.accept(Oritech.id("compat/energizedpower/alloyfurance/" + suffix), recipe, null);
    }

    private static void offerEPAssemblingMachineRecipe(RecipeExporter exporter, AssemblingMachineRecipe.IngredientWithCount[] inputs, ItemStack output, String suffix) {
        var recipe = new AssemblingMachineRecipe(output, inputs);
        exporter.accept(Oritech.id("compat/energizedpower/assemblingmachine/" + suffix), recipe, null);
    }

    private static void offerEPOreFiltrationRecipe(RecipeExporter exporter, FiltrationPlantRecipe.OutputItemStackWithPercentages output, FiltrationPlantRecipe.OutputItemStackWithPercentages secondaryOutput, String suffix) {
        var recipe = new FiltrationPlantRecipe(output, secondaryOutput, Registries.ITEM.getId(output.output().getItem()));
        exporter.accept(Oritech.id("compat/energizedpower/filtrationplant/" + suffix), recipe, null);
    }
    
}
