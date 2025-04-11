package rearth.oritech.fabricgen.datagen.compat;

import me.jddev0.ep.item.ModItems;
import me.jddev0.ep.recipe.AlloyFurnaceRecipe;
import me.jddev0.ep.recipe.AssemblingMachineRecipe;
import me.jddev0.ep.recipe.FiltrationPlantRecipe;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.datagen.OreTransform;
import rearth.oritech.util.datagen.RecipeGeneratorUtil;

import static rearth.oritech.util.datagen.RecipeGeneratorUtil.of;

public class EnergizedPowerRecipeGenerator {

    public static void generateRecipes(RecipeExporter exporter) {
        addOritechAlloys(exporter);
        addEPMetalProcessingRecipes(exporter);
        addOritechAssemblerRecipes(exporter);
        // not adding EP assembling recipes to Oritech because EP uses multiple ingredients from each slot and Oritech only supports single ingredients
        addOritechOreFiltrationRecipes(exporter);
    }

    public static void addOritechAlloys(RecipeExporter exporter) {
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(ConventionalItemTags.DIAMOND_GEMS), 1)},
            new ItemStack(ItemContent.ADAMANT_INGOT), 800, "adamant");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(ConventionalItemTags.IRON_INGOTS), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.RAW_BIOPOLYMER.asItem()), 1)},
            new ItemStack(ItemContent.BIOSTEEL_INGOT.asItem()), 500, "biosteel");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(TagContent.PLATINUM_INGOTS), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(ConventionalItemTags.NETHERITE_INGOTS), 1)},
            new ItemStack(ItemContent.DURATIUM_INGOT), 1000, "duratium");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(ConventionalItemTags.GOLD_INGOTS), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(ConventionalItemTags.REDSTONE_DUSTS), 1)},
            new ItemStack(ItemContent.ELECTRUM_INGOT.asItem()), 500, "oritech_electrum");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.FLUXITE.asItem()), 1)},
            new ItemStack(ItemContent.ENERGITE_INGOT.asItem()), 500, "energite");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.COPPER_GEM.asItem()), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.COPPER_GEM.asItem()), 1)},
            new ItemStack(Items.COPPER_INGOT, 2), 800, "copper_gems");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.IRON_GEM.asItem()), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.IRON_GEM.asItem()), 1)},
            new ItemStack(Items.IRON_INGOT, 2), 800, "iron_gems");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.NICKEL_GEM.asItem()), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.NICKEL_GEM.asItem()), 1)},
            new ItemStack(ItemContent.NICKEL_INGOT, 2), 800, "nickel_gems");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.PLATINUM_GEM.asItem()), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(ItemContent.PLATINUM_GEM.asItem()), 1)},
            new ItemStack(ItemContent.PLATINUM_INGOT, 2), 800, "platinum_gems");
        offerEPAlloyFurnaceRecipe(exporter, new AlloyFurnaceRecipe.IngredientWithCount[]{
                new AlloyFurnaceRecipe.IngredientWithCount(of(ConventionalItemTags.IRON_INGOTS), 1),
                new AlloyFurnaceRecipe.IngredientWithCount(of(TagContent.COAL_DUSTS), 1)},
            new ItemStack(ItemContent.STEEL_INGOT), 500, "steel_with_dust");
    }

    public static void addEPMetalProcessingRecipes(RecipeExporter exporter) {
        var tin = new OreTransform(
            of(TagContent.TIN_ORES),
            of(TagContent.TIN_RAW_MATERIALS), ModItems.RAW_TIN, Items.RAW_COPPER,
            null, null,
            null, null, null,
            of(TagContent.TIN_DUSTS), ModItems.TIN_DUST,
            null, null, ModItems.TIN_NUGGET,
            // Adding dust as "gemItem" to give dust as an output from the centrifuge recipes
            null, ModItems.TIN_DUST,
            null,
            of(TagContent.TIN_NUGGETS), ModItems.TIN_NUGGET,
            of(TagContent.TIN_INGOTS), ModItems.TIN_INGOT,
            1.5f, "compat/energizedpower/tin", 2, false);
        RecipeGeneratorUtil.addMetalProcessingChain(exporter, tin);
    }

    public static void addOritechAssemblerRecipes(RecipeExporter exporter) {
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.HONEYCOMB), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.BIOFUEL), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.BIOFUEL), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.BIOFUEL), 1)}, 
            new ItemStack(Items.SLIME_BALL), "slime");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.GUNPOWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.BLAZE_POWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemTags.COALS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemTags.COALS), 1)},
            new ItemStack(Items.FIRE_CHARGE), "fireball");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.BLAZE_POWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.BLAZE_POWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.BLAZE_POWDER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.BLAZE_POWDER), 1)},
            new ItemStack(Items.BLAZE_ROD), "blazerod");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(ConventionalItemTags.AMETHYST_GEMS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ConventionalItemTags.AMETHYST_GEMS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.ENDERIC_COMPOUND), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.OVERCHARGED_CRYSTAL), 1)},
            new ItemStack(Items.BUDDING_AMETHYST), "amethystbud");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.PAPER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.PAPER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.PAPER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ConventionalItemTags.LEATHERS), 1)},
            new ItemStack(Items.BOOK, 2), "book");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.BIOFUEL), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.BIOFUEL), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.BIOFUEL), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemTags.PLANKS), 1)},
            new ItemStack(ItemContent.SOLID_BIOFUEL), "solidbiofuel");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(Items.COPPER_INGOT), 1)},
            new ItemStack(ItemContent.INSULATED_WIRE, 12), "fwire");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.WIRES), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.WIRES), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.WIRES), 1)},
            new ItemStack(ItemContent.MAGNETIC_COIL, 2), "magnet");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.MAGNETIC_COIL), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.MAGNETIC_COIL), 1)},
            new ItemStack(ItemContent.MOTOR, 2), "motor");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ConventionalItemTags.COPPER_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1)},
            new ItemStack(BlockContent.MACHINE_PLATING_BLOCK.asItem(), 8), "plating");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ConventionalItemTags.IRON_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1)},
            new ItemStack(BlockContent.IRON_PLATING_BLOCK.asItem(), 8), "platingiron");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1)},
            new ItemStack(BlockContent.NICKEL_PLATING_BLOCK.asItem(), 8), "platingnickel");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.ELECTRUM_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.ELECTRUM_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1)},
            new ItemStack(ItemContent.BASIC_BATTERY), "battery");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.FLUXITE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.FLUXITE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.STEEL_INGOTS), 1)},
            new ItemStack(ItemContent.BASIC_BATTERY, 2), "batterybetter");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.CARBON_FIBRE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.ELECTRUM_INGOTS), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ConventionalItemTags.REDSTONE_DUSTS), 1)},
            new ItemStack(ItemContent.PROCESSING_UNIT), "processingunit");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.ADAMANT_INGOT), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.CARBON_FIBRE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.ENDERIC_COMPOUND), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.ENDERIC_COMPOUND), 1)},
            new ItemStack(ItemContent.ENDERIC_LENS), "enderlens");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.PROCESSING_UNIT), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.FLUXITE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.FLUXITE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.PLATINUM_INGOTS), 1)},
            new ItemStack(ItemContent.FLUX_GATE), "fluxgate");
        offerEPAssemblingMachineRecipe(exporter,
            new AssemblingMachineRecipe.IngredientWithCount[]{
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.FLUX_GATE), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(TagContent.WIRES), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.DUBIOS_CONTAINER), 1),
                new AssemblingMachineRecipe.IngredientWithCount(of(ItemContent.ENERGITE_INGOT), 1)},
            new ItemStack(BlockContent.SUPERCONDUCTOR.asItem()), "superconductor");
    }

    public static void addOritechOreFiltrationRecipes(RecipeExporter exporter) {
        offerEPOreFiltrationRecipe(exporter,
            new FiltrationPlantRecipe.OutputItemStackWithPercentages(new ItemStack(ModItems.STONE_PEBBLE), new double[]{0.33}),
            new FiltrationPlantRecipe.OutputItemStackWithPercentages(new ItemStack(ItemContent.RAW_NICKEL), new double[]{0.05}), "nickel");
        offerEPOreFiltrationRecipe(exporter,
            new FiltrationPlantRecipe.OutputItemStackWithPercentages(new ItemStack(ModItems.STONE_PEBBLE), new double[]{0.33}),
            new FiltrationPlantRecipe.OutputItemStackWithPercentages(new ItemStack(ItemContent.RAW_PLATINUM), new double[]{0.005}), "platinum");
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
