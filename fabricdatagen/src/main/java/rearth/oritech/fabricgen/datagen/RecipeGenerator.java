package rearth.oritech.fabricgen.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.architectury.fluid.FluidStack;
import me.jddev0.ep.EnergizedPowerMod;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.impl.resource.conditions.conditions.AllModsLoadedResourceCondition;
import net.fabricmc.fabric.impl.resource.conditions.conditions.TagsPopulatedResourceCondition;
import net.minecraft.block.Blocks;
import net.minecraft.data.server.recipe.CraftingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import nourl.mythicmetals.MythicMetals;
import rearth.oritech.api.recipe.OreTransform;
import rearth.oritech.api.recipe.FuelGeneratorRecipeBuilder;
import rearth.oritech.fabricgen.datagen.compat.AlloyForgeryRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.EnergizedPowerRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.MythicMetalsRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.TechRebornRecipeGenerator;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.util.SizedIngredient;
import techreborn.TechReborn;
import wraith.alloyforgery.AlloyForgery;

import static rearth.oritech.util.datagen.RecipeGeneratorUtil.*;

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
        addAssemblerRecipe(exporter, of(Items.HONEYCOMB), of(TagContent.BIOFUEL), of(TagContent.BIOFUEL), of(TagContent.BIOFUEL), Items.SLIME_BALL, 1f, "slime");
        // fireball in assembler (gunpowder, blaze powder + coal) = 5 charges
        addAssemblerRecipe(exporter, of(Items.GUNPOWDER), of(Items.BLAZE_POWDER), of(ItemTags.COALS), of(ItemTags.COALS), Items.FIRE_CHARGE, 4, 1f, "fireball");
        // blaze rod (4 powder in assembler)
        addAssemblerRecipe(exporter, of(Items.BLAZE_POWDER), of(Items.BLAZE_POWDER), of(Items.BLAZE_POWDER), of(Items.BLAZE_POWDER), Items.BLAZE_ROD, 1f, "blazerod");
        // enderic compound from sculk
        addCentrifugeRecipe(exporter, of(Items.SCULK), ItemContent.ENDERIC_COMPOUND, 4f, "endericsculk");
        // budding amethyst (amethyst shard x2, enderic compound, overcharged crystal)
        addAssemblerRecipe(exporter, of(ConventionalItemTags.AMETHYST_GEMS), of(ConventionalItemTags.AMETHYST_GEMS), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.OVERCHARGED_CRYSTAL), Items.BUDDING_AMETHYST, 1f, "amethystbud");
        // netherite alloying (yes this is pretty OP)
        addAlloyRecipe(exporter, of(ConventionalItemTags.GOLD_INGOTS), of(Items.NETHERITE_SCRAP), Items.NETHERITE_INGOT, "netherite");
        // books
        addAssemblerRecipe(exporter, of(Items.PAPER), of(Items.PAPER), of(Items.PAPER), of(ConventionalItemTags.LEATHERS), Items.BOOK, 2, 1f, "book");
        // reinforced deepslate
        addAtomicForgeRecipe(exporter, of(ItemContent.DURATIUM_INGOT), of(Items.DEEPSLATE), Items.REINFORCED_DEEPSLATE, 100, "reinfdeepslate");
        // cobblestone to gravel
        addPulverizerRecipe(exporter, of(ConventionalItemTags.COBBLESTONES), Items.GRAVEL, "gravel");
        addGrinderRecipe(exporter, of(ConventionalItemTags.COBBLESTONES), Items.GRAVEL, "gravel");
        // gravel to sand
        addPulverizerRecipe(exporter, of(cItemTag("gravels")), Items.SAND, "sand_from_gravel");
        addGrinderRecipe(exporter, of(cItemTag("gravels")), Items.SAND, "sand_from_gravel");
        // sandstone to sand
        addPulverizerRecipe(exporter, of(ConventionalItemTags.SANDSTONE_BLOCKS), Items.SAND, "sand_from_sandstone");
        addGrinderRecipe(exporter, of(ConventionalItemTags.SANDSTONE_BLOCKS), Items.SAND, "sand_from_sandstone");
        // red sandstone to red sand
        addPulverizerRecipe(exporter, of(ConventionalItemTags.RED_SANDSTONE_BLOCKS), Items.RED_SAND, "red_sand");
        addGrinderRecipe(exporter, of(ConventionalItemTags.RED_SANDSTONE_BLOCKS), Items.RED_SAND, "red_sand");
        // centrifuge dirt into clay
        addCentrifugeFluidRecipe(exporter, of(ItemTags.DIRT), Items.CLAY, Fluids.WATER, 0.25f, null, 0, 1.0f, "clay");
        // create dirt from sand + biomass
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Items.DIRT, 2).input('s', ItemTags.SAND).input('b', TagContent.BIOFUEL).pattern("sb").pattern("bs").criterion("has_biomass", conditionsFromTag(TagContent.BIOFUEL)).offerTo(exporter);
        // dripstone from dripstone block
        addPulverizerRecipe(exporter, of(Items.DRIPSTONE_BLOCK), Items.POINTED_DRIPSTONE, 4, "dripstone");
        addGrinderRecipe(exporter, of(Items.DRIPSTONE_BLOCK), Items.POINTED_DRIPSTONE, 4, "dripstone");
        // shroomlight from logs and 3 glowstone
        addAssemblerRecipe(exporter, of(ItemTags.LOGS), of(Items.GLOWSTONE), of(Items.GLOWSTONE), of(Items.GLOWSTONE), Items.SHROOMLIGHT, 1f, "shroomlight");
    }
    
    private void addDyes(RecipeExporter exporter) {
      addPulverizerRecipe(exporter, Ingredient.fromTag(TagContent.RAW_WHITE_DYE), Items.WHITE_DYE, "dyes/white");
        addGrinderRecipe(exporter, of(TagContent.RAW_WHITE_DYE), Items.WHITE_DYE, "dyes/white");
        addPulverizerRecipe(exporter, of(TagContent.RAW_LIGHT_GRAY_DYE), Items.LIGHT_GRAY_DYE, "dyes/light_gray");
        addGrinderRecipe(exporter, of(TagContent.RAW_LIGHT_GRAY_DYE), Items.LIGHT_GRAY_DYE, "dyes/light_gray");
        addPulverizerRecipe(exporter, of(TagContent.RAW_BLACK_DYE), Items.BLACK_DYE, "dyes/black");
        addGrinderRecipe(exporter, of(TagContent.RAW_BLACK_DYE), Items.BLACK_DYE, "dyes/black");
        addPulverizerRecipe(exporter, of(TagContent.RAW_RED_DYE), Items.RED_DYE, "dyes/red");
        addGrinderRecipe(exporter, of(TagContent.RAW_RED_DYE), Items.RED_DYE, "dyes/red");
        addPulverizerRecipe(exporter, of(TagContent.RAW_ORANGE_DYE), Items.ORANGE_DYE, "dyes/orange");
        addGrinderRecipe(exporter, of(TagContent.RAW_ORANGE_DYE), Items.ORANGE_DYE, "dyes/orange");
        addPulverizerRecipe(exporter, of(TagContent.RAW_YELLOW_DYE), Items.YELLOW_DYE, "dyes/yellow");
        addGrinderRecipe(exporter, of(TagContent.RAW_YELLOW_DYE), Items.YELLOW_DYE, "dyes/yellow");
        addPulverizerRecipe(exporter, of(TagContent.RAW_CYAN_DYE), Items.CYAN_DYE, "dyes/cyan");
        addGrinderRecipe(exporter, of(TagContent.RAW_CYAN_DYE), Items.CYAN_DYE, "dyes/cyan");
        addPulverizerRecipe(exporter, of(TagContent.RAW_BLUE_DYE), Items.BLUE_DYE, "dyes/blue");
        addGrinderRecipe(exporter, of(TagContent.RAW_BLUE_DYE), Items.BLUE_DYE, "dyes/blue");
        addPulverizerRecipe(exporter, of(TagContent.RAW_MAGENTA_DYE), Items.MAGENTA_DYE, "dyes/magenta");
        addGrinderRecipe(exporter, of(TagContent.RAW_MAGENTA_DYE), Items.MAGENTA_DYE, "dyes/magenta");
        addPulverizerRecipe(exporter, of(TagContent.RAW_PINK_DYE), Items.PINK_DYE, "dyes/pink");
        addGrinderRecipe(exporter, of(TagContent.RAW_PINK_DYE), Items.PINK_DYE, "dyes/pink");
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
        addBioGenRecipe(exporter, of(TagContent.BIOMASS), 15, "rawbio");
        addBioGenRecipe(exporter, of(ItemContent.PACKED_WHEAT), 200, "packedwheat");
        addBioGenRecipe(exporter, of(TagContent.BIOFUEL), 25, "biomass");
        addBioGenRecipe(exporter, of(ItemContent.SOLID_BIOFUEL), 160, "solidbiomass");
        addBioGenRecipe(exporter, of(TagContent.BIOFUEL_BLOCK), 160, "biomassblock");
        addBioGenRecipe(exporter, of(ItemContent.RAW_BIOPOLYMER), 300, "polymer");
        addBioGenRecipe(exporter, of(ItemContent.UNHOLY_INTELLIGENCE), 3000, "vex");
        // lava
        addLavaGen(exporter, FluidStack.create(Fluids.LAVA, 8100), 12, "lava");
        // fuel
        FuelGeneratorRecipeBuilder.build().fluidInput(FluidContent.STILL_OIL.get(), 1f).timeInSeconds(3).export(exporter, "crude");
        FuelGeneratorRecipeBuilder.build().fluidInput(FluidContent.STILL_FUEL.get(), 1f).timeInSeconds(12).export(exporter, "fuel");
        //steam
        addSteamEngineGen(exporter, FluidStack.create(FluidContent.STILL_STEAM.get(), 32), 1, "steameng");
    }
    
    private void addBiomass(RecipeExporter exporter) {
        // biomass
        addPulverizerRecipe(exporter, of(TagContent.BIOMASS), ItemContent.BIOMASS, 1, "biobasic");
        addPulverizerRecipe(exporter, of(ItemContent.PACKED_WHEAT), ItemContent.BIOMASS, 16, "packagedwheatbio");
        addPulverizerRecipe(exporter, of(cItemTag("storage_blocks/wheat")), ItemContent.BIOMASS, 16, "hay_block");
        addAssemblerRecipe(exporter, of(TagContent.BIOFUEL), of(TagContent.BIOFUEL), of(TagContent.BIOFUEL), of(ItemTags.PLANKS), ItemContent.SOLID_BIOFUEL, 1, "solidbiofuel");
    }
    
    private void addEquipment(RecipeExporter exporter) {
        offerDrillRecipe(exporter, ToolsContent.HAND_DRILL, of(TagContent.STEEL_INGOTS), of(ItemContent.MOTOR), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.ADAMANT_INGOT), "handdrill");
        offerChainsawRecipe(exporter, ToolsContent.CHAINSAW, of(TagContent.STEEL_INGOTS), of(ItemContent.MOTOR), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.ADAMANT_INGOT), "chainsaw");
        offerAxeRecipe(exporter, ToolsContent.PROMETHIUM_AXE, of(ItemContent.PROMETHEUM_INGOT), of(BlockContent.DESTROYER_BLOCK.asItem()), "promaxe");
        offerPickaxeRecipe(exporter, ToolsContent.PROMETHIUM_PICKAXE, of(ItemContent.PROMETHEUM_INGOT), of(BlockContent.DESTROYER_BLOCK.asItem()), "prompick");
        
        // designator
        offerDrillRecipe(exporter, ItemContent.TARGET_DESIGNATOR, of(TagContent.STEEL_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.PROCESSING_UNIT), of(TagContent.PLASTIC_PLATES), "designator");
        // weed killer
        offerDrillRecipe(exporter, ItemContent.WEED_KILLER, of(ConventionalItemTags.FOOD_POISONING_FOODS), of(ConventionalItemTags.FOOD_POISONING_FOODS), of(ItemContent.RAW_BIOPOLYMER), of(Items.GLASS_BOTTLE), "weedex");
        // wrench
        offerWrenchRecipe(exporter, ItemContent.WRENCH, of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), "wrench");
        
        // helmet (enderic lens + machine plating)
        offerHelmetRecipe(exporter, ToolsContent.EXO_HELMET, of(TagContent.MACHINE_PLATING), of(ItemContent.ENDERIC_LENS), "exohelm");
        // chestplate (advanced battery + machine plating)
        offerChestplateRecipe(exporter, ToolsContent.EXO_CHESTPLATE, of(TagContent.MACHINE_PLATING), of(ItemContent.ADVANCED_BATTERY), "exochest");
        // legs (motor + plating)
        offerLegsRecipe(exporter, ToolsContent.EXO_LEGGINGS, of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), "exolegs");
        // feet (silicon + plating)
        offerFeetRecipe(exporter, ToolsContent.EXO_BOOTS, of(TagContent.MACHINE_PLATING), of(TagContent.SILICON), "exoboots");
        
        // basic jetpack main
        offerParticleMotorRecipe(exporter, ToolsContent.JETPACK, of(TagContent.STEEL_INGOTS), of(ConventionalItemTags.LEATHERS), of(ItemContent.ADVANCED_BATTERY), of(Items.GUNPOWDER), "basicjetpack");
        // jetpack alt
        offerParticleMotorRecipe(exporter, ToolsContent.JETPACK, of(TagContent.STEEL_INGOTS), of(ConventionalItemTags.LEATHERS), of(Items.REDSTONE_BLOCK), of(Items.BLAZE_POWDER), "basicjetpackalt");
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
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.CEILING_LIGHT.asItem(), 6), of(Items.GLOWSTONE_DUST), of(TagContent.STEEL_INGOTS), "ceilightlight");
        // hanging light
        offerTwoComponentRecipe(exporter, BlockContent.CEILING_LIGHT_HANGING.asItem(), of(ConventionalItemTags.CHAINS), of(BlockContent.CEILING_LIGHT.asItem()), "hanginglight");
        // tech button
        offerLeverRecipe(exporter, BlockContent.TECH_BUTTON.asItem(), of(ConventionalItemTags.COPPER_INGOTS), of(TagContent.STEEL_INGOTS), "techbutton");
        // tech lever
        offerLeverRecipe(exporter, BlockContent.TECH_LEVER.asItem(), of(TagContent.CARBON_FIBRE), of(TagContent.STEEL_INGOTS), "techlever");
        // tech door
        offerDoorRecipe(exporter, BlockContent.TECH_DOOR.asItem(), of(TagContent.STEEL_INGOTS), "techdoor");
        // metal beam
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.METAL_BEAM_BLOCK.asItem(), 6), of(TagContent.CARBON_FIBRE), of(TagContent.STEEL_INGOTS), "metalbeams");
        // tech glass
        offerMachinePlatingRecipe(exporter, BlockContent.INDUSTRIAL_GLASS_BLOCK.asItem(), of(TagContent.STEEL_INGOTS), of(ConventionalItemTags.GLASS_BLOCKS), of(TagContent.MACHINE_PLATING), 4, "industrialglass");
        // machine plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.MACHINE_PLATING_SLAB.asItem(), of(BlockContent.MACHINE_PLATING_BLOCK.asItem()), "machine");
        offerStairsRecipe(exporter, BlockContent.MACHINE_PLATING_STAIRS.asItem(), of(BlockContent.MACHINE_PLATING_BLOCK.asItem()), "machine");
        offerPressurePlateRecipe(exporter, BlockContent.MACHINE_PLATING_PRESSURE_PLATE.asItem(), of(BlockContent.MACHINE_PLATING_BLOCK.asItem()), "machine");
        // iron plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.IRON_PLATING_SLAB.asItem(), of(BlockContent.IRON_PLATING_BLOCK.asItem()), "iron");
        offerStairsRecipe(exporter, BlockContent.IRON_PLATING_STAIRS.asItem(), of(BlockContent.IRON_PLATING_BLOCK.asItem()), "iron");
        offerPressurePlateRecipe(exporter, BlockContent.IRON_PLATING_PRESSURE_PLATE.asItem(), of(BlockContent.IRON_PLATING_BLOCK.asItem()), "iron");
        // nickel plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.NICKEL_PLATING_SLAB.asItem(), of(BlockContent.NICKEL_PLATING_BLOCK.asItem()), "nickel");
        offerStairsRecipe(exporter, BlockContent.NICKEL_PLATING_STAIRS.asItem(), of(BlockContent.NICKEL_PLATING_BLOCK.asItem()), "nickel");
        offerPressurePlateRecipe(exporter, BlockContent.NICKEL_PLATING_PRESSURE_PLATE.asItem(), of(BlockContent.NICKEL_PLATING_BLOCK.asItem()), "nickel");
    }
    
    private void addMachines(RecipeExporter exporter) {
        // basic generator
        offerGeneratorRecipe(exporter, BlockContent.BASIC_GENERATOR_BLOCK.asItem(), of(ConventionalItemTags.PLAYER_WORKSTATIONS_FURNACES), of(ItemContent.MAGNETIC_COIL), of(ConventionalItemTags.COPPER_INGOTS), of(TagContent.NICKEL_INGOTS), "basicgen");
        // pulverizer
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), of(ConventionalItemTags.STORAGE_BLOCKS_IRON), of(ItemContent.MOTOR), of(TagContent.NICKEL_INGOTS), of(TagContent.STEEL_INGOTS), "pulverizer");
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(TagContent.NICKEL_INGOTS), of(ConventionalItemTags.IRON_INGOTS), "pulverizeralt");
        // electric furnace
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), of(ConventionalItemTags.PLAYER_WORKSTATIONS_FURNACES), of(ItemContent.MAGNETIC_COIL), of(TagContent.SILICON), of(TagContent.ELECTRUM_INGOTS), of(ConventionalItemTags.COPPER_INGOTS), "electricfurnace");
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), of(ConventionalItemTags.PLAYER_WORKSTATIONS_FURNACES), of(ItemContent.MAGNETIC_COIL), of(TagContent.PLATINUM_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(ConventionalItemTags.COPPER_INGOTS), "electricfurnacealt");
        // assembler
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), of(Blocks.BLAST_FURNACE.asItem()), of(ItemContent.MOTOR), of(Items.CRAFTER), of(ItemContent.ADAMANT_INGOT), of(ConventionalItemTags.COPPER_INGOTS), "assembler");
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(Items.CRAFTER), of(ItemContent.ADAMANT_INGOT), of(ConventionalItemTags.COPPER_INGOTS), "assembleralt");
        // foundry
        offerGeneratorRecipe(exporter, BlockContent.FOUNDRY_BLOCK.asItem(), of(Blocks.CAULDRON.asItem()), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.MOTOR), of(ConventionalItemTags.COPPER_INGOTS), "foundry");
        // cooler
        offerGeneratorRecipe(exporter, BlockContent.COOLER_BLOCK.asItem(), of(Blocks.CAULDRON.asItem()), of(Blocks.ICE.asItem()), of(ItemContent.MOTOR), of(ConventionalItemTags.IRON_INGOTS), "cooler");
        // centrifuge
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.PROCESSING_UNIT), of(ItemContent.MOTOR), of(TagContent.STEEL_INGOTS), of(Items.GLASS_BOTTLE), "centrifuge");
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE_BLOCK.asItem(), of(ItemContent.MOTOR), of(ConventionalItemTags.STORAGE_BLOCKS_IRON), of(ConventionalItemTags.COPPER_INGOTS), of(ItemContent.MOTOR), of(Items.GLASS_BOTTLE), "centrifugealt");
        // laser arm
        offerAtomicForgeRecipe(exporter, BlockContent.LASER_ARM_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENDERIC_LENS), of(TagContent.CARBON_FIBRE), "laserarm");
        // crusher
        offerGeneratorRecipe(exporter, BlockContent.FRAGMENT_FORGE_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(ItemContent.FLUX_GATE), of(TagContent.PLASTIC_PLATES), "crusher");
        // atomic forge
        offerAtomicForgeRecipe(exporter, BlockContent.ATOMIC_FORGE_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.PLASTIC_PLATES), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.DURATIUM_INGOT), of(ItemContent.FLUX_GATE), "atomicforge");
        
        // biofuel generator
        offerGeneratorRecipe(exporter, BlockContent.BIO_GENERATOR_BLOCK.asItem(), of(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), of(ItemContent.MAGNETIC_COIL), of(ItemContent.FLUX_GATE), of(ItemContent.BIOSTEEL_INGOT), "biogen");
        // lava generator
        offerGeneratorRecipe(exporter, BlockContent.LAVA_GENERATOR_BLOCK.asItem(), of(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), of(TagContent.MACHINE_PLATING), of(ItemContent.MAGNETIC_COIL), of(TagContent.ELECTRUM_INGOTS), "lavagen");
        // steam engine
        offerGeneratorRecipe(exporter, BlockContent.STEAM_ENGINE_BLOCK.asItem(), of(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), of(ConventionalItemTags.COPPER_INGOTS), of(ItemContent.MAGNETIC_COIL), of(TagContent.ELECTRUM_INGOTS), "steamgen");
        // diesel generator
        offerGeneratorRecipe(exporter, BlockContent.FUEL_GENERATOR_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(BlockContent.BASIC_GENERATOR_BLOCK), of(ItemContent.ENDERIC_LENS), of(TagContent.STEEL_INGOTS), "fuelgen");
        // large solar
        offerGeneratorRecipe(exporter, BlockContent.BIG_SOLAR_PANEL_BLOCK.asItem(), of(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), of(ItemContent.FLUX_GATE), of(ItemContent.ADVANCED_BATTERY), of(ItemContent.FLUXITE), "solar");
        
        // charger
        offerAtomicForgeRecipe(exporter, BlockContent.CHARGER_BLOCK.asItem(), of(ConventionalItemTags.WOODEN_CHESTS), of(BlockContent.ENERGY_PIPE), of(ConventionalItemTags.STORAGE_BLOCKS_REDSTONE), of(Items.DISPENSER), of(TagContent.STEEL_INGOTS), "charger");
        offerAtomicForgeRecipe(exporter, BlockContent.CHARGER_BLOCK.asItem(), of(ConventionalItemTags.WOODEN_CHESTS), of(BlockContent.ENERGY_PIPE), of(ItemContent.PROCESSING_UNIT), of(Items.DISPENSER), of(TagContent.STEEL_INGOTS), "chargeralt");
        
        // small storage
        offerAtomicForgeRecipe(exporter, BlockContent.SMALL_STORAGE_BLOCK.asItem(), of(ItemContent.BASIC_BATTERY), of(TagContent.SILICON), of(ItemContent.MAGNETIC_COIL), of(TagContent.NICKEL_INGOTS), of(TagContent.WIRES), "smallstorage");
        // large storage
        offerAtomicForgeRecipe(exporter, BlockContent.LARGE_STORAGE_BLOCK.asItem(), of(ItemContent.ADVANCED_BATTERY), of(TagContent.STEEL_INGOTS), of(ItemContent.DUBIOS_CONTAINER), of(ItemContent.FLUX_GATE), of(TagContent.WIRES), "bigstorage");
        // unstable container
        offerAtomicForgeRecipe(exporter, ItemContent.UNSTABLE_CONTAINER, of(ItemContent.FLUXITE), of(ItemContent.DURATIUM_INGOT), of(BlockContent.LARGE_STORAGE_BLOCK), of(ItemContent.FLUX_GATE), of(ItemContent.SUPER_AI_CHIP), "unstablecontainer");
        
        // fluid tank
        offerTankRecipe(exporter, BlockContent.SMALL_TANK_BLOCK.asItem(), of(ConventionalItemTags.COPPER_INGOTS), of(ConventionalItemTags.GLASS_BLOCKS), of(BlockContent.FLUID_PIPE.asItem()), "stank");
        // pump
        offerGeneratorRecipe(exporter, BlockContent.PUMP_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.SILICON), of(ItemContent.MOTOR), of(ConventionalItemTags.COPPER_INGOTS), "pump");
        // block placer
        offerFurnaceRecipe(exporter, BlockContent.PLACER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(ItemContent.PROCESSING_UNIT), of(BlockContent.MACHINE_FRAME_BLOCK.asItem()), of(ConventionalItemTags.COPPER_INGOTS), "placer");
        // block destroyer
        offerAtomicForgeRecipe(exporter, BlockContent.DESTROYER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(BlockContent.PULVERIZER_BLOCK), of(BlockContent.LASER_ARM_BLOCK), of(ItemContent.MOTOR), "destroyer");
        // fertilizer
        offerFurnaceRecipe(exporter, BlockContent.FERTILIZER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(TagContent.SILICON), of(ItemContent.PROCESSING_UNIT), of(ConventionalItemTags.COPPER_INGOTS), "fertilizer");
        // tree feller
        offerGeneratorRecipe(exporter, BlockContent.TREEFELLER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(Items.IRON_AXE), of(ItemContent.MOTOR), of(TagContent.ELECTRUM_INGOTS), "treefeller");
        // pipe booster
        offerTankRecipe(exporter, BlockContent.PIPE_BOOSTER_BLOCK.asItem(), of(BlockContent.ITEM_PIPE), of(ItemContent.MOTOR), of(BlockContent.FLUID_PIPE), "booster");
        
        // machine frame
        offerMachineFrameRecipe(exporter, BlockContent.MACHINE_FRAME_BLOCK.asItem(), of(Items.IRON_BARS), of(TagContent.NICKEL_INGOTS), 16, "frame");
        // energy pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE.asItem(), 6), of(TagContent.ELECTRUM_INGOTS), of(TagContent.WIRES), "energy");
        // item pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE.asItem(), 6), of(TagContent.NICKEL_INGOTS), of(ItemTags.PLANKS), "item");
        // item filter
        offerGeneratorRecipe(exporter, BlockContent.ITEM_FILTER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.WIRES), of(ItemContent.PROCESSING_UNIT), of(TagContent.WIRES), "itemfilter");
        // fluid pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE.asItem(), 6), of(TagContent.SILICON), of(ConventionalItemTags.COPPER_INGOTS), "fluidpipe");
        
        // framed energy pipe
        offerFramedCableRecipe(exporter, new ItemStack(BlockContent.FRAMED_ENERGY_PIPE, 8), of(BlockContent.ENERGY_PIPE), "energy");
        offerCableFromFrameRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE, 1), of(BlockContent.FRAMED_ENERGY_PIPE), "energy");
        // framed superconductor
        offerFramedCableRecipe(exporter, new ItemStack(BlockContent.FRAMED_SUPERCONDUCTOR, 8), of(BlockContent.SUPERCONDUCTOR.asItem()), "superconductor");
        offerCableFromFrameRecipe(exporter, new ItemStack(BlockContent.SUPERCONDUCTOR.asItem(), 1), of(BlockContent.FRAMED_SUPERCONDUCTOR), "superconductor");
        // framed fluid pipe
        offerFramedCableRecipe(exporter, new ItemStack(BlockContent.FRAMED_FLUID_PIPE, 8), of(BlockContent.FLUID_PIPE), "fluid");
        offerCableFromFrameRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE, 1), of(BlockContent.FRAMED_FLUID_PIPE), "fluid");
        // framed item pipe
        offerFramedCableRecipe(exporter, new ItemStack(BlockContent.FRAMED_ITEM_PIPE, 8), of(BlockContent.ITEM_PIPE), "item");
        offerCableFromFrameRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE, 1), of(BlockContent.FRAMED_ITEM_PIPE), "item");
        
        // transparent pipe
        offerTankRecipe(exporter, BlockContent.TRANSPARENT_ITEM_PIPE.asItem(), 6, of(ItemTags.PLANKS), of(TagContent.NICKEL_INGOTS), of(ConventionalItemTags.GLASS_BLOCKS), "transparentitem");
        offerMachineCoreRecipe(exporter, BlockContent.TRANSPARENT_ITEM_PIPE.asItem(), 8, of(BlockContent.ITEM_PIPE), of(ConventionalItemTags.GLASS_BLOCKS), "totransparent");
        offerMachineCoreRecipe(exporter, BlockContent.ITEM_PIPE.asItem(), 8, of(BlockContent.TRANSPARENT_ITEM_PIPE), of(ItemTags.PLANKS), "fromtransparent");
        
        // energy pipe duct
        offerCableDuctRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE_DUCT_BLOCK, 4), of(BlockContent.ENERGY_PIPE), "energy");
        offerCableFromDuctRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE, 1), of(BlockContent.ENERGY_PIPE_DUCT_BLOCK), "energy");
        // superconductor duct
        offerCableDuctRecipe(exporter, new ItemStack(BlockContent.SUPERCONDUCTOR_DUCT_BLOCK, 4), of(BlockContent.SUPERCONDUCTOR.asItem()), "superconductor");
        offerCableFromDuctRecipe(exporter, new ItemStack(BlockContent.SUPERCONDUCTOR.asItem(), 1), of(BlockContent.SUPERCONDUCTOR_DUCT_BLOCK), "superconductor");
        // fluid pipe duct
        offerCableDuctRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE_DUCT_BLOCK, 4), of(BlockContent.FLUID_PIPE), "fluid");
        offerCableFromDuctRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE, 1), of(BlockContent.FLUID_PIPE_DUCT_BLOCK), "fluid");
        // item pipe duct
        offerCableDuctRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE_DUCT_BLOCK, 4), of(BlockContent.ITEM_PIPE), "item");
        offerCableFromDuctRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE, 1), of(BlockContent.ITEM_PIPE_DUCT_BLOCK), "item");
        
        // deep drill
        offerAtomicForgeRecipe(exporter, BlockContent.DEEP_DRILL_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(ItemContent.HEISENBERG_COMPENSATOR), of(ItemContent.OVERCHARGED_CRYSTAL), of(ItemContent.DURATIUM_INGOT), "deepdrill");
        // drone port
        offerAtomicForgeRecipe(exporter, BlockContent.DRONE_PORT_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(BlockContent.SUPERCONDUCTOR.asItem()), of(ItemContent.UNHOLY_INTELLIGENCE), of(ItemContent.ADVANCED_COMPUTING_ENGINE), "droneport");
        offerAtomicForgeRecipe(exporter, BlockContent.DRONE_PORT_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(BlockContent.SUPERCONDUCTOR.asItem()), of(ItemContent.SUPER_AI_CHIP), of(ItemContent.ADVANCED_COMPUTING_ENGINE), "droneportalt");
        
        // arcane catalyst
        offerFurnaceRecipe(exporter, BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem(), of(Items.ENCHANTING_TABLE), of(ItemContent.ADAMANT_INGOT), of(ConventionalItemTags.NORMAL_OBSIDIANS), of(ItemContent.UNHOLY_INTELLIGENCE), of(ItemContent.FLUXITE), "catalyst");
        offerFurnaceRecipe(exporter, BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem(), of(Items.ENCHANTING_TABLE), of(ItemContent.ADAMANT_INGOT), of(ConventionalItemTags.NORMAL_OBSIDIANS), of(ItemContent.SUPER_AI_CHIP), of(ItemContent.FLUXITE), "catalyst_alt");
        // enchanter
        offerGeneratorRecipe(exporter, BlockContent.ENCHANTER_BLOCK.asItem(), of(ItemContent.DURATIUM_INGOT), of(ItemContent.ENERGITE_INGOT), of(BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem()), of(Items.BOOK), "enchanter");
        // spawner
        offerTankRecipe(exporter, BlockContent.SPAWNER_CONTROLLER_BLOCK.asItem(), of(BlockContent.SPAWNER_CAGE_BLOCK), of(Blocks.RESPAWN_ANCHOR), of(BlockContent.ENCHANTMENT_CATALYST_BLOCK), "spawner");
        // spawner cage
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.SPAWNER_CAGE_BLOCK, 2), of(TagContent.PLASTIC_PLATES), of(Items.IRON_BARS), "cage");
        // withered rose
        offerMachineFrameRecipe(exporter, BlockContent.WITHER_CROP_BLOCK.asItem(), of(Items.WITHER_ROSE), of(ItemTags.FLOWERS), 1, "witherrose");
        
        // particle accelerator
        // motor
        offerParticleMotorRecipe(exporter, BlockContent.ACCELERATOR_MOTOR.asItem(), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.SUPERCONDUCTOR.asItem()), of(ItemContent.DURATIUM_INGOT), of(ItemContent.ADVANCED_BATTERY), "particlemotor");
        // ring
        offerDrillRecipe(exporter, BlockContent.ACCELERATOR_RING.asItem(), of(BlockContent.INDUSTRIAL_GLASS_BLOCK.asItem()), of(BlockContent.SUPERCONDUCTOR.asItem()), of(TagContent.STEEL_INGOTS), of(Items.REDSTONE_TORCH), "acceleratorring");
        // controller
        offerGeneratorRecipe(exporter, BlockContent.ACCELERATOR_CONTROLLER.asItem(), of(BlockContent.ACCELERATOR_MOTOR.asItem()), of(ItemContent.FLUX_GATE), of(Items.DROPPER), of(ItemContent.DURATIUM_INGOT), "particlecontroller");
        // sensor
        offerTwoComponentRecipe(exporter, BlockContent.ACCELERATOR_SENSOR.asItem(), of(BlockContent.ACCELERATOR_RING.asItem()), of(Items.OBSERVER), "particlesensor");
        // collector
        offerTankRecipe(exporter, BlockContent.PARTICLE_COLLECTOR_BLOCK.asItem(), of(BlockContent.SUPERCONDUCTOR.asItem()), of(BlockContent.BIG_SOLAR_PANEL_BLOCK.asItem()), of(ItemContent.HEISENBERG_COMPENSATOR), "particlecollector");
        
        
        // addons
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_SPEED_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MAGNETIC_COIL), of(ItemContent.BIOSTEEL_INGOT), of(TagContent.PLASTIC_PLATES), "addon/speed");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_PROCESSING_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.SUPER_AI_CHIP), of(ItemContent.FLUX_GATE), of(TagContent.PLATINUM_INGOTS), of(ItemContent.MOTOR), "addon/processing");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_PROCESSING_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.UNHOLY_INTELLIGENCE), of(Items.COMPARATOR), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.MOTOR), "addon/processingalt");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_ULTIMATE_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.HEISENBERG_COMPENSATOR), of(BlockContent.MACHINE_SPEED_ADDON), of(BlockContent.MACHINE_EFFICIENCY_ADDON), of(ItemContent.OVERCHARGED_CRYSTAL), "addon/ultimate");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_EFFICIENCY_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.CARBON_FIBRE), of(TagContent.ELECTRUM_INGOTS), of(TagContent.PLASTIC_PLATES), "addon/eff");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_CAPACITOR_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.ENERGITE_INGOT), of(ItemContent.MAGNETIC_COIL), of(TagContent.PLASTIC_PLATES), "addon/capacitor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_ACCEPTOR_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENERGITE_INGOT), of(TagContent.PLASTIC_PLATES), "addon/acceptor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_YIELD_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENDERIC_LENS), of(TagContent.PLASTIC_PLATES), "addon/yield");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_FLUID_ADDON.asItem(), of(TagContent.SILICON), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.FLUID_PIPE), of(TagContent.CARBON_FIBRE), "addon/fluid");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_INVENTORY_PROXY_ADDON.asItem(), of(ItemContent.MOTOR), of(ConventionalItemTags.CHESTS), of(ItemContent.PROCESSING_UNIT), of(TagContent.CARBON_FIBRE), "addon/invproxy");
        offerGeneratorRecipe(exporter, BlockContent.CROP_FILTER_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(ItemContent.PROCESSING_UNIT), of(TagContent.CARBON_FIBRE), "addon/cropfilter");
        offerGeneratorRecipe(exporter, BlockContent.QUARRY_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(Items.DIAMOND_PICKAXE), of(TagContent.PLASTIC_PLATES), "addon/quarry");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_HUNTER_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(Items.IRON_SWORD), of(TagContent.PLASTIC_PLATES), "_hunter");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.ADAMANT_INGOT), of(ConventionalItemTags.COPPER_INGOTS), of(BlockContent.FLUID_PIPE), "addon/steamboiler");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), of(TagContent.SILICON), of(ItemContent.ADAMANT_INGOT), of(BlockContent.FLUID_PIPE), of(TagContent.COAL_DUSTS), "addon/steamboileralt");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_REDSTONE_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(Items.REPEATER), of(Items.COMPARATOR), of(ConventionalItemTags.REDSTONE_DUSTS), "addon/redstone");
        offerTwoComponentRecipe(exporter, BlockContent.CAPACITOR_ADDON_EXTENDER.asItem(), of(BlockContent.MACHINE_EXTENDER.asItem()), of(BlockContent.MACHINE_CAPACITOR_ADDON), "addon/capextender");
        
        // cores
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_1.asItem(), of(ItemTags.PLANKS), of(Items.CRAFTING_TABLE), "core1");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), of(ConventionalItemTags.COPPER_INGOTS), of(ConventionalItemTags.LAPIS_GEMS), "core2");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), of(ConventionalItemTags.IRON_INGOTS), of(ConventionalItemTags.LAPIS_GEMS), "core2alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), of(TagContent.CARBON_FIBRE), of(ConventionalItemTags.REDSTONE_DUSTS), "core3");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), of(TagContent.NICKEL_INGOTS), of(ConventionalItemTags.REDSTONE_DUSTS), "core3alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_4.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.ENDERIC_COMPOUND), "core4");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_5.asItem(), of(ItemContent.ADAMANT_INGOT), of(ItemContent.ADVANCED_COMPUTING_ENGINE), "core5");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_6.asItem(), of(ItemContent.DURATIUM_INGOT), of(ItemContent.DUBIOS_CONTAINER), "core6");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_7.asItem(), of(ItemContent.PROMETHEUM_INGOT), of(BlockContent.SUPERCONDUCTOR.asItem()), "core7");
        
        // machine extender
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_EXTENDER.asItem(), of(TagContent.MACHINE_PLATING), of(BlockContent.MACHINE_CORE_2.asItem()), "extender");
        
        // augmenter
        // machine itself
        offerAtomicForgeRecipe(exporter, BlockContent.AUGMENT_APPLICATION_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(BlockContent.LARGE_STORAGE_BLOCK.asItem()), of(TagContent.CARBON_FIBRE), of(ItemContent.DUBIOS_CONTAINER), "augment/applicator");
        // basic station
        offerGeneratorRecipe(exporter, BlockContent.SIMPLE_AUGMENT_STATION.asItem(), of(Items.BREWING_STAND), of(TagContent.MACHINE_PLATING), of(ConventionalItemTags.STORAGE_BLOCKS_REDSTONE), of(TagContent.ELECTRUM_INGOTS), "augment/basic");
        // adv station
        offerGeneratorRecipe(exporter, BlockContent.ADVANCED_AUGMENT_STATION.asItem(), of(BlockContent.CENTRIFUGE_BLOCK), of(TagContent.MACHINE_PLATING), of(ItemContent.FLUX_GATE), of(ItemContent.DURATIUM_INGOT), "augment/advanced");
        // arcane station
        offerGeneratorRecipe(exporter, BlockContent.ARCANE_AUGMENT_STATION.asItem(), of(Items.ENDER_EYE), of(TagContent.MACHINE_PLATING), of(ItemContent.ENDERIC_LENS), of(ItemContent.OVERCHARGED_CRYSTAL), "augment/arcane");
        
    }
    
    private void addComponents(RecipeExporter exporter) {
        // coal stuff (including basic steel)
        addCentrifugeRecipe(exporter, of(TagContent.COAL_DUSTS), ItemContent.CARBON_FIBRE_STRANDS, 0.5f, "carbon");
        offerManualAlloyRecipe(exporter, ItemContent.STEEL_INGOT, of(ConventionalItemTags.IRON_INGOTS), of(ItemTags.COALS), "steel");
        
        // manual alloys
        offerManualAlloyRecipe(exporter, ItemContent.ELECTRUM_INGOT, of(ConventionalItemTags.GOLD_INGOTS), of(ConventionalItemTags.REDSTONE_DUSTS), "electrum");
        offerManualAlloyRecipe(exporter, ItemContent.ADAMANT_INGOT, of(TagContent.NICKEL_INGOTS), of(ConventionalItemTags.DIAMOND_GEMS), "adamant");
        
        // enderic entry
        addPulverizerRecipe(exporter, of(ConventionalItemTags.ENDER_PEARLS), ItemContent.ENDERIC_COMPOUND, 8, "pearl_enderic");
        addGrinderRecipe(exporter, of(ConventionalItemTags.ENDER_PEARLS), ItemContent.ENDERIC_COMPOUND, 12, "pearl_enderic");
        addGrinderRecipe(exporter, of(Blocks.END_STONE), ItemContent.ENDERIC_COMPOUND, 1, "stone_enderic");
        
        // fine wires
        offerCableRecipe(exporter, new ItemStack(ItemContent.INSULATED_WIRE, 4), of(TagContent.NICKEL_INGOTS), "insulatedwire");
        addAssemblerRecipe(exporter, of(TagContent.NICKEL_INGOTS), of(TagContent.NICKEL_INGOTS), of(TagContent.NICKEL_INGOTS), of(ConventionalItemTags.COPPER_INGOTS), ItemContent.INSULATED_WIRE, 12, 0.5f, "fwire");
        
        // magnetic coils
        offerInsulatedCableRecipe(exporter, new ItemStack(ItemContent.MAGNETIC_COIL, 2), of(TagContent.STEEL_INGOTS), of(TagContent.WIRES), "magnet");
        addAssemblerRecipe(exporter, of(TagContent.STEEL_INGOTS), of(TagContent.WIRES), of(TagContent.WIRES), of(TagContent.WIRES), ItemContent.MAGNETIC_COIL, 2, 0.5f, "magnet");
        
        // motor
        offerMotorRecipe(exporter, ItemContent.MOTOR, of(TagContent.NICKEL_INGOTS), of(ItemContent.MAGNETIC_COIL), of(TagContent.STEEL_INGOTS), "motorcraft");
        addAssemblerRecipe(exporter, of(TagContent.NICKEL_INGOTS), of(TagContent.STEEL_INGOTS), of(ItemContent.MAGNETIC_COIL), of(ItemContent.MAGNETIC_COIL), ItemContent.MOTOR, 2, 0.5f, "motor");
        
        // machine plating variants
        offerMachinePlatingRecipe(exporter, BlockContent.MACHINE_PLATING_BLOCK.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(ConventionalItemTags.COPPER_INGOTS), 2, "plating");
        addAssemblerRecipe(exporter, of(TagContent.STEEL_INGOTS), of(TagContent.STEEL_INGOTS), of(ConventionalItemTags.COPPER_INGOTS), of(TagContent.PLASTIC_PLATES), BlockContent.MACHINE_PLATING_BLOCK.asItem(), 8, 1f, "plating");
        offerMachinePlatingRecipe(exporter, BlockContent.IRON_PLATING_BLOCK.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(ConventionalItemTags.IRON_INGOTS), 2, "iron");
        addAssemblerRecipe(exporter, of(TagContent.STEEL_INGOTS), of(TagContent.STEEL_INGOTS), of(ConventionalItemTags.IRON_INGOTS), of(TagContent.PLASTIC_PLATES), BlockContent.IRON_PLATING_BLOCK.asItem(), 8, 1f, "platingiron");
        offerMachinePlatingRecipe(exporter, BlockContent.NICKEL_PLATING_BLOCK.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(TagContent.NICKEL_INGOTS), 2, "nickel");
        addAssemblerRecipe(exporter, of(TagContent.STEEL_INGOTS), of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), of(TagContent.PLASTIC_PLATES), BlockContent.NICKEL_PLATING_BLOCK.asItem(), 8, 1f, "platingnickel");
        
        // basic battery
        offerMotorRecipe(exporter, ItemContent.BASIC_BATTERY, of(TagContent.STEEL_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(TagContent.PLASTIC_PLATES), "manualbattery");
        addAssemblerRecipe(exporter, of(TagContent.PLASTIC_PLATES), of(TagContent.ELECTRUM_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(TagContent.STEEL_INGOTS), ItemContent.BASIC_BATTERY, 1, 0.5f, "battery");
        addAssemblerRecipe(exporter, of(TagContent.PLASTIC_PLATES), of(ItemContent.FLUXITE), of(ItemContent.FLUXITE), of(TagContent.STEEL_INGOTS), ItemContent.BASIC_BATTERY, 2, 1f, "batterybetter");
        
        // silicon
        offerManualAlloyRecipe(exporter, ItemContent.RAW_SILICON, of(TagContent.QUARTZ_DUSTS), of(ItemTags.SAND), 3, "rawsilicon");
        offerSmelting(exporter, List.of(ItemContent.RAW_SILICON), RecipeCategory.MISC, ItemContent.SILICON, 0.5f, 60, "siliconfurnace");
        
        // plastic
        offer2x2CompactingRecipe(exporter, RecipeCategory.MISC, ItemContent.PACKED_WHEAT, Items.WHEAT);
        addCentrifugeFluidRecipe(exporter, of(ItemContent.PACKED_WHEAT), ItemContent.RAW_BIOPOLYMER, Fluids.WATER, 0.25f, null, 0, 1f, "biopolymer");
        addCentrifugeFluidRecipe(exporter, of(ItemContent.SOLID_BIOFUEL), ItemContent.RAW_BIOPOLYMER, Fluids.WATER, 0.25f, null, 0, 1f, "biopolymer_biomass");
        addCentrifugeFluidRecipe(exporter, of(TagContent.BIOFUEL_BLOCK), ItemContent.RAW_BIOPOLYMER, Fluids.WATER, 0.25f, null, 0, 1f, "biopolymer_bioblock");
        addCentrifugeFluidRecipe(exporter, of(ItemTags.SAND), ItemContent.POLYMER_RESIN, FluidContent.STILL_OIL.get(), 0.1f, null, 0, 0.5f, "polymerresin");
        addCentrifugeFluidRecipe(exporter, of(ItemContent.RAW_BIOPOLYMER), ItemContent.PLASTIC_SHEET, Fluids.WATER, 0.5f, null, 0, 1f, "plasticoil");
        addCentrifugeFluidRecipe(exporter, of(ItemContent.POLYMER_RESIN), ItemContent.PLASTIC_SHEET, Fluids.WATER, 0.5f, null, 0, 0.33f, "plasticbio");
        
        // processing unit
        addAssemblerRecipe(exporter, of(TagContent.PLASTIC_PLATES), of(TagContent.CARBON_FIBRE), of(TagContent.ELECTRUM_INGOTS), of(ConventionalItemTags.REDSTONE_DUSTS), ItemContent.PROCESSING_UNIT, 1f, "processingunit");
        // enderic lens
        addAssemblerRecipe(exporter, of(ItemContent.ADAMANT_INGOT), of(TagContent.CARBON_FIBRE), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.ENDERIC_COMPOUND), ItemContent.ENDERIC_LENS, 1.5f, "enderlens");
        // flux gate
        addAssemblerRecipe(exporter, of(ItemContent.PROCESSING_UNIT), of(ItemContent.FLUXITE), of(ItemContent.FLUXITE), of(TagContent.PLATINUM_INGOTS), ItemContent.FLUX_GATE, 1.5f, "fluxgate");
        
        // ai processor tree
        addAtomicForgeRecipe(exporter, of(TagContent.SILICON), of(TagContent.CARBON_FIBRE), ItemContent.SILICON_WAFER, 5, "wafer");
        addAtomicForgeRecipe(exporter, of(ItemContent.SILICON_WAFER), of(ItemContent.PROCESSING_UNIT), ItemContent.ADVANCED_COMPUTING_ENGINE, 5, "advcomputer");
        addAtomicForgeRecipe(exporter, of(ItemContent.ADVANCED_COMPUTING_ENGINE), of(ItemContent.DURATIUM_INGOT), ItemContent.SUPER_AI_CHIP, 50, "aicomputer");
        
        // dubios container
        offerMotorRecipe(exporter, ItemContent.DUBIOS_CONTAINER, of(TagContent.PLASTIC_PLATES), of(ItemContent.ADAMANT_INGOT), of(ItemContent.ENDERIC_COMPOUND), "dubios");
        // adv battery
        offerMotorRecipe(exporter, ItemContent.ADVANCED_BATTERY, of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENERGITE_INGOT), of(TagContent.STEEL_INGOTS), "advbattery");
        
        // fuel
        addCentrifugeFluidRecipe(exporter, of(ItemContent.FLUXITE), (Item)null, FluidContent.STILL_OIL.get(), 1f, FluidContent.STILL_FUEL.get(), 1f, 1f, "fuel");
        addCentrifugeFluidRecipe(exporter, of(ItemContent.FLUXITE), (Item)null, FluidContent.STILL_BIOFUEL.get(), 1f, FluidContent.STILL_FUEL.get(), 1f, 1f, "fuel_from_biofuel");
        addCentrifugeFluidRecipe(exporter, of(TagContent.BIOFUEL), (Item)null, null, 0f, FluidContent.STILL_BIOFUEL.get(), 0.1f, 1f, "biofuel");
        
        // biosteel
        addAlloyRecipe(exporter, of(ItemContent.RAW_BIOPOLYMER), of(ConventionalItemTags.IRON_INGOTS), ItemContent.BIOSTEEL_INGOT, "biosteel");
        
        // endgame components
        addAtomicForgeRecipe(exporter, of(ItemContent.ADAMANT_INGOT), of(ItemContent.SUPER_AI_CHIP), ItemContent.HEISENBERG_COMPENSATOR, 60, "compensator");
        addAtomicForgeRecipe(exporter, of(ItemContent.ADAMANT_INGOT), of(ItemContent.UNHOLY_INTELLIGENCE), ItemContent.HEISENBERG_COMPENSATOR, 60, "compensatoralt");
        offerMotorRecipe(exporter, ItemContent.OVERCHARGED_CRYSTAL, of(Items.AMETHYST_BLOCK), of(ItemContent.ADVANCED_BATTERY), of(BlockContent.SUPERCONDUCTOR.asItem()), "overchargedcrystal");
        addAssemblerRecipe(exporter, of(ItemContent.FLUX_GATE), of(TagContent.WIRES), of(ItemContent.DUBIOS_CONTAINER), of(ItemContent.ENERGITE_INGOT), BlockContent.SUPERCONDUCTOR.asItem(), 3, 2f, "superconductor");
        addAtomicForgeRecipe(exporter, of(ItemContent.OVERCHARGED_CRYSTAL), of(ItemContent.HEISENBERG_COMPENSATOR), ItemContent.PROMETHEUM_INGOT, 240, "prometheum");
        
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
        
        List<OreTransform> oreChains = List.of(
          // iron chain
          new OreTransform(
            of(TagContent.IRON_ORES),
            of(ConventionalItemTags.IRON_RAW_MATERIALS), Items.RAW_IRON, ItemContent.RAW_NICKEL, 
            of(TagContent.IRON_CLUMPS), ItemContent.IRON_CLUMP,
            of(ItemContent.SMALL_IRON_CLUMP), ItemContent.SMALL_IRON_CLUMP, ItemContent.SMALL_NICKEL_CLUMP,
            of(TagContent.IRON_DUSTS), ItemContent.IRON_DUST,
            of(ItemContent.SMALL_IRON_DUST), ItemContent.SMALL_IRON_DUST, ItemContent.SMALL_NICKEL_DUST, 
            of(ItemContent.IRON_GEM), ItemContent.IRON_GEM,
            of(ItemContent.FLUXITE),
            of(ConventionalItemTags.IRON_NUGGETS), Items.IRON_NUGGET,
            of(ConventionalItemTags.IRON_INGOTS), Items.IRON_INGOT,
            1f, "iron", 3, true),

          // copper chain
          new OreTransform(
            of(TagContent.COPPER_ORES),
            of(ConventionalItemTags.COPPER_RAW_MATERIALS), Items.RAW_COPPER, Items.RAW_GOLD,
            of(TagContent.COPPER_CLUMPS), ItemContent.COPPER_CLUMP,
            of(ItemContent.SMALL_COPPER_CLUMP), ItemContent.SMALL_COPPER_CLUMP, ItemContent.SMALL_GOLD_CLUMP,
            of(TagContent.COPPER_DUSTS), ItemContent.COPPER_DUST,
            of(ItemContent.SMALL_COPPER_DUST), ItemContent.SMALL_COPPER_DUST, ItemContent.SMALL_GOLD_DUST,
            of(ItemContent.COPPER_GEM), ItemContent.COPPER_GEM,
            of(ItemContent.FLUXITE),
            of(TagContent.COPPER_NUGGETS), ItemContent.COPPER_NUGGET,
            of(ConventionalItemTags.COPPER_INGOTS), Items.COPPER_INGOT,
            1f, "copper", 3, true),
        
          // gold chain
          new OreTransform(
            of(TagContent.GOLD_ORES),
            of(ConventionalItemTags.GOLD_RAW_MATERIALS), Items.RAW_GOLD, Items.RAW_COPPER,
            of(TagContent.GOLD_CLUMPS), ItemContent.GOLD_CLUMP,
            of(ItemContent.SMALL_GOLD_CLUMP), ItemContent.SMALL_GOLD_CLUMP, ItemContent.SMALL_COPPER_CLUMP,
            of(TagContent.GOLD_DUSTS), ItemContent.GOLD_DUST,
            of(ItemContent.SMALL_GOLD_DUST), ItemContent.SMALL_GOLD_DUST, ItemContent.SMALL_COPPER_DUST,
            of(ItemContent.GOLD_GEM), ItemContent.GOLD_GEM,
            of(ItemContent.FLUXITE),
            of(ConventionalItemTags.GOLD_NUGGETS), Items.GOLD_NUGGET,
            of(ConventionalItemTags.GOLD_INGOTS), Items.GOLD_INGOT,
            1f, "gold", 3, true),
        
          // nickel chain
          new OreTransform(
            of(TagContent.NICKEL_ORES),
            of(TagContent.NICKEL_RAW_MATERIALS), ItemContent.RAW_NICKEL, ItemContent.RAW_PLATINUM,
            of(TagContent.NICKEL_CLUMPS), ItemContent.NICKEL_CLUMP,
            of(ItemContent.SMALL_NICKEL_CLUMP), ItemContent.SMALL_NICKEL_CLUMP, ItemContent.SMALL_PLATINUM_CLUMP,
            of(TagContent.NICKEL_DUSTS), ItemContent.NICKEL_DUST,
            of(ItemContent.SMALL_NICKEL_DUST), ItemContent.SMALL_NICKEL_DUST, ItemContent.SMALL_PLATINUM_DUST,
            of(ItemContent.NICKEL_GEM), ItemContent.NICKEL_GEM,
            of(ItemContent.FLUXITE),
            of(TagContent.NICKEL_NUGGETS), ItemContent.NICKEL_NUGGET,
            of(TagContent.NICKEL_INGOTS), ItemContent.NICKEL_INGOT,
            1f, "nickel", 2, true),
        
          // platinum chain
          new OreTransform(
          of(TagContent.PLATINUM_ORES),
          of(TagContent.PLATINUM_RAW_MATERIALS), ItemContent.RAW_PLATINUM, ItemContent.FLUXITE,
          of(TagContent.PLATINUM_CLUMPS), ItemContent.PLATINUM_CLUMP,
          of(ItemContent.SMALL_PLATINUM_CLUMP), ItemContent.SMALL_PLATINUM_CLUMP, ItemContent.FLUXITE,
          of(TagContent.PLATINUM_DUSTS), ItemContent.PLATINUM_DUST,
          of(ItemContent.SMALL_PLATINUM_DUST), ItemContent.SMALL_PLATINUM_DUST, ItemContent.FLUXITE,
          of(ItemContent.PLATINUM_GEM), ItemContent.PLATINUM_GEM,
          of(ItemContent.FLUXITE),
          of(TagContent.PLATINUM_NUGGETS), ItemContent.PLATINUM_NUGGET,
          of(TagContent.PLATINUM_INGOTS), ItemContent.PLATINUM_INGOT,
          1.5f, "platinum", 1, true));

          oreChains.forEach(ore -> addMetalProcessingChain(exporter, ore));

          // Uranium clumps don't exist in Oritech, but Oritech should still be able to do something with them if they're added by another mod (like Create).
          // in the compat space so that the Fabric versions won't go into Neoforge
          addCentrifugeRecipe(this.withConditions(exporter, new TagsPopulatedResourceCondition(TagContent.URANIUM_CLUMPS)), of(TagContent.URANIUM_CLUMPS), List.of(new ItemStack(ItemContent.URANIUM_DUST, 2), new ItemStack(ItemContent.SMALL_PLUTONIUM_DUST)), 0.5f, "compat/clump/crushed_uranium");
          addCentrifugeFluidRecipe(this.withConditions(exporter, new TagsPopulatedResourceCondition(TagContent.URANIUM_CLUMPS)), of(TagContent.URANIUM_CLUMPS), ItemContent.URANIUM_DUST, 3, Fluids.WATER, 1, null, 0, 0.5f, "compat/clumpwet/crushed_uranium");
    }
    
    private void addAlloys(RecipeExporter exporter) {
        addAlloyRecipe(exporter, of(TagContent.PLATINUM_INGOTS), of(ConventionalItemTags.NETHERITE_INGOTS), ItemContent.DURATIUM_INGOT, "duratium");
        addAlloyRecipe(exporter, of(ConventionalItemTags.GOLD_INGOTS), of(ConventionalItemTags.REDSTONE_DUSTS), ItemContent.ELECTRUM_INGOT, "electrum");
        addAlloyRecipe(exporter, of(ConventionalItemTags.DIAMOND_GEMS), of(TagContent.NICKEL_INGOTS), ItemContent.ADAMANT_INGOT, "adamant");
        addAlloyRecipe(exporter, of(TagContent.NICKEL_INGOTS), of(ItemContent.FLUXITE), ItemContent.ENERGITE_INGOT, "energite");
        addAlloyRecipe(exporter, of(ConventionalItemTags.IRON_INGOTS), of(TagContent.COAL_DUSTS), ItemContent.STEEL_INGOT, 1, 0.3333f, "steel");
    }
    
    private void addParticleCollisions(RecipeExporter exporter) {
        // diamond from coal dust
        addParticleCollisionRecipe(exporter, of(TagContent.COAL_DUSTS), of(TagContent.COAL_DUSTS), new ItemStack(Items.DIAMOND), 500, "diamond");
        // overcharged crystal from fluxite and energite dust
        addParticleCollisionRecipe(exporter, of(ItemContent.FLUXITE), of(ItemContent.ENERGITE_DUST), new ItemStack(ItemContent.OVERCHARGED_CRYSTAL), 5000, "overcharged_crystal");
        // platinum from gold dust
        addParticleCollisionRecipe(exporter, of(TagContent.GOLD_DUSTS), of(TagContent.GOLD_DUSTS), new ItemStack(ItemContent.PLATINUM_DUST), 500, "platinum_dust");
        // enderic compound from redstone and flesh
        addParticleCollisionRecipe(exporter, of(ConventionalItemTags.REDSTONE_DUSTS), of(Items.ROTTEN_FLESH), new ItemStack(ItemContent.ENDERIC_COMPOUND), 500, "enderic_compound");
        // fluxite from electrum dust and redstone
        addParticleCollisionRecipe(exporter, of(TagContent.ELECTRUM_DUSTS), of(ConventionalItemTags.REDSTONE_DUSTS), new ItemStack(ItemContent.FLUXITE), 1000, "fluxite");
        // netherite scrap from adamant dust and netherrack
        addParticleCollisionRecipe(exporter, of(ItemContent.ADAMANT_DUST), of(Items.NETHERRACK), new ItemStack(Items.NETHERITE_SCRAP), 2500, "netherite");
        // elytra from feather and saddle
        addParticleCollisionRecipe(exporter, of(cItemTag("feathers")), of(Items.SADDLE), new ItemStack(Items.ELYTRA), 10000, "elytra");
        // nether star from overcharged crystal and netherite
        addParticleCollisionRecipe(exporter, of(ItemContent.OVERCHARGED_CRYSTAL), of(ConventionalItemTags.NETHERITE_INGOTS), new ItemStack(Items.NETHER_STAR), 15000, "nether_star");
        // echo shard from ender pearl and amethyst shard
        addParticleCollisionRecipe(exporter, of(ConventionalItemTags.ENDER_PEARLS), of(ConventionalItemTags.AMETHYST_GEMS), new ItemStack(Items.ECHO_SHARD), 1000, "echo_shard");
        // heavy core from reinforced deepslate block and duration dust
        addParticleCollisionRecipe(exporter, of(Items.REINFORCED_DEEPSLATE), of(ItemContent.DURATIUM_DUST), new ItemStack(Items.HEAVY_CORE), 8000, "heavy_core");
    }
    
    private void addDusts(RecipeExporter exporter) {
        addDustRecipe(exporter, of(ItemContent.BIOSTEEL_INGOT), ItemContent.BIOSTEEL_DUST, ItemContent.BIOSTEEL_INGOT, "biosteel");
        addDustRecipe(exporter, of(ItemContent.DURATIUM_INGOT), ItemContent.DURATIUM_DUST, ItemContent.DURATIUM_INGOT, "duratium");
        addDustRecipe(exporter, of(TagContent.ELECTRUM_INGOTS), ItemContent.ELECTRUM_DUST, ItemContent.ELECTRUM_INGOT, "electrum");
        addDustRecipe(exporter, of(ItemContent.ADAMANT_INGOT), ItemContent.ADAMANT_DUST, ItemContent.ADAMANT_INGOT, "adamant");
        addDustRecipe(exporter, of(ItemContent.ENERGITE_INGOT), ItemContent.ENERGITE_DUST, ItemContent.ENERGITE_INGOT, "energite");
        addDustRecipe(exporter, of(TagContent.STEEL_INGOTS), ItemContent.STEEL_DUST, ItemContent.STEEL_INGOT, "steel");
        addDustRecipe(exporter, of(ItemTags.COALS), ItemContent.COAL_DUST, "coal");
        addDustRecipe(exporter, of(ConventionalItemTags.QUARTZ_GEMS), ItemContent.QUARTZ_DUST, "quartz");
        
        // raw ores without processing chains
        // coal
        addGrinderRecipe(exporter, of(ItemTags.COAL_ORES), Items.COAL, 3, "coalore");
        addPulverizerRecipe(exporter, of(ItemTags.COAL_ORES), Items.COAL, 2, "coalore");
        // redstone
        addGrinderRecipe(exporter, of(ItemTags.REDSTONE_ORES), Items.REDSTONE, 12, "redstoneore");
        addPulverizerRecipe(exporter, of(ItemTags.REDSTONE_ORES), Items.REDSTONE, 8, "redstoneore");
        // diamond
        addGrinderRecipe(exporter, of(ItemTags.DIAMOND_ORES), Items.DIAMOND, 2, "diamondore");
        addPulverizerRecipe(exporter, of(ItemTags.DIAMOND_ORES), Items.DIAMOND, 1, "diamondore");
        // quartz
        addGrinderRecipe(exporter, of(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 3, "quartzore");
        addPulverizerRecipe(exporter, of(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 2, "quartzore");
        // glowstone
        addGrinderRecipe(exporter, of(Blocks.GLOWSTONE), Items.GLOWSTONE_DUST, 4, "glowstoneore");
        addPulverizerRecipe(exporter, of(Blocks.GLOWSTONE), Items.GLOWSTONE_DUST, 3, "glowstoneore");
        // lapis
        addGrinderRecipe(exporter, of(ItemTags.LAPIS_ORES), Items.LAPIS_LAZULI, 8, "lapisore");
        addPulverizerRecipe(exporter, of(ItemTags.LAPIS_ORES), Items.LAPIS_LAZULI, 6, "lapisore");
        // bone
        addGrinderRecipe(exporter, of(Items.BONE), Items.BONE_MEAL, 8, "bone");
        addPulverizerRecipe(exporter, of(Items.BONE), Items.BONE_MEAL, 6, "bone");
        // blaze powder
        addGrinderRecipe(exporter, of(Items.BLAZE_ROD), Items.BLAZE_POWDER, 4, "blaze");
        addPulverizerRecipe(exporter, of(Items.BLAZE_ROD), Items.BLAZE_POWDER, 3, "blaze");
        // wool
        addGrinderRecipe(exporter, of(ItemTags.WOOL), Items.STRING, 4, "string");
        addPulverizerRecipe(exporter, of(ItemTags.WOOL), Items.STRING, 3, "string");
        // ancient debris
        addGrinderRecipe(exporter, of(Items.ANCIENT_DEBRIS), Items.NETHERITE_SCRAP, 2, "netheritescrap");
    }
    
    private void addUraniumProcessing(RecipeExporter exporter) {
        // uranium order is:
        // raw ore -> dust/gem, dust -> gem, gem -> pellets
        
        // plutonium can be made via either ender laser on crystals (manually, usually low amount)
        // or via the particle accelerator
        
        // small uranium dust from redstone
        addCentrifugeRecipe(exporter, of(ConventionalItemTags.REDSTONE_DUSTS), ItemContent.SMALL_URANIUM_DUST, 1, "redstoneuran");
        
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
            new SizedIngredient(32, of(TagContent.CARBON_FIBRE)),
            new SizedIngredient(16, of(ItemContent.BIOSTEEL_INGOT)),
            new SizedIngredient(4, of(ConventionalItemTags.DIAMOND_GEMS))),
          List.of(
            new SizedIngredient(8, of(TagContent.CARBON_FIBRE)),
            new SizedIngredient(4, of(ItemContent.DURATIUM_INGOT))),
          List.of("oritech:armor"), SIMPLE_AUGMENT_STATION_ID, 80, 70, 800, 50_000_000, "hpboostmore");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENERGITE_INGOT)),
            new SizedIngredient(32, of(ItemContent.DURATIUM_INGOT)),
            new SizedIngredient(1, of(Items.NETHER_STAR))),
          List.of(
            new SizedIngredient(64, of(ItemContent.DURATIUM_DUST)),
            new SizedIngredient(64, of(ConventionalItemTags.STORAGE_BLOCKS_REDSTONE))),
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
            new SizedIngredient(32, of(ConventionalItemTags.REDSTONE_DUSTS))),
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
            new SizedIngredient(16, of(ConventionalItemTags.STORAGE_BLOCKS_IRON))),
          List.of(
            new SizedIngredient(16, of(ItemContent.MOTOR)),
            new SizedIngredient(64, of(ConventionalItemTags.IRON_INGOTS))),
          List.of("oritech:superspeedboost"), SIMPLE_AUGMENT_STATION_ID, 80, 50, 800, 75_000_000, "stepassist");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.SILICON_WAFER)),
            new SizedIngredient(32, of(ItemContent.PROCESSING_UNIT)),
            new SizedIngredient(16, of(ConventionalItemTags.STORAGE_BLOCKS_GOLD))),
          List.of(
            new SizedIngredient(32, of(TagContent.SILICON)),
            new SizedIngredient(32, of(ConventionalItemTags.STORAGE_BLOCKS_REDSTONE))),
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
            new SizedIngredient(16, of(ConventionalItemTags.DIAMOND_GEMS))),
          List.of(
            new SizedIngredient(4, of(ItemContent.DURATIUM_INGOT)),
            new SizedIngredient(32, of(ConventionalItemTags.IRON_INGOTS))),
          List.of(), SIMPLE_AUGMENT_STATION_ID, 30, 50, 800, 80_000_000, "armor");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENERGITE_INGOT)),
            new SizedIngredient(32, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(32, of(ConventionalItemTags.DIAMOND_GEMS))),
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
            new SizedIngredient(16, of(ConventionalItemTags.NORMAL_OBSIDIANS))),
          List.of("oritech:betterarmor"), ADVANCED_AUGMENT_STATION_ID, 155, 50, 2400, 500_000_000, "ultimatearmor");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(48, of(TagContent.ELECTRUM_INGOTS)),
            new SizedIngredient(32, of(ConventionalItemTags.STORAGE_BLOCKS_REDSTONE))),
          List.of(
            new SizedIngredient(32, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(64, of(ConventionalItemTags.STORAGE_BLOCKS_IRON))),
          List.of("oritech:blockreach"), ADVANCED_AUGMENT_STATION_ID, 140, 70, 1600, 150_000_000, "weaponreach");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.MOTOR)),
            new SizedIngredient(48, of(TagContent.STEEL_INGOTS)),
            new SizedIngredient(32, of(ConventionalItemTags.STORAGE_BLOCKS_COPPER))),
          List.of(
            new SizedIngredient(32, of(ItemContent.MOTOR)),
            new SizedIngredient(64, of(ConventionalItemTags.COPPER_INGOTS))),
          List.of(), SIMPLE_AUGMENT_STATION_ID, 115, 90, 900, 100_000_000, "blockreach");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(16, of(ConventionalItemTags.ENDER_PEARLS)),
            new SizedIngredient(16, of(ConventionalItemTags.STORAGE_BLOCKS_DIAMOND))),
          List.of(
            new SizedIngredient(32, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(64, of(ConventionalItemTags.NORMAL_OBSIDIANS))),
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
            new SizedIngredient(64, of(ConventionalItemTags.STORAGE_BLOCKS_REDSTONE))),
          List.of("oritech:miningspeed", "oritech:superspeedboost"), ADVANCED_AUGMENT_STATION_ID, 80, 10, 2400, 450_000_000, "superminingspeed");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(TagContent.STEEL_INGOTS)),
            new SizedIngredient(48, of(ConventionalItemTags.DIAMOND_GEMS)),
            new SizedIngredient(32, of(ItemContent.FLUXITE))),
          List.of(
            new SizedIngredient(16, of(TagContent.STEEL_INGOTS)),
            new SizedIngredient(4, of(ItemContent.DURATIUM_INGOT))),
          List.of(), SIMPLE_AUGMENT_STATION_ID, 5, 10, 1600, 150_000_000, "attackdamage");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.ENDERIC_COMPOUND)),
            new SizedIngredient(64, of(ItemContent.FLUXITE)),
            new SizedIngredient(64, of(ConventionalItemTags.BLAZE_RODS))),
          List.of(
            new SizedIngredient(32, of(ItemContent.ENDERIC_COMPOUND)),
            new SizedIngredient(64, of(ConventionalItemTags.STORAGE_BLOCKS_GOLD))),
          List.of("oritech:hpboostultra", "oritech:ultimatearmor"), ARCANE_AUGMENT_STATION_ID, 180, 50, 2800, 500_000_000, "superattackdamage");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(TagContent.ELECTRUM_INGOTS)),
            new SizedIngredient(48, of(ConventionalItemTags.STORAGE_BLOCKS_LAPIS)),
            new SizedIngredient(32, of(ConventionalItemTags.STORAGE_BLOCKS_GOLD))),
          List.of(
            new SizedIngredient(32, of(ConventionalItemTags.STORAGE_BLOCKS_LAPIS)),
            new SizedIngredient(64, of(ConventionalItemTags.STORAGE_BLOCKS_REDSTONE))),
          List.of(), ARCANE_AUGMENT_STATION_ID, 55, 30, 1800, 200_000_000, "luck");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(48, of(ItemContent.FLUXITE)),
            new SizedIngredient(8, of(Items.PHANTOM_MEMBRANE))),
          List.of(
            new SizedIngredient(32, of(ItemContent.MAGNETIC_COIL)),
            new SizedIngredient(16, of(ConventionalItemTags.STORAGE_BLOCKS_IRON))),
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
            new SizedIngredient(8, of(ConventionalItemTags.DIAMOND_GEMS))),
          List.of(
            new SizedIngredient(32, of(ItemContent.ENDERIC_LENS)),
            new SizedIngredient(64, of(Items.GLOWSTONE))),
          List.of("oritech:orefinder"), ARCANE_AUGMENT_STATION_ID, 155, 10, 3200, 100_000_000, "cloak");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(16, of(ConventionalItemTags.ENDER_PEARLS)),
            new SizedIngredient(48, of(ConventionalItemTags.NORMAL_OBSIDIANS)),
            new SizedIngredient(1, of(ItemContent.UNHOLY_INTELLIGENCE)),
            new SizedIngredient(32, of(ItemContent.ADAMANT_INGOT))),
          List.of(
            new SizedIngredient(8, of(ConventionalItemTags.ENDER_PEARLS)),
            new SizedIngredient(32, of(ConventionalItemTags.CRYING_OBSIDIANS))),
          List.of(), ARCANE_AUGMENT_STATION_ID, 130, 30, 3000, 250_000_000, "portal");
        
        addAugmentRecipe(exporter,
          List.of(
            new SizedIngredient(64, of(ConventionalItemTags.GOLD_INGOTS)),
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
        return ShapedRecipeJsonBuilder.create(category, output, count).input('c', input).input('p', Ingredient.fromTag(TagContent.MACHINE_PLATING)).input('s', of(Blocks.STONE)).pattern("csc").pattern("sps").pattern("csc");
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
