package rearth.oritech.fabricgen.datagen;

import dev.architectury.fluid.FluidStack;
import me.jddev0.ep.EnergizedPowerMod;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.impl.resource.conditions.conditions.AllModsLoadedResourceCondition;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.server.recipe.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import nourl.mythicmetals.MythicMetals;
import rearth.oritech.Oritech;
import rearth.oritech.fabricgen.datagen.compat.AlloyForgeryRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.EnergizedPowerRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.MythicMetalsRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.TechRebornRecipeGenerator;
import rearth.oritech.init.*;
import rearth.oritech.init.recipes.AugmentRecipe;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.SizedIngredient;
import techreborn.TechReborn;
import wraith.alloyforgery.AlloyForgery;

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
        addParticleCollisions(exporter);
        addDusts(exporter);
        addDecorative(exporter);
        addVanillaAdditions(exporter);
        addDyes(exporter);
        addCompactingRecipes(exporter);
        addReactorFuels(exporter);
        addLaserTransformations(exporter);
        addUraniumProcessing(exporter);
        addReactorBlocks(exporter);
        addAugmentRecipes(exporter);
        
        TechRebornRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(TechReborn.MOD_ID))));
        EnergizedPowerRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(EnergizedPowerMod.MODID))));
        AlloyForgeryRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(AlloyForgery.MOD_ID))));
        MythicMetalsRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(MythicMetals.MOD_ID))));
    }
    
    private void addVanillaAdditions(RecipeExporter exporter) {
        
        // slimeball from honey and biomass
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.HONEYCOMB), Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.ofItems(ItemContent.BIOMASS), Items.SLIME_BALL, 1f, "slime");
        // fireball in assembler (gunpowder, blaze powder + coal) = 5 charges
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.GUNPOWDER), Ingredient.ofItems(Items.BLAZE_POWDER), Ingredient.fromTag(ItemTags.COALS), Ingredient.fromTag(ItemTags.COALS), Items.FIRE_CHARGE, 4, 1f, "fireball");
        // blaze rod (4 powder in assembler)
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.BLAZE_POWDER), Ingredient.ofItems(Items.BLAZE_POWDER), Ingredient.ofItems(Items.BLAZE_POWDER), Ingredient.ofItems(Items.BLAZE_POWDER), Items.BLAZE_ROD, 1f, "blazerod");
        // enderic compound from sculk
        addCentrifugeRecipe(exporter, Ingredient.ofItems(Items.SCULK), ItemContent.ENDERIC_COMPOUND, 4f, "endericsculk");
        // budding amethyst (amethyst shard x2, enderic compound, overcharged crystal)
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.AMETHYST_SHARD), Ingredient.ofItems(Items.AMETHYST_SHARD), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), Items.BUDDING_AMETHYST, 1f, "amethystbud");
        // netherite alloying (yes this is pretty OP)
        addAlloyRecipe(exporter, Items.GOLD_INGOT, Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT, "netherite");
        // books
        addAssemblerRecipe(exporter, Ingredient.ofItems(Items.PAPER), Ingredient.ofItems(Items.PAPER), Ingredient.ofItems(Items.PAPER), Ingredient.ofItems(Items.LEATHER), Items.BOOK, 2, 1f, "book");
        // reinforced deepslate
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(Items.DEEPSLATE), Items.REINFORCED_DEEPSLATE, 100, "reinfdeepslate");
        // cobblestone to gravel
        addPulverizerRecipe(exporter, Ingredient.fromTag(ConventionalItemTags.COBBLESTONES), Items.GRAVEL, "gravel");
        addGrinderRecipe(exporter, Ingredient.fromTag(ConventionalItemTags.COBBLESTONES), Items.GRAVEL, "gravel");
        // gravel to sand
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.GRAVEL), Items.SAND, "sand_from_gravel");
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.GRAVEL), Items.SAND, "sand_from_gravel");
        // sandstone to sand
        addPulverizerRecipe(exporter, Ingredient.fromTag(ConventionalItemTags.SANDSTONE_BLOCKS), Items.SAND, "sand_from_sandstone");
        addGrinderRecipe(exporter, Ingredient.fromTag(ConventionalItemTags.SANDSTONE_BLOCKS), Items.SAND, "sand_from_sandstone");
        // red sandstone to red sand
        addPulverizerRecipe(exporter, Ingredient.fromTag(ConventionalItemTags.RED_SANDSTONE_BLOCKS), Items.RED_SAND, "red_sand");
        addGrinderRecipe(exporter, Ingredient.fromTag(ConventionalItemTags.RED_SANDSTONE_BLOCKS), Items.RED_SAND, "red_sand");
        // centrifuge dirt into clay
        addCentrifugeFluidRecipe(exporter, Ingredient.fromTag(ItemTags.DIRT), Items.CLAY, Fluids.WATER, 0.25f, null, 0, 1.0f, "clay");
        // create dirt from sand + biomass
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Items.DIRT, 2).input('s', Items.SAND).input('b', ItemContent.BIOMASS).pattern("sb").pattern("bs").criterion(hasItem(ItemContent.BIOMASS), conditionsFromItem(ItemContent.BIOMASS)).offerTo(exporter);
        // dripstone from dripstone block
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.DRIPSTONE_BLOCK), Items.POINTED_DRIPSTONE, 4, "dripstone");
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.DRIPSTONE_BLOCK), Items.POINTED_DRIPSTONE, 4, "dripstone");
        // shroomlight from logs and 3 glowstone
        addAssemblerRecipe(exporter, Ingredient.fromTag(ItemTags.LOGS), Ingredient.ofItems(Items.GLOWSTONE), Ingredient.ofItems(Items.GLOWSTONE), Ingredient.ofItems(Items.GLOWSTONE), Items.SHROOMLIGHT, 1f, "shroomlight");
    }
    
    private void addDyes(RecipeExporter exporter) {
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_WHITE_DYE), Items.WHITE_DYE, "dyes/white");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_WHITE_DYE), Items.WHITE_DYE, "dyes/white");
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_LIGHT_GRAY_DYE), Items.LIGHT_GRAY_DYE, "dyes/light_gray");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_LIGHT_GRAY_DYE), Items.LIGHT_GRAY_DYE, "dyes/light_gray");
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_BLACK_DYE), Items.BLACK_DYE, "dyes/black");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_BLACK_DYE), Items.BLACK_DYE, "dyes/black");
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_RED_DYE), Items.RED_DYE, "dyes/red");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_RED_DYE), Items.RED_DYE, "dyes/red");
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_ORANGE_DYE), Items.ORANGE_DYE, "dyes/orange");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_ORANGE_DYE), Items.ORANGE_DYE, "dyes/orange");
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_YELLOW_DYE), Items.YELLOW_DYE, "dyes/yellow");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_YELLOW_DYE), Items.YELLOW_DYE, "dyes/yellow");
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_CYAN_DYE), Items.CYAN_DYE, "dyes/cyan");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_CYAN_DYE), Items.CYAN_DYE, "dyes/cyan");
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_BLUE_DYE), Items.BLUE_DYE, "dyes/blue");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_BLUE_DYE), Items.BLUE_DYE, "dyes/blue");
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_MAGENTA_DYE), Items.MAGENTA_DYE, "dyes/magenta");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_MAGENTA_DYE), Items.MAGENTA_DYE, "dyes/magenta");
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_PINK_DYE), Items.PINK_DYE, "dyes/pink");
        addGrinderRecipe(exporter, Ingredient.fromTag(TagContent.RAW_PINK_DYE), Items.PINK_DYE, "dyes/pink");
    }
    
    private void addDeepDrillOres(RecipeExporter exporter) {
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_REDSTONE, Items.REDSTONE, 1, "redstone");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_LAPIS, Items.LAPIS_LAZULI, 1, "lapis");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_IRON, Items.RAW_IRON, 1, "iron");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_COAL, Items.COAL, 1, "coal");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_COPPER, Items.RAW_COPPER, 1, "copper");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_GOLD, Items.RAW_GOLD, 1, "gold");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_EMERALD, Items.EMERALD, 1, "emerald");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_DIAMOND, Items.DIAMOND, 1, "diamond");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_NICKEL, ItemContent.RAW_NICKEL, 1, "nickel");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_PLATINUM, ItemContent.RAW_PLATINUM, 1, "platinum");
        addDeepDrillRecipe(exporter, BlockContent.RESOURCE_NODE_URANIUM, ItemContent.RAW_URANIUM, 1, "uranium");
    }
    
    private void addFuels(RecipeExporter exporter) {
        
        // bio
        addBioGenRecipe(exporter, Ingredient.fromTag(TagContent.BIOMASS), 15, "rawbio");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.PACKED_WHEAT), 200, "packedwheat");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.BIOMASS), 25, "biomass");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.SOLID_BIOFUEL), 160, "solidbiomass");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER), 300, "polymer");
        addBioGenRecipe(exporter, Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), 3000, "vex");
        // lava
        addLavaGen(exporter, FluidStack.create(Fluids.LAVA, 8100), 12, "lava");
        // fuel
        addFuelGenRecipe(exporter, FluidStack.create(FluidContent.STILL_OIL.get(), 8100), 3, "crude");
        addFuelGenRecipe(exporter, FluidStack.create(FluidContent.STILL_FUEL.get(), 8100), 12, "fuel");
        //steam
        addSteamEngineGen(exporter, FluidStack.create(FluidContent.STILL_STEAM.get(), 32), 1, "steameng");
    }
    
    private void addBiomass(RecipeExporter exporter) {
        // biomass
        addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.BIOMASS), ItemContent.BIOMASS, 1, "biobasic");
        addPulverizerRecipe(exporter, Ingredient.ofItems(ItemContent.PACKED_WHEAT), ItemContent.BIOMASS, 16, "packagedwheatbio");
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.HAY_BLOCK), ItemContent.BIOMASS, 16, "hay_block");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.ofItems(ItemContent.BIOMASS), Ingredient.fromTag(ItemTags.PLANKS), ItemContent.SOLID_BIOFUEL, 1, "solidbiofuel");
    }
    
    private void addEquipment(RecipeExporter exporter) {
        
        
        offerDrillRecipe(exporter, ToolsContent.HAND_DRILL, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), "handdrill");
        offerChainsawRecipe(exporter, ToolsContent.CHAINSAW, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), "chainsaw");
        offerAxeRecipe(exporter, ToolsContent.PROMETHIUM_AXE, Ingredient.ofItems(ItemContent.PROMETHEUM_INGOT), Ingredient.ofItems(BlockContent.DESTROYER_BLOCK.asItem()), "promaxe");
        offerPickaxeRecipe(exporter, ToolsContent.PROMETHIUM_PICKAXE, Ingredient.ofItems(ItemContent.PROMETHEUM_INGOT), Ingredient.ofItems(BlockContent.DESTROYER_BLOCK.asItem()), "prompick");
        
        // designator
        offerDrillRecipe(exporter, ItemContent.TARGET_DESIGNATOR, Ingredient.fromTag(TagContent.STEEL_INGOTS), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), of(TagContent.PLASTIC_PLATES), "designator");
        // weed killer
        offerDrillRecipe(exporter, ItemContent.WEED_KILLER, Ingredient.ofItems(Items.ROTTEN_FLESH), Ingredient.ofItems(Items.ROTTEN_FLESH), Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER), Ingredient.ofItems(Items.GLASS_BOTTLE), "weedex");
        // wrench
        offerWrenchRecipe(exporter, ItemContent.WRENCH, of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), "wrench");
        
        // helmet (enderic lens + machine plating)
        offerHelmetRecipe(exporter, ToolsContent.EXO_HELMET, Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ENDERIC_LENS), "exohelm");
        // chestplate (advanced battery + machine plating)
        offerChestplateRecipe(exporter, ToolsContent.EXO_CHESTPLATE, Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), "exochest");
        // legs (motor + plating)
        offerLegsRecipe(exporter, ToolsContent.EXO_LEGGINGS, Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), "exolegs");
        // feet (silicon + plating)
        offerFeetRecipe(exporter, ToolsContent.EXO_BOOTS, Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.fromTag(TagContent.SILICON), "exoboots");
        
        // basic jetpack main
        offerParticleMotorRecipe(exporter, ToolsContent.JETPACK, of(TagContent.STEEL_INGOTS), of(Items.LEATHER), of(ItemContent.ADVANCED_BATTERY), of(Items.GUNPOWDER), "basicjetpack");
        // jetpack alt
        offerParticleMotorRecipe(exporter, ToolsContent.JETPACK, of(TagContent.STEEL_INGOTS), of(Items.LEATHER), of(Items.REDSTONE_BLOCK), of(Items.BLAZE_POWDER), "basicjetpackalt");
        // exo jetpack
        offerGeneratorRecipe(exporter, ToolsContent.EXO_JETPACK, of(ToolsContent.JETPACK), of(BlockContent.SMALL_TANK_BLOCK), of(ToolsContent.EXO_CHESTPLATE), of(TagContent.PLASTIC_PLATES), "exojetpack");
        // boosted elytra
        offerGeneratorRecipe(exporter, ToolsContent.JETPACK_ELYTRA, of(Items.ELYTRA), of(ItemContent.PROCESSING_UNIT), of(ToolsContent.JETPACK), of(Items.GUNPOWDER), "boostedelytra");
        // exo elytra (exo jetpack + elytra)
        offerGeneratorRecipe(exporter, ToolsContent.JETPACK_EXO_ELYTRA, of(ToolsContent.EXO_JETPACK), of(ItemContent.PROCESSING_UNIT), of(Items.ELYTRA), of(Items.GUNPOWDER), "exoboostedelytra");
        // exo elytra (boosted elytra + exo chestplate)
        offerGeneratorRecipe(exporter, ToolsContent.JETPACK_EXO_ELYTRA, of(ToolsContent.EXO_CHESTPLATE), of(BlockContent.SMALL_TANK_BLOCK), of(ToolsContent.JETPACK_ELYTRA), of(TagContent.PLASTIC_PLATES), "exoboostedelytraalt");
        
    }
    
    private void addDecorative(RecipeExporter exporter) {
        // ceiling light
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.CEILING_LIGHT.asItem(), 6), Ingredient.ofItems(Items.GLOWSTONE_DUST), Ingredient.fromTag(TagContent.STEEL_INGOTS), "ceilightlight");
        // hanging light
        offerTwoComponentRecipe(exporter, BlockContent.CEILING_LIGHT_HANGING.asItem(), Ingredient.ofItems(Items.CHAIN), Ingredient.ofItems(BlockContent.CEILING_LIGHT.asItem()), "hanginglight");
        // tech button
        offerLeverRecipe(exporter, BlockContent.TECH_BUTTON.asItem(), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.fromTag(TagContent.STEEL_INGOTS), "techbutton");
        // tech lever
        offerLeverRecipe(exporter, BlockContent.TECH_LEVER.asItem(), Ingredient.fromTag(TagContent.CARBON_FIBRE), Ingredient.fromTag(TagContent.STEEL_INGOTS), "techlever");
        // tech door
        offerDoorRecipe(exporter, BlockContent.TECH_DOOR.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), "techdoor");
        // metal beam
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.METAL_BEAM_BLOCK.asItem(), 6), Ingredient.fromTag(TagContent.CARBON_FIBRE), Ingredient.fromTag(TagContent.STEEL_INGOTS), "metalbeams");
        // tech glass
        offerMachinePlatingRecipe(exporter, BlockContent.INDUSTRIAL_GLASS_BLOCK.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(ConventionalItemTags.GLASS_BLOCKS), Ingredient.fromTag(TagContent.MACHINE_PLATING), 4, "industrialglass");
        // machine plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.MACHINE_PLATING_SLAB.asItem(), Ingredient.ofItems(BlockContent.MACHINE_PLATING_BLOCK.asItem()), "machine");
        offerStairsRecipe(exporter, BlockContent.MACHINE_PLATING_STAIRS.asItem(), Ingredient.ofItems(BlockContent.MACHINE_PLATING_BLOCK.asItem()), "machine");
        offerPressurePlateRecipe(exporter, BlockContent.MACHINE_PLATING_PRESSURE_PLATE.asItem(), Ingredient.ofItems(BlockContent.MACHINE_PLATING_BLOCK.asItem()), "machine");
        // iron plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.IRON_PLATING_SLAB.asItem(), Ingredient.ofItems(BlockContent.IRON_PLATING_BLOCK.asItem()), "iron");
        offerStairsRecipe(exporter, BlockContent.IRON_PLATING_STAIRS.asItem(), Ingredient.ofItems(BlockContent.IRON_PLATING_BLOCK.asItem()), "iron");
        offerPressurePlateRecipe(exporter, BlockContent.IRON_PLATING_PRESSURE_PLATE.asItem(), Ingredient.ofItems(BlockContent.IRON_PLATING_BLOCK.asItem()), "iron");
        // nickel plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.NICKEL_PLATING_SLAB.asItem(), Ingredient.ofItems(BlockContent.NICKEL_PLATING_BLOCK.asItem()), "nickel");
        offerStairsRecipe(exporter, BlockContent.NICKEL_PLATING_STAIRS.asItem(), Ingredient.ofItems(BlockContent.NICKEL_PLATING_BLOCK.asItem()), "nickel");
        offerPressurePlateRecipe(exporter, BlockContent.NICKEL_PLATING_PRESSURE_PLATE.asItem(), Ingredient.ofItems(BlockContent.NICKEL_PLATING_BLOCK.asItem()), "nickel");
    }
    
    private void addMachines(RecipeExporter exporter) {
        // basic generator
        offerGeneratorRecipe(exporter, BlockContent.BASIC_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(Blocks.FURNACE.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.fromTag(TagContent.NICKEL_INGOTS), "basicgen");
        // pulverizer
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), Ingredient.ofItems(Blocks.IRON_BLOCK.asItem()), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "pulverizer");
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(Items.IRON_INGOT), "pulverizeralt");
        // electric furnace
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), Ingredient.ofItems(Blocks.FURNACE.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.SILICON), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(Items.COPPER_INGOT), "electricfurnace");
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), Ingredient.ofItems(Blocks.FURNACE.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.PLATINUM_INGOTS), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(Items.COPPER_INGOT), "electricfurnacealt");
        // assembler
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), Ingredient.ofItems(Blocks.BLAST_FURNACE.asItem()), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.CRAFTER), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.COPPER_INGOT), "assembler");
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.CRAFTER), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.COPPER_INGOT), "assembleralt");
        // foundry
        offerGeneratorRecipe(exporter, BlockContent.FOUNDRY_BLOCK.asItem(), Ingredient.ofItems(Blocks.CAULDRON.asItem()), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.COPPER_INGOT), "foundry");
        // cooler
        offerGeneratorRecipe(exporter, BlockContent.COOLER_BLOCK.asItem(), Ingredient.ofItems(Blocks.CAULDRON.asItem()), of(Blocks.ICE.asItem()), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.IRON_INGOT), "cooler");
        // centrifuge
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Items.GLASS_BOTTLE), "centrifuge");
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.IRON_BLOCK), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.GLASS_BOTTLE), "centrifugealt");
        // laser arm
        offerAtomicForgeRecipe(exporter, BlockContent.LASER_ARM_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(ItemContent.ENDERIC_LENS), Ingredient.fromTag(TagContent.CARBON_FIBRE), "laserarm");
        // crusher
        offerGeneratorRecipe(exporter, BlockContent.FRAGMENT_FORGE_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.FLUX_GATE), of(TagContent.PLASTIC_PLATES), "crusher");
        // atomic forge
        offerAtomicForgeRecipe(exporter, BlockContent.ATOMIC_FORGE_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), of(TagContent.PLASTIC_PLATES), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(ItemContent.FLUX_GATE), "atomicforge");
        
        // biofuel generator
        offerGeneratorRecipe(exporter, BlockContent.BIO_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT), "biogen");
        // lava generator
        offerGeneratorRecipe(exporter, BlockContent.LAVA_GENERATOR_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), of(TagContent.ELECTRUM_INGOTS), "lavagen");
        // steam engine
        offerGeneratorRecipe(exporter, BlockContent.STEAM_ENGINE_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), of(TagContent.ELECTRUM_INGOTS), "steamgen");
        // diesel generator
        offerGeneratorRecipe(exporter, BlockContent.FUEL_GENERATOR_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK), Ingredient.ofItems(ItemContent.ENDERIC_LENS), Ingredient.fromTag(TagContent.STEEL_INGOTS), "fuelgen");
        // large solar
        offerGeneratorRecipe(exporter, BlockContent.BIG_SOLAR_PANEL_BLOCK.asItem(), Ingredient.ofItems(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), Ingredient.ofItems(ItemContent.FLUXITE), "solar");
        
        // charger
        offerAtomicForgeRecipe(exporter, BlockContent.CHARGER_BLOCK.asItem(), of(Items.CHEST), of(BlockContent.ENERGY_PIPE), of(Items.REDSTONE_BLOCK), of(Items.DISPENSER), of(TagContent.STEEL_INGOTS), "charger");
        offerAtomicForgeRecipe(exporter, BlockContent.CHARGER_BLOCK.asItem(), of(Items.CHEST), of(BlockContent.ENERGY_PIPE), of(ItemContent.PROCESSING_UNIT), of(Items.DISPENSER), of(TagContent.STEEL_INGOTS), "chargeralt");
        
        // small storage
        offerAtomicForgeRecipe(exporter, BlockContent.SMALL_STORAGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.BASIC_BATTERY), Ingredient.fromTag(TagContent.SILICON), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.WIRES), "smallstorage");
        // large storage
        offerAtomicForgeRecipe(exporter, BlockContent.LARGE_STORAGE_BLOCK.asItem(), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.DUBIOS_CONTAINER), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.fromTag(TagContent.WIRES), "bigstorage");
        // unstable container
        offerAtomicForgeRecipe(exporter, ItemContent.UNSTABLE_CONTAINER, Ingredient.ofItems(ItemContent.FLUXITE), of(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(BlockContent.LARGE_STORAGE_BLOCK), Ingredient.ofItems(ItemContent.FLUX_GATE), of(ItemContent.SUPER_AI_CHIP), "unstablecontainer");
        
        // fluid tank
        offerTankRecipe(exporter, BlockContent.SMALL_TANK_BLOCK.asItem(), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.fromTag(ConventionalItemTags.GLASS_BLOCKS), Ingredient.ofItems(BlockContent.FLUID_PIPE.asItem()), "stank");
        // pump
        offerGeneratorRecipe(exporter, BlockContent.PUMP_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.fromTag(TagContent.SILICON), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.COPPER_INGOT), "pump");
        // block placer
        offerFurnaceRecipe(exporter, BlockContent.PLACER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(BlockContent.MACHINE_FRAME_BLOCK.asItem()), Ingredient.ofItems(Items.COPPER_INGOT), "placer");
        // block destroyer
        offerAtomicForgeRecipe(exporter, BlockContent.DESTROYER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(BlockContent.PULVERIZER_BLOCK), Ingredient.ofItems(BlockContent.LASER_ARM_BLOCK), Ingredient.ofItems(ItemContent.MOTOR), "destroyer");
        // fertilizer
        offerFurnaceRecipe(exporter, BlockContent.FERTILIZER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(TagContent.SILICON), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(Items.COPPER_INGOT), "fertilizer");
        // tree feller
        offerGeneratorRecipe(exporter, BlockContent.TREEFELLER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(Items.IRON_AXE), Ingredient.ofItems(ItemContent.MOTOR), of(TagContent.ELECTRUM_INGOTS), "treefeller");
        // pipe booster
        offerTankRecipe(exporter, BlockContent.PIPE_BOOSTER_BLOCK.asItem(), of(BlockContent.ITEM_PIPE), of(ItemContent.MOTOR), of(BlockContent.FLUID_PIPE), "booster");
        
        // machine frame
        offerMachineFrameRecipe(exporter, BlockContent.MACHINE_FRAME_BLOCK.asItem(), Ingredient.ofItems(Items.IRON_BARS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), 16, "frame");
        // energy pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE.asItem(), 6), of(TagContent.ELECTRUM_INGOTS), Ingredient.fromTag(TagContent.WIRES), "energy");
        // item pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE.asItem(), 6), of(TagContent.NICKEL_INGOTS), Ingredient.fromTag(ItemTags.PLANKS), "item");
        // item filter
        offerGeneratorRecipe(exporter, BlockContent.ITEM_FILTER_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.fromTag(TagContent.WIRES), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.fromTag(TagContent.WIRES), "itemfilter");
        // fluid pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE.asItem(), 6), Ingredient.fromTag(TagContent.SILICON), Ingredient.ofItems(Items.COPPER_INGOT), "fluidpipe");
        
        // framed energy pipe
        offerFramedCableRecipe(exporter, new ItemStack(BlockContent.FRAMED_ENERGY_PIPE, 8), Ingredient.ofItems(BlockContent.ENERGY_PIPE), "energy");
        offerCableFromFrameRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE, 1), Ingredient.ofItems(BlockContent.FRAMED_ENERGY_PIPE), "energy");
        // framed superconductor
        offerFramedCableRecipe(exporter, new ItemStack(BlockContent.FRAMED_SUPERCONDUCTOR, 8), Ingredient.ofItems(BlockContent.SUPERCONDUCTOR.asItem()), "superconductor");
        offerCableFromFrameRecipe(exporter, new ItemStack(BlockContent.SUPERCONDUCTOR.asItem(), 1), Ingredient.ofItems(BlockContent.FRAMED_SUPERCONDUCTOR), "superconductor");
        // framed fluid pipe
        offerFramedCableRecipe(exporter, new ItemStack(BlockContent.FRAMED_FLUID_PIPE, 8), Ingredient.ofItems(BlockContent.FLUID_PIPE), "fluid");
        offerCableFromFrameRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE, 1), Ingredient.ofItems(BlockContent.FRAMED_FLUID_PIPE), "fluid");
        // framed item pipe
        offerFramedCableRecipe(exporter, new ItemStack(BlockContent.FRAMED_ITEM_PIPE, 8), Ingredient.ofItems(BlockContent.ITEM_PIPE), "item");
        offerCableFromFrameRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE, 1), Ingredient.ofItems(BlockContent.FRAMED_ITEM_PIPE), "item");
        
        // transparent pipe
        offerTankRecipe(exporter, BlockContent.TRANSPARENT_ITEM_PIPE.asItem(), 6, of(ItemTags.PLANKS), of(TagContent.NICKEL_INGOTS), of(ConventionalItemTags.GLASS_BLOCKS), "transparentitem");
        offerMachineCoreRecipe(exporter, BlockContent.TRANSPARENT_ITEM_PIPE.asItem(), 8, of(BlockContent.ITEM_PIPE), of(ConventionalItemTags.GLASS_BLOCKS), "totransparent");
        offerMachineCoreRecipe(exporter, BlockContent.ITEM_PIPE.asItem(), 8, of(BlockContent.TRANSPARENT_ITEM_PIPE), of(ItemTags.PLANKS), "fromtransparent");
        
        // energy pipe duct
        offerCableDuctRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE_DUCT_BLOCK, 4), Ingredient.ofItems(BlockContent.ENERGY_PIPE), "energy");
        offerCableFromDuctRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE, 1), Ingredient.ofItems(BlockContent.ENERGY_PIPE_DUCT_BLOCK), "energy");
        // superconductor duct
        offerCableDuctRecipe(exporter, new ItemStack(BlockContent.SUPERCONDUCTOR_DUCT_BLOCK, 4), Ingredient.ofItems(BlockContent.SUPERCONDUCTOR.asItem()), "superconductor");
        offerCableFromDuctRecipe(exporter, new ItemStack(BlockContent.SUPERCONDUCTOR.asItem(), 1), Ingredient.ofItems(BlockContent.SUPERCONDUCTOR_DUCT_BLOCK), "superconductor");
        // fluid pipe duct
        offerCableDuctRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE_DUCT_BLOCK, 4), Ingredient.ofItems(BlockContent.FLUID_PIPE), "fluid");
        offerCableFromDuctRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE, 1), Ingredient.ofItems(BlockContent.FLUID_PIPE_DUCT_BLOCK), "fluid");
        // item pipe duct
        offerCableDuctRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE_DUCT_BLOCK, 4), Ingredient.ofItems(BlockContent.ITEM_PIPE), "item");
        offerCableFromDuctRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE, 1), Ingredient.ofItems(BlockContent.ITEM_PIPE_DUCT_BLOCK), "item");
        
        // deep drill
        offerAtomicForgeRecipe(exporter, BlockContent.DEEP_DRILL_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.HEISENBERG_COMPENSATOR), Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), "deepdrill");
        // drone port
        offerAtomicForgeRecipe(exporter, BlockContent.DRONE_PORT_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(BlockContent.SUPERCONDUCTOR.asItem()), Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), "droneport");
        offerAtomicForgeRecipe(exporter, BlockContent.DRONE_PORT_BLOCK.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(BlockContent.SUPERCONDUCTOR.asItem()), Ingredient.ofItems(ItemContent.SUPER_AI_CHIP), Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), "droneportalt");
        
        // arcane catalyst
        offerFurnaceRecipe(exporter, BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem(), Ingredient.ofItems(Items.ENCHANTING_TABLE), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.OBSIDIAN), Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), Ingredient.ofItems(ItemContent.FLUXITE), "catalyst");
        offerFurnaceRecipe(exporter, BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem(), Ingredient.ofItems(Items.ENCHANTING_TABLE), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.OBSIDIAN), Ingredient.ofItems(ItemContent.SUPER_AI_CHIP), Ingredient.ofItems(ItemContent.FLUXITE), "catalyst_alt");
        // enchanter
        offerGeneratorRecipe(exporter, BlockContent.ENCHANTER_BLOCK.asItem(), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), Ingredient.ofItems(BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem()), Ingredient.ofItems(Items.BOOK), "enchanter");
        // spawner
        offerTankRecipe(exporter, BlockContent.SPAWNER_CONTROLLER_BLOCK.asItem(), Ingredient.ofItems(BlockContent.SPAWNER_CAGE_BLOCK), Ingredient.ofItems(Blocks.RESPAWN_ANCHOR), Ingredient.ofItems(BlockContent.ENCHANTMENT_CATALYST_BLOCK), "spawner");
        // spawner cage
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.SPAWNER_CAGE_BLOCK, 2), of(TagContent.PLASTIC_PLATES), Ingredient.ofItems(Items.IRON_BARS), "cage");
        // withered rose
        offerMachineFrameRecipe(exporter, BlockContent.WITHER_CROP_BLOCK.asItem(), Ingredient.ofItems(Items.WITHER_ROSE), Ingredient.fromTag(ItemTags.FLOWERS), 1, "witherrose");
        
        // particle accelerator
        // motor
        offerParticleMotorRecipe(exporter, BlockContent.ACCELERATOR_MOTOR.asItem(), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(BlockContent.SUPERCONDUCTOR.asItem()), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), "particlemotor");
        // ring
        offerDrillRecipe(exporter, BlockContent.ACCELERATOR_RING.asItem(), Ingredient.ofItems(BlockContent.INDUSTRIAL_GLASS_BLOCK.asItem()), Ingredient.ofItems(BlockContent.SUPERCONDUCTOR.asItem()), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Items.REDSTONE_TORCH), "acceleratorring");
        // controller
        offerGeneratorRecipe(exporter, BlockContent.ACCELERATOR_CONTROLLER.asItem(), Ingredient.ofItems(BlockContent.ACCELERATOR_MOTOR.asItem()), Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.ofItems(Items.DROPPER), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), "particlecontroller");
        // sensor
        offerTwoComponentRecipe(exporter, BlockContent.ACCELERATOR_SENSOR.asItem(), Ingredient.ofItems(BlockContent.ACCELERATOR_RING.asItem()), Ingredient.ofItems(Items.OBSERVER), "particlesensor");
        // collector
        offerTankRecipe(exporter, BlockContent.PARTICLE_COLLECTOR_BLOCK.asItem(), of(BlockContent.SUPERCONDUCTOR.asItem()), of(BlockContent.BIG_SOLAR_PANEL_BLOCK.asItem()), of(ItemContent.HEISENBERG_COMPENSATOR), "particlecollector");
        
        
        // addons
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_SPEED_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT), of(TagContent.PLASTIC_PLATES), "addon/speed");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_PROCESSING_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.SUPER_AI_CHIP), of(ItemContent.FLUX_GATE), of(TagContent.PLATINUM_INGOTS), of(ItemContent.MOTOR), "addon/processing");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_PROCESSING_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.UNHOLY_INTELLIGENCE), of(Items.COMPARATOR), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.MOTOR), "addon/processingalt");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_ULTIMATE_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.HEISENBERG_COMPENSATOR), of(BlockContent.MACHINE_SPEED_ADDON), of(BlockContent.MACHINE_EFFICIENCY_ADDON), of(ItemContent.OVERCHARGED_CRYSTAL), "addon/ultimate");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_EFFICIENCY_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.fromTag(TagContent.CARBON_FIBRE), of(TagContent.ELECTRUM_INGOTS), of(TagContent.PLASTIC_PLATES), "addon/eff");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_CAPACITOR_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), of(TagContent.PLASTIC_PLATES), "addon/capacitor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_ACCEPTOR_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), of(TagContent.PLASTIC_PLATES), "addon/acceptor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_YIELD_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(ItemContent.ENDERIC_LENS), of(TagContent.PLASTIC_PLATES), "addon/yield");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_FLUID_ADDON.asItem(), Ingredient.fromTag(TagContent.SILICON), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(BlockContent.FLUID_PIPE), Ingredient.fromTag(TagContent.CARBON_FIBRE), "addon/fluid");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_INVENTORY_PROXY_ADDON.asItem(), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.fromTag(ConventionalItemTags.CHESTS), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.fromTag(TagContent.CARBON_FIBRE), "addon/invproxy");
        offerGeneratorRecipe(exporter, BlockContent.CROP_FILTER_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.fromTag(TagContent.CARBON_FIBRE), "addon/cropfilter");
        offerGeneratorRecipe(exporter, BlockContent.QUARRY_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.DIAMOND_PICKAXE), of(TagContent.PLASTIC_PLATES), "addon/quarry");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_HUNTER_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.MOTOR), Ingredient.ofItems(Items.IRON_SWORD), of(TagContent.PLASTIC_PLATES), "_hunter");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(BlockContent.FLUID_PIPE), "addon/steamboiler");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), Ingredient.fromTag(TagContent.SILICON), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(BlockContent.FLUID_PIPE), Ingredient.ofItems(ItemContent.COAL_DUST), "addon/steamboileralt");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_REDSTONE_ADDON.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(Items.REPEATER), Ingredient.ofItems(Items.COMPARATOR), Ingredient.ofItems(Items.REDSTONE), "addon/redstone");
        offerTwoComponentRecipe(exporter, BlockContent.CAPACITOR_ADDON_EXTENDER.asItem(), Ingredient.ofItems(BlockContent.MACHINE_EXTENDER.asItem()), Ingredient.ofItems(BlockContent.MACHINE_CAPACITOR_ADDON), "addon/capextender");
        
        // cores
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_1.asItem(), Ingredient.fromTag(ItemTags.PLANKS), Ingredient.ofItems(Items.CRAFTING_TABLE), "core1");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.ofItems(Items.LAPIS_LAZULI), "core2");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), Ingredient.ofItems(Items.IRON_INGOT), Ingredient.ofItems(Items.LAPIS_LAZULI), "core2alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), Ingredient.fromTag(TagContent.CARBON_FIBRE), Ingredient.ofItems(Items.REDSTONE), "core3");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(Items.REDSTONE), "core3alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_4.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), "core4");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_5.asItem(), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), "core5");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_6.asItem(), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), Ingredient.ofItems(ItemContent.DUBIOS_CONTAINER), "core6");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_7.asItem(), Ingredient.ofItems(ItemContent.PROMETHEUM_INGOT), Ingredient.ofItems(BlockContent.SUPERCONDUCTOR.asItem()), "core7");
        
        // machine extender
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_EXTENDER.asItem(), Ingredient.fromTag(TagContent.MACHINE_PLATING), Ingredient.ofItems(BlockContent.MACHINE_CORE_2.asItem()), "extender");
        
        // augmenter
        // machine itself
        offerAtomicForgeRecipe(exporter, BlockContent.AUGMENT_APPLICATION_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(BlockContent.LARGE_STORAGE_BLOCK.asItem()), of(TagContent.CARBON_FIBRE), of(ItemContent.DUBIOS_CONTAINER), "augment/applicator");
        // basic station
        offerGeneratorRecipe(exporter, BlockContent.SIMPLE_AUGMENT_STATION.asItem(), of(Items.BREWING_STAND), of(TagContent.MACHINE_PLATING), of(Items.REDSTONE_BLOCK), of(TagContent.ELECTRUM_INGOTS), "augment/basic");
        // adv station
        offerGeneratorRecipe(exporter, BlockContent.ADVANCED_AUGMENT_STATION.asItem(), of(BlockContent.CENTRIFUGE_BLOCK), of(TagContent.MACHINE_PLATING), of(ItemContent.FLUX_GATE), of(ItemContent.DURATIUM_INGOT), "augment/advanced");
        // arcane station
        offerGeneratorRecipe(exporter, BlockContent.ARCANE_AUGMENT_STATION.asItem(), of(Items.ENDER_EYE), of(TagContent.MACHINE_PLATING), of(ItemContent.ENDERIC_LENS), of(ItemContent.OVERCHARGED_CRYSTAL), "augment/arcane");
        
    }
    
    private void addComponents(RecipeExporter exporter) {
        // coal stuff (including basic steel)
        addCentrifugeRecipe(exporter, Ingredient.fromTag(TagContent.COAL_DUSTS), ItemContent.CARBON_FIBRE_STRANDS, 0.5f, "carbon");
        offerManualAlloyRecipe(exporter, ItemContent.STEEL_INGOT, Ingredient.ofItems(Items.IRON_INGOT), Ingredient.ofItems(Items.COAL), "steel");
        
        // manual alloys
        offerManualAlloyRecipe(exporter, ItemContent.ELECTRUM_INGOT, Ingredient.ofItems(Items.GOLD_INGOT), Ingredient.ofItems(Items.REDSTONE), "electrum");
        offerManualAlloyRecipe(exporter, ItemContent.ADAMANT_INGOT, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(Items.DIAMOND), "adamant");
        
        // enderic entry
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.ENDER_PEARL), ItemContent.ENDERIC_COMPOUND, 8, "pearl_enderic");
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.ENDER_PEARL), ItemContent.ENDERIC_COMPOUND, 12, "pearl_enderic");
        addGrinderRecipe(exporter, Ingredient.ofItems(Blocks.END_STONE), ItemContent.ENDERIC_COMPOUND, 1, "stone_enderic");
        
        // fine wires
        offerCableRecipe(exporter, new ItemStack(ItemContent.INSULATED_WIRE, 4), Ingredient.fromTag(TagContent.NICKEL_INGOTS), "insulatedwire");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(Items.COPPER_INGOT), ItemContent.INSULATED_WIRE, 12, 0.5f, "fwire");
        
        // magnetic coils
        offerInsulatedCableRecipe(exporter, new ItemStack(ItemContent.MAGNETIC_COIL, 2), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.WIRES), "magnet");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.WIRES), Ingredient.fromTag(TagContent.WIRES), Ingredient.fromTag(TagContent.WIRES), ItemContent.MAGNETIC_COIL, 2, 0.5f, "magnet");
        
        // motor
        offerMotorRecipe(exporter, ItemContent.MOTOR, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.fromTag(TagContent.STEEL_INGOTS), "motorcraft");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), Ingredient.ofItems(ItemContent.MAGNETIC_COIL), ItemContent.MOTOR, 2, 0.5f, "motor");
        
        // machine plating variants
        offerMachinePlatingRecipe(exporter, BlockContent.MACHINE_PLATING_BLOCK.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Blocks.STONE.asItem()), Ingredient.ofItems(Items.COPPER_INGOT), 2, "plating");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Items.COPPER_INGOT), of(TagContent.PLASTIC_PLATES), BlockContent.MACHINE_PLATING_BLOCK.asItem(), 8, 1f, "plating");
        offerMachinePlatingRecipe(exporter, BlockContent.IRON_PLATING_BLOCK.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Blocks.STONE.asItem()), Ingredient.ofItems(Items.IRON_INGOT), 2, "iron");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Items.IRON_INGOT), of(TagContent.PLASTIC_PLATES), BlockContent.IRON_PLATING_BLOCK.asItem(), 8, 1f, "platingiron");
        offerMachinePlatingRecipe(exporter, BlockContent.NICKEL_PLATING_BLOCK.asItem(), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.ofItems(Blocks.STONE.asItem()), Ingredient.fromTag(TagContent.NICKEL_INGOTS), 2, "nickel");
        addAssemblerRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), Ingredient.fromTag(TagContent.NICKEL_INGOTS), of(TagContent.PLASTIC_PLATES), BlockContent.NICKEL_PLATING_BLOCK.asItem(), 8, 1f, "platingnickel");
        
        // basic battery
        offerMotorRecipe(exporter, ItemContent.BASIC_BATTERY, Ingredient.fromTag(TagContent.STEEL_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(TagContent.PLASTIC_PLATES), "manualbattery");
        addAssemblerRecipe(exporter, of(TagContent.PLASTIC_PLATES), of(TagContent.ELECTRUM_INGOTS), of(TagContent.ELECTRUM_INGOTS), Ingredient.fromTag(TagContent.STEEL_INGOTS), ItemContent.BASIC_BATTERY, 1, 0.5f, "battery");
        addAssemblerRecipe(exporter, of(TagContent.PLASTIC_PLATES), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.fromTag(TagContent.STEEL_INGOTS), ItemContent.BASIC_BATTERY, 2, 1f, "batterybetter");
        
        // silicon
        offerManualAlloyRecipe(exporter, ItemContent.RAW_SILICON, Ingredient.fromTag(TagContent.QUARTZ_DUSTS), Ingredient.fromTag(ItemTags.SAND), 3, "rawsilicon");
        offerSmelting(exporter, List.of(ItemContent.RAW_SILICON), RecipeCategory.MISC, ItemContent.SILICON, 0.5f, 60, "siliconfurnace");
        
        // plastic
        offer2x2CompactingRecipe(exporter, RecipeCategory.MISC, ItemContent.PACKED_WHEAT, Items.WHEAT);
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.PACKED_WHEAT), ItemContent.RAW_BIOPOLYMER, Fluids.WATER, 0.25f, null, 0, 1f, "biopolymer");
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.SOLID_BIOFUEL), ItemContent.RAW_BIOPOLYMER, Fluids.WATER, 0.25f, null, 0, 1f, "biopolymer_biomass");
        addCentrifugeFluidRecipe(exporter, Ingredient.fromTag(ItemTags.SAND), ItemContent.POLYMER_RESIN, FluidContent.STILL_OIL.get(), 0.1f, null, 0, 0.5f, "polymerresin");
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER), ItemContent.PLASTIC_SHEET, Fluids.WATER, 0.5f, null, 0, 1f, "plasticoil");
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.POLYMER_RESIN), ItemContent.PLASTIC_SHEET, Fluids.WATER, 0.5f, null, 0, 0.33f, "plasticbio");
        
        // processing unit
        addAssemblerRecipe(exporter, of(TagContent.PLASTIC_PLATES), Ingredient.fromTag(TagContent.CARBON_FIBRE), of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(Items.REDSTONE), ItemContent.PROCESSING_UNIT, 1f, "processingunit");
        // enderic lens
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.fromTag(TagContent.CARBON_FIBRE), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), ItemContent.ENDERIC_LENS, 1.5f, "enderlens");
        // flux gate
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.PROCESSING_UNIT), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.ofItems(ItemContent.FLUXITE), Ingredient.fromTag(TagContent.PLATINUM_INGOTS), ItemContent.FLUX_GATE, 1.5f, "fluxgate");
        
        // ai processor tree
        addAtomicForgeRecipe(exporter, Ingredient.fromTag(TagContent.SILICON), Ingredient.fromTag(TagContent.CARBON_FIBRE), ItemContent.SILICON_WAFER, 5, "wafer");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.SILICON_WAFER), Ingredient.ofItems(ItemContent.PROCESSING_UNIT), ItemContent.ADVANCED_COMPUTING_ENGINE, 5, "advcomputer");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.ADVANCED_COMPUTING_ENGINE), Ingredient.ofItems(ItemContent.DURATIUM_INGOT), ItemContent.SUPER_AI_CHIP, 50, "aicomputer");
        
        // dubios container
        offerMotorRecipe(exporter, ItemContent.DUBIOS_CONTAINER, of(TagContent.PLASTIC_PLATES), Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.ENDERIC_COMPOUND), "dubios");
        // adv battery
        offerMotorRecipe(exporter, ItemContent.ADVANCED_BATTERY, of(TagContent.ELECTRUM_INGOTS), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), Ingredient.fromTag(TagContent.STEEL_INGOTS), "advbattery");
        
        // fuel
        addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.FLUXITE), null, FluidContent.STILL_OIL.get(), 1f, FluidContent.STILL_FUEL.get(), 1f, 1f, "fuel");
        
        // biosteel
        addAlloyRecipe(exporter, ItemContent.RAW_BIOPOLYMER, Items.IRON_INGOT, ItemContent.BIOSTEEL_INGOT, "biosteel");
        
        // endgame components
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.SUPER_AI_CHIP), ItemContent.HEISENBERG_COMPENSATOR, 60, "compensator");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), Ingredient.ofItems(ItemContent.UNHOLY_INTELLIGENCE), ItemContent.HEISENBERG_COMPENSATOR, 60, "compensatoralt");
        offerMotorRecipe(exporter, ItemContent.OVERCHARGED_CRYSTAL, Ingredient.ofItems(Items.AMETHYST_BLOCK), Ingredient.ofItems(ItemContent.ADVANCED_BATTERY), Ingredient.ofItems(BlockContent.SUPERCONDUCTOR.asItem()), "overchargedcrystal");
        addAssemblerRecipe(exporter, Ingredient.ofItems(ItemContent.FLUX_GATE), Ingredient.fromTag(TagContent.WIRES), Ingredient.ofItems(ItemContent.DUBIOS_CONTAINER), Ingredient.ofItems(ItemContent.ENERGITE_INGOT), BlockContent.SUPERCONDUCTOR.asItem(), 3, 2f, "superconductor");
        addAtomicForgeRecipe(exporter, Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), Ingredient.ofItems(ItemContent.HEISENBERG_COMPENSATOR), ItemContent.PROMETHEUM_INGOT, 240, "prometheum");
        
        // ice in cooler
        addCoolerRecipe(exporter, FluidStack.create(Fluids.WATER, FluidConstants.BUCKET), Items.ICE, 3, 1f, "ice");
        
        // snow from steam in cooler
        addCoolerRecipe(exporter, FluidStack.create(FluidContent.STILL_STEAM.get(), FluidConstants.BUCKET), Items.SNOW_BLOCK, 3, 1f, "snow");
        
    }
    
    private void addCompactingRecipes(RecipeExporter exporter) {
        addCompactingRecipe(exporter, BlockContent.STEEL_BLOCK, ItemContent.STEEL_INGOT, of(ItemTagGenerator.getIngotTag("steel")), of(ItemTagGenerator.getStorageBlockTag("steel")));
        addCompactingRecipe(exporter, BlockContent.ENERGITE_BLOCK, ItemContent.ENERGITE_INGOT, of(ItemTagGenerator.getIngotTag("energite")), of(ItemTagGenerator.getStorageBlockTag("energite")));
        addCompactingRecipe(exporter, BlockContent.NICKEL_BLOCK, ItemContent.NICKEL_INGOT, of(ItemTagGenerator.getIngotTag("nickel")), of(ItemTagGenerator.getStorageBlockTag("nickel")));
        addCompactingRecipe(exporter, BlockContent.BIOSTEEL_BLOCK, ItemContent.BIOSTEEL_INGOT, of(ItemContent.BIOSTEEL_INGOT), of(ItemTagGenerator.getStorageBlockTag("biosteel")));
        addCompactingRecipe(exporter, BlockContent.PLATINUM_BLOCK, ItemContent.PLATINUM_INGOT, of(ItemTagGenerator.getIngotTag("platinum")), of(ItemTagGenerator.getStorageBlockTag("platinum")));
        addCompactingRecipe(exporter, BlockContent.ADAMANT_BLOCK, ItemContent.ADAMANT_INGOT, of(ItemTagGenerator.getIngotTag("adamant")), of(ItemTagGenerator.getStorageBlockTag("adamant")));
        addCompactingRecipe(exporter, BlockContent.ELECTRUM_BLOCK, ItemContent.ELECTRUM_INGOT, of(ItemTagGenerator.getIngotTag("electrum")), of(ItemTagGenerator.getStorageBlockTag("electrum")));
        addCompactingRecipe(exporter, BlockContent.DURATIUM_BLOCK, ItemContent.DURATIUM_INGOT, of(ItemTagGenerator.getIngotTag("duratium")), of(ItemTagGenerator.getStorageBlockTag("duratium")));
        addCompactingRecipe(exporter, BlockContent.BIOMASS_BLOCK, ItemContent.BIOMASS, of(ItemContent.BIOMASS), of(ItemTagGenerator.getStorageBlockTag("biomass")));
        addCompactingRecipe(exporter, BlockContent.PLASTIC_BLOCK, ItemContent.PLASTIC_SHEET, of(TagContent.PLASTIC_PLATES), of(ItemTagGenerator.getStorageBlockTag("plastic")));
        addCompactingRecipe(exporter, BlockContent.FLUXITE_BLOCK, ItemContent.FLUXITE, of(ItemContent.FLUXITE), of(ItemTagGenerator.getStorageBlockTag("fluxite")));
        addCompactingRecipe(exporter, BlockContent.SILICON_BLOCK, ItemContent.SILICON, of(TagContent.SILICON), of(ItemTagGenerator.getStorageBlockTag("silicon")));
        addCompactingRecipe(exporter, BlockContent.RAW_NICKEL_BLOCK, ItemContent.RAW_NICKEL, of(TagContent.NICKEL_RAW_MATERIALS), of(ItemTagGenerator.getStorageBlockTag("raw_nickel")));
        addCompactingRecipe(exporter, BlockContent.RAW_PLATINUM_BLOCK, ItemContent.RAW_PLATINUM, of(TagContent.PLATINUM_RAW_MATERIALS), of(ItemTagGenerator.getStorageBlockTag("raw_platinum")));
        
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
          Ingredient.ofItems(ItemContent.FLUXITE),
          Items.IRON_NUGGET,
          Items.IRON_INGOT,
          1f,
          "iron",
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
          Ingredient.ofItems(ItemContent.FLUXITE),
          ItemContent.COPPER_NUGGET,
          Items.COPPER_INGOT,
          1f,
          "copper",
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
          Ingredient.ofItems(ItemContent.FLUXITE),
          Items.GOLD_NUGGET,
          Items.GOLD_INGOT,
          1f,
          "gold",
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
          Ingredient.ofItems(ItemContent.FLUXITE),
          ItemContent.NICKEL_NUGGET,
          ItemContent.NICKEL_INGOT,
          1f,
          "nickel",
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
          Ingredient.ofItems(ItemContent.FLUXITE),
          ItemContent.PLATINUM_NUGGET,
          ItemContent.PLATINUM_INGOT,
          1.5f,
          "platinum",
          1
        );
    }
    
    private void addAlloys(RecipeExporter exporter) {
        addAlloyRecipe(exporter, Ingredient.fromTag(TagContent.PLATINUM_INGOTS), Ingredient.ofItems(Items.NETHERITE_INGOT), ItemContent.DURATIUM_INGOT, "duratium");
        addAlloyRecipe(exporter, Items.GOLD_INGOT, Items.REDSTONE, ItemContent.ELECTRUM_INGOT, "electrum");
        addAlloyRecipe(exporter, Ingredient.ofItems(Items.DIAMOND), Ingredient.fromTag(TagContent.NICKEL_INGOTS), ItemContent.ADAMANT_INGOT, "adamant");
        addAlloyRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.FLUXITE), ItemContent.ENERGITE_INGOT, "energite");
        addAlloyRecipe(exporter, Ingredient.ofItems(Items.IRON_INGOT), Ingredient.fromTag(TagContent.COAL_DUSTS), ItemContent.STEEL_INGOT, 1, 0.3333f, "steel");
    }
    
    private void addParticleCollisions(RecipeExporter exporter) {
        // diamond from coal dust
        addParticleCollisionRecipe(exporter, Ingredient.fromTag(TagContent.COAL_DUSTS), Ingredient.fromTag(TagContent.COAL_DUSTS), new ItemStack(Items.DIAMOND), 500, "diamond");
        // overcharged crystal from fluxite and energite dust
        addParticleCollisionRecipe(exporter, Ingredient.ofItems(ItemContent.FLUXITE), of(ItemContent.ENERGITE_DUST), new ItemStack(ItemContent.OVERCHARGED_CRYSTAL), 5000, "overcharged_crystal");
        // platinum from gold dust
        addParticleCollisionRecipe(exporter, Ingredient.fromTag(TagContent.GOLD_DUSTS), Ingredient.fromTag(TagContent.GOLD_DUSTS), new ItemStack(ItemContent.PLATINUM_DUST), 500, "platinum_dust");
        // enderic compound from redstone and flesh
        addParticleCollisionRecipe(exporter, Ingredient.ofItems(Items.REDSTONE), Ingredient.ofItems(Items.ROTTEN_FLESH), new ItemStack(ItemContent.ENDERIC_COMPOUND), 500, "enderic_compound");
        // fluxite from electrum dust and redstone
        addParticleCollisionRecipe(exporter, of(TagContent.ELECTRUM_DUSTS), Ingredient.ofItems(Items.REDSTONE), new ItemStack(ItemContent.FLUXITE), 1000, "fluxite");
        // netherite scrap from adamant dust and netherrack
        addParticleCollisionRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_DUST), Ingredient.ofItems(Items.NETHERRACK), new ItemStack(Items.NETHERITE_SCRAP), 2500, "netherite");
        // elytra from feather and saddle
        addParticleCollisionRecipe(exporter, Ingredient.ofItems(Items.FEATHER), Ingredient.ofItems(Items.SADDLE), new ItemStack(Items.ELYTRA), 10000, "elytra");
        // nether star from overcharged crystal and netherite
        addParticleCollisionRecipe(exporter, Ingredient.ofItems(ItemContent.OVERCHARGED_CRYSTAL), Ingredient.ofItems(Items.NETHERITE_INGOT), new ItemStack(Items.NETHER_STAR), 15000, "nether_star");
        // echo shard from ender pearl and amethyst shard
        addParticleCollisionRecipe(exporter, Ingredient.ofItems(Items.ENDER_PEARL), Ingredient.ofItems(Items.AMETHYST_SHARD), new ItemStack(Items.ECHO_SHARD), 1000, "echo_shard");
        // heavy core from reinforced deepslate block and duration dust
        addParticleCollisionRecipe(exporter, Ingredient.ofItems(Items.REINFORCED_DEEPSLATE), Ingredient.ofItems(ItemContent.DURATIUM_DUST), new ItemStack(Items.HEAVY_CORE), 8000, "heavy_core");
    }
    
    private void addDusts(RecipeExporter exporter) {
        addDustRecipe(exporter, Ingredient.ofItems(Items.COPPER_INGOT), ItemContent.COPPER_DUST, "copper");
        addDustRecipe(exporter, Ingredient.ofItems(Items.IRON_INGOT), ItemContent.IRON_DUST, "iron");
        addDustRecipe(exporter, Ingredient.ofItems(Items.GOLD_INGOT), ItemContent.GOLD_DUST, "gold");
        addDustRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), ItemContent.NICKEL_DUST, "nickel");
        addDustRecipe(exporter, Ingredient.fromTag(TagContent.PLATINUM_INGOTS), ItemContent.PLATINUM_DUST, "platinum");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT), ItemContent.BIOSTEEL_DUST, ItemContent.BIOSTEEL_INGOT, "biosteel");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.DURATIUM_INGOT), ItemContent.DURATIUM_DUST, ItemContent.DURATIUM_INGOT, "duratium");
        addDustRecipe(exporter, of(TagContent.ELECTRUM_INGOTS), ItemContent.ELECTRUM_DUST, ItemContent.ELECTRUM_INGOT, "electrum");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), ItemContent.ADAMANT_DUST, ItemContent.ADAMANT_INGOT, "adamant");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.ENERGITE_INGOT), ItemContent.ENERGITE_DUST, ItemContent.ENERGITE_INGOT, "energite");
        addDustRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), ItemContent.STEEL_DUST, ItemContent.STEEL_INGOT, "steel");
        addDustRecipe(exporter, Ingredient.ofItems(Items.COAL), ItemContent.COAL_DUST, "coal");
        addDustRecipe(exporter, Ingredient.ofItems(Items.QUARTZ), ItemContent.QUARTZ_DUST, "quartz");
        
        // raw ores without processing chains
        // coal
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.COAL_ORES), Items.COAL, 3, "coalore");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.COAL_ORES), Items.COAL, 2, "coalore");
        // redstone
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.REDSTONE_ORES), Items.REDSTONE, 12, "redstoneore");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.REDSTONE_ORES), Items.REDSTONE, 8, "redstoneore");
        // diamond
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.DIAMOND_ORES), Items.DIAMOND, 2, "diamondore");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.DIAMOND_ORES), Items.DIAMOND, 1, "diamondore");
        // quartz
        addGrinderRecipe(exporter, Ingredient.ofItems(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 3, "quartzore");
        addPulverizerRecipe(exporter, Ingredient.ofItems(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 2, "quartzore");
        // glowstone
        addGrinderRecipe(exporter, Ingredient.ofItems(Blocks.GLOWSTONE), Items.GLOWSTONE_DUST, 4, "glowstoneore");
        addPulverizerRecipe(exporter, Ingredient.ofItems(Blocks.GLOWSTONE), Items.GLOWSTONE_DUST, 3, "glowstoneore");
        // lapis
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.LAPIS_ORES), Items.LAPIS_LAZULI, 8, "lapisore");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.LAPIS_ORES), Items.LAPIS_LAZULI, 6, "lapisore");
        // bone
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.BONE), Items.BONE_MEAL, 8, "bone");
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.BONE), Items.BONE_MEAL, 6, "bone");
        // blaze powder
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.BLAZE_ROD), Items.BLAZE_POWDER, 4, "blaze");
        addPulverizerRecipe(exporter, Ingredient.ofItems(Items.BLAZE_ROD), Items.BLAZE_POWDER, 3, "blaze");
        // wool
        addGrinderRecipe(exporter, Ingredient.fromTag(ItemTags.WOOL), Items.STRING, 4, "string");
        addPulverizerRecipe(exporter, Ingredient.fromTag(ItemTags.WOOL), Items.STRING, 3, "string");
        // ancient debris
        addGrinderRecipe(exporter, Ingredient.ofItems(Items.ANCIENT_DEBRIS), Items.NETHERITE_SCRAP, 2, "netheritescrap");
    }
    
    private void addUraniumProcessing(RecipeExporter exporter) {
        
        // uranium order is:
        // raw ore -> dust/gem, dust -> gem, gem -> pellets
        
        // plutonium can be made via either ender laser on crystals (manually, usually low amount)
        // or via the particle accelerator
        
        // small uranium dust from redstone
        addCentrifugeRecipe(exporter, of(Items.REDSTONE), ItemContent.SMALL_URANIUM_DUST, 1, "redstoneuran");
        
        // uranium ore blocks
        addGrinderRecipe(exporter, of(BlockContent.DEEPSLATE_URANIUM_ORE), List.of(new ItemStack(ItemContent.RAW_URANIUM, 3), new ItemStack(ItemContent.SMALL_PLUTONIUM_DUST)), "uraniumore");
        addPulverizerRecipe(exporter, of(BlockContent.DEEPSLATE_URANIUM_ORE), ItemContent.RAW_URANIUM, 2, "uraniumore");
        
        // uranium crystal blocks
        addGrinderRecipe(exporter, of(BlockContent.URANIUM_CRYSTAL), List.of(new ItemStack(ItemContent.RAW_URANIUM, 5), new ItemStack(ItemContent.SMALL_PLUTONIUM_DUST)), "uraniumcrystal");
        addPulverizerRecipe(exporter, of(BlockContent.URANIUM_CRYSTAL), ItemContent.RAW_URANIUM, 4, "uraniumcrystal");
        
        // raw uranium in grinder
        addGrinderRecipe(exporter, of(TagContent.URANIUM_RAW_MATERIALS), List.of(new ItemStack(ItemContent.URANIUM_DUST, 2), new ItemStack(ItemContent.SMALL_PLUTONIUM_DUST)), "uranium");
        addPulverizerRecipe(exporter, of(TagContent.URANIUM_RAW_MATERIALS), ItemContent.URANIUM_DUST, 2, "uranium");
        
        // uranium gem from raw uranium / uranium dust in atomic forge
        addAtomicForgeRecipe(exporter, of(TagContent.URANIUM_RAW_MATERIALS), of(TagContent.COPPER_DUSTS), ItemContent.URANIUM_GEM, 5, "urandust");
        addAtomicForgeRecipe(exporter, of(TagContent.URANIUM_DUSTS), of(TagContent.COPPER_DUSTS), ItemContent.URANIUM_GEM, 5, "urandustgem");
        
        // uranium pellets in assembler
        addAssemblerRecipe(exporter, of(ItemContent.URANIUM_GEM), of(ItemContent.URANIUM_GEM), of(TagContent.PLASTIC_PLATES), of(TagContent.NICKEL_INGOTS), ItemContent.URANIUM_PELLET, 2, 1f, "uranpelletbasic");
        addAssemblerRecipe(exporter, of(ItemContent.URANIUM_GEM), of(ItemContent.URANIUM_GEM), of(TagContent.PLASTIC_PLATES), of(ItemContent.ADAMANT_INGOT), ItemContent.URANIUM_PELLET, 3, 1f, "uranpelletbetter");
        addAssemblerRecipe(exporter, of(ItemContent.URANIUM_GEM), of(ItemContent.URANIUM_GEM), of(TagContent.PLASTIC_PLATES), of(ItemContent.DURATIUM_INGOT), ItemContent.URANIUM_PELLET, 4, 1f, "uranpelletult");
        
        // plutonium pellets in assembler
        addAssemblerRecipe(exporter, of(ItemContent.PLUTONIUM_DUST), of(ItemContent.PLUTONIUM_DUST), of(TagContent.PLASTIC_PLATES), of(TagContent.NICKEL_INGOTS), ItemContent.PLUTONIUM_PELLET, 2, 1f, "plutoniumpelletbasic");
        addAssemblerRecipe(exporter, of(ItemContent.PLUTONIUM_DUST), of(ItemContent.PLUTONIUM_DUST), of(TagContent.PLASTIC_PLATES), of(ItemContent.ADAMANT_INGOT), ItemContent.PLUTONIUM_PELLET, 3, 1f, "plutoniumpelletbetter");
        addAssemblerRecipe(exporter, of(ItemContent.PLUTONIUM_DUST), of(ItemContent.PLUTONIUM_DUST), of(TagContent.PLASTIC_PLATES), of(ItemContent.DURATIUM_INGOT), ItemContent.PLUTONIUM_PELLET, 4, 1f, "plutoniumpelletult");
        
        // dust compacting
        addCompactingRecipe(exporter, ItemContent.URANIUM_DUST, ItemContent.SMALL_URANIUM_DUST, of(ItemContent.SMALL_URANIUM_DUST), of(TagContent.URANIUM_DUSTS));
        addCompactingRecipe(exporter, ItemContent.PLUTONIUM_DUST, ItemContent.SMALL_PLUTONIUM_DUST, of(ItemContent.SMALL_PLUTONIUM_DUST), of(TagContent.PLUTONIUM_DUSTS));
        
        // uranium to plutonium
        addParticleCollisionRecipe(exporter, of(TagContent.URANIUM_DUSTS), of(ItemContent.FLUXITE), new ItemStack(ItemContent.PLUTONIUM_DUST), 2500, "plutonium");
        
        // pellet compacting
        addCompactingRecipe(exporter, ItemContent.URANIUM_PELLET, ItemContent.SMALL_URANIUM_PELLET, of(ItemContent.SMALL_URANIUM_PELLET), of(ItemContent.URANIUM_PELLET));
        addCompactingRecipe(exporter, ItemContent.PLUTONIUM_PELLET, ItemContent.SMALL_PLUTONIUM_PELLET, of(ItemContent.SMALL_PLUTONIUM_PELLET), of(ItemContent.PLUTONIUM_PELLET));
    }
    
    private void addAugmentRecipes(RecipeExporter exporter) {
        
        var SIMPLE_AUGMENT_STATION_ID = Registries.BLOCK.getId(BlockContent.SIMPLE_AUGMENT_STATION);
        var ADVANCED_AUGMENT_STATION_ID = Registries.BLOCK.getId(BlockContent.ADVANCED_AUGMENT_STATION);
        var ARCANE_AUGMENT_STATION_ID = Registries.BLOCK.getId(BlockContent.ARCANE_AUGMENT_STATION);
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(TagContent.MACHINE_PLATING)),
            new SizedIngredient(32, of(TagContent.COAL_DUSTS)),
            new SizedIngredient(8, of(ItemContent.BIOSTEEL_INGOT))),
          List.of(
            new SizedIngredient(8, of(TagContent.STEEL_INGOTS)),
            new SizedIngredient(16, of(ConventionalItemTags.IRON_INGOTS))),
          List.of(), SIMPLE_AUGMENT_STATION_ID, 5, 70, 400, 10_000_000, "hpboost");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(32, of(ItemContent.CARBON_FIBRE_STRANDS)),
            new SizedIngredient(16, of(ItemContent.BIOSTEEL_INGOT)),
            new SizedIngredient(4, of(Items.DIAMOND))),
          List.of(
            new SizedIngredient(8, of(ItemContent.CARBON_FIBRE_STRANDS)),
            new SizedIngredient(4, of(ItemContent.DURATIUM_INGOT))),
          List.of("oritech:armor"), SIMPLE_AUGMENT_STATION_ID, 80, 70, 800, 50_000_000, "hpboostmore");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENERGITE_INGOT)),
            new SizedIngredient(32, of(ItemContent.DURATIUM_INGOT)),
            new SizedIngredient(1, of(Items.NETHER_STAR))),
          List.of(
            new SizedIngredient(64, of(ItemContent.DURATIUM_DUST)),
            new SizedIngredient(64, of(Items.REDSTONE_BLOCK))),
          List.of("oritech:ultimatearmor"), ADVANCED_AUGMENT_STATION_ID, 165, 70, 1600, 200_000_000, "hpboostultra");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ADAMANT_INGOT)),
            new SizedIngredient(8, of(Items.NETHER_STAR)),
            new SizedIngredient(64, of(ItemContent.URANIUM_PELLET)),
            new SizedIngredient(64, of(BlockContent.FLUXITE_BLOCK))),
          List.of(
            new SizedIngredient(32, of(ItemContent.ADAMANT_INGOT)),
            new SizedIngredient(1, of(ItemContent.OVERCHARGED_CRYSTAL)),
            new SizedIngredient(64, of(ItemContent.FLUXITE))),
          List.of("oritech:hpboostultra", "oritech:gravity"), ADVANCED_AUGMENT_STATION_ID, 205, 40, 2400, 500_000_000, "hpboostultimate");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(32, of(ItemContent.MOTOR)),
            new SizedIngredient(64, of(ItemContent.BIOSTEEL_INGOT)),
            new SizedIngredient(32, of(Items.REDSTONE))),
          List.of(
            new SizedIngredient(16, of(ItemContent.MOTOR)),
            new SizedIngredient(32, of(ConventionalItemTags.IRON_INGOTS))),
          List.of(), SIMPLE_AUGMENT_STATION_ID, 5, 30, 600, 30_000_000, "speedboost");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENERGITE_INGOT)),
            new SizedIngredient(32, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(16, of(ItemContent.FLUX_GATE))),
          List.of(
            new SizedIngredient(32, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(1, of(ItemContent.OVERCHARGED_CRYSTAL)),
            new SizedIngredient(64, of(TagContent.ELECTRUM_DUSTS))),
          List.of("oritech:speedboost", "oritech:armor"), ADVANCED_AUGMENT_STATION_ID, 55, 50, 1800, 350_000_000, "superspeedboost");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(32, of(ItemContent.MOTOR)),
            new SizedIngredient(64, of(TagContent.STEEL_INGOTS)),
            new SizedIngredient(16, of(Items.IRON_BLOCK))),
          List.of(
            new SizedIngredient(16, of(ItemContent.MOTOR)),
            new SizedIngredient(64, of(ConventionalItemTags.IRON_INGOTS))),
          List.of("oritech:superspeedboost"), SIMPLE_AUGMENT_STATION_ID, 80, 50, 800, 75_000_000, "stepassist");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.SILICON_WAFER)),
            new SizedIngredient(32, of(ItemContent.PROCESSING_UNIT)),
            new SizedIngredient(16, of(Items.GOLD_BLOCK))),
          List.of(
            new SizedIngredient(32, of(TagContent.SILICON)),
            new SizedIngredient(32, of(Items.REDSTONE_BLOCK))),
          List.of("oritech:hpboost"), SIMPLE_AUGMENT_STATION_ID, 30, 90, 400, 50_000_000, "dwarf");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.RAW_BIOPOLYMER)),
            new SizedIngredient(32, of(ItemContent.SMALL_URANIUM_DUST)),
            new SizedIngredient(64, of(TagContent.BIOMASS))),
          List.of(
            new SizedIngredient(32, of(ItemContent.RAW_BIOPOLYMER)),
            new SizedIngredient(64, of(ConventionalItemTags.IRON_INGOTS))),
          List.of("oritech:dwarf", "oritech:armor"), SIMPLE_AUGMENT_STATION_ID, 55, 90, 1600, 300_000_000, "giant");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(TagContent.STEEL_INGOTS)),
            new SizedIngredient(8, of(ItemContent.DURATIUM_INGOT)),
            new SizedIngredient(16, of(Items.DIAMOND))),
          List.of(
            new SizedIngredient(4, of(ItemContent.DURATIUM_INGOT)),
            new SizedIngredient(32, of(ConventionalItemTags.IRON_INGOTS))),
          List.of(), SIMPLE_AUGMENT_STATION_ID, 30, 50, 800, 80_000_000, "armor");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENERGITE_INGOT)),
            new SizedIngredient(32, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(32, of(Items.DIAMOND))),
          List.of(
            new SizedIngredient(16, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(1, of(ItemContent.OVERCHARGED_CRYSTAL)),
            new SizedIngredient(8, of(ItemContent.DURATIUM_INGOT))),
          List.of("oritech:autofeeder"), SIMPLE_AUGMENT_STATION_ID, 105, 50, 1600, 280_000_000, "betterarmor");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.FLUXITE)),
            new SizedIngredient(32, of(ItemContent.HEISENBERG_COMPENSATOR)),
            new SizedIngredient(64, of(ItemContent.PLUTONIUM_PELLET)),
            new SizedIngredient(8, of(Items.NETHER_STAR))),
          List.of(
            new SizedIngredient(32, of(BlockContent.FLUXITE_BLOCK)),
            new SizedIngredient(1, of(ItemContent.OVERCHARGED_CRYSTAL)),
            new SizedIngredient(16, of(Items.OBSIDIAN))),
          List.of("oritech:betterarmor"), ADVANCED_AUGMENT_STATION_ID, 155, 50, 2400, 500_000_000, "ultimatearmor");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(48, of(TagContent.ELECTRUM_INGOTS)),
            new SizedIngredient(32, of(Items.REDSTONE_BLOCK))),
          List.of(
            new SizedIngredient(32, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(64, of(Items.IRON_BLOCK))),
          List.of("oritech:blockreach"), ADVANCED_AUGMENT_STATION_ID, 140, 70, 1600, 150_000_000, "weaponreach");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.MOTOR)),
            new SizedIngredient(48, of(TagContent.STEEL_INGOTS)),
            new SizedIngredient(32, of(Items.COPPER_BLOCK))),
          List.of(
            new SizedIngredient(32, of(ItemContent.MOTOR)),
            new SizedIngredient(64, of(Items.COPPER_INGOT))),
          List.of(), SIMPLE_AUGMENT_STATION_ID, 115, 90, 900, 100_000_000, "blockreach");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(16, of(Items.ENDER_PEARL)),
            new SizedIngredient(16, of(Items.DIAMOND_BLOCK))),
          List.of(
            new SizedIngredient(32, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(64, of(Items.OBSIDIAN))),
          List.of("oritech:blockreach"), ADVANCED_AUGMENT_STATION_ID, 140, 90, 800, 200_000_000, "farblockreach");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(48, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(64, of(Items.QUARTZ_BLOCK)),
            new SizedIngredient(32, of(ItemContent.BASIC_BATTERY))),
          List.of(
            new SizedIngredient(16, of(Items.QUARTZ_BLOCK)),
            new SizedIngredient(32, of(ConventionalItemTags.IRON_INGOTS))),
          List.of("oritech:attackdamage", "oritech:speedboost"), SIMPLE_AUGMENT_STATION_ID, 30, 10, 1200, 100_000_000, "miningspeed");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENERGITE_INGOT)),
            new SizedIngredient(48, of(ItemContent.FLUX_GATE)),
            new SizedIngredient(64, of(ItemContent.DURATIUM_INGOT))),
          List.of(
            new SizedIngredient(32, of(ItemContent.ENERGITE_INGOT)),
            new SizedIngredient(64, of(Items.REDSTONE_BLOCK))),
          List.of("oritech:miningspeed", "oritech:superspeedboost"), ADVANCED_AUGMENT_STATION_ID, 80, 10, 2400, 450_000_000, "superminingspeed");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(TagContent.STEEL_INGOTS)),
            new SizedIngredient(48, of(Items.DIAMOND)),
            new SizedIngredient(32, of(ItemContent.FLUXITE))),
          List.of(
            new SizedIngredient(16, of(TagContent.STEEL_INGOTS)),
            new SizedIngredient(4, of(ItemContent.DURATIUM_INGOT))),
          List.of(), SIMPLE_AUGMENT_STATION_ID, 5, 10, 1600, 150_000_000, "attackdamage");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENDERIC_COMPOUND)),
            new SizedIngredient(64, of(ItemContent.FLUXITE)),
            new SizedIngredient(64, of(Items.BLAZE_ROD))),
          List.of(
            new SizedIngredient(32, of(ItemContent.ENDERIC_COMPOUND)),
            new SizedIngredient(64, of(Items.GOLD_BLOCK))),
          List.of("oritech:hpboostultra", "oritech:ultimatearmor"), ARCANE_AUGMENT_STATION_ID, 180, 50, 2800, 500_000_000, "superattackdamage");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(TagContent.ELECTRUM_INGOTS)),
            new SizedIngredient(48, of(Items.LAPIS_BLOCK)),
            new SizedIngredient(32, of(Items.GOLD_BLOCK))),
          List.of(
            new SizedIngredient(32, of(Items.LAPIS_BLOCK)),
            new SizedIngredient(64, of(Items.REDSTONE_BLOCK))),
          List.of(), ARCANE_AUGMENT_STATION_ID, 55, 30, 1800, 200_000_000, "luck");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(48, of(ItemContent.FLUXITE)),
            new SizedIngredient(8, of(Items.PHANTOM_MEMBRANE))),
          List.of(
            new SizedIngredient(32, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(16, of(Items.IRON_BLOCK))),
          List.of("oritech:flight"), ARCANE_AUGMENT_STATION_ID, 180, 10, 2200, 400_000_000, "gravity");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.FLUX_GATE)),
            new SizedIngredient(16, of(Items.WIND_CHARGE)),
            new SizedIngredient(16, of(ItemContent.PROMETHEUM_INGOT)),
            new SizedIngredient(32, of(ItemContent.PLUTONIUM_PELLET))),
          List.of(
            new SizedIngredient(32, of(ItemContent.FLUX_GATE)),
            new SizedIngredient(8, of(ItemContent.PLUTONIUM_PELLET))),
          List.of("oritech:betterarmor", "oritech:portal"), ARCANE_AUGMENT_STATION_ID, 155, 30, 3600, 500_000_000, "flight");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(Items.ENDER_EYE)),
            new SizedIngredient(48, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(8, of(Items.DIAMOND))),
          List.of(
            new SizedIngredient(32, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(64, of(Items.GLOWSTONE))),
          List.of("oritech:orefinder"), ARCANE_AUGMENT_STATION_ID, 155, 10, 3200, 100_000_000, "cloak");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(16, of(Items.ENDER_PEARL)),
            new SizedIngredient(48, of(Items.OBSIDIAN)),
            new SizedIngredient(1, of(ItemContent.UNHOLY_INTELLIGENCE)),
            new SizedIngredient(32, of(ItemContent.ADAMANT_INGOT))),
          List.of(
            new SizedIngredient(8, of(Items.ENDER_PEARL)),
            new SizedIngredient(32, of(Items.CRYING_OBSIDIAN))),
          List.of(), ARCANE_AUGMENT_STATION_ID, 130, 30, 3000, 250_000_000, "portal");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(Items.GOLD_INGOT)),
            new SizedIngredient(48, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(64, of(Items.GLOWSTONE))),
          List.of(
            new SizedIngredient(4, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(8, of(Items.GLOWSTONE)),
            new SizedIngredient(8, of(Items.REDSTONE_LAMP))),
          List.of(), ADVANCED_AUGMENT_STATION_ID, 105, 30, 2400, 50_000_000, "nightvision");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(Items.PRISMARINE_CRYSTALS)),
            new SizedIngredient(48, of(ItemContent.BIOSTEEL_INGOT)),
            new SizedIngredient(1, of(Items.HEART_OF_THE_SEA))),
          List.of(
            new SizedIngredient(32, of(ItemContent.BIOSTEEL_INGOT)),
            new SizedIngredient(1, of(Items.CONDUIT))),
          List.of(), SIMPLE_AUGMENT_STATION_ID, 5, 90, 800, 50_000_000, "waterbreath");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.PROCESSING_UNIT)),
            new SizedIngredient(48, of(TagContent.BIOMASS)),
            new SizedIngredient(64, of(Items.GOLDEN_CARROT))),
          List.of(
            new SizedIngredient(32, of(TagContent.BIOMASS)),
            new SizedIngredient(64, of(BlockContent.ITEM_PIPE)),
            new SizedIngredient(8, of(Items.HOPPER))),
          List.of("oritech:armor", "oritech:hpboostmore"), SIMPLE_AUGMENT_STATION_ID, 90, 90, 500, 30_000_000, "autofeeder");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(48, of(ItemContent.ENERGITE_INGOT)),
            new SizedIngredient(2, of(Items.LODESTONE))),
          List.of(
            new SizedIngredient(32, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(64, of(ConventionalItemTags.COPPER_INGOTS))),
          List.of("oritech:superminingspeed"), SIMPLE_AUGMENT_STATION_ID, 105, 10, 2400, 400_000_000, "magnet");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(48, of(Items.AMETHYST_BLOCK)),
            new SizedIngredient(1, of(ItemContent.OVERCHARGED_CRYSTAL)),
            new SizedIngredient(8, of(ItemContent.PROMETHEUM_INGOT)),
            new SizedIngredient(4, of(Items.SCULK_SENSOR))),
          List.of(
            new SizedIngredient(32, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(64, of(Items.REDSTONE_TORCH))),
          List.of("oritech:nightvision", "oritech:magnet"), ARCANE_AUGMENT_STATION_ID, 130, 10, 3200, 200_000_000, "orefinder");
    }
    
    private void addReactorBlocks(RecipeExporter exporter) {
        
        // single rod
        offerRodRecipe(exporter, BlockContent.REACTOR_ROD.asItem(), of(TagContent.PLASTIC_PLATES), of(ItemContent.ENERGITE_INGOT), "singlerod");
        // dual rod
        offerRodCombinationRecipe(exporter, BlockContent.REACTOR_DOUBLE_ROD.asItem(), of(BlockContent.REACTOR_REFLECTOR), of(BlockContent.REACTOR_ROD), "doublerod");
        // quad rod
        offerRodCombinationRecipe(exporter, BlockContent.REACTOR_QUAD_ROD.asItem(), of(BlockContent.REACTOR_REFLECTOR), of(BlockContent.REACTOR_DOUBLE_ROD), "quadrod");
        
        // reactor plating: steel and machine plating in crafting table / assembler
        offerMachinePlatingRecipe(exporter, BlockContent.REACTOR_WALL.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), 4, "reactorplatingcrafting");
        addAssemblerRecipe(exporter, of(TagContent.MACHINE_PLATING), of(TagContent.MACHINE_PLATING), of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), BlockContent.REACTOR_WALL.asItem(), 3, 1, "reactorplatingalt");
        
        // neutron reflectors: expensive, needs duratium core, adamant frame and reactor walls
        offerMachinePlatingRecipe(exporter, BlockContent.REACTOR_REFLECTOR.asItem(), of(BlockContent.REACTOR_WALL), of(ItemContent.ADAMANT_INGOT), of(ItemContent.DURATIUM_INGOT), 1, "reflector");
        
        // reactor controller: reactor wall, processing unit
        offerRodCombinationRecipe(exporter, BlockContent.REACTOR_CONTROLLER.asItem(), of(BlockContent.REACTOR_WALL), of(ItemContent.PROCESSING_UNIT), "controller");
        
        // reactor energy port: reactor wall, storage unit, electrum
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_ENERGY_PORT.asItem(), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.ENERGY_PIPE), of(BlockContent.REACTOR_WALL), of(ConventionalItemTags.IRON_INGOTS), "energyport");
        
        // reactor redstone port: wall, processing unit, repeater, torch
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_REDSTONE_PORT.asItem(), of(ItemContent.PROCESSING_UNIT), of(Items.REPEATER), of(BlockContent.REACTOR_WALL), of(Items.REDSTONE_TORCH), "redstoneport");
        
        // reactor fuel port: wall, hopper, motor, item pipe
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_FUEL_PORT.asItem(), of(BlockContent.ITEM_PIPE), of(Items.HOPPER), of(BlockContent.REACTOR_WALL), of(ConventionalItemTags.CHESTS), "fuelport");
        
        // reactor absorber port: wall, ice, motor, item pipe
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_ABSORBER_PORT.asItem(), of(BlockContent.ITEM_PIPE), of(Items.HOPPER), of(BlockContent.REACTOR_WALL), of(Blocks.ICE), "absorberport");
        
        // reactor absorber : wall, steel, ice
        offerBatteryRecipe(exporter, BlockContent.REACTOR_CONDENSER.asItem(), of(Items.ICE), of(ConventionalItemTags.GLASS_BLOCKS), of(TagContent.STEEL_INGOTS), "condenser");
        
        // reactor vent: motor, carbon fibre
        offerStarRecipe(exporter, BlockContent.REACTOR_VENT.asItem(), of(ItemContent.MOTOR), of(TagContent.CARBON_FIBRE), "reactorvent");
        
        // reactor heat pipe: electrum, gold
        offerStarRecipe(exporter, BlockContent.REACTOR_HEAT_PIPE.asItem(), of(TagContent.ELECTRUM_INGOTS), of(ConventionalItemTags.GOLD_INGOTS), "reactorheatpipe");
        
        // explosives
        offerMachinePlatingRecipe(exporter, BlockContent.LOW_YIELD_NUKE.asItem(), of(ItemContent.DUBIOS_CONTAINER), of(ItemContent.URANIUM_PELLET), of(Items.TNT), 1, "nuke");
        offerMachinePlatingRecipe(exporter, BlockContent.NUKE.asItem(), of(ItemContent.HEISENBERG_COMPENSATOR), of(ItemContent.PLUTONIUM_PELLET), of(Items.TNT), 1, "nukebetter");
    }
    
    private void addReactorFuels(RecipeExporter exporter) {
        addReactorGen(exporter, of(ItemContent.SMALL_URANIUM_PELLET), 400, "smallpellet");
        addReactorGen(exporter, of(ItemContent.URANIUM_PELLET), 4000, "pellet");
        addReactorGen(exporter, of(ItemContent.SMALL_PLUTONIUM_PELLET), 4000, "smallplutoniumpellet");
        addReactorGen(exporter, of(ItemContent.PLUTONIUM_PELLET), 40000, "plutoniumpellet");
    }
    
    private void addLaserTransformations(RecipeExporter exporter) {
        addLaserRecipe(exporter, of(Items.AMETHYST_CLUSTER), ItemContent.FLUXITE, "fluxite");
        addLaserRecipe(exporter, of(BlockContent.URANIUM_CRYSTAL), ItemContent.PLUTONIUM_DUST, "plutoniumdust");
    }
    
    private Ingredient of(ItemConvertible item) {
        return Ingredient.ofItems(item);
    }
    
    private Ingredient of(TagKey<Item> item) {
        return Ingredient.fromTag(item);
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
    
    private static void addParticleCollisionRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, ItemStack result, int requiredSpeed, String suffix) {
        var particle = new OritechRecipe(requiredSpeed, List.of(A, B), List.of(result), RecipeContent.PARTICLE_COLLISION, null, null);
        exporter.accept(Oritech.id("particle/" + suffix), particle, null);
    }
    
    public static void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addGrinderRecipe(exporter, ingot, dust, 1, suffix);
    }
    
    public static void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, int dustCount, String suffix) {
        addGrinderRecipe(exporter, ingot, List.of(new ItemStack(dust, dustCount)), suffix);
    }
    
    public static void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, List<ItemStack> outputs, String suffix) {
        var grinderDefaultSpeed = 140;
        
        var grinder = new OritechRecipe(grinderDefaultSpeed, List.of(ingot), outputs, RecipeContent.GRINDER, null, null);
        exporter.accept(Oritech.id("grinder/dust/" + suffix), grinder, null);
    }
    
    
    public static void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addPulverizerRecipe(exporter, ingot, dust, 1, suffix);
    }
    
    public static void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, int dustCount, String suffix) {
        var pulverizerDefaultSpeed = 200;
        
        var pulverizer = new OritechRecipe(pulverizerDefaultSpeed, List.of(ingot), List.of(new ItemStack(dust, dustCount)), RecipeContent.PULVERIZER, null, null);
        exporter.accept(Oritech.id("pulverizer/dust/" + suffix), pulverizer, null);
    }
    
    private void addAssemblerRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Ingredient C, Ingredient D, Item result, float timeMultiplier, String suffix) {
        addAssemblerRecipe(exporter, A, B, C, D, result, 1, timeMultiplier, suffix);
    }
    
    private void addAssemblerRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Ingredient C, Ingredient D, Item result, int count, float timeMultiplier, String suffix) {
        var defaultSpeed = 160;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var inputs = new ArrayList<Ingredient>();
        inputs.add(A);
        if (B != null) inputs.add(B);
        if (C != null) inputs.add(C);
        if (D != null) inputs.add(D);
        var entry = new OritechRecipe(speed, inputs, List.of(new ItemStack(result, count)), RecipeContent.ASSEMBLER, null, null);
        exporter.accept(Oritech.id("assembler/" + suffix), entry, null);
    }
    
    private void addCompactingRecipe(RecipeExporter exporter, ItemConvertible resBlock, ItemConvertible resItem, Ingredient itemIng, Ingredient blockIng) {
        
        ShapelessRecipeJsonBuilder
          .create(RecipeCategory.MISC, resItem, 9)
          .input(blockIng)
          .criterion(hasItem(resBlock), conditionsFromItem(resBlock))
          .offerTo(exporter, Identifier.of(RecipeProvider.getRecipeName(resBlock) + "blockinv"));
        ShapedRecipeJsonBuilder
          .create(RecipeCategory.MISC, resBlock)
          .input('#', itemIng)
          .pattern("###")
          .pattern("###")
          .pattern("###")
          .criterion(hasItem(resItem), conditionsFromItem(resItem))
          .offerTo(exporter, Identifier.of(RecipeProvider.getRecipeName(resBlock) + "block"));
        
    }
    
    private void addCentrifugeRecipe(RecipeExporter exporter, Ingredient input, Item result, float timeMultiplier, String suffix) {
        addCentrifugeRecipe(exporter, input, result, 1, timeMultiplier, suffix);
    }
    
    private void addCentrifugeRecipe(RecipeExporter exporter, Ingredient input, Item result, int count, float timeMultiplier, String suffix) {
        var defaultSpeed = 200;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var entry = new OritechRecipe(speed, List.of(input), List.of(new ItemStack(result, count)), RecipeContent.CENTRIFUGE, null, null);
        exporter.accept(Oritech.id("centrifuge/" + suffix), entry, null);
    }
    
    public static void addCentrifugeFluidRecipe(RecipeExporter exporter, Ingredient input, Item result, Fluid in, float bucketsIn, Fluid out, float bucketsOut, float timeMultiplier, String suffix) {
        var defaultSpeed = 200;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var inputStack = in != null ? FluidStack.create(in, (long) (bucketsIn * 81000)) : null;
        var outputStack = out != null ? FluidStack.create(out, (long) (bucketsOut * 81000)) : null;
        List<ItemStack> outputItem = result != null ? List.of(new ItemStack(result)) : List.of();
        var entry = new OritechRecipe(speed, List.of(input), outputItem, RecipeContent.CENTRIFUGE_FLUID, inputStack, outputStack);
        exporter.accept(Oritech.id("centrifuge/fluid/" + suffix), entry, null);
    }
    
    public static void addAlloyRecipe(RecipeExporter exporter, Item A, Item B, Item result, String suffix) {
        addAlloyRecipe(exporter, Ingredient.ofItems(A), Ingredient.ofItems(B), result, suffix);
    }
    
    public static void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, String suffix) {
        addAlloyRecipe(exporter, A, B, result, 1, suffix);
    }
    
    public static void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, int count, String suffix) {
        addAlloyRecipe(exporter, A, B, result, count, 1f, suffix);
    }
    
    public static void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, int count, float speedMultiplier, String suffix) {
        var foundryDefaultSpeed = (int) (200 * speedMultiplier);
        
        var entry = new OritechRecipe(foundryDefaultSpeed, List.of(A, B), List.of(new ItemStack(result, count)), RecipeContent.FOUNDRY, null, null);
        exporter.accept(Oritech.id("foundry/alloy/" + suffix), entry, null);
    }
    
    public static void addCoolerRecipe(RecipeExporter exporter, FluidStack input, Item result, int count, float speedMultiplier, String suffix) {
        var coolerDefaultSpeed = (int) (200 * speedMultiplier);
        
        var entry = new OritechRecipe(coolerDefaultSpeed, List.of(), List.of(new ItemStack(result, count)), RecipeContent.COOLER, input, null);
        exporter.accept(Oritech.id("cooler/" + suffix), entry, null);
    }
    
    // A is inserted twice, surrounding B
    private void addAtomicForgeRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, int time, String suffix) {
        var entry = new OritechRecipe(time, List.of(B, A, A), List.of(new ItemStack(result)), RecipeContent.ATOMIC_FORGE, null, null);
        exporter.accept(Oritech.id("atomicforge/" + suffix), entry, null);
    }
    
    private void addDeepDrillRecipe(RecipeExporter exporter, Block input, Item result, int time, String suffix) {
        var entry = new OritechRecipe(time, List.of(Ingredient.ofItems(input.asItem())), List.of(new ItemStack(result)), RecipeContent.DEEP_DRILL, null, null);
        exporter.accept(Oritech.id("deepdrill/" + suffix), entry, null);
    }
    
    public static void addBioGenRecipe(RecipeExporter exporter, Ingredient A, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(A), List.of(), RecipeContent.BIO_GENERATOR, null, null);
        exporter.accept(Oritech.id("biogen/" + suffix), entry, null);
    }
    
    public static void addFuelGenRecipe(RecipeExporter exporter, FluidStack input, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(), List.of(), RecipeContent.FUEL_GENERATOR, input, null);
        exporter.accept(Oritech.id("fuelgen/" + suffix), entry, null);
    }
    
    private void addLavaGen(RecipeExporter exporter, FluidStack input, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(), List.of(), RecipeContent.LAVA_GENERATOR, input, null);
        exporter.accept(Oritech.id("lavagen/" + suffix), entry, null);
    }
    
    private void addSteamEngineGen(RecipeExporter exporter, FluidStack input, int timeInTicks, String suffix) {
        var entry = new OritechRecipe(timeInTicks, List.of(), List.of(), RecipeContent.STEAM_ENGINE, input, null);
        exporter.accept(Oritech.id("steamgen/" + suffix), entry, null);
    }
    
    private void addReactorGen(RecipeExporter exporter, Ingredient input, int timeInTicks, String suffix) {
        var entry = new OritechRecipe(timeInTicks, List.of(input), List.of(), RecipeContent.REACTOR, null, null);
        exporter.accept(Oritech.id("reactor/" + suffix), entry, null);
    }
    
    private void addLaserRecipe(RecipeExporter exporter, Ingredient input, ItemConvertible output, String suffix) {
        var entry = new OritechRecipe(1, List.of(input), List.of(new ItemStack(output)), RecipeContent.LASER, null, null);
        exporter.accept(Oritech.id("laser/" + suffix), entry, null);
    }
    
    private void addAugmentRecipe(RecipeExporter exporter, List<SizedIngredient> inputs, List<SizedIngredient> applyCost, List<String> requirements, Identifier requiredStation, int uiX, int uiY, int time, long rfCost, String id) {
        var entry = new AugmentRecipe(RecipeContent.AUGMENT, inputs, applyCost, requirements.stream().map(elem -> Identifier.of(elem)).toList(), requiredStation, uiX, uiY, time, rfCost);
        exporter.accept(Oritech.id(id), entry, null);
    }
    
    private void addMetalProcessingChain(RecipeExporter exporter, Ingredient oreInput, Ingredient rawOre, Item rawMain, Item rawSecondary, Item clump, Item smallClump,
                                         Item smallSecondaryClump, Item dust, Item smallDust, Item smallSecondaryDust, Item gem, Ingredient gemCatalyst, Item nugget,
                                         Item ingot, float timeMultiplier, String suffix, int byproductAmount) {
        
        // ore block -> raw ores
        var pulverizerOre = new OritechRecipe((int) (200 * timeMultiplier), List.of(oreInput), List.of(new ItemStack(rawMain, 2)), RecipeContent.PULVERIZER, null, null);
        var grinderOre = new OritechRecipe((int) (140 * timeMultiplier), List.of(oreInput), List.of(new ItemStack(rawMain, 2), new ItemStack(rawSecondary, 1)), RecipeContent.GRINDER, null, null);
        
        // raw ores -> dusts / clumps
        var pulverizerRaw = new OritechRecipe((int) (200 * timeMultiplier), List.of(rawOre), List.of(new ItemStack(dust, 1), new ItemStack(smallDust, 3)), RecipeContent.PULVERIZER, null, null);
        var grinderRaw = new OritechRecipe((int) (140 * timeMultiplier), List.of(rawOre), List.of(new ItemStack(clump, 1), new ItemStack(smallClump, 3), new ItemStack(smallSecondaryClump, byproductAmount)), RecipeContent.GRINDER, null, null);
        
        // clump processing into gems
        var centrifugeClumpDry = new OritechRecipe((int) (200 * timeMultiplier), List.of(Ingredient.ofItems(clump)), List.of(new ItemStack(gem, 1), new ItemStack(smallSecondaryDust, byproductAmount)), RecipeContent.CENTRIFUGE, null, null);
        var centrifugeClumpWet = new OritechRecipe((int) (300 * timeMultiplier), List.of(Ingredient.ofItems(clump)), List.of(new ItemStack(gem, 2)), RecipeContent.CENTRIFUGE_FLUID, FluidStack.create(Fluids.WATER, 81000), null);
        // gems can either be directly smelted for 1:1 results, atomic forge for 1:2, and foundry for 1:1.5
        
        // gems to dust (doubling)
        var atomicForgeDust = new OritechRecipe(20, List.of(Ingredient.ofItems(gem), gemCatalyst, gemCatalyst), List.of(new ItemStack(dust, 2)), RecipeContent.ATOMIC_FORGE, null, null);
        
        // atomic forge alternative: 2 gems -> 3 ingots
        var foundryGem = new OritechRecipe(200, List.of(Ingredient.ofItems(gem), Ingredient.ofItems(gem)), List.of(new ItemStack(ingot, 3)), RecipeContent.FOUNDRY, null, null);
        
        // smelting/compacting
        RecipeProvider.offerSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingot, 1f, 200, Oritech.MOD_ID);
        RecipeProvider.offerSmelting(exporter, List.of(gem), RecipeCategory.MISC, ingot, 1f, 200, Oritech.MOD_ID);
        RecipeProvider.offerSmelting(exporter, List.of(smallDust), RecipeCategory.MISC, nugget, 0.5f, 50, Oritech.MOD_ID);
        RecipeProvider.offerBlasting(exporter, List.of(dust), RecipeCategory.MISC, ingot, 1, 100, Oritech.MOD_ID);
        RecipeProvider.offerBlasting(exporter, List.of(gem), RecipeCategory.MISC, ingot, 1, 100, Oritech.MOD_ID);
        RecipeProvider.offerBlasting(exporter, List.of(smallDust), RecipeCategory.MISC, nugget, 0.5f, 50, Oritech.MOD_ID);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, clump, smallClump);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, dust, smallDust);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, ingot, nugget);
        
        // registration
        exporter.accept(Oritech.id("pulverizer/ore/" + suffix), pulverizerOre, null);
        exporter.accept(Oritech.id("grinder/ore/" + suffix), grinderOre, null);
        exporter.accept(Oritech.id("pulverizer/raw/" + suffix), pulverizerRaw, null);
        exporter.accept(Oritech.id("grinder/raw/" + suffix), grinderRaw, null);
        exporter.accept(Oritech.id("centrifuge/clumpdry/" + suffix), centrifugeClumpDry, null);
        exporter.accept(Oritech.id("centrifuge/clumpwet/" + suffix), centrifugeClumpWet, null);
        exporter.accept(Oritech.id("atomicforge/dust/" + suffix), atomicForgeDust, null);
        exporter.accept(Oritech.id("foundry/gem/" + suffix), foundryGem, null);
        
    }
    
    // crafting shapes
    public void offerCableRecipe(RecipeExporter exporter, ItemStack output, Ingredient input, String suffix) {
        var item = output.getItem();
        createCableRecipe(RecipeCategory.MISC, output.getItem(), output.getCount(), input).criterion(hasItem(item), conditionsFromItem(item)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerInsulatedCableRecipe(RecipeExporter exporter, ItemStack output, Ingredient input, Ingredient insulation, String suffix) {
        var item = output.getItem();
        createInsulatedCableRecipe(RecipeCategory.MISC, output.getItem(), output.getCount(), input, insulation).criterion(hasItem(item), conditionsFromItem(item)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerFramedCableRecipe(RecipeExporter exporter, ItemStack output, Ingredient input, String suffix) {
        var item = output.getItem();
        createFramedCableRecipe(RecipeCategory.MISC, output.getItem(), output.getCount(), input).criterion(hasItem(item), conditionsFromItem(item)).offerTo(exporter, "crafting/frame_" + suffix);
    }
    
    public void offerCableFromFrameRecipe(RecipeExporter exporter, ItemStack output, Ingredient frame, String suffix) {
        var item = output.getItem();
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, item, output.getCount()).input(frame).criterion(hasItem(item), conditionsFromItem(item)).offerTo(exporter, "crafting/unframe_" + suffix);
    }
    
    public void offerCableDuctRecipe(RecipeExporter exporter, ItemStack output, Ingredient input, String suffix) {
        var item = output.getItem();
        createCableDuctRecipe(RecipeCategory.MISC, item, output.getCount(), input).criterion(hasItem(item), conditionsFromItem(item)).offerTo(exporter, "crafting/duct_" + suffix);
    }
    
    public void offerCableFromDuctRecipe(RecipeExporter exporter, ItemStack output, Ingredient duct, String suffix) {
        var item = output.getItem();
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, item, output.getCount()).input(duct).criterion(hasItem(item), conditionsFromItem(item)).offerTo(exporter, "crafting/unduct_" + suffix);
    }
    
    public CraftingRecipeJsonBuilder createCableRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return ShapedRecipeJsonBuilder.create(category, output, count).input('#', input).pattern("   ").pattern("###");
    }
    
    public CraftingRecipeJsonBuilder createInsulatedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input, Ingredient insulation) {
        return ShapedRecipeJsonBuilder.create(category, output, count).input('c', input).input('i', insulation).pattern("iii").pattern("ccc").pattern("iii");
    }
    
    public CraftingRecipeJsonBuilder createFramedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return ShapedRecipeJsonBuilder.create(category, output, count).input('c', input).input('p', Ingredient.fromTag(TagContent.MACHINE_PLATING)).pattern("ccc").pattern("cpc").pattern("ccc");
    }
    
    public CraftingRecipeJsonBuilder createCableDuctRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return ShapedRecipeJsonBuilder.create(category, output, count).input('c', input).input('p', Ingredient.fromTag(TagContent.MACHINE_PLATING)).input('s', Ingredient.ofItems(Blocks.STONE)).pattern("csc").pattern("sps").pattern("csc");
    }
    
    public void offerMotorRecipe(RecipeExporter exporter, Item output, Ingredient shaft, Ingredient core, Ingredient wall, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', shaft).input('c', core).input('w', wall).pattern(" s ").pattern("wcw").pattern("wcw");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "motor/" + suffix);
    }
    
    public void offerManualAlloyRecipe(RecipeExporter exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        offerManualAlloyRecipe(exporter, output, A, B, 1, suffix);
    }
    
    public void offerManualAlloyRecipe(RecipeExporter exporter, Item output, Ingredient A, Ingredient B, int count, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, count).input('a', A).input('b', B).pattern("aa ").pattern("bb ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/alloy/" + suffix);
    }
    
    public void offerGeneratorRecipe(RecipeExporter exporter, Item output, Ingredient base, Ingredient sides, Ingredient core, Ingredient frame, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', sides).input('c', core).input('f', frame).input('b', base)
                        .pattern("fff")
                        .pattern("fcf")
                        .pattern("sbs");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerFurnaceRecipe(RecipeExporter exporter, Item output, Ingredient bottom, Ingredient botSides, Ingredient middleSides, Ingredient core, Ingredient top, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', botSides).input('c', core).input('f', top).input('b', bottom).input('m', middleSides)
                        .pattern("fff")
                        .pattern("mcm")
                        .pattern("sbs");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerAtomicForgeRecipe(RecipeExporter exporter, Item output, Ingredient base, Ingredient middleSides, Ingredient core, Ingredient top, Ingredient frame, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', top).input('c', core).input('f', frame).input('b', base).input('m', middleSides)
                        .pattern("fsf")
                        .pattern("mcm")
                        .pattern("bbb");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerBatteryRecipe(RecipeExporter exporter, Item output, Ingredient inner, Ingredient sides, Ingredient top, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('t', top).input('c', inner).input('f', sides)
                        .pattern(" t ")
                        .pattern("fcf")
                        .pattern("fcf");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerMachineFrameRecipe(RecipeExporter exporter, Item output, Ingredient base, Ingredient alt, int count, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, count).input('s', base).input('c', alt)
                        .pattern(" s ")
                        .pattern("csc")
                        .pattern(" s ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerMachineCoreRecipe(RecipeExporter exporter, Item output, Ingredient base, Ingredient alt, String suffix) {
        offerMachineCoreRecipe(exporter, output, 1, base, alt, suffix);
    }
    
    public void offerMachineCoreRecipe(RecipeExporter exporter, Item output, int count, Ingredient base, Ingredient alt, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, count).input('s', base).input('c', alt)
                        .pattern("sss")
                        .pattern("scs")
                        .pattern("sss");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerDrillRecipe(RecipeExporter exporter, Item output, Ingredient core, Ingredient motor, Ingredient center, Ingredient head, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', core).input('m', motor).input('a', center).input('e', head)
                        .pattern(" a ")
                        .pattern("aea")
                        .pattern("mss");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerWrenchRecipe(RecipeExporter exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('a', A).input('b', B)
                        .pattern(" a ")
                        .pattern(" ba")
                        .pattern("a  ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerChainsawRecipe(RecipeExporter exporter, Item output, Ingredient core, Ingredient motor, Ingredient center, Ingredient head, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('s', core).input('m', motor).input('a', center).input('e', head)
                        .pattern("aa ")
                        .pattern("ae ")
                        .pattern("mss");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerAxeRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("pp ")
                        .pattern("pc ")
                        .pattern(" c ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerPickaxeRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("ppp")
                        .pattern(" c ")
                        .pattern(" c ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerHelmetRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("ppp")
                        .pattern("pcp")
                        .pattern("   ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerChestplateRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("p p")
                        .pattern("ppp")
                        .pattern("pcp");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerLegsRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("ppp")
                        .pattern("pcp")
                        .pattern("p p");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerFeetRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('p', plating).input('c', core)
                        .pattern("   ")
                        .pattern("p p")
                        .pattern("c c");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerRodRecipe(RecipeExporter exporter, Item output, Ingredient cap, Ingredient rod, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('c', cap).input('r', rod)
                        .pattern(" c ")
                        .pattern(" r ")
                        .pattern(" r ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerRodCombinationRecipe(RecipeExporter exporter, Item output, Ingredient cap, Ingredient rod, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('c', cap).input('r', rod)
                        .pattern("   ")
                        .pattern("rcr")
                        .pattern("   ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerStarRecipe(RecipeExporter exporter, Item output, Ingredient inner, Ingredient outer, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('c', inner).input('o', outer)
                        .pattern(" o ")
                        .pattern("oco")
                        .pattern(" o ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerTankRecipe(RecipeExporter exporter, Item output, Ingredient plating, Ingredient core, Ingredient sides, String suffix) {
        offerTankRecipe(exporter, output, 1, plating, core, sides, suffix);
    }
    
    public void offerTankRecipe(RecipeExporter exporter, Item output, int count, Ingredient plating, Ingredient core, Ingredient sides, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, count).input('p', plating).input('s', sides).input('c', core)
                        .pattern("ppp")
                        .pattern("scs")
                        .pattern("ppp");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerTwoComponentRecipe(RecipeExporter exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('a', A).input('b', B)
                        .pattern("ab ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerLeverRecipe(RecipeExporter exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('a', A).input('b', B)
                        .pattern("a  ")
                        .pattern("b  ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerParticleMotorRecipe(RecipeExporter exporter, Item output, Ingredient rail, Ingredient top, Ingredient baseInner, Ingredient baseOuter, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output, 1).input('r', rail).input('t', top).input('i', baseInner).input('o', baseOuter)
                        .pattern(" t ")
                        .pattern("rrr")
                        .pattern("oio");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerMachinePlatingRecipe(RecipeExporter exporter, Item output, Ingredient side, Ingredient edge, Ingredient core, int count, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, output, count).input('a', side).input('e', edge).input('c', core)
                        .pattern("eae")
                        .pattern("aca")
                        .pattern("eae");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerDoorRecipe(RecipeExporter exporter, Item output, Ingredient A, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, output, 1).input('a', A)
                        .pattern("aa ")
                        .pattern("aa ")
                        .pattern("aa ");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/" + suffix);
    }
    
    public void offerSlabRecipe(RecipeExporter exporter, Item output, Ingredient A, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, output, 6).input('a', A)
                        .pattern("aaa");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/slab/" + suffix);
    }
    
    public void offerStairsRecipe(RecipeExporter exporter, Item output, Ingredient A, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, output, 4).input('a', A)
                        .pattern("a  ")
                        .pattern("aa ")
                        .pattern("aaa");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/stairs/" + suffix);
    }
    
    public void offerPressurePlateRecipe(RecipeExporter exporter, Item output, Ingredient A, String suffix) {
        var builder = ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, output, 1).input('a', A)
                        .pattern("aa");
        builder.criterion(hasItem(output), conditionsFromItem(output)).offerTo(exporter, "crafting/pressureplate/" + suffix);
    }
}
