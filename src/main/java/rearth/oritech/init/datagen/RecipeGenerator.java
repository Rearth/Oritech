package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.block.Block;
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
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.init.datagen.data.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RecipeGenerator extends FabricRecipeProvider {
    
    public RecipeGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }
    
    @Override
    public void generate(RecipeExporter exporter) {
        
        addDeepDrillOres(exporter);
        addFuels(exporter);
        addBiomass(exporter);
        addEquipment(exporter);
        addMachines(exporter);
        addComponents(exporter);
        addOreChains(exporter);
        addAlloys(exporter);
        addDusts(exporter);
        addDecorative(exporter);
        addVanillaAdditions(exporter);
        
    }
    
    private void addVanillaAdditions(RecipeExporter exporter) {
    
        // slimeball from honey and biomass
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.HONEYCOMB), Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.ofItems(ItemContent.BIOMASS), Items.SLIME_BALL, 1f, "_assemblerslime");
        // fireball in assembler (gunpowder, blaze powder + coal) = 5 charges
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.GUNPOWDER), Ingredient.ofItems(Items.BLAZE_POWDER), Ingredient.fromTag(ItemTags.COALS), Ingredient.fromTag(ItemTags.COALS), Items.FIRE_CHARGE, 1f, "_assemblerfireball");
        // blaze rod (4 powder in assembler)
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.BLAZE_POWDER), Ingredient.ofItems(Items.BLAZE_POWDER), Ingredient.ofItems(Items.BLAZE_POWDER), Ingredient.ofItems(Items.BLAZE_POWDER), Items.BLAZE_ROD, 1f, "_assemblerblazerod");
        // enderic compound from sculk
        addCentrifugeRecipe(exporter, Ingredient.ofItems(Items.SCULK), ItemContent.ENDERIC_COMPOUND, 4f, "_endericsculk");
        // budding amethyst (amethyst shard x2, enderic compound, overcharged crystal)
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.AMETHYST_SHARD), Ingredient.ofItems(Items.AMETHYST_SHARD), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), Items.BUDDING_AMETHYST, 1f, "_assembleramethystbud");
        // netherite alloying (yes this is pretty OP)
        addAlloyRecipe(exporter, Items.GOLD_INGOT, Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT, "_netheritealloying");
        // books
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.PAPER), Ingredient.ofItems(Items.PAPER), Ingredient.ofItems(Items.PAPER), Ingredient.ofItems(Items.LEATHER), Items.BOOK, 2, 1f, "_assemblerbook");
        // reinforced deepslate
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(Items.DEEPSLATE), Items.REINFORCED_DEEPSLATE, 100, "_reinfdeepslate");
    }
    
    private void addDeepDrillOres(RecipeExporter exporter) {
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_REDSTONE, Items.REDSTONE, 1, "_redstone");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_LAPIS, Items.LAPIS_LAZULI, 1, "_lapis");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_IRON, Items.RAW_IRON, 1, "_iron");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_COAL, Items.COAL, 1, "_coal");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_COPPER, Items.RAW_COPPER, 1, "_copper");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_GOLD, Items.RAW_GOLD, 1, "_gold");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_EMERALD, Items.EMERALD, 1, "_emerald");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_DIAMOND, Items.DIAMOND, 1, "_diamond");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_NICKEL, ItemContent.RAW_NICKEL, 1, "_nickel");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_PLATINUM, ItemContent.RAW_PLATINUM, 1, "_platinum");
    }
    
    private void addFuels(RecipeExporter exporter) {
        
        // bio
        addBioGenRecipe(exporter, Ingredient.fromTag(TagContent.BIOMASS), 15, "_rawbio");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.PACKED_WHEAT), 200, "_packedwheat");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.BIOMASS), 25, "_biomass");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.SOLID_BIOFUEL), 160, "_solidbiomass");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER), 300, "_polymer");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), 3000, "_vex");
        // lava
        addLavaGen(exporter, new FluidStack(Fluids.LAVA, 8100), 12, "_lava");
        // fuel
        addFuelGenRecipe(exporter, new FluidStack(FluidContent.STILL_OIL, 8100), 8, "_crude");
        addFuelGenRecipe(exporter, new FluidStack(FluidContent.STILL_FUEL, 8100), 24, "_fuel");
        //steam
        addSteamEngineGen(exporter, new FluidStack(FluidContent.STILL_STEAM, 32), 1, "_steameng");
    }
    
    private void addBiomass(RecipeExporter exporter) {
        // biomass
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.BIOMASS), ItemContent.BIOMASS, 1, "_biobasic");
        addPulverizerRecipe(exporter, Ingredient.ofItems(ItemContent.PACKED_WHEAT), ItemContent.BIOMASS, 16, "_packagedwheatbio");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.fromTag(ItemTags.PLANKS), ItemContent.SOLID_BIOFUEL, 1, "_solidbiofuel");
    }
    
    private void addEquipment(RecipeExporter exporter) {
        
        
        offerDrillRecipe(exporter, ToolsContent.HAND_DRILL, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), "_handdrill");
        offerChainsawRecipe(exporter, ToolsContent.CHAINSAW, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), "_chainsaw");
        offerAxeRecipe(exporter, ToolsContent.PROMETHIUM_AXE, Ingredient.ofItems(ItemContent.PROMETHEUM_INGOT), Ingredient.ofItems(BlockContent.DESTROYER_BLOCK.asItem()), "_promaxe");
        offerPickaxeRecipe(exporter, ToolsContent.PROMETHIUM_PICKAXE, Ingredient.ofItems(ItemContent.PROMETHEUM_INGOT), Ingredient.ofItems(BlockContent.DESTROYER_BLOCK.asItem()), "_prompick");
        
        // designator
        offerDrillRecipe(exporter, ItemContent.TARGET_DESIGNATOR, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "_designator");
        // weed killer
        offerDrillRecipe(exporter, ItemContent.WEED_KILLER, Ingredient.ofItems(Items.ROTTEN_FLESH), Ingredient.ofItems(Items.ROTTEN_FLESH), Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER), Ingredient.ofItems(Items.GLASS_BOTTLE), "_weedex");
        
        // helmet (enderic lens + machine plating)
        offerHelmetRecipe(exporter, ToolsContent.EXO_HELMET, Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ENDERIC_LENS), "_exohelm");
        // chestplate (advanced battery + machine plating)
        offerChestplateRecipe(exporter, ToolsContent.EXO_CHESTPLATE, Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), "_exochest");
        // legs (motor + plating)
        offerLegsRecipe(exporter, ToolsContent.EXO_LEGGINGS, Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), "_exolegs");
        // feet (silicon + plating)
        offerFeetRecipe(exporter, ToolsContent.EXO_BOOTS, Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.SILICON), "_exoboots");
        
        // guidebook (any ingot + lapis)
        offerHelmetRecipe(exporter, ItemContent.ORITECH_GUIDE, Ingredient.fromTag(ConventionalItemTags.INGOTS), Ingredient.ofItems(Items.LAPIS_LAZULI), "_guidebook");
    }
    
    private void addDecorative(RecipeExporter exporter) {
        // ceiling light
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.CEILING_LIGHT.asItem(), 6), Ingredient.ofItems(Items.GLOWSTONE_DUST), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_ceilightlight");
        // hanging light
        offerTwoComponentRecipe(exporter, BlockContent.CEILING_LIGHT_HANGING.asItem(), Ingredient.ofItems(Items.CHAIN), Ingredient.ofItems(BlockContent.CEILING_LIGHT.asItem()), "_hanginglight");
        // tech button
        offerLeverRecipe(exporter, BlockContent.TECH_BUTTON.asItem(), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_techbutton");
        // tech lever
        offerLeverRecipe(exporter, BlockContent.TECH_LEVER.asItem(), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_techlever");
        // tech door
        offerDoorRecipe(exporter, BlockContent.TECH_DOOR.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_techdoor");
        // metal beam
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.METAL_BEAM_BLOCK.asItem(), 6), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_metalbeams");
        // tech glass
        offerMachinePlatingRecipe(exporter, BlockContent.INDUSTRIAL_GLASS_BLOCK.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(ConventionalItemTags.GLASS_BLOCKS), Ingredient.fromTag(TagContent.MACHINE_PLATING), 4, "_industrialglass");
    }
    
    private void addMachines(RecipeExporter exporter) {
        // basic generator
        offerGeneratorRecipe(exporter, BlockContent.BASIC_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(Blocks.FURNACE.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.fromTag(TagContent.NICKEL_INGOTS), "_basicgen");
        // pulverizer
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), Ingredient.ofItems(Blocks.IRON_BLOCK.asItem()), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_pulverizer");
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(Items.IRON_INGOT), "_pulverizeralt");
        // electric furnace
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), Ingredient.ofItems(Blocks.FURNACE.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(Items.COPPER_INGOT), "_electricfurnace");
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), Ingredient.ofItems(Blocks.FURNACE.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.PLATINUM_INGOTS), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(Items.COPPER_INGOT), "_electricfurnacealt");
        // assembler
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), Ingredient.ofItems(Blocks.BLAST_FURNACE.asItem()), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.CRAFTER), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.COPPER_INGOT), "_assembler");
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.CRAFTER), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.COPPER_INGOT), "_assembleralt");
        // foundry
        offerGeneratorRecipe(exporter, BlockContent.FOUNDRY_BLOCK.asItem(), Ingredient.ofItems(Blocks.CAULDRON.asItem()), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.COPPER_INGOT), "_foundry");
        // centrifuge
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Items.GLASS_BOTTLE), "_centrifuge");
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.IRON_BLOCK), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.GLASS_BOTTLE), "_centrifugealt");
        // laser arm
        offerAtomicForgeRecipe(exporter, BlockContent.LASER_ARM_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.ENDERIC_LENS), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), "_atomicforge");
        // crusher
        offerGeneratorRecipe(exporter, BlockContent.FRAGMENT_FORGE_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "_crusher");
        // atomic forge
        offerAtomicForgeRecipe(exporter, BlockContent.ATOMIC_FORGE_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(ItemContent.FLUX_GATE), "_atomicforge");
        
        // biofuel generator
        offerGeneratorRecipe(exporter, BlockContent.BIO_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT), "_biogen");
        // lava generator
        offerGeneratorRecipe(exporter, BlockContent.LAVA_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), "_lavagen");
         // steam engine
        offerGeneratorRecipe(exporter, BlockContent.STEAM_ENGINE_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), "_steamgen");
        // diesel generator
        offerGeneratorRecipe(exporter, BlockContent.FUEL_GENERATOR_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK), Ingredient.ofItems(ItemContent.ENDERIC_LENS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_fuelgen");
        // large solar
        offerGeneratorRecipe(exporter, BlockContent.BIG_SOLAR_PANEL_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), Ingredient.ofItems(ItemContent.FLUXITE), "_solar");
        
        // small storage
        offerAtomicForgeRecipe(exporter, BlockContent.SMALL_STORAGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.BASIC_BATTERY), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.INSULATED_WIRE), "_smallstorage");
        // large storage
        offerAtomicForgeRecipe(exporter, BlockContent.LARGE_STORAGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.DUBIOS_CONTAINER), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.INSULATED_WIRE), "_bigstorage");
        
        // fluid tank
        offerTankRecipe(exporter, BlockContent.SMALL_TANK_BLOCK.asItem(), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(BlockContent.FLUID_PIPE.asItem()), "_stank");
        // pump
        offerGeneratorRecipe(exporter, BlockContent.PUMP_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.COPPER_INGOT), "_pump");
        // block placer
        offerFurnaceRecipe(exporter, BlockContent.PLACER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(BlockContent.MACHINE_FRAME_BLOCK.asItem()), Ingredient.ofItems(Items.COPPER_INGOT), "_placer");
        // block destroyer
        offerAtomicForgeRecipe(exporter, BlockContent.DESTROYER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), Ingredient.ofItems(ItemContent.ENDERIC_LENS), Ingredient.ofItems(ItemContent.FLUX_GATE), "_destroyer");
        // fertilizer
        offerFurnaceRecipe(exporter, BlockContent.FERTILIZER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(Items.COPPER_INGOT), "_fertilizer");
        // tree feller
        offerGeneratorRecipe(exporter, BlockContent.TREEFELLER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(Items.IRON_AXE), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), "_treefeller");
        
        // machine frame
        offerMachineFrameRecipe(exporter, BlockContent.MACHINE_FRAME_BLOCK.asItem(), Ingredient.ofItems(Items.IRON_BARS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), 16, "_frame");
        // energy pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE.asItem(), 6), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.INSULATED_WIRE), "_energy");
        // item pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE.asItem(), 6), Ingredient.ofItems(ItemContent.NICKEL_INGOT), Ingredient.fromTag(ItemTags.PLANKS), "_item");
        // item filter
        offerGeneratorRecipe(exporter, BlockContent.ITEM_FILTER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.INSULATED_WIRE), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.INSULATED_WIRE), "_itemfilter");
        // fluid pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE.asItem(), 6), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(Items.COPPER_INGOT), "_fluid");
        
        // deep drill
        offerAtomicForgeRecipe(exporter, BlockContent.DEEP_DRILL_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.HEISENBERG_COMPENSATOR), Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), "_deepdrill");
        // drone port
        offerAtomicForgeRecipe(exporter, BlockContent.DRONE_PORT_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.SUPERCONDUCTOR), Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), "_droneport");
        offerAtomicForgeRecipe(exporter, BlockContent.DRONE_PORT_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.SUPERCONDUCTOR), Ingredient.ofItems(ItemContent.SUPER_AI_CHIP), Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), "_droneportalt");
        
        // arcane catalyst
        offerFurnaceRecipe(exporter, BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem(), Ingredient.ofItems(Items.ENCHANTING_TABLE), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.OBSIDIAN), Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), Ingredient.ofItems(ItemContent.FLUXITE), "catalyst");
        offerFurnaceRecipe(exporter, BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem(), Ingredient.ofItems(Items.ENCHANTING_TABLE), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.OBSIDIAN), Ingredient.ofItems(ItemContent.SUPER_AI_CHIP), Ingredient.ofItems(ItemContent.FLUXITE), "catalyst_alt");
        // enchanter
        offerGeneratorRecipe(exporter, BlockContent.ENCHANTER_BLOCK.asItem(), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), Ingredient.ofItems(BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem()), Ingredient.ofItems(Items.BOOK), "_enchanter");
        // spawner
        offerTankRecipe(exporter, BlockContent.SPAWNER_CONTROLLER_BLOCK.asItem(), Ingredient.ofItems(BlockContent.SPAWNER_CAGE_BLOCK), Ingredient.ofItems(BlockContent.ENCHANTMENT_CATALYST_BLOCK), "_spawner");
        // spawner cage
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.SPAWNER_CAGE_BLOCK, 2), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(Items.IRON_BARS), "_cage");
        // withered rose
        offerMachineFrameRecipe(exporter, BlockContent.WITHER_CROP_BLOCK.asItem(), Ingredient.ofItems(Items.WITHER_ROSE), Ingredient.fromTag(ItemTags.FLOWERS), 1, "_witherrose");
        
        // addons
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_SPEED_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "_speedaddon");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_EFFICIENCY_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "_effaddon");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_CAPACITOR_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "_capacitor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_ACCEPTOR_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "_acceptor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_YIELD_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.ENDERIC_LENS), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "_yield");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_FLUID_ADDON.asItem(), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(BlockContent.FLUID_PIPE), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), "_fluid");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_INVENTORY_PROXY_ADDON.asItem(), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(ConventionalItemTags.CHESTS), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), "_invproxy");
        offerGeneratorRecipe(exporter, BlockContent.CROP_FILTER_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), "_cropfilter");
        offerGeneratorRecipe(exporter, BlockContent.QUARRY_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.DIAMOND_PICKAXE), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "_quarryaddon");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(BlockContent.FLUID_PIPE), "_steamboiler");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(BlockContent.FLUID_PIPE), Ingredient.ofItems(ItemContent.COAL_DUST), "_steamboileralt");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_REDSTONE_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(Items.REPEATER), Ingredient.ofItems(Items.COMPARATOR), Ingredient.ofItems(Items.REDSTONE), "_redstoneaddon");
        offerTwoComponentRecipe(exporter, BlockContent.CAPACITOR_ADDON_EXTENDER.asItem(), Ingredient.ofItems(BlockContent.MACHINE_EXTENDER.asItem()), Ingredient.ofItems(BlockContent.MACHINE_CAPACITOR_ADDON), "_capextender");
        
        // cores
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_1.asItem(), Ingredient.fromTag(ItemTags.PLANKS), Ingredient.ofItems(Items.CRAFTING_TABLE), "_core1");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(Items.LAPIS_LAZULI), "_core2");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), Ingredient.ofItems(Items.IRON_INGOT), Ingredient.ofItems(Items.LAPIS_LAZULI), "_core2alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), Ingredient.ofItems(Items.REDSTONE), "_core3");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(Items.REDSTONE), "_core3alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_4.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), "_core4");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_5.asItem(), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), "_core5");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_6.asItem(), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(ItemContent.DUBIOS_CONTAINER), "_core6");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_7.asItem(), Ingredient.ofItems(ItemContent.PROMETHEUM_INGOT), Ingredient.ofItems(ItemContent.SUPERCONDUCTOR), "_core7");
        
        // machine extender
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_EXTENDER.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(BlockContent.MACHINE_CORE_2.asItem()), "_extender");
    }
    
    private void addComponents(RecipeExporter exporter) {
        // coal stuff (including basic steel)
        addCentrifugeRecipe(exporter, Ingredient.fromTag(TagContent.COAL_DUSTS), ItemContent.CARBON_FIBRE_STRANDS, 0.5f, "_carbon");
        offerManualAlloyRecipe(exporter, ItemContent.STEEL_INGOT, Ingredient.ofItems(Items.IRON_INGOT), Ingredient.ofItems(Items.COAL), "manualsteel");
        
        // manual alloys
        offerManualAlloyRecipe(exporter, ItemContent.ELECTRUM_INGOT, Ingredient.ofItems(Items.GOLD_INGOT), Ingredient.ofItems(Items.REDSTONE), "manualelectrum");
        offerManualAlloyRecipe(exporter, ItemContent.ADAMANT_INGOT, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(Items.DIAMOND), "manualadamant");
        
        // enderic entry
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.ENDER_PEARL), ItemContent.ENDERIC_COMPOUND, 8, "_pearl_enderic");
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.ENDER_PEARL), ItemContent.ENDERIC_COMPOUND, 12, "_pearl_enderic");
        addGrinderRecipe(exporter, Ingredient.ofItems(Blocks.END_STONE), ItemContent.ENDERIC_COMPOUND, 1, "_stone_enderic");
        
        // fine wires
        offerCableRecipe(exporter, new ItemStack(ItemContent.INSULATED_WIRE, 4), Ingredient.fromTag(TagContent.NICKEL_INGOTS));
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(Items.COPPER_INGOT), ItemContent.INSULATED_WIRE, 12, 0.5f, "_fwire");
        
        // magnetic coils
        offerInsulatedCableRecipe(exporter, new ItemStack(ItemContent.MAGNETIC_COIL, 2), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.INSULATED_WIRE), "magnet");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.INSULATED_WIRE), Ingredient.ofItems(ItemContent.INSULATED_WIRE), Ingredient.ofItems(ItemContent.INSULATED_WIRE), ItemContent.MAGNETIC_COIL, 2, 0.5f, "magnet");
        
        // motor
        offerMotorRecipe(exporter, ItemContent.MOTOR, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_motorcraft");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), ItemContent.MOTOR, 2, 0.5f, "motor");
        
        // machine plating variants
        offerMachinePlatingRecipe(exporter, BlockContent.MACHINE_PLATING_BLOCK.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Blocks.STONE.asItem()), Ingredient.ofItems(Items.COPPER_INGOT), 2, "_platingmanual");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), BlockContent.MACHINE_PLATING_BLOCK.asItem(), 8, 1f, "plating");
        offerMachinePlatingRecipe(exporter, BlockContent.IRON_PLATING_BLOCK.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Blocks.STONE.asItem()), Ingredient.ofItems(Items.IRON_INGOT), 2, "_platingmanualiron");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Items.IRON_INGOT), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), BlockContent.IRON_PLATING_BLOCK.asItem(), 8, 1f, "platingiron");
        offerMachinePlatingRecipe(exporter, BlockContent.NICKEL_PLATING_BLOCK.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Blocks.STONE.asItem()), Ingredient.fromTag(TagContent.NICKEL_INGOTS), 2, "_platingmanualnickel");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), BlockContent.NICKEL_PLATING_BLOCK.asItem(), 8, 1f, "platingnickel");
        
        // basic battery
        offerMotorRecipe(exporter, ItemContent.BASIC_BATTERY, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), "manualbattery");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.fromTag(TagContent.STEEL_INGOTS), ItemContent.BASIC_BATTERY, 1, 0.5f, "battery");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.fromTag(TagContent.STEEL_INGOTS), ItemContent.BASIC_BATTERY, 2, 1f, "batterybetter");
        
        // silicon
        offerManualAlloyRecipe(exporter, ItemContent.RAW_SILICON, Ingredient.fromTag(TagContent.QUARTZ_DUSTS), Ingredient.fromTag(ItemTags.SAND), 3, "manualrawsilicon");
        offerSmelting(exporter, List.of(ItemContent.RAW_SILICON), RecipeCategory.MISC, ItemContent.SILICON, 0.5f, 60, "siliconfurnace");
        
        // plastic
        offer2x2CompactingRecipe(exporter, RecipeCategory.MISC, ItemContent.PACKED_WHEAT, Items.WHEAT);
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.PACKED_WHEAT), ItemContent.RAW_BIOPOLYMER, Fluids.WATER, 0.25f, null, 0, 1f, "_biopolymer");
        addCentrifugeFluidRecipe(exporter, Ingredient.fromTag(ItemTags.SAND), ItemContent.POLYMER_RESIN, FluidContent.STILL_OIL, 0.1f, null, 0, 0.5f, "_polymerresin");
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER), ItemContent.PLASTIC_SHEET, Fluids.WATER, 0.5f, null, 0, 1f, "_plasticoil");
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.POLYMER_RESIN), ItemContent.PLASTIC_SHEET, Fluids.WATER, 0.5f, null, 0, 0.33f, "_plasticbio");
        
        // processing unit
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(Items.REDSTONE), ItemContent.PROCESSING_UNIT, 1f, "_processingunit");
        // enderic lens
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), ItemContent.ENDERIC_LENS, 1.5f, "_enderlens");
        // flux gate
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.fromTag(TagContent.PLATINUM_INGOTS), ItemContent.FLUX_GATE, 1.5f, "_fluxgate");
        
        // ai processor tree
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.SILICON), Ingredient.ofItems(ItemContent.CARBON_FIBRE_STRANDS), ItemContent.SILICON_WAFER, 5, "_wafer");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.SILICON_WAFER), Ingredient.ofItems(ItemContent.PLASTIC_SHEET), ItemContent.ADVANCED_COMPUTING_ENGINE, 5, "_advcomputer");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), ItemContent.SUPER_AI_CHIP, 50, "_aicomputer");
        
        // dubios container
        offerMotorRecipe(exporter, ItemContent.DUBIOS_CONTAINER, Ingredient.ofItems(ItemContent.PLASTIC_SHEET), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), "_dubios");
        // adv battery
        offerMotorRecipe(exporter, ItemContent.ADVANCED_BATTERY, Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), Ingredient.fromTag(TagContent.STEEL_INGOTS), "_advbattery");
        
        // fuel
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.FLUXITE), null, FluidContent.STILL_OIL, 1f, FluidContent.STILL_FUEL, 1f, 1f, "_fuel");
        
        // biosteel
        addAlloyRecipe(exporter, ItemContent.RAW_BIOPOLYMER, Items.IRON_INGOT, ItemContent.BIOSTEEL_INGOT, "_biosteel");
        
        // endgame components
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.SUPER_AI_CHIP), ItemContent.HEISENBERG_COMPENSATOR, 100, "_compensator");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), ItemContent.HEISENBERG_COMPENSATOR, 100, "_compensatoralt");
        offerMotorRecipe(exporter, ItemContent.OVERCHARGED_CRYSTAL, Ingredient.ofItems(Items.AMETHYST_BLOCK), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), Ingredient.ofItems(ItemContent.SUPERCONDUCTOR), "_overchargedcrystal");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.INSULATED_WIRE), Ingredient.ofItems(ItemContent.DUBIOS_CONTAINER), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), ItemContent.SUPERCONDUCTOR, 2f, "_superconductor");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), Ingredient.ofItems(ItemContent.HEISENBERG_COMPENSATOR), ItemContent.PROMETHEUM_INGOT, 1000, "_prometheum");
    }
    
    private void addOreChains(RecipeExporter exporter) {
        
        // basic smelting for nickel + platinum
        offerSmelting(exporter, List.of(ItemContent.RAW_NICKEL), RecipeCategory.MISC, ItemContent.NICKEL_INGOT, 1f, 200, "nickelsmelting");
        offerSmelting(exporter, List.of(ItemContent.RAW_PLATINUM), RecipeCategory.MISC, ItemContent.PLATINUM_INGOT, 1f, 200, "platinumsmelting");
        offerBlasting(exporter, List.of(ItemContent.RAW_NICKEL), RecipeCategory.MISC, ItemContent.NICKEL_INGOT, 1f, 100, "nickelblasting");
        offerBlasting(exporter, List.of(ItemContent.RAW_PLATINUM), RecipeCategory.MISC, ItemContent.PLATINUM_INGOT, 1f, 100, "platinumblasting");
        
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
          Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND),
          Items.IRON_NUGGET,
          Items.IRON_INGOT,
          1f,
          "_iron",
          3
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
          Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND),
          ItemContent.COPPER_NUGGET,
          Items.COPPER_INGOT,
          1f,
          "_copper",
          3
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
          Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND),
          Items.GOLD_NUGGET,
          Items.GOLD_INGOT,
          1f,
          "_gold",
          3
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
          Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND),
          ItemContent.NICKEL_NUGGET,
          ItemContent.NICKEL_INGOT,
          1f,
          "_nickel",
          2
        );
        
        // platinum chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(TagContent.PLATINUM_ORES),
          Ingredient.ofItems(ItemContent.RAW_PLATINUM),
          ItemContent.RAW_PLATINUM,
          ItemContent.FLUXITE,
          ItemContent.PLATINUM_CLUMP,
          ItemContent.SMALL_PLATINUM_CLUMP,
          ItemContent.FLUXITE,
          ItemContent.PLATINUM_DUST,
          ItemContent.SMALL_PLATINUM_DUST,
          ItemContent.FLUXITE,
          ItemContent.PLATINUM_GEM,
          Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND),
          ItemContent.PLATINUM_NUGGET,
          ItemContent.PLATINUM_INGOT,
          1.5f,
          "_platinum",
          1
        );
    }
    
    private void addAlloys(RecipeExporter exporter) {
        addAlloyRecipe(exporter, Ingredient.fromTag(TagContent.PLATINUM_INGOTS), Ingredient.ofItems(Items.NETHERITE_INGOT), ItemContent.DURATIUM_INGOT, "_duratium");
        addAlloyRecipe(exporter, Items.GOLD_INGOT, Items.REDSTONE, ItemContent.ELECTRUM_INGOT, "_electrum");
        addAlloyRecipe(exporter, Ingredient.ofItems(Items.DIAMOND), Ingredient.fromTag(TagContent.NICKEL_INGOTS), ItemContent.ADAMANT_INGOT, "_adamant");
        addAlloyRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.FLUXITE), ItemContent.ENERGITE_INGOT, "_energite");
        addAlloyRecipe(exporter, Ingredient.ofItems(Items.IRON_INGOT), Ingredient.fromTag(TagContent.COAL_DUSTS), ItemContent.STEEL_INGOT, 0.3333f, "_steel");
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
        
        // raw ores without processing chains
        // coal
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.COAL_ORES), Items.COAL, 3, "_coaloregrinder");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.COAL_ORES), Items.COAL, 2, "coalorepulverizer");
        // redstone
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.REDSTONE_ORES), Items.REDSTONE, 12, "_redstoneoregrinder");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.REDSTONE_ORES), Items.REDSTONE, 8, "_redstoneorepulverizer");
        // diamond
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.DIAMOND_ORES), Items.DIAMOND, 2, "_diamondoregrinder");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.DIAMOND_ORES), Items.DIAMOND, 1, "_diamondorepulverizer");
        // quartz
        addGrinderRecipe(exporter, Ingredient.ofItems(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 3, "_quartzoregrinder");
        addPulverizerRecipe(exporter, Ingredient.ofItems(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 2, "_quartzorepulverizer");
        // glowstone
        addGrinderRecipe(exporter, Ingredient.ofItems(Blocks.GLOWSTONE), Items.GLOWSTONE_DUST, 4, "_glowstoneoregrinder");
        addPulverizerRecipe(exporter, Ingredient.ofItems(Blocks.GLOWSTONE), Items.GLOWSTONE_DUST, 3, "_glowstoneorepulverizer");
        // lapis
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.LAPIS_ORES), Items.LAPIS_LAZULI, 8, "_lapisoregrinder");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.LAPIS_ORES), Items.LAPIS_LAZULI, 6, "_lapisorepulverizer");
        // bone
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.BONE), Items.BONE_MEAL, 8, "_bonegrinder");
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.BONE), Items.BONE_MEAL, 6, "_bonepulverizer");
        // blaze powder
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.BLAZE_ROD), Items.BLAZE_POWDER, 4, "_blazegrinder");
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.BLAZE_ROD), Items.BLAZE_POWDER, 3, "_blazepulverizer");
        // wool
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.WOOL), Items.STRING, 4, "_stringgrinder");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.WOOL), Items.STRING, 3, "_stringpulverizer");
        // ancient debris
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.ANCIENT_DEBRIS), Items.NETHERITE_SCRAP, 2, "_netheritescrapgrinder");
    }
    
    private void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addDustRecipe(exporter, ingot, dust, null, suffix);
    }
    private void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, Item ingotSmelted, String suffix) {
        addPulverizerRecipe(exporter, ingot, dust, suffix);
        addGrinderRecipe(exporter, ingot, dust, suffix);
        if (ingotSmelted != null) {
            RecipeProvider.offerSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 200, Oritech.MOD_ID);
            RecipeProvider.offerBlasting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 100, Oritech.MOD_ID);
        }
    }
    
    
    private void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addGrinderRecipe(exporter, ingot, dust, 1, suffix);
    }
    
    private void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, int dustCount, String suffix) {
        var grinderDefaultSpeed = 200;
        
        var grinder = new OritechRecipe(grinderDefaultSpeed, List.of(ingot), List.of(new ItemStack(dust, dustCount)), RecipeContent.GRINDER, null, null);
        exporter.accept(Oritech.id("grinderdust" + suffix), grinder, null);
    }
    
    
    private void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addPulverizerRecipe(exporter, ingot, dust, 1, suffix);
    }
    
    private void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, int dustCount, String suffix) {
        var pulverizerDefaultSpeed = 300;
        
        var pulverizer = new OritechRecipe(pulverizerDefaultSpeed, List.of(ingot), List.of(new ItemStack(dust, dustCount)), RecipeContent.PULVERIZER, null, null);
        exporter.accept(Oritech.id("pulverizerdust" + suffix), pulverizer, null);
    }
    
    private void addAssemblerRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Ingredient C, Ingredient D, Item result, float timeMultiplier, String suffix) {
        addAssemblerRecipe(exporter, A, B, C, D, result, 1, timeMultiplier, suffix);
    }
    
    private void addAssemblerRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Ingredient C, Ingredient D, Item result, int count, float timeMultiplier, String suffix) {
        var defaultSpeed = 300;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var inputs = new ArrayList<Ingredient>();
        inputs.add(A);
        if (B != null) inputs.add(B);
        if (C != null) inputs.add(C);
        if (D != null) inputs.add(D);
        var entry = new OritechRecipe(speed, inputs, List.of(new ItemStack(result, count)), RecipeContent.ASSEMBLER, null, null);
        exporter.accept(Oritech.id("assembler" + suffix), entry, null);
    }
    
    private void addCentrifugeRecipe(RecipeExporter exporter, Ingredient input, Item result, float timeMultiplier, String suffix) {
        addCentrifugeRecipe(exporter, input, result, 1, timeMultiplier, suffix);
    }
    private void addCentrifugeRecipe(RecipeExporter exporter, Ingredient input, Item result, int count, float timeMultiplier, String suffix) {
        var defaultSpeed = 300;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var entry = new OritechRecipe(speed, List.of(input), List.of(new ItemStack(result, count)), RecipeContent.CENTRIFUGE, null, null);
        exporter.accept(Oritech.id("centrifuge" + suffix), entry, null);
    }
    
    private void addCentrifugeFluidRecipe(RecipeExporter exporter, Ingredient input, Item result, Fluid in, float bucketsIn, Fluid out, float bucketsOut, float timeMultiplier, String suffix) {
        var defaultSpeed = 300;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var inputStack = in != null ? new FluidStack(in, (long) (bucketsIn * 81000)) : null;
        var outputStack = out != null ? new FluidStack(out, (long) (bucketsOut * 81000)) : null;
        List<ItemStack> outputItem = result != null ? List.of(new ItemStack(result)) : List.of();
        var entry = new OritechRecipe(speed, List.of(input), outputItem, RecipeContent.CENTRIFUGE_FLUID, inputStack, outputStack);
        exporter.accept(Oritech.id("centrifugefluid" + suffix), entry, null);
    }
    
    private void addAlloyRecipe(RecipeExporter exporter, Item A, Item B, Item result, String suffix) {
        addAlloyRecipe(exporter, Ingredient.ofItems(A), Ingredient.ofItems(B), result, suffix);
    }
    
    private void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, String suffix) {
        addAlloyRecipe(exporter, A, B, result, 1f, suffix);
    }
    
    
    private void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, float speedMultiplier, String suffix) {
        var foundryDefaultSpeed = (int) (300 * speedMultiplier);
        
        var entry = new OritechRecipe(foundryDefaultSpeed, List.of(A, B), List.of(new ItemStack(result)), RecipeContent.FOUNDRY, null, null);
        exporter.accept(Oritech.id("foundryalloy" + suffix), entry, null);
        
        var entryInverse = new OritechRecipe(foundryDefaultSpeed, List.of(B, A), List.of(new ItemStack(result)), RecipeContent.FOUNDRY, null, null);
        exporter.accept(Oritech.id("foundryalloyinv" + suffix), entryInverse, null);
    }
    
    // A is inserted twice, surrounding B
    private void addAtomicForgeRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, int time, String suffix) {
        var entry = new OritechRecipe(time, List.of(A, B, A), List.of(new ItemStack(result)), RecipeContent.ATOMIC_FORGE, null, null);
        exporter.accept(Oritech.id("atomicforge" + suffix), entry, null);
    }
    
    private void addDeepDrillRecipe(RecipeExporter exporter, Block input, Item result, int time, String suffix) {
        var entry = new OritechRecipe(time, List.of(Ingredient.ofItems(input.asItem())), List.of(new ItemStack(result)), RecipeContent.DEEP_DRILL, null, null);
        exporter.accept(Oritech.id("deepdrill" + suffix), entry, null);
    }
    
    private void addBioGenRecipe(RecipeExporter exporter, Ingredient A, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(A), List.of(), RecipeContent.BIO_GENERATOR, null, null);
        exporter.accept(Oritech.id("biogen" + suffix), entry, null);
    }
    
    private void addFuelGenRecipe(RecipeExporter exporter, FluidStack input, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(), List.of(), RecipeContent.FUEL_GENERATOR, input, null);
        exporter.accept(Oritech.id("fuelgen" + suffix), entry, null);
    }
    
    private void addLavaGen(RecipeExporter exporter, FluidStack input, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(), List.of(), RecipeContent.LAVA_GENERATOR, input, null);
        exporter.accept(Oritech.id("lavagen" + suffix), entry, null);
    }
    
    private void addSteamEngineGen(RecipeExporter exporter, FluidStack input, int timeInTicks, String suffix) {
        var entry = new OritechRecipe(timeInTicks, List.of(), List.of(), RecipeContent.STEAM_ENGINE, input, null);
        exporter.accept(Oritech.id("steamgen" + suffix), entry, null);
    }
    
    private void addMetalProcessingChain(RecipeExporter exporter, Ingredient oreInput, Ingredient rawOre, Item rawMain, Item rawSecondary, Item clump, Item smallClump,
                                         Item smallSecondaryClump, Item dust, Item smallDust, Item smallSecondaryDust, Item gem, Ingredient gemCatalyst, Item nugget,
                                         Item ingot, float timeMultiplier, String suffix, int byproductAmount) {
                
        // ore block -> raw ores
        var pulverizerOre = new OritechRecipe((int) (400 * timeMultiplier), List.of(oreInput), List.of(new ItemStack(rawMain, 2)), RecipeContent.PULVERIZER, null, null);
        var grinderOre = new OritechRecipe((int) (400 * timeMultiplier), List.of(oreInput), List.of(new ItemStack(rawMain, 2), new ItemStack(rawSecondary, 1)), RecipeContent.GRINDER, null, null);
        
        // raw ores -> dusts / clumps
        var pulverizerRaw = new OritechRecipe((int) (500 * timeMultiplier), List.of(rawOre), List.of(new ItemStack(dust, 1), new ItemStack(smallDust, 3)), RecipeContent.PULVERIZER, null, null);
        var grinderRaw = new OritechRecipe((int) (800 * timeMultiplier), List.of(rawOre), List.of(new ItemStack(clump, 1), new ItemStack(smallClump, 3), new ItemStack(smallSecondaryClump, byproductAmount)), RecipeContent.GRINDER, null, null);
        
        // clump processing
        var centrifugeClumpDry = new OritechRecipe((int) (400 * timeMultiplier), List.of(Ingredient.ofItems(clump)), List.of(new ItemStack(dust, 1), new ItemStack(smallSecondaryDust, byproductAmount)), RecipeContent.CENTRIFUGE, null, null);
        var centrifugeClumpWet = new OritechRecipe((int) (600 * timeMultiplier), List.of(Ingredient.ofItems(clump)), List.of(new ItemStack(dust, 2)), RecipeContent.CENTRIFUGE_FLUID, new FluidStack(Fluids.WATER, 81000), null);
        
        // gems
        var atomicForgeDust = new OritechRecipe(3, List.of(gemCatalyst, Ingredient.ofItems(dust), gemCatalyst), List.of(new ItemStack(gem, 1)), RecipeContent.ATOMIC_FORGE, null, null);
        var foundryGem = new OritechRecipe(800, List.of(Ingredient.ofItems(gem), Ingredient.ofItems(gem)), List.of(new ItemStack(ingot, 4)), RecipeContent.FOUNDRY, null, null);
        
        // smelting/compacting
        RecipeProvider.offerSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingot, 1f, 200, Oritech.MOD_ID);
        RecipeProvider.offerSmelting(exporter, List.of(smallDust), RecipeCategory.MISC, nugget, 0.5f, 50, Oritech.MOD_ID);
        RecipeProvider.offerBlasting(exporter, List.of(dust), RecipeCategory.MISC, ingot, 1, 100, Oritech.MOD_ID);
        RecipeProvider.offerBlasting(exporter, List.of(smallDust), RecipeCategory.MISC, nugget, 0.5f, 50, Oritech.MOD_ID);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, clump, smallClump);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, dust, smallDust);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, ingot, nugget);
        
        // registration
        exporter.accept(Oritech.id("pulverizerore" + suffix), pulverizerOre, null);
        exporter.accept(Oritech.id("grinderore" + suffix), grinderOre, null);
        exporter.accept(Oritech.id("pulverizerraw" + suffix), pulverizerRaw, null);
        exporter.accept(Oritech.id("grinderraw" + suffix), grinderRaw, null);
        exporter.accept(Oritech.id("centrifugeclumpdry" + suffix), centrifugeClumpDry, null);
        exporter.accept(Oritech.id("centrifugeclumpwet" + suffix), centrifugeClumpWet, null);
        exporter.accept(Oritech.id("atomicforgedust" + suffix), atomicForgeDust, null);
        exporter.accept(Oritech.id("foundrygem" + suffix), foundryGem, null);
        
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
    
    public void offerFurnaceRecipe(RecipeExporter exporter, Item output, Ingredient bottom, Ingredient botSides, Ingredient middleSides, Ingredient core, Ingredient top, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', botSides).input('c', core).input('f', top).input('b', bottom).input('m', middleSides)
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
    
    public void offerMachineCoreRecipe(RecipeExporter exporter, Item output, Ingredient base, Ingredient alt, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', base).input('c', alt)
                        .pattern("sss")
                        .pattern("scs")
                        .pattern("sss");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerDrillRecipe(RecipeExporter exporter, Item output, Ingredient core, Ingredient motor, Ingredient center, Ingredient head, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', core).input('m', motor).input('a', center).input('e', head)
                        .pattern(" a ")
                        .pattern("aea")
                        .pattern("mss");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerChainsawRecipe(RecipeExporter exporter, Item output, Ingredient core, Ingredient motor, Ingredient center, Ingredient head, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', core).input('m', motor).input('a', center).input('e', head)
                        .pattern("aa ")
                        .pattern("ae ")
                        .pattern("mss");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerAxeRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("pp ")
                        .pattern("pc ")
                        .pattern(" c ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerPickaxeRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("ppp")
                        .pattern(" c ")
                        .pattern(" c ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerHelmetRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("ppp")
                        .pattern("pcp")
                        .pattern("   ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerChestplateRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("p p")
                        .pattern("ppp")
                        .pattern("pcp");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerLegsRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("ppp")
                        .pattern("pcp")
                        .pattern("p p");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerFeetRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("   ")
                        .pattern("p p")
                        .pattern("c c");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerTankRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("ppp")
                        .pattern("pcp")
                        .pattern("ppp");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerTwoComponentRecipe(RecipeExporter exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('a', A).input('b', B)
                        .pattern("ab ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerLeverRecipe(RecipeExporter exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('a', A).input('b', B)
                        .pattern("a  ")
                        .pattern("b  ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerDoorRecipe(RecipeExporter exporter, Item output, Ingredient A, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, output, 1).input('a', A)
                        .pattern("aa ")
                        .pattern("aa ")
                        .pattern("aa ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
    
    public void offerMachinePlatingRecipe(RecipeExporter exporter, Item output, Ingredient side, Ingredient edge, Ingredient core, int count, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, output, count).input('a', side).input('e', edge).input('c', core)
                        .pattern("eae")
                        .pattern("aca")
                        .pattern("eae");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, getItemPath(output) + suffix);
    }
}
