package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.block.Blocks;
import net.minecraft.data.server.recipe.CraftingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.datagen.data.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class RecipeGenerator extends FabricRecipeProvider {
    
    public RecipeGenerator(FabricDataOutput output) {
        super(output);
    }
    
    @Override
    public void generate(RecipeExporter exporter) {
        
        // basic generator
        offerGeneratorRecipe(exporter, BlockContent.BASIC_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(Blocks.FURNACE.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_basicgen");
        // pulverizer
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), Ingredient.ofItems(Blocks.IRON_BLOCK.asItem()), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_pulverizer");
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_pulverizeralt");
        // electric furnace
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), Ingredient.ofItems(Blocks.FURNACE.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), "_electricfurnace");
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), Ingredient.ofItems(Blocks.FURNACE.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.PLATINUM_INGOTS), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), "_electricfurnacealt");
        // assembler
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), Ingredient.ofItems(Blocks.BLAST_FURNACE.asItem()), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), "_assembler");
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), "_assembleralt");
        // foundry
        offerGeneratorRecipe(exporter, BlockContent.FOUNDRY_BLOCK.asItem(), Ingredient.ofItems(Blocks.CAULDRON.asItem()), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), "_foundry");
        // centrifuge
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(ItemContent.STEEL_INGOT), Ingredient.ofItems(Items.GLASS_BOTTLE), "_centrifuge");
        // laser arm
        offerAtomicForgeRecipe(exporter, BlockContent.LASER_ARM_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.ENDERIC_LENS), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), "_atomicforge");
        // crusher
        offerGeneratorRecipe(exporter, BlockContent.FRAGMENT_FORGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "_crusher");
        // atomic forge
        offerAtomicForgeRecipe(exporter, BlockContent.ATOMIC_FORGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.STRANGE_MATTER), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(ItemContent.FLUX_GATE), "_atomicforge");
        
        // biofuel generator
        offerGeneratorRecipe(exporter, BlockContent.BIO_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT), "_biogen");
        // lava generator
        offerGeneratorRecipe(exporter, BlockContent.LAVA_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), "_lavagen");
        // diesel generator
        offerGeneratorRecipe(exporter, BlockContent.FUEL_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK), Ingredient.ofItems(ItemContent.ENDERIC_LENS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_fuelgen");
        // large solar
        offerGeneratorRecipe(exporter, BlockContent.BIG_SOLAR_PANEL_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), Ingredient.ofItems(ItemContent.FLUXITE), "_solar");
        
        // small storage
        offerAtomicForgeRecipe(exporter, BlockContent.SMALL_STORAGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.BASIC_BATTERY), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.INSULATED_WIRE), "_smallstorage");
        // large storage
        offerAtomicForgeRecipe(exporter, BlockContent.LARGE_STORAGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), Ingredient.ofItems(ItemContent.STEEL_INGOT), Ingredient.ofItems(ItemContent.DUBIOS_CONTAINER), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.INSULATED_WIRE), "_bigstorage");
        
        // pump
        offerGeneratorRecipe(exporter, BlockContent.PUMP_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), "_pump");
        // block placer
        offerFurnaceRecipe(exporter, BlockContent.PLACER_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(BlockContent.MACHINE_FRAME_BLOCK.asItem()), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), "_placer");
        // block destroyer
        offerAtomicForgeRecipe(exporter, BlockContent.DESTROYER_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), Ingredient.ofItems(ItemContent.ENDERIC_LENS), Ingredient.ofItems(ItemContent.FLUX_GATE), "_destroyer");
        // fertilizer
        offerFurnaceRecipe(exporter, BlockContent.FERTILIZER_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), "_fertilizer");
        
        // machine frame
        offerMachineFrameRecipe(exporter, BlockContent.MACHINE_FRAME_BLOCK.asItem(), Ingredient.ofItems(Items.IRON_BARS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), 16, "_frame");
        // energy pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE.asItem(), 6), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.INSULATED_WIRE), "_energy");
        // item pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE.asItem(), 6), Ingredient.ofItems(ItemContent.NICKEL_INGOT), Ingredient.fromTag(ItemTags.PLANKS), "_item");
        // item filter
        offerGeneratorRecipe(exporter, BlockContent.ITEM_FILTER_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.INSULATED_WIRE), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.INSULATED_WIRE), "_itemfilter");
        // fluid pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE.asItem(), 6), Ingredient.ofItems(ItemContent.SILICON), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), "_fluid");
        
        // deep drill
        offerAtomicForgeRecipe(exporter, BlockContent.DEEP_DRILL_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.HEISENBERG_COMPENSATOR), Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), "_deepdrill");
        // drone port
        offerAtomicForgeRecipe(exporter, BlockContent.DRONE_PORT_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.SUPERCONDUCTOR), Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), "_droneport");
        
        // addons
        
        // cores
        
        addComponents(exporter);
        addOreChains(exporter);
        addAlloys(exporter);
        addDusts(exporter);
        
    }
    
    private void addComponents(RecipeExporter exporter) {
        // coal stuff (including basic steel)
        addCentrifugeRecipe(exporter, Ingredient.fromTag(TagContent.COAL_DUSTS), ItemContent.CARBON_FIBRE_STRANDS, 1f, "_carbon");
        offerManualAlloyRecipe(exporter, ItemContent.STEEL_INGOT, Ingredient.ofItems(Items.IRON_INGOT), Ingredient.ofItems(Items.COAL), "manualsteel");
        
        // manual alloys
        offerManualAlloyRecipe(exporter, ItemContent.ELECTRUM_INGOT, Ingredient.ofItems(Items.GOLD_INGOT), Ingredient.ofItems(Items.REDSTONE), "manualelectrum");
        offerManualAlloyRecipe(exporter, ItemContent.ADAMANT_INGOT, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(Items.DIAMOND), "manualadamant");
        
        // enderic entry
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.ENDER_PEARL), ItemContent.ENDERIC_COMPOUND, 8, "_pearl_enderic");
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.ENDER_PEARL), ItemContent.ENDERIC_COMPOUND, 12, "_pearl_enderic");
        addGrinderRecipe(exporter, Ingredient.ofItems(Blocks.END_STONE), ItemContent.ENDERIC_COMPOUND, 1, "_stone_enderic");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.ELECTRUM_DUST), ItemContent.STRANGE_MATTER, 2f, "_strange");
        
        // fine wires
        offerCableRecipe(exporter, new ItemStack(ItemContent.FINE_WIRE, 4), Ingredient.fromTag(TagContent.NICKEL_INGOTS));
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), ItemContent.FINE_WIRE, 8, 1f, "_fwire");
        
        // insulated fine wires
        offerInsulatedCableRecipe(exporter, new ItemStack(ItemContent.INSULATED_WIRE, 4), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.SILICON), "_base");
        offerInsulatedCableRecipe(exporter, new ItemStack(ItemContent.INSULATED_WIRE, 3), Ingredient.ofItems(ItemContent.FINE_WIRE), Ingredient.ofItems(ItemContent.SILICON), "_upgrade");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.FINE_WIRE), Ingredient.ofItems(ItemContent.FINE_WIRE), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.SILICON), ItemContent.INSULATED_WIRE, 2, 0.5f, "_iwire");
        
        // magnetic coils
        offerInsulatedCableRecipe(exporter, new ItemStack(ItemContent.MAGNETIC_COIL, 2), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.FINE_WIRE), "magnet");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.FINE_WIRE), Ingredient.ofItems(ItemContent.FINE_WIRE), Ingredient.ofItems(ItemContent.FINE_WIRE), ItemContent.MAGNETIC_COIL, 1, 1f, "magnet");
        
        // motor
        offerMotorRecipe(exporter, ItemContent.MOTOR, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_motorcraft");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), ItemContent.MOTOR, 1, 1f, "motor");
        
        // machine plating
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), ItemContent.MACHINE_PLATING, 2, 1f, "plating");
        
        // basic battery
        offerMotorRecipe(exporter, ItemContent.BASIC_BATTERY, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "manualbattery");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.fromTag(TagContent.STEEL_INGOTS), ItemContent.BASIC_BATTERY, 1, 1f, "battery");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.fromTag(TagContent.STEEL_INGOTS), ItemContent.BASIC_BATTERY, 2, 1f, "batterybetter");
        
        // silicon
        offerManualAlloyRecipe(exporter, ItemContent.RAW_SILICON, Ingredient.fromTag(TagContent.QUARTZ_DUSTS), Ingredient.fromTag(ItemTags.SAND), 2, "manualrawsilicon");
        offerSmelting(exporter, List.of(ItemContent.RAW_SILICON), RecipeCategory.MISC, ItemContent.SILICON, 0.5f, 30, "siliconfurnace");
        
        // plastic
        offerCompactingRecipe(exporter, RecipeCategory.MISC, ItemContent.PACKED_WHEAT, Items.WHEAT);
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.PACKED_WHEAT), ItemContent.RAW_BIOPOLYMER, Fluids.WATER, 0.25f, null, 0, 1f, "_biopolymer");
        addCentrifugeFluidRecipe(exporter, Ingredient.fromTag(ItemTags.SAND), ItemContent.POLYMER_RESIN, FluidContent.STILL_OIL, 0.1f, null, 0, 1f, "_polymerresin");
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER), ItemContent.PLASTIC_SHEET, Fluids.WATER, 0.5f, null, 0, 1f, "_plasticoil");
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.POLYMER_RESIN), ItemContent.PLASTIC_SHEET, Fluids.WATER, 0.5f, null, 0, 1f, "_plasticbio");
        
        // processing unit
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(Items.REDSTONE), ItemContent.PROCESSING_UNIT, 1f, "_processingunit");
        // enderic lens
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), Ingredient.ofItems(ItemContent.STRANGE_MATTER), Ingredient.ofItems(ItemContent.STRANGE_MATTER), ItemContent.ENDERIC_LENS, 1f, "_enderlens");
        // flux gate
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.fromTag(TagContent.PLATINUM_INGOTS), ItemContent.FLUX_GATE, 1f, "_fluxgate");
        
        // ai processor tree
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), ItemContent.SILICON_WAFER, 5, "_wafer");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.SILICON_WAFER), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), ItemContent.ADVANCED_COMPUTING_ENGINE, 5, "_advcomputer");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), ItemContent.SUPER_AI_CHIP, 50, "_aicomputer");
        
        // dubios container
        offerMotorRecipe(exporter, ItemContent.DUBIOS_CONTAINER, Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.STRANGE_MATTER), "_dubios");
        // adv battery
        offerMotorRecipe(exporter, ItemContent.ADVANCED_BATTERY, Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_advbattery");
        
        // endgame components
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.SUPER_AI_CHIP), ItemContent.HEISENBERG_COMPENSATOR, 100, "_compensator");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), ItemContent.HEISENBERG_COMPENSATOR, 100, "_compensatoralt");
        offerMotorRecipe(exporter, ItemContent.OVERCHARGED_CRYSTAL, Ingredient.ofItems(Items.ENCHANTED_BOOK), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), Ingredient.ofItems(ItemContent.SUPERCONDUCTOR), "_overchargedcrystal");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.INSULATED_WIRE), Ingredient.ofItems(ItemContent.DUBIOS_CONTAINER), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), ItemContent.SUPERCONDUCTOR, 1f, "_superconductor");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), Ingredient.ofItems(ItemContent.HEISENBERG_COMPENSATOR), ItemContent.PROMETHEUM_INGOT, 1000, "_prometheum");
    }
    
    private void addOreChains(RecipeExporter exporter) {
        // iron chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(ItemTags.IRON_ORES),
          Ingredient.ofItems(Items.RAW_IRON),
          Items.RAW_IRON,
          ItemContent.RAW_NICKEL,
          ItemContent.IRON_CLUMP,
          ItemContent.SMALL_IRON_CLUMP,
          ItemContent.SMALL_NICKEL_CLUMP,
          ItemContent.IRON_DUST,
          ItemContent.SMALL_IRON_DUST,
          ItemContent.SMALL_NICKEL_DUST,
          ItemContent.IRON_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          Items.IRON_NUGGET,
          Items.IRON_INGOT,
          1f,
          "_iron"
        );
        
        // copper chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(ItemTags.COPPER_ORES),
          Ingredient.ofItems(Items.RAW_COPPER),
          Items.RAW_COPPER,
          Items.RAW_GOLD,
          ItemContent.COPPER_CLUMP,
          ItemContent.SMALL_COPPER_CLUMP,
          ItemContent.SMALL_GOLD_CLUMP,
          ItemContent.COPPER_DUST,
          ItemContent.SMALL_COPPER_DUST,
          ItemContent.SMALL_GOLD_DUST,
          ItemContent.COPPER_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          ItemContent.COPPER_NUGGET,
          Items.COPPER_INGOT,
          1f,
          "_copper"
        );
        
        // gold chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(ItemTags.GOLD_ORES),
          Ingredient.ofItems(Items.RAW_GOLD),
          Items.RAW_GOLD,
          Items.RAW_COPPER,
          ItemContent.GOLD_CLUMP,
          ItemContent.SMALL_GOLD_CLUMP,
          ItemContent.SMALL_COPPER_CLUMP,
          ItemContent.GOLD_DUST,
          ItemContent.SMALL_GOLD_DUST,
          ItemContent.SMALL_COPPER_DUST,
          ItemContent.GOLD_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          Items.GOLD_NUGGET,
          Items.GOLD_INGOT,
          1f,
          "_gold"
        );
        
        // nickel chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(TagContent.NICKEL_ORES),
          Ingredient.ofItems(ItemContent.RAW_NICKEL),
          ItemContent.RAW_NICKEL,
          ItemContent.RAW_PLATINUM,
          ItemContent.NICKEL_CLUMP,
          ItemContent.SMALL_NICKEL_CLUMP,
          ItemContent.SMALL_PLATINUM_CLUMP,
          ItemContent.NICKEL_DUST,
          ItemContent.SMALL_NICKEL_DUST,
          ItemContent.SMALL_PLATINUM_DUST,
          ItemContent.NICKEL_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          ItemContent.NICKEL_NUGGET,
          ItemContent.NICKEL_INGOT,
          1f,
          "_nickel"
        );
        
        // platinum chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(TagContent.PLATINUM_ORES),
          Ingredient.ofItems(ItemContent.RAW_PLATINUM),
          ItemContent.RAW_PLATINUM,
          ItemContent.FLUXITE,
          ItemContent.PLATINUM_CLUMP,
          ItemContent.SMALL_PLATINUM_CLUMP,
          Items.AMETHYST_SHARD,
          ItemContent.PLATINUM_DUST,
          ItemContent.SMALL_PLATINUM_DUST,
          Items.AMETHYST_SHARD,
          ItemContent.PLATINUM_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          ItemContent.PLATINUM_NUGGET,
          ItemContent.PLATINUM_INGOT,
          1.5f,
          "_platinum"
        );
    }
    
    private void addAlloys(RecipeExporter exporter) {
        addAlloyRecipe(exporter, Ingredient.fromTag(TagContent.PLATINUM_INGOTS), Ingredient.ofItems(Items.NETHERITE_INGOT), ItemContent.DURATIUM_INGOT, "_duratium");
        addAlloyRecipe(exporter, Items.GOLD_INGOT, Items.REDSTONE, ItemContent.ELECTRUM_INGOT, "_electrum");
        addAlloyRecipe(exporter, Ingredient.ofItems(Items.DIAMOND), Ingredient.fromTag(TagContent.NICKEL_INGOTS), ItemContent.ADAMANT_INGOT, "_adamant");
        addAlloyRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.FLUXITE), ItemContent.ENERGITE_INGOT, "_energite");
        addAlloyRecipe(exporter, Ingredient.ofItems(Items.IRON_INGOT), Ingredient.fromTag(TagContent.COAL_DUSTS), ItemContent.STEEL_INGOT, "_steel");
    }
    
    private void addDusts(RecipeExporter exporter) {
        addDustRecipe(exporter, Ingredient.ofItems(Items.COPPER_INGOT), ItemContent.COPPER_DUST, "_copper");
        addDustRecipe(exporter, Ingredient.ofItems(Items.IRON_INGOT), ItemContent.IRON_DUST, "_iron");
        addDustRecipe(exporter, Ingredient.ofItems(Items.GOLD_INGOT), ItemContent.GOLD_DUST, "_gold");
        addDustRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), ItemContent.NICKEL_DUST, "_nickel");
        addDustRecipe(exporter, Ingredient.fromTag(TagContent.PLATINUM_INGOTS), ItemContent.PLATINUM_DUST, "_platinum");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT), ItemContent.BIOSTEEL_DUST, ItemContent.BIOSTEEL_INGOT, "_biosteel");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.DURATIUM_INGOT), ItemContent.DURATIUM_DUST, ItemContent.DURATIUM_INGOT, "_duratium");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), ItemContent.ELECTRUM_DUST, ItemContent.ELECTRUM_INGOT, "_electrum");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), ItemContent.ADAMANT_DUST, ItemContent.ADAMANT_INGOT, "_adamant");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.ENERGITE_INGOT), ItemContent.ENERGITE_DUST, ItemContent.ENERGITE_INGOT, "_energite");
        addDustRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), ItemContent.STEEL_DUST, ItemContent.STEEL_INGOT, "_steel");
        addDustRecipe(exporter, Ingredient.ofItems(Items.COAL), ItemContent.COAL_DUST, "_coal");
        addDustRecipe(exporter, Ingredient.ofItems(Items.QUARTZ), ItemContent.QUARTZ_DUST, "_quartz");
    }
    
    private void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addDustRecipe(exporter, ingot, dust, null, suffix);
    }
    private void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, Item ingotSmelted, String suffix) {
        addPulverizerRecipe(exporter, ingot, dust, suffix);
        addGrinderRecipe(exporter, ingot, dust, suffix);
        if (ingotSmelted != null)
            RecipeProvider.offerSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 0.5f, 300, Oritech.MOD_ID);
    }
    
    
    private void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addGrinderRecipe(exporter, ingot, dust, 1, suffix);
    }
    
    private void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, int dustCount, String suffix) {
        var grinderDefaultSpeed = 100;
        
        var grinder = new OritechRecipe(grinderDefaultSpeed, List.of(ingot), List.of(new ItemStack(dust, dustCount)), RecipeContent.GRINDER, null, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "grinderdust" + suffix), grinder, null);
    }
    
    
    private void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addPulverizerRecipe(exporter, ingot, dust, 1, suffix);
    }
    
    private void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, int dustCount, String suffix) {
        var pulverizerDefaultSpeed = 300;
        
        var pulverizer = new OritechRecipe(pulverizerDefaultSpeed, List.of(ingot), List.of(new ItemStack(dust, dustCount)), RecipeContent.PULVERIZER, null, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "pulverizerdust" + suffix), pulverizer, null);
    }
    
    private void addAssemblerRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Ingredient C, Ingredient D, Item result, float timeMultiplier, String suffix) {
        addAssemblerRecipe(exporter, A, B, C, D, result, 1, timeMultiplier, suffix);
    }
    
    private void addAssemblerRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Ingredient C, Ingredient D, Item result, int count, float timeMultiplier, String suffix) {
        var defaultSpeed = 400;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var inputs = new ArrayList<Ingredient>();
        inputs.add(A);
        if (B != null) inputs.add(B);
        if (C != null) inputs.add(C);
        if (D != null) inputs.add(D);
        var entry = new OritechRecipe(speed, inputs, List.of(new ItemStack(result, count)), RecipeContent.ASSEMBLER, null, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "assembler" + suffix), entry, null);
    }
    
    private void addCentrifugeRecipe(RecipeExporter exporter, Ingredient input, Item result, float timeMultiplier, String suffix) {
        var defaultSpeed = 600;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var entry = new OritechRecipe(speed, List.of(input), List.of(new ItemStack(result)), RecipeContent.CENTRIFUGE, null, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "centrifuge" + suffix), entry, null);
    }
    
    private void addCentrifugeFluidRecipe(RecipeExporter exporter, Ingredient input, Item result, Fluid in, float bucketsIn, Fluid out, float bucketsOut, float timeMultiplier, String suffix) {
        var defaultSpeed = 600;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var inputStack = in != null ? new FluidStack(in, (long) (bucketsIn * 81000)) : null;
        var outputStack = out != null ? new FluidStack(out, (long) (bucketsOut * 81000)) : null;
        var entry = new OritechRecipe(speed, List.of(input), List.of(new ItemStack(result)), RecipeContent.CENTRIFUGE_FLUID, inputStack, outputStack);
        exporter.accept(new Identifier(Oritech.MOD_ID, "centrifugefluid" + suffix), entry, null);
    }
    
    private void addAlloyRecipe(RecipeExporter exporter, Item A, Item B, Item result, String suffix) {
        addAlloyRecipe(exporter, Ingredient.ofItems(A), Ingredient.ofItems(B), result, suffix);
    }
    
    private void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, String suffix) {
        var foundryDefaultSpeed = 300;
        var entry = new OritechRecipe(foundryDefaultSpeed, List.of(A, B), List.of(new ItemStack(result)), RecipeContent.FOUNDRY, null, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "foundryalloy" + suffix), entry, null);
    }
    
    // A is inserted twice, surrounding B
    private void addAtomicForgeRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, int time, String suffix) {
        var entry = new OritechRecipe(time, List.of(A, B, A), List.of(new ItemStack(result)), RecipeContent.ATOMIC_FORGE, null, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "atomicforge" + suffix), entry, null);
    }
    
    private void addMetalProcessingChain(RecipeExporter exporter, Ingredient oreInput, Ingredient rawOre, Item rawMain, Item rawSecondary, Item clump, Item smallClump,
                                         Item smallSecondaryClump, Item dust, Item smallDust, Item smallSecondaryDust, Item gem, Ingredient gemCatalyst, Item nugget,
                                         Item ingot, float timeMultiplier, String suffix) {
                
        // ore block -> raw ores
        var pulverizerOre = new OritechRecipe((int) (400 * timeMultiplier), List.of(oreInput), List.of(new ItemStack(rawMain, 2)), RecipeContent.PULVERIZER, null, null);
        var grinderOre = new OritechRecipe((int) (400 * timeMultiplier), List.of(oreInput), List.of(new ItemStack(rawMain, 2), new ItemStack(rawSecondary, 1)), RecipeContent.GRINDER, null, null);
        
        // raw ores -> dusts / clumps
        var pulverizerRaw = new OritechRecipe((int) (500 * timeMultiplier), List.of(rawOre), List.of(new ItemStack(dust, 1), new ItemStack(smallDust, 3)), RecipeContent.PULVERIZER, null, null);
        var grinderRaw = new OritechRecipe((int) (800 * timeMultiplier), List.of(rawOre), List.of(new ItemStack(clump, 1), new ItemStack(smallClump, 3), new ItemStack(smallSecondaryClump, 3)), RecipeContent.GRINDER, null, null);
        
        // clump processing
        var centrifugeClumpDry = new OritechRecipe((int) (400 * timeMultiplier), List.of(Ingredient.ofItems(clump)), List.of(new ItemStack(dust, 1), new ItemStack(smallSecondaryDust, 3)), RecipeContent.CENTRIFUGE, null, null);
        var centrifugeClumpWet = new OritechRecipe((int) (600 * timeMultiplier), List.of(Ingredient.ofItems(clump)), List.of(new ItemStack(dust, 2)), RecipeContent.CENTRIFUGE_FLUID, new FluidStack(Fluids.WATER, 81000), null);
        
        // gems
        var atomicForgeDust = new OritechRecipe(3, List.of(Ingredient.ofItems(dust), gemCatalyst), List.of(new ItemStack(gem, 1)), RecipeContent.ATOMIC_FORGE, null, null);
        var foundryGem = new OritechRecipe(800, List.of(Ingredient.ofItems(gem), Ingredient.ofItems(gem)), List.of(new ItemStack(ingot, 4)), RecipeContent.FOUNDRY, null, null);
        
        // smelting/compacting
        RecipeProvider.offerSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingot, 0.5f, 300, Oritech.MOD_ID);
        RecipeProvider.offerSmelting(exporter, List.of(smallDust), RecipeCategory.MISC, nugget, 0.5f, 300, Oritech.MOD_ID);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, clump, smallClump);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, dust, smallDust);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, ingot, nugget);
        
        // registration
        exporter.accept(new Identifier(Oritech.MOD_ID, "pulverizerore" + suffix), pulverizerOre, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "grinderore" + suffix), grinderOre, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "pulverizerraw" + suffix), pulverizerRaw, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "grinderraw" + suffix), grinderRaw, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "centrifugeclumpdry" + suffix), centrifugeClumpDry, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "centrifugeclumpwet" + suffix), centrifugeClumpWet, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "atomicforgedust" + suffix), atomicForgeDust, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "foundrygem" + suffix), foundryGem, null);
        
    }
    
    // crafting shapes
    public void offerCableRecipe(RecipeExporter exporter, ItemStack output, Ingredient input) {
        var item = output.getItem();
        createCableRecipe(RecipeCategory.MISC, output.getItem(), output.getCount(), input).criterion(hasItem(item), conditionsFromItem(item)).offerTo(exporter);
    }
    
    public void offerInsulatedCableRecipe(RecipeExporter exporter, ItemStack output, Ingredient input, Ingredient insulation, String suffix) {
        var item = output.getItem();
        createInsulatedCableRecipe(RecipeCategory.MISC, output.getItem(), output.getCount(), input, insulation).criterion(hasItem(item), conditionsFromItem(item)).offerTo(exporter, getItemPath(item) + suffix);
    }
    
    public CraftingRecipeJsonBuilder createCableRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return ShapedRecipeJsonBuilder.create(category, output, count).input('#', input).pattern("   ").pattern("###");
    }
    
    public CraftingRecipeJsonBuilder createInsulatedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input, Ingredient insulation) {
        return ShapedRecipeJsonBuilder.create(category, output, count).input('c', input).input('i', insulation).pattern("iii").pattern("ccc").pattern("iii");
    }
    
    public void offerMotorRecipe(RecipeExporter exporter, Item output, Ingredient shaft, Ingredient core, Ingredient wall, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', shaft).input('c', core).input('w', wall).pattern(" s ").pattern("wcw").pattern("wcw");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerManualAlloyRecipe(RecipeExporter exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        offerManualAlloyRecipe(exporter, output, A, B, 1, suffix);
    }
    public void offerManualAlloyRecipe(RecipeExporter exporter, Item output, Ingredient A, Ingredient B, int count, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, count).input('a', A).input('b', B).pattern("aa ").pattern("bb ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerGeneratorRecipe(RecipeExporter exporter, Item output, Ingredient base, Ingredient sides, Ingredient core, Ingredient frame, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', sides).input('c', core).input('f', frame).input('b', base)
                        .pattern("fff")
                        .pattern("fcf")
                        .pattern("sbs");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerFurnaceRecipe(RecipeExporter exporter, Item output, Ingredient base, Ingredient sides, Ingredient middleSides, Ingredient core, Ingredient frame, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', sides).input('c', core).input('f', frame).input('b', base).input('m', middleSides)
                        .pattern("fff")
                        .pattern("mcm")
                        .pattern("sbs");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerAtomicForgeRecipe(RecipeExporter exporter, Item output, Ingredient base, Ingredient middleSides, Ingredient core, Ingredient top, Ingredient frame, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', top).input('c', core).input('f', frame).input('b', base).input('m', middleSides)
                        .pattern("fsf")
                        .pattern("mcm")
                        .pattern("bbb");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerMachineFrameRecipe(RecipeExporter exporter, Item output, Ingredient base, Ingredient alt, int count, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, count).input('s', base).input('c', alt)
                        .pattern(" s ")
                        .pattern("csc")
                        .pattern(" s ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
}
