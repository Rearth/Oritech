package rearth.oritech.fabricgen.datagen.compat;

import me.jddev0.ep.item.EPItems;
import me.jddev0.ep.recipe.AlloyFurnaceRecipe;
import me.jddev0.ep.recipe.AssemblingMachineRecipe;
import me.jddev0.ep.recipe.FiltrationPlantRecipe;
import me.jddev0.ep.recipe.IngredientWithCount;
import me.jddev0.ep.recipe.OutputItemStackWithPercentages;
import me.jddev0.ep.registry.tags.CommonItemTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.util.MetalProcessingChainBuilder;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;
import static rearth.oritech.util.TagUtils.cItemTag;

public class EnergizedPowerRecipeGenerator {
    private static final String PATH = "compat/energizedpower/";

    public static void generateRecipes(RecipeExporter exporter) {
        addOritechAlloys(exporter);
        addEPMetalProcessingRecipes(exporter);
        addOritechAssemblerRecipes(exporter);
        // not adding EP assembling recipes to Oritech because EP uses multiple ingredients from each slot and Oritech only supports single ingredients
        addOritechOreFiltrationRecipes(exporter);
    }

    public static void addOritechAlloys(RecipeExporter exporter) {
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new IngredientWithCount(of(ConventionalItemTags.DIAMOND_GEMS), 1)},
            new ItemStack(ItemContent.ADAMANT_INGOT), 800, "adamant");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(ConventionalItemTags.IRON_INGOTS), 1),
                new IngredientWithCount(of(ItemContent.RAW_BIOPOLYMER.asItem()), 1)},
            new ItemStack(ItemContent.BIOSTEEL_INGOT.asItem()), 500, "biosteel");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.PLATINUM_INGOTS), 1),
                new IngredientWithCount(of(ConventionalItemTags.NETHERITE_INGOTS), 1)},
            new ItemStack(ItemContent.DURATIUM_INGOT), 1000, "duratium");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(ConventionalItemTags.GOLD_INGOTS), 1),
                new IngredientWithCount(of(ConventionalItemTags.REDSTONE_DUSTS), 1)},
            new ItemStack(ItemContent.ELECTRUM_INGOT.asItem()), 500, "oritech_electrum");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new IngredientWithCount(of(ItemContent.FLUXITE.asItem()), 1)},
            new ItemStack(ItemContent.ENERGITE_INGOT.asItem()), 500, "energite");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(ItemContent.COPPER_GEM.asItem()), 1),
                new IngredientWithCount(of(ItemContent.COPPER_GEM.asItem()), 1)},
            new ItemStack(Items.COPPER_INGOT, 3), 800, "copper_gems");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(ItemContent.IRON_GEM.asItem()), 1),
                new IngredientWithCount(of(ItemContent.IRON_GEM.asItem()), 1)},
            new ItemStack(Items.IRON_INGOT, 3), 800, "iron_gems");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(ItemContent.NICKEL_GEM.asItem()), 1),
                new IngredientWithCount(of(ItemContent.NICKEL_GEM.asItem()), 1)},
            new ItemStack(ItemContent.NICKEL_INGOT, 3), 800, "nickel_gems");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(ItemContent.PLATINUM_GEM.asItem()), 1),
                new IngredientWithCount(of(ItemContent.PLATINUM_GEM.asItem()), 1)},
            new ItemStack(ItemContent.PLATINUM_INGOT, 3), 800, "platinum_gems");
        offerEPAlloyFurnaceRecipe(exporter, new IngredientWithCount[]{
                new IngredientWithCount(of(ConventionalItemTags.IRON_INGOTS), 1),
                new IngredientWithCount(of(TagContent.COAL_DUSTS), 1)},
            new ItemStack(ItemContent.STEEL_INGOT), 500, "steel_with_dust");
    }

    public static void addEPMetalProcessingRecipes(RecipeExporter exporter) {
        MetalProcessingChainBuilder.build("tin").resourcePath(PATH)
            .ore(CommonItemTags.ORES_TIN)
            .rawOre(CommonItemTags.RAW_MATERIALS_TIN, EPItems.RAW_TIN).rawOreByproduct(Items.RAW_GOLD)
            .ingot(CommonItemTags.INGOTS_TIN, EPItems.TIN_INGOT)
            .nugget(CommonItemTags.NUGGETS_TIN, EPItems.TIN_NUGGET)
            .dust(EPItems.TIN_DUST).dustByproduct(ItemContent.COPPER_NUGGET)
            .export(exporter);
    }

    public static void addOritechAssemblerRecipes(RecipeExporter exporter) {
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(Items.HONEYCOMB), 1),
                new IngredientWithCount(of(TagContent.BIOMASS), 1),
                new IngredientWithCount(of(TagContent.BIOMASS), 1),
                new IngredientWithCount(of(TagContent.BIOMASS), 1)}, 
            new ItemStack(Items.SLIME_BALL), "slime");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(Items.GUNPOWDER), 1),
                new IngredientWithCount(of(Items.BLAZE_POWDER), 1),
                new IngredientWithCount(of(ItemTags.COALS), 1),
                new IngredientWithCount(of(ItemTags.COALS), 1)},
            new ItemStack(Items.FIRE_CHARGE), "fireball");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(Items.BLAZE_POWDER), 1),
                new IngredientWithCount(of(Items.BLAZE_POWDER), 1),
                new IngredientWithCount(of(Items.BLAZE_POWDER), 1),
                new IngredientWithCount(of(Items.BLAZE_POWDER), 1)},
            new ItemStack(Items.BLAZE_ROD), "blazerod");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(ConventionalItemTags.AMETHYST_GEMS), 1),
                new IngredientWithCount(of(ConventionalItemTags.AMETHYST_GEMS), 1),
                new IngredientWithCount(of(ItemContent.ENDERIC_COMPOUND), 1),
                new IngredientWithCount(of(ItemContent.OVERCHARGED_CRYSTAL), 1)},
            new ItemStack(Items.BUDDING_AMETHYST), "amethystbud");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(Items.PAPER), 1),
                new IngredientWithCount(of(Items.PAPER), 1),
                new IngredientWithCount(of(Items.PAPER), 1),
                new IngredientWithCount(of(ConventionalItemTags.LEATHERS), 1)},
            new ItemStack(Items.BOOK, 2), "book");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.BIOMASS), 1),
                new IngredientWithCount(of(TagContent.BIOMASS), 1),
                new IngredientWithCount(of(TagContent.BIOMASS), 1),
                new IngredientWithCount(of(ItemTags.PLANKS), 1)},
            new ItemStack(ItemContent.SOLID_BIOFUEL), "solidbiofuel");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new IngredientWithCount(of(cItemTag("ingots/copper")), 1)},
            new ItemStack(ItemContent.MAGNETIC_COIL, 2), "magnet");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new IngredientWithCount(of(ItemContent.MAGNETIC_COIL), 1),
                new IngredientWithCount(of(ItemContent.MAGNETIC_COIL), 1)},
            new ItemStack(ItemContent.MOTOR, 2), "motor");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new IngredientWithCount(of(ConventionalItemTags.COPPER_INGOTS), 1),
                new IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1)},
            new ItemStack(BlockContent.MACHINE_PLATING_BLOCK.asItem(), 8), "plating");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new IngredientWithCount(of(ConventionalItemTags.IRON_INGOTS), 1),
                new IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1)},
            new ItemStack(BlockContent.IRON_PLATING_BLOCK.asItem(), 8), "platingiron");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1),
                new IngredientWithCount(of(TagContent.NICKEL_INGOTS), 1),
                new IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1)},
            new ItemStack(BlockContent.NICKEL_PLATING_BLOCK.asItem(), 8), "platingnickel");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1),
                new IngredientWithCount(of(TagContent.ELECTRUM_INGOTS), 1),
                new IngredientWithCount(of(TagContent.ELECTRUM_INGOTS), 1),
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1)},
            new ItemStack(ItemContent.BASIC_BATTERY), "battery");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1),
                new IngredientWithCount(of(ItemContent.FLUXITE), 1),
                new IngredientWithCount(of(ItemContent.FLUXITE), 1),
                new IngredientWithCount(of(TagContent.STEEL_INGOTS), 1)},
            new ItemStack(ItemContent.BASIC_BATTERY, 2), "batterybetter");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(TagContent.PLASTIC_PLATES), 1),
                new IngredientWithCount(of(TagContent.CARBON_FIBRE), 1),
                new IngredientWithCount(of(TagContent.ELECTRUM_INGOTS), 1),
                new IngredientWithCount(of(ConventionalItemTags.REDSTONE_DUSTS), 1)},
            new ItemStack(ItemContent.PROCESSING_UNIT), "processingunit");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(ItemContent.ADAMANT_INGOT), 1),
                new IngredientWithCount(of(TagContent.CARBON_FIBRE), 1),
                new IngredientWithCount(of(ItemContent.ENDERIC_COMPOUND), 1),
                new IngredientWithCount(of(ItemContent.ENDERIC_COMPOUND), 1)},
            new ItemStack(ItemContent.ENDERIC_LENS), "enderlens");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(ItemContent.PROCESSING_UNIT), 1),
                new IngredientWithCount(of(ItemContent.FLUXITE), 1),
                new IngredientWithCount(of(ItemContent.FLUXITE), 1),
                new IngredientWithCount(of(TagContent.PLATINUM_INGOTS), 1)},
            new ItemStack(ItemContent.FLUX_GATE), "fluxgate");
        offerEPAssemblingMachineRecipe(exporter,
            new IngredientWithCount[]{
                new IngredientWithCount(of(ItemContent.FLUX_GATE), 1),
                new IngredientWithCount(of(TagContent.ELECTRUM_INGOTS), 1),
                new IngredientWithCount(of(ItemContent.DUBIOS_CONTAINER), 1),
                new IngredientWithCount(of(ItemContent.ENERGITE_INGOT), 1)},
            new ItemStack(BlockContent.SUPERCONDUCTOR.asItem()), "superconductor");
    }

    public static void addOritechOreFiltrationRecipes(RecipeExporter exporter) {
        offerEPOreFiltrationRecipe(exporter,
            new OutputItemStackWithPercentages(new ItemStack(EPItems.STONE_PEBBLE), new double[]{0.33}),
            new OutputItemStackWithPercentages(new ItemStack(ItemContent.RAW_NICKEL), new double[]{0.05}), "nickel");
        offerEPOreFiltrationRecipe(exporter,
            new OutputItemStackWithPercentages(new ItemStack(EPItems.STONE_PEBBLE), new double[]{0.33}),
            new OutputItemStackWithPercentages(new ItemStack(ItemContent.RAW_PLATINUM), new double[]{0.005}), "platinum");
    }

    private static void offerEPAlloyFurnaceRecipe(RecipeExporter exporter, IngredientWithCount[] inputs, ItemStack output, int ticks, String suffix) {
        // Items.EMPTY would be better, but exporter is rejecting that. 0% chance of dropping iron ingot should be fine.
        var secondary = new OutputItemStackWithPercentages(new ItemStack(Items.IRON_INGOT), new double[0]);
        var recipe = new AlloyFurnaceRecipe(output, secondary, inputs, ticks);
        exporter.accept(Oritech.id(PATH + "/alloyfurnace/" + suffix), recipe, null);
    }

    private static void offerEPAssemblingMachineRecipe(RecipeExporter exporter, IngredientWithCount[] inputs, ItemStack output, String suffix) {
        var recipe = new AssemblingMachineRecipe(output, inputs);
        exporter.accept(Oritech.id(PATH + "/assemblingmachine/" + suffix), recipe, null);
    }

    private static void offerEPOreFiltrationRecipe(RecipeExporter exporter, OutputItemStackWithPercentages output, OutputItemStackWithPercentages secondaryOutput, String suffix) {
        var recipe = new FiltrationPlantRecipe(output, secondaryOutput, Registries.ITEM.getId(output.output().getItem()));
        exporter.accept(Oritech.id(PATH + "/filtrationplant/" + suffix), recipe, null);
    }   
}
