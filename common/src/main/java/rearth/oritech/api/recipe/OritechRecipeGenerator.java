package rearth.oritech.api.recipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.util.MetalProcessingChainBuilder;
import rearth.oritech.block.entity.augmenter.api.CustomAugmentsCollection;
import rearth.oritech.init.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static rearth.oritech.api.recipe.util.RecipeHelpers.*;
import static rearth.oritech.util.TagUtils.*;

public class OritechRecipeGenerator extends RecipeProvider {
    
    public OritechRecipeGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }
    
    @Override
    public void buildRecipes(RecipeOutput exporter) {
        
        addDeepDrillOres(exporter);
        addFuels(exporter);
        addFluidProcessing(exporter);
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
    }
    
    private void addVanillaAdditions(RecipeOutput exporter) {
        
        // slimeball from honey and biomass
        AssemblerRecipeBuilder.build().input(Items.HONEYCOMB).input(TagContent.BIOMASS).input(TagContent.BIOMASS).input(TagContent.BIOMASS).result(Items.SLIME_BALL).timeMultiplier(0.8f).export(exporter, "slime");
        // fireball in assembler (gunpowder, blaze powder + coal) = 5 charges
        AssemblerRecipeBuilder.build().input(Items.GUNPOWDER).input(Items.BLAZE_POWDER).input(ItemTags.COALS).input(ItemTags.COALS).result(Items.FIRE_CHARGE, 4).timeMultiplier(0.8f).export(exporter, "fireball");
        // blaze rod (4 powder in assembler)
        AssemblerRecipeBuilder.build().input(Items.BLAZE_POWDER).input(Items.BLAZE_POWDER).input(Items.BLAZE_POWDER).input(Items.BLAZE_POWDER).result(Items.BLAZE_ROD).timeMultiplier(0.8f).export(exporter, "blazerod");
        // enderic compound from sculk
        CentrifugeRecipeBuilder.build().input(Items.SCULK).result(ItemContent.ENDERIC_COMPOUND).timeMultiplier(4f).export(exporter, "endericsculk");
        // budding amethyst (amethyst shard x2, enderic compound, overcharged crystal)
        AssemblerRecipeBuilder.build().input(cItemTag("gems/amethyst")).input(cItemTag("gems/amethyst")).input(ItemContent.ENDERIC_COMPOUND).input(ItemContent.OVERCHARGED_CRYSTAL).result(Items.BUDDING_AMETHYST).time(160).export(exporter, "amethystbud");
        // netherite alloying (yes this is pretty OP)
        FoundryRecipeBuilder.build().input(cItemTag("ingots/gold")).input(Items.NETHERITE_SCRAP).result(Items.NETHERITE_INGOT).export(exporter, "netherite");
        // books
        AssemblerRecipeBuilder.build().input(Items.PAPER).input(Items.PAPER).input(Items.PAPER).input(cItemTag("leathers")).result(Items.BOOK, 2).timeMultiplier(0.8f).export(exporter, "book");
        // reinforced deepslate
        AtomicForgeRecipeBuilder.build().input(Items.DEEPSLATE).input(ItemContent.DURATIUM_INGOT).input(ItemContent.DURATIUM_INGOT).result(Items.REINFORCED_DEEPSLATE).time(100).export(exporter, "reinfdeepslate");
        // cobblestone to gravel
        PulverizerRecipeBuilder.build().input(cItemTag("cobblestones")).result(Items.GRAVEL).addToGrinder().export(exporter, "gravel");
        // gravel to sand
        PulverizerRecipeBuilder.build().input(Items.GRAVEL).result(Items.SAND).addToGrinder().export(exporter, "sand");
        // sandstone to sand
        PulverizerRecipeBuilder.build().input(cItemTag("sandstone/blocks")).result(Items.SAND).addToGrinder().export(exporter, "sand_from_sandstone");
        // red sandstone to red sand
        PulverizerRecipeBuilder.build().input(cItemTag("sandstone/red_blocks")).result(Items.RED_SAND).addToGrinder().export(exporter, "red_sand");
        // centrifuge dirt into clay
        CentrifugeFluidRecipeBuilder.build().input(ItemTags.DIRT).result(Items.CLAY).fluidInput(Fluids.WATER, 0.25f).export(exporter, "clay");
        // create dirt from sand + biomass
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.DIRT, 2).define('s', ItemTags.SAND).define('b', TagContent.BIOMASS).pattern("sb").pattern("bs").unlockedBy("has_biomass", has(TagContent.BIOMASS)).save(exporter, Oritech.id("dirt_from_sand_and_biomass"));
        // dripstone from dripstone block
        PulverizerRecipeBuilder.build().input(Items.DRIPSTONE_BLOCK).result(Items.POINTED_DRIPSTONE, 4).addToGrinder().export(exporter, "dripstone");
        // shroomlight from logs and 3 glowstone
        AssemblerRecipeBuilder.build().input(ItemTags.LOGS).input(Items.GLOWSTONE).input(Items.GLOWSTONE).input(Items.GLOWSTONE).result(Items.SHROOMLIGHT).timeMultiplier(0.8f).export(exporter, "shroomlight");
        // prismarine shards to crystals
        PulverizerRecipeBuilder.build().input(Items.PRISMARINE_SHARD).result(Items.PRISMARINE_CRYSTALS, 2).addToGrinder().export(exporter, "prismarine");
        
        // recyclables
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_NETHERITE_SCRAP).result(Items.NETHERITE_SCRAP).addToGrinder().export(exporter, "recycle/netherite_scrap");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_DIAMOND).result(Items.DIAMOND).addToGrinder().export(exporter, "recycle/diamond");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_IRON_DUST).result(ItemContent.IRON_DUST).addToGrinder().export(exporter, "recycle/iron_dust");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_SMALL_IRON_DUST).result(ItemContent.SMALL_IRON_DUST).export(exporter, "recycle/small_iron_dust");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_GOLD_DUST).result(ItemContent.GOLD_DUST).export(exporter, "recycle/gold_dust");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_SMALL_GOLD_DUST).result(ItemContent.SMALL_GOLD_DUST).export(exporter, "recycle/small_gold_dust");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_COPPER_DUST).result(ItemContent.COPPER_DUST).export(exporter, "recycle/copper_dust");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_SMALL_COPPER_DUST).result(ItemContent.SMALL_COPPER_DUST).export(exporter, "recycle/small_copper_dust");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_2_QUARTZ_DUST).result(ItemContent.QUARTZ_DUST, 2).export(exporter, "recycle/2_quartz_dust");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_4_QUARTZ_DUST).result(ItemContent.QUARTZ_DUST, 4).export(exporter, "recycle/4_quartz_dust");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_REDSTONE_DUST).result(Items.REDSTONE).export(exporter, "recycle/redstone_dust");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_GRAVEL).result(Items.GRAVEL).export(exporter, "recycle/gravel");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_SAND).result(Items.SAND).export(exporter, "recycle/sand");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_RED_SAND).result(Items.RED_SAND).export(exporter, "recycle/red_sand");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_STRING).result(Items.STRING, 3).export(exporter, "recycle/string");
        PulverizerRecipeBuilder.build().input(TagContent.RECYCLES_TO_BIOMASS).result(ItemContent.BIOMASS).export(exporter, "recycle/biomass");
    }
    
    private void addDyes(RecipeOutput exporter) {
        PulverizerRecipeBuilder.build().input(TagContent.RAW_WHITE_DYE).result(Items.WHITE_DYE).addToGrinder().export(exporter, "dyes/white");
        PulverizerRecipeBuilder.build().input(TagContent.RAW_LIGHT_GRAY_DYE).result(Items.LIGHT_GRAY_DYE).addToGrinder().export(exporter, "dyes/light_gray");
        PulverizerRecipeBuilder.build().input(TagContent.RAW_BLACK_DYE).result(Items.BLACK_DYE).addToGrinder().export(exporter, "dyes/black");
        PulverizerRecipeBuilder.build().input(TagContent.RAW_RED_DYE).result(Items.RED_DYE).addToGrinder().export(exporter, "dyes/red");
        PulverizerRecipeBuilder.build().input(TagContent.RAW_ORANGE_DYE).result(Items.ORANGE_DYE).addToGrinder().export(exporter, "dyes/orange");
        PulverizerRecipeBuilder.build().input(TagContent.RAW_YELLOW_DYE).result(Items.YELLOW_DYE).addToGrinder().export(exporter, "dyes/yellow");
        PulverizerRecipeBuilder.build().input(TagContent.RAW_CYAN_DYE).result(Items.CYAN_DYE).addToGrinder().export(exporter, "dyes/cyan");
        PulverizerRecipeBuilder.build().input(TagContent.RAW_BLUE_DYE).result(Items.BLUE_DYE).addToGrinder().export(exporter, "dyes/blue");
        PulverizerRecipeBuilder.build().input(TagContent.RAW_MAGENTA_DYE).result(Items.MAGENTA_DYE).addToGrinder().export(exporter, "dyes/magenta");
        PulverizerRecipeBuilder.build().input(TagContent.RAW_PINK_DYE).result(Items.PINK_DYE).addToGrinder().export(exporter, "dyes/pink");
    }
    
    private void addDeepDrillOres(RecipeOutput exporter) {
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_REDSTONE).result(Items.REDSTONE).export(exporter, "redstone");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_LAPIS).result(Items.LAPIS_LAZULI).export(exporter, "lapis");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_IRON).result(Items.RAW_IRON).export(exporter, "iron");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_COAL).result(Items.COAL).export(exporter, "coal");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_COPPER).result(Items.RAW_COPPER).export(exporter, "copper");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_GOLD).result(Items.RAW_GOLD).export(exporter, "gold");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_EMERALD).result(Items.EMERALD).export(exporter, "emerald");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_DIAMOND).result(Items.DIAMOND).export(exporter, "diamond");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_NICKEL).result(ItemContent.RAW_NICKEL).export(exporter, "nickel");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_PLATINUM).result(ItemContent.RAW_PLATINUM).export(exporter, "platinum");
        DeepDrillRecipeBuilder.build().input(BlockContent.RESOURCE_NODE_URANIUM).result(ItemContent.RAW_URANIUM).export(exporter, "uranium");
    }
    
    private void addFuels(RecipeOutput exporter) {
        
        // bio
        BioGeneratorRecipeBuilder.build().input(TagContent.BIOMATTER).timeInSeconds(15).export(exporter, "rawbio");
        BioGeneratorRecipeBuilder.build().input(ItemContent.PACKED_WHEAT).timeInSeconds(200).export(exporter, "packedwheat");
        BioGeneratorRecipeBuilder.build().input(TagContent.BIOMASS).timeInSeconds(25).export(exporter, "biomass");
        BioGeneratorRecipeBuilder.build().input(ItemContent.SOLID_BIOFUEL).timeInSeconds(160).export(exporter, "solidbiomass");
        BioGeneratorRecipeBuilder.build().input(TagContent.BIOMASS_BLOCK).timeInSeconds(270).export(exporter, "biomassblock");
        BioGeneratorRecipeBuilder.build().input(ItemContent.RAW_BIOPOLYMER).timeInSeconds(300).export(exporter, "polymer");
        BioGeneratorRecipeBuilder.build().input(ItemContent.UNHOLY_INTELLIGENCE).timeInSeconds(3000).export(exporter, "vex");
        // lava
        LavaGeneratorRecipeBuilder.build().fluidInput(Fluids.LAVA, 0.1f).timeInSeconds(6).export(exporter, "lava");
        LavaGeneratorRecipeBuilder.build().fluidInput(FluidContent.STILL_SHEOL_FIRE.get(), 0.1f).timeInSeconds(40).export(exporter, "sheolfire");
        // fuel
        FuelGeneratorRecipeBuilder.build().fluidInput(cFluidTag("oil"), 0.1f).timeInSeconds(1).export(exporter, "crude");
        FuelGeneratorRecipeBuilder.build().fluidInput(FluidContent.STILL_HEAVY_OIL.get(), 0.1f).timeInSeconds(2).export(exporter, "heavyoil");
        FuelGeneratorRecipeBuilder.build().fluidInput(TagContent.DIESEL, 0.1f).timeInSeconds(4).export(exporter, "diesel");
        FuelGeneratorRecipeBuilder.build().fluidInput(TagContent.NAPHTHA, 0.1f).timeInSeconds(2).export(exporter, "naptha");
        FuelGeneratorRecipeBuilder.build().fluidInput(TagContent.TURBOFUEL, 0.1f).timeInSeconds(16).export(exporter, "fuel");
        //steam
        // 32 fabric droplets / 32 neoforge mb (yes this will works, as we produce 2 millis per RF in the generator boilers, and then consume it at a 1:1 ratio)
        SteamGeneratorRecipeBuilder.build().specificFluidInput(FluidContent.STILL_STEAM.get(), 32).time(1).export(exporter, "steameng");
    }
    
    private void addFluidProcessing(RecipeOutput exporter) {
        
        // crude oil processing
        RefineryRecipeBuilder.build()
          .fluidInput(cFluidTag("oil"))
          .fluidOutput(FluidContent.STILL_HEAVY_OIL.get(), 0.5f)
          .fluidOutput(FluidContent.STILL_NAPHTHA.get(), 0.25f)
          .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 0.25f)
          .export(exporter, "oilbase");
        
        RefineryRecipeBuilder.build()
          .input(ItemContent.CLAY_CATALYST_BEADS)
          .fluidInput(cFluidTag("oil"))
          .fluidOutput(FluidContent.STILL_DIESEL.get(), 0.5f)
          .fluidOutput(FluidContent.STILL_NAPHTHA.get(), 0.5f)
          .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 0.5f)
          .timeMultiplier(1.6f)
          .export(exporter, "oilalt");
        
        // heavy oil
        RefineryRecipeBuilder.build()
          .input(ItemTags.SAND)
          .fluidInput(FluidContent.STILL_HEAVY_OIL.get())
          .fluidOutput(FluidContent.STILL_DIESEL.get(), 1f)
          .fluidOutput(FluidContent.STILL_NAPHTHA.get(), 0.25f)
          .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 0.25f)
          .export(exporter, "heavyoil");
        
        // lava
        RefineryRecipeBuilder.build()
          .fluidInput(Fluids.LAVA)
          .fluidOutput(FluidStack.create(FluidContent.STILL_STEAM.get(), 4_000))
          .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 0.1f)
          .fluidOutput(FluidContent.STILL_SHEOL_FIRE.get(), 0.2f)
          .export(exporter, "lava");
        
        RefineryRecipeBuilder.build()
          .input(ItemContent.ENDERIC_COMPOUND)
          .fluidInput(Fluids.LAVA)
          .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 1f)
          .fluidOutput(FluidContent.STILL_SHEOL_FIRE.get(), 0.25f)
          .fluidOutput(FluidContent.STILL_STRANGE_MATTER.get(), 0.1f)
          .timeMultiplier(1.6f)
          .export(exporter, "lavaalt");
        
        // biodiesel
        RefineryRecipeBuilder.build()
          .input(ItemContent.CLAY_CATALYST_BEADS)
          .fluidInput(TagContent.BIOFUEL)
          .fluidOutput(FluidContent.STILL_DIESEL.get(), 0.5f)
          .fluidOutput(FluidContent.STILL_NAPHTHA.get(), 0.2f)
          .export(exporter, "biodiesel");
        
        // centrifuge turbofuel
        CentrifugeFluidRecipeBuilder
          .build()
          .input(ItemContent.FLUXITE)
          .fluidInput(TagContent.DIESEL)
          .fluidOutput(FluidContent.STILL_FUEL.get())
          .export(exporter, "fuel");
        
        // centrifuge biofuel
        CentrifugeFluidRecipeBuilder
          .build()
          .input(TagContent.BIOMASS)
          .fluidInput(Fluids.WATER, 0.25f)
          .fluidOutput(FluidContent.STILL_BIOFUEL.get(), 0.1f)
          .timeMultiplier(0.2f)
          .export(exporter, "biofuel");
        
        // silicon wash from naphtha in centrifuge
        CentrifugeFluidRecipeBuilder.build()
          .input(TagContent.QUARTZ_DUSTS)
          .fluidInput(TagContent.NAPHTHA)
          .fluidOutput(FluidContent.STILL_SILICON_WASH.get(), 1f)
          .export(exporter, "siliconwash");
        
        CentrifugeFluidRecipeBuilder.build()
          .input(Items.GRAVEL)
          .fluidInput(TagContent.NAPHTHA)
          .fluidOutput(FluidContent.STILL_SILICON_WASH.get(), 0.05f)
          .timeMultiplier(1.6f)
          .export(exporter, "siliconwashbad");
        
        // polymer resin from naphtha (manual)
        offerManualFluidApplication(exporter, ItemContent.POLYMER_RESIN, of(FluidContent.STILL_NAPHTHA_BUCKET.get()), of(ItemTags.SAND), "manualresin");
        
        // polymer resin from naphtha in centrifuge
        CentrifugeFluidRecipeBuilder.build()
          .input(ItemTags.SAND)
          .fluidInput(TagContent.NAPHTHA, 0.1f)
          .result(ItemContent.POLYMER_RESIN, 2)
          .export(exporter, "naptharesin");
        
        // basic battery in centrifuge with sulfuric acid
        CentrifugeFluidRecipeBuilder.build()
          .input(TagContent.STEEL_INGOTS)
          .fluidInput(TagContent.SULFURIC_ACID)
          .result(ItemContent.BASIC_BATTERY, 2)
          .export(exporter, "batteryacid");
        
        // adv battery in centrifuge with sulfuric acid
        CentrifugeFluidRecipeBuilder.build()
          .input(ItemContent.DUBIOS_CONTAINER)
          .fluidInput(TagContent.SULFURIC_ACID)
          .result(ItemContent.ADVANCED_BATTERY, 8)
          .timeMultiplier(2f)
          .export(exporter, "advbatteryacid");
        
        // silicon from silicon wash + sand in refinery
        RefineryRecipeBuilder.build()
          .input(ItemTags.SAND)
          .fluidInput(FluidContent.STILL_SILICON_WASH.get())
          .result(ItemContent.SILICON, 4)
          .timeMultiplier(2f)
          .export(exporter, "siliconwashing");
        
        // silicon wafer in centrifuge
        CentrifugeFluidRecipeBuilder.build()
          .input(ItemContent.CARBON_FIBRE_STRANDS)
          .fluidInput(FluidContent.STILL_SILICON_WASH.get())
          .result(ItemContent.SILICON_WAFER, 4)
          .timeMultiplier(2f)
          .export(exporter, "siliconwafers");
        
        // quartz from mineral wash in refinery
        RefineryRecipeBuilder.build()
          .input(ItemContent.CLAY_CATALYST_BEADS)
          .fluidInput(FluidContent.STILL_MINERAL_SLURRY.get(), 0.25f)
          .result(Items.QUARTZ)
          .timeMultiplier(2f)
          .export(exporter, "quartz");
        
        // reinforced carbon sheeting
        RefineryRecipeBuilder.build()
          .input(ItemContent.CARBON_FIBRE_STRANDS)
          .fluidInput(TagContent.NAPHTHA, 0.5f)
          .result(ItemContent.REINFORCED_CARBON_SHEET)
          .timeMultiplier(3f)
          .export(exporter, "carbonsheet");
        
        // dubious container and strange matter in centrifuge
        CentrifugeFluidRecipeBuilder.build()
          .input(ItemContent.DUBIOS_CONTAINER)
          .fluidInput(FluidContent.STILL_STRANGE_MATTER.get())
          .result(ItemContent.UNHOLY_INTELLIGENCE, 1)
          .timeMultiplier(8f)
          .export(exporter, "unholyai");
    }
    
    private void addBiomass(RecipeOutput exporter) {
        // biomass
        PulverizerRecipeBuilder.build().input(TagContent.BIOMATTER).result(ItemContent.BIOMASS).addToGrinder().export(exporter, "biobasic");
        PulverizerRecipeBuilder.build().input(ItemContent.PACKED_WHEAT).result(ItemContent.BIOMASS, 16).addToGrinder().export(exporter, "packagedwheatbio");
        PulverizerRecipeBuilder.build().input(cItemTag("storage_blocks/wheat")).result(ItemContent.BIOMASS, 16).addToGrinder().export(exporter, "hay_block");
        AssemblerRecipeBuilder.build().input(TagContent.BIOMASS).input(TagContent.BIOMASS).input(TagContent.BIOMASS).input(ItemTags.PLANKS).result(ItemContent.SOLID_BIOFUEL).timeMultiplier(0.8f).export(exporter, "solidbiofuel");
    }
    
    private void addEquipment(RecipeOutput exporter) {
        offerDrillRecipe(exporter, ToolsContent.HAND_DRILL, of(TagContent.STEEL_INGOTS), of(ItemContent.MOTOR), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.ADAMANT_INGOT), "handdrill");
        offerChainsawRecipe(exporter, ToolsContent.CHAINSAW, of(TagContent.STEEL_INGOTS), of(ItemContent.MOTOR), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.ADAMANT_INGOT), "chainsaw");
        offerAxeRecipe(exporter, ToolsContent.PROMETHIUM_AXE, of(ItemContent.PROMETHEUM_INGOT), of(BlockContent.DESTROYER_BLOCK.asItem()), "promaxe");
        offerPickaxeRecipe(exporter, ToolsContent.PROMETHIUM_PICKAXE, of(ItemContent.PROMETHEUM_INGOT), of(BlockContent.DESTROYER_BLOCK.asItem()), "prompick");
        
        // enderic laser / portable laser
        offerChainsawRecipe(exporter, ToolsContent.PORTABLE_LASER, of(ItemContent.ADVANCED_BATTERY), of(BlockContent.ACCELERATOR_MOTOR), of(ItemContent.ADAMANT_INGOT), of(BlockContent.LASER_ARM_BLOCK), "portablelaser");
        
        // electric mace
        offerDrillRecipe(exporter, ToolsContent.ELECTRIC_MACE, of(ItemContent.ADVANCED_BATTERY), of(ItemContent.CARBON_FIBRE_STRANDS), of(ItemContent.ADAMANT_INGOT), of(Items.HEAVY_CORE), "_emace");
        
        // designator
        offerDrillRecipe(exporter, ItemContent.TARGET_DESIGNATOR, of(TagContent.STEEL_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.PROCESSING_UNIT), of(TagContent.PLASTIC_PLATES), "designator");
        // weed killer
        offerDrillRecipe(exporter, ItemContent.WEED_KILLER, of(cItemTag("foods/food_poisoning")), of(cItemTag("foods/food_poisoning")), of(ItemContent.RAW_BIOPOLYMER), of(Items.GLASS_BOTTLE), "weedex");
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
        offerParticleMotorRecipe(exporter, ToolsContent.JETPACK, of(TagContent.STEEL_INGOTS), of(cItemTag("leathers")), of(ItemContent.ADVANCED_BATTERY), of(Items.GUNPOWDER), "basicjetpack");
        // jetpack alt
        offerParticleMotorRecipe(exporter, ToolsContent.JETPACK, of(TagContent.STEEL_INGOTS), of(cItemTag("leathers")), of(Items.REDSTONE_BLOCK), of(Items.BLAZE_POWDER), "basicjetpackalt");
        // exo jetpack
        offerGeneratorRecipe(exporter, ToolsContent.EXO_JETPACK, of(ToolsContent.JETPACK), of(BlockContent.SMALL_TANK_BLOCK), of(ToolsContent.EXO_CHESTPLATE), of(ItemContent.ION_THRUSTER), "exojetpack");
        // boosted elytra
        offerGeneratorRecipe(exporter, ToolsContent.JETPACK_ELYTRA, of(Items.ELYTRA), of(ItemContent.PROCESSING_UNIT), of(ToolsContent.JETPACK), of(Items.GUNPOWDER), "boostedelytra");
        // exo elytra (exo jetpack + elytra)
        offerGeneratorRecipe(exporter, ToolsContent.JETPACK_EXO_ELYTRA, of(ToolsContent.EXO_JETPACK), of(ItemContent.PROCESSING_UNIT), of(Items.ELYTRA), of(Items.GUNPOWDER), "exoboostedelytra");
        // exo elytra (boosted elytra + exo chestplate)
        offerGeneratorRecipe(exporter, ToolsContent.JETPACK_EXO_ELYTRA, of(ToolsContent.EXO_CHESTPLATE), of(BlockContent.SMALL_TANK_BLOCK), of(ToolsContent.JETPACK_ELYTRA), of(ItemContent.ION_THRUSTER), "exoboostedelytraalt");
        
    }
    
    private void addDecorative(RecipeOutput exporter) {
        // ceiling light
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.CEILING_LIGHT.asItem(), 6), of(Items.GLOWSTONE_DUST), of(TagContent.STEEL_INGOTS), "ceilightlight");
        // hanging light
        offerTwoComponentRecipe(exporter, BlockContent.CEILING_LIGHT_HANGING.asItem(), of(cItemTag("chains")), of(BlockContent.CEILING_LIGHT.asItem()), "hanginglight");
        // tech button
        offerLeverRecipe(exporter, BlockContent.TECH_BUTTON.asItem(), of(cItemTag("ingots/copper")), of(TagContent.STEEL_INGOTS), "techbutton");
        // tech lever
        offerLeverRecipe(exporter, BlockContent.TECH_LEVER.asItem(), of(TagContent.CARBON_FIBRE), of(TagContent.STEEL_INGOTS), "techlever");
        // tech door
        offerDoorRecipe(exporter, BlockContent.TECH_DOOR.asItem(), of(TagContent.STEEL_INGOTS), "techdoor");
        // metal beam
        offerRotatedCableRecipe(exporter, new ItemStack(BlockContent.METAL_BEAM_BLOCK.asItem(), 6), of(TagContent.CARBON_FIBRE), of(TagContent.STEEL_INGOTS), "metalbeams");
        // metal girder
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.METAL_GIRDER_BLOCK.asItem(), 6), of(TagContent.CARBON_FIBRE), of(TagContent.STEEL_INGOTS), "metalgirder");
        // tech glass
        offerMachinePlatingRecipe(exporter, BlockContent.INDUSTRIAL_GLASS_BLOCK.asItem(), of(TagContent.STEEL_INGOTS), of(cItemTag("glass_blocks")), of(TagContent.MACHINE_PLATING), 4, "industrialglass");
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
    
    private void addMachines(RecipeOutput exporter) {
        // basic generator
        offerGeneratorRecipe(exporter, BlockContent.BASIC_GENERATOR_BLOCK.asItem(), of(cItemTag("player_workstations/furnaces")), of(ItemContent.MAGNETIC_COIL), of(cItemTag("ingots/copper")), of(TagContent.NICKEL_INGOTS), "basicgen");
        // pulverizer
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), of(cItemTag("storage_blocks/copper")), of(ItemContent.MOTOR), of(TagContent.NICKEL_INGOTS), of(TagContent.STEEL_INGOTS), "pulverizer");
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(TagContent.NICKEL_INGOTS), of(TagContent.STEEL_INGOTS), "pulverizeralt");
        // electric furnace
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), of(cItemTag("player_workstations/furnaces")), of(ItemContent.MAGNETIC_COIL), of(TagContent.SILICON), of(TagContent.ELECTRUM_INGOTS), of(cItemTag("ingots/copper")), "electricfurnace");
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE_BLOCK.asItem(), of(cItemTag("player_workstations/furnaces")), of(ItemContent.MAGNETIC_COIL), of(TagContent.PLATINUM_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(cItemTag("ingots/copper")), "electricfurnacealt");
        // assembler
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), of(Blocks.BLAST_FURNACE.asItem()), of(ItemContent.MOTOR), of(Items.CRAFTER), of(ItemContent.ADAMANT_INGOT), of(cItemTag("ingots/copper")), "assembler");
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(Items.CRAFTER), of(ItemContent.ADAMANT_INGOT), of(cItemTag("ingots/copper")), "assembleralt");
        // foundry
        offerGeneratorRecipe(exporter, BlockContent.FOUNDRY_BLOCK.asItem(), of(Blocks.CAULDRON.asItem()), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.MOTOR), of(cItemTag("ingots/copper")), "foundry");
        // refinery
        offerParticleMotorRecipe(exporter, BlockContent.REFINERY_BLOCK.asItem(), of(BlockContent.REFINERY_MODULE_BLOCK.asItem()), of(ItemContent.MOTOR), of(Items.CAULDRON), of(cItemTag("ingots/steel")), "refinery");
        // refinery module
        offerGeneratorRecipe(exporter, BlockContent.REFINERY_MODULE_BLOCK.asItem(), of(BlockContent.SMALL_TANK_BLOCK.asItem()), of(Items.SLIME_BALL), of(BlockContent.METAL_BEAM_BLOCK), of(cItemTag("ingots/copper")), "refinerymodule");
        offerGeneratorRecipe(exporter, BlockContent.REFINERY_MODULE_BLOCK.asItem(), of(BlockContent.SMALL_TANK_BLOCK.asItem()), of(TagContent.SILICON), of(BlockContent.METAL_BEAM_BLOCK), of(cItemTag("ingots/copper")), "refinerymodulealt");
        // cooler
        offerGeneratorRecipe(exporter, BlockContent.COOLER_BLOCK.asItem(), of(Blocks.CAULDRON.asItem()), of(Blocks.ICE.asItem()), of(ItemContent.MOTOR), of(cItemTag("ingots/iron")), "cooler");
        // centrifuge
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.PROCESSING_UNIT), of(ItemContent.MOTOR), of(TagContent.STEEL_INGOTS), of(Items.GLASS_BOTTLE), "centrifuge");
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE_BLOCK.asItem(), of(ItemContent.MOTOR), of(cItemTag("storage_blocks/iron")), of(cItemTag("ingots/copper")), of(ItemContent.MOTOR), of(Items.GLASS_BOTTLE), "centrifugealt");
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
        offerGeneratorRecipe(exporter, BlockContent.STEAM_ENGINE_BLOCK.asItem(), of(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), of(cItemTag("ingots/copper")), of(ItemContent.MAGNETIC_COIL), of(TagContent.ELECTRUM_INGOTS), "steamgen");
        // diesel generator
        offerGeneratorRecipe(exporter, BlockContent.FUEL_GENERATOR_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(BlockContent.BASIC_GENERATOR_BLOCK), of(ItemContent.ENDERIC_LENS), of(TagContent.STEEL_INGOTS), "fuelgen");
        // large solar
        offerGeneratorRecipe(exporter, BlockContent.BIG_SOLAR_PANEL_BLOCK.asItem(), of(BlockContent.BASIC_GENERATOR_BLOCK.asItem()), of(ItemContent.FLUX_GATE), of(ItemContent.ADVANCED_BATTERY), of(ItemContent.FLUXITE), "solar");
        
        // charger
        offerAtomicForgeRecipe(exporter, BlockContent.CHARGER_BLOCK.asItem(), of(cItemTag("chests/wooden")), of(BlockContent.ENERGY_PIPE), of(cItemTag("storage_blocks/redstone")), of(Items.DISPENSER), of(TagContent.STEEL_INGOTS), "charger");
        offerAtomicForgeRecipe(exporter, BlockContent.CHARGER_BLOCK.asItem(), of(cItemTag("chests/wooden")), of(BlockContent.ENERGY_PIPE), of(ItemContent.PROCESSING_UNIT), of(Items.DISPENSER), of(TagContent.STEEL_INGOTS), "chargeralt");
        
        // small storage
        offerAtomicForgeRecipe(exporter, BlockContent.SMALL_STORAGE_BLOCK.asItem(), of(ItemContent.BASIC_BATTERY), of(TagContent.SILICON), of(ItemContent.MAGNETIC_COIL), of(TagContent.NICKEL_INGOTS), of(TagContent.NICKEL_INGOTS), "smallstorage");
        // large storage
        offerAtomicForgeRecipe(exporter, BlockContent.LARGE_STORAGE_BLOCK.asItem(), of(ItemContent.ADVANCED_BATTERY), of(TagContent.STEEL_INGOTS), of(ItemContent.DUBIOS_CONTAINER), of(ItemContent.FLUX_GATE), of(ItemContent.MAGNETIC_COIL), "bigstorage");
        // unstable container
        offerAtomicForgeRecipe(exporter, ItemContent.UNSTABLE_CONTAINER, of(ItemContent.FLUXITE), of(ItemContent.DURATIUM_INGOT), of(BlockContent.LARGE_STORAGE_BLOCK), of(ItemContent.FLUX_GATE), of(ItemContent.SUPER_AI_CHIP), "unstablecontainer");
        
        // fluid tank
        offerTankRecipe(exporter, BlockContent.SMALL_TANK_BLOCK.asItem(), of(cItemTag("ingots/copper")), of(cItemTag("glass_blocks")), of(BlockContent.FLUID_PIPE.asItem()), "stank");
        // pump
        offerGeneratorRecipe(exporter, BlockContent.PUMP_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.SILICON), of(ItemContent.MOTOR), of(cItemTag("ingots/copper")), "pump");
        // block placer
        offerFurnaceRecipe(exporter, BlockContent.PLACER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(ItemContent.PROCESSING_UNIT), of(BlockContent.MACHINE_FRAME_BLOCK.asItem()), of(cItemTag("ingots/copper")), "placer");
        // block destroyer
        offerAtomicForgeRecipe(exporter, BlockContent.DESTROYER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(BlockContent.PULVERIZER_BLOCK), of(BlockContent.LASER_ARM_BLOCK), of(ItemContent.MOTOR), "destroyer");
        // fertilizer
        offerFurnaceRecipe(exporter, BlockContent.FERTILIZER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(TagContent.SILICON), of(ItemContent.PROCESSING_UNIT), of(cItemTag("ingots/copper")), "fertilizer");
        // tree feller
        offerGeneratorRecipe(exporter, BlockContent.TREEFELLER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(Items.IRON_AXE), of(ItemContent.MOTOR), of(TagContent.ELECTRUM_INGOTS), "treefeller");
        // pipe booster
        offerTankRecipe(exporter, BlockContent.PIPE_BOOSTER_BLOCK.asItem(), of(BlockContent.ITEM_PIPE), of(ItemContent.MOTOR), of(BlockContent.FLUID_PIPE), "booster");
        
        // machine frame
        offerMachineFrameRecipe(exporter, BlockContent.MACHINE_FRAME_BLOCK.asItem(), of(Items.IRON_BARS), of(TagContent.NICKEL_INGOTS), 16, "frame");
        // energy pipe
        offerCableRecipe(exporter, new ItemStack(BlockContent.ENERGY_PIPE.asItem(), 6), of(TagContent.ELECTRUM_INGOTS), "energy");
        // item pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.ITEM_PIPE.asItem(), 6), of(TagContent.NICKEL_INGOTS), of(ItemTags.PLANKS), "item");
        // item filter
        offerGeneratorRecipe(exporter, BlockContent.ITEM_FILTER_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemTags.PLANKS), of(ItemContent.PROCESSING_UNIT), of(ItemTags.PLANKS), "itemfilter");
        // fluid pipe
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.FLUID_PIPE.asItem(), 6), of(TagContent.SILICON), of(cItemTag("ingots/copper")), "fluidpipe");
        
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
        offerTankRecipe(exporter, BlockContent.TRANSPARENT_ITEM_PIPE.asItem(), 6, of(ItemTags.PLANKS), of(TagContent.NICKEL_INGOTS), of(cItemTag("glass_blocks")), "transparentitem");
        offerMachineCoreRecipe(exporter, BlockContent.TRANSPARENT_ITEM_PIPE.asItem(), 8, of(BlockContent.ITEM_PIPE), of(cItemTag("glass_blocks")), "totransparent");
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
        offerFurnaceRecipe(exporter, BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem(), of(Items.ENCHANTING_TABLE), of(ItemContent.ADAMANT_INGOT), of(cItemTag("obsidians/normal")), of(ItemContent.UNHOLY_INTELLIGENCE), of(ItemContent.FLUXITE), "catalyst");
        offerFurnaceRecipe(exporter, BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem(), of(Items.ENCHANTING_TABLE), of(ItemContent.ADAMANT_INGOT), of(cItemTag("obsidians/normal")), of(ItemContent.SUPER_AI_CHIP), of(ItemContent.FLUXITE), "catalyst_alt");
        // enchanter
        offerGeneratorRecipe(exporter, BlockContent.ENCHANTER_BLOCK.asItem(), of(ItemContent.DURATIUM_INGOT), of(ItemContent.ENERGITE_INGOT), of(BlockContent.ENCHANTMENT_CATALYST_BLOCK.asItem()), of(Items.BOOK), "enchanter");
        // spawner
        offerTankRecipe(exporter, BlockContent.SPAWNER_CONTROLLER_BLOCK.asItem(), of(BlockContent.SPAWNER_CAGE_BLOCK), of(Blocks.RESPAWN_ANCHOR), of(BlockContent.ENCHANTMENT_CATALYST_BLOCK), "spawner");
        // spawner cage
        offerInsulatedCableRecipe(exporter, new ItemStack(BlockContent.SPAWNER_CAGE_BLOCK, 2), of(TagContent.PLASTIC_PLATES), of(Items.IRON_BARS), "cage");
        // withered rose
        offerMachineFrameRecipe(exporter, BlockContent.WITHER_CROP_BLOCK.asItem(), of(Items.WITHER_ROSE), of(ItemTags.FLOWERS), 1, "witherrose");
        
        // shrinker
        offerTankRecipe(exporter, BlockContent.SHRINKER_BLOCK.asItem(), of(ItemContent.DUBIOS_CONTAINER), of(FluidContent.STILL_STRANGE_MATTER_BUCKET.get()), of(BlockContent.SUPERCONDUCTOR), "shrinker");
        
        // particle accelerator
        // motor
        offerParticleMotorRecipe(exporter, BlockContent.ACCELERATOR_MOTOR.asItem(), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.SUPERCONDUCTOR.asItem()), of(ItemContent.DURATIUM_INGOT), of(ItemContent.ION_THRUSTER), "particlemotor");
        // ring
        offerDrillRecipe(exporter, BlockContent.ACCELERATOR_RING.asItem(), of(BlockContent.INDUSTRIAL_GLASS_BLOCK.asItem()), of(BlockContent.SUPERCONDUCTOR.asItem()), of(TagContent.STEEL_INGOTS), of(Items.REDSTONE_TORCH), "acceleratorring");
        // controller
        offerGeneratorRecipe(exporter, BlockContent.ACCELERATOR_CONTROLLER.asItem(), of(BlockContent.ACCELERATOR_MOTOR.asItem()), of(ItemContent.FLUX_GATE), of(Items.DROPPER), of(ItemContent.DURATIUM_INGOT), "particlecontroller");
        // sensor
        offerTwoComponentRecipe(exporter, BlockContent.ACCELERATOR_SENSOR.asItem(), of(BlockContent.ACCELERATOR_RING.asItem()), of(Items.OBSERVER), "particlesensor");
        // collector
        offerTankRecipe(exporter, BlockContent.PARTICLE_COLLECTOR_BLOCK.asItem(), of(BlockContent.SUPERCONDUCTOR.asItem()), of(BlockContent.BIG_SOLAR_PANEL_BLOCK.asItem()), of(ItemContent.HEISENBERG_COMPENSATOR), "particlecollector");
        
        // addons
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_SPEED_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MAGNETIC_COIL), of(TagContent.STEEL_INGOTS), of(TagContent.PLASTIC_PLATES), "addon/speed");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_PROCESSING_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.SUPER_AI_CHIP), of(ItemContent.FLUX_GATE), of(TagContent.PLATINUM_INGOTS), of(ItemContent.MOTOR), "addon/processing");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_PROCESSING_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.UNHOLY_INTELLIGENCE), of(Items.COMPARATOR), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.MOTOR), "addon/processingalt");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_ULTIMATE_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.HEISENBERG_COMPENSATOR), of(BlockContent.MACHINE_SPEED_ADDON), of(BlockContent.MACHINE_EFFICIENCY_ADDON), of(ItemContent.OVERCHARGED_CRYSTAL), "addon/ultimate");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_BURST_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.METAL_GIRDER_BLOCK), of(TagContent.STEEL_INGOTS), of(Items.REDSTONE), "addon/burst");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_EFFICIENCY_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.CARBON_FIBRE), of(TagContent.ELECTRUM_INGOTS), of(TagContent.PLASTIC_PLATES), "addon/eff");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_CAPACITOR_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.ENERGITE_INGOT), of(ItemContent.MAGNETIC_COIL), of(TagContent.PLASTIC_PLATES), "addon/capacitor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_ACCEPTOR_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENERGITE_INGOT), of(TagContent.PLASTIC_PLATES), "addon/acceptor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_YIELD_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENDERIC_LENS), of(TagContent.PLASTIC_PLATES), "addon/yield");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_FLUID_ADDON.asItem(), of(TagContent.SILICON), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.FLUID_PIPE), of(TagContent.CARBON_FIBRE), "addon/fluid");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_INVENTORY_PROXY_ADDON.asItem(), of(ItemContent.MOTOR), of(cItemTag("chests")), of(ItemContent.PROCESSING_UNIT), of(TagContent.CARBON_FIBRE), "addon/invproxy");
        offerGeneratorRecipe(exporter, BlockContent.CROP_FILTER_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(ItemContent.PROCESSING_UNIT), of(TagContent.CARBON_FIBRE), "addon/cropfilter");
        offerGeneratorRecipe(exporter, BlockContent.QUARRY_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(Items.DIAMOND_PICKAXE), of(TagContent.PLASTIC_PLATES), "addon/quarry");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_HUNTER_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(Items.IRON_SWORD), of(TagContent.PLASTIC_PLATES), "_hunter");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.ADAMANT_INGOT), of(cItemTag("ingots/copper")), of(BlockContent.FLUID_PIPE), "addon/steamboiler");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), of(TagContent.SILICON), of(ItemContent.ADAMANT_INGOT), of(BlockContent.FLUID_PIPE), of(TagContent.COAL_DUSTS), "addon/steamboileralt");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_REDSTONE_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(Items.REPEATER), of(Items.COMPARATOR), of(cItemTag("dusts/redstone")), "addon/redstone");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_SILK_TOUCH_ADDON.asItem(), of(TagContent.MACHINE_PLATING), of(ItemTags.WOOL), of(Items.DIAMOND_PICKAXE), of(TagContent.PLASTIC_PLATES), "addon/silktouch");
        offerTwoComponentRecipe(exporter, BlockContent.CAPACITOR_ADDON_EXTENDER.asItem(), of(BlockContent.MACHINE_EXTENDER.asItem()), of(BlockContent.MACHINE_CAPACITOR_ADDON), "addon/capextender");
        
        // cores
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_1.asItem(), of(ItemTags.PLANKS), of(Items.CRAFTING_TABLE), "core1");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), of(cItemTag("ingots/copper")), of(cItemTag("gems/lapis")), "core2");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), of(cItemTag("ingots/iron")), of(cItemTag("gems/lapis")), "core2alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), of(TagContent.CARBON_FIBRE), of(cItemTag("dusts/redstone")), "core3");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), of(TagContent.NICKEL_INGOTS), of(cItemTag("dusts/redstone")), "core3alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_4.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.ENDERIC_COMPOUND), "core4");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_5.asItem(), of(ItemContent.ADAMANT_INGOT), of(ItemContent.ADVANCED_COMPUTING_ENGINE), "core5");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_6.asItem(), of(ItemContent.DURATIUM_INGOT), of(ItemContent.DUBIOS_CONTAINER), "core6");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_7.asItem(), of(ItemContent.PROMETHEUM_INGOT), of(BlockContent.SUPERCONDUCTOR.asItem()), "core7");
        
        // machine extender
        offerMachinePlatingRecipe(exporter, BlockContent.MACHINE_EXTENDER.asItem(), of(TagContent.MACHINE_PLATING), of(BlockContent.MACHINE_CORE_2.asItem()), of(ItemContent.DURATIUM_INGOT), 1, "extender");
        
        // augmenter
        // machine itself
        offerAtomicForgeRecipe(exporter, BlockContent.AUGMENT_APPLICATION_BLOCK.asItem(), of(TagContent.MACHINE_PLATING), of(ItemContent.MOTOR), of(Items.CHEST), of(TagContent.CARBON_FIBRE), of(ItemContent.DUBIOS_CONTAINER), "augment/applicator");
        // basic station
        offerGeneratorRecipe(exporter, BlockContent.SIMPLE_AUGMENT_STATION.asItem(), of(Items.BREWING_STAND), of(TagContent.MACHINE_PLATING), of(cItemTag("storage_blocks/redstone")), of(TagContent.ELECTRUM_INGOTS), "augment/basic");
        // adv station
        offerGeneratorRecipe(exporter, BlockContent.ADVANCED_AUGMENT_STATION.asItem(), of(BlockContent.CENTRIFUGE_BLOCK), of(TagContent.MACHINE_PLATING), of(ItemContent.FLUX_GATE), of(ItemContent.DURATIUM_INGOT), "augment/advanced");
        // arcane station
        offerGeneratorRecipe(exporter, BlockContent.ARCANE_AUGMENT_STATION.asItem(), of(Items.ENDER_EYE), of(TagContent.MACHINE_PLATING), of(ItemContent.ENDERIC_LENS), of(ItemContent.OVERCHARGED_CRYSTAL), "augment/arcane");
        
    }
    
    private void addComponents(RecipeOutput exporter) {
        // coal stuff (including basic steel)
        CentrifugeRecipeBuilder.build().input(TagContent.COAL_DUSTS).result(ItemContent.CARBON_FIBRE_STRANDS).timeMultiplier(0.5f).export(exporter, "carbon");
        offerManualAlloyRecipe(exporter, ItemContent.STEEL_INGOT, of(cItemTag("ingots/iron")), of(ItemTags.COALS), "steel");
        
        // manual alloys
        offerManualAlloyRecipe(exporter, ItemContent.ELECTRUM_INGOT, of(cItemTag("ingots/gold")), of(cItemTag("dusts/redstone")), "electrum");
        offerManualAlloyRecipe(exporter, ItemContent.ADAMANT_INGOT, of(TagContent.NICKEL_INGOTS), of(cItemTag("gems/diamond")), "adamant");
        
        // enderic entry
        PulverizerRecipeBuilder.build().input(cItemTag("ender_pearls")).result(ItemContent.ENDERIC_COMPOUND, 8).export(exporter, "pearl_enderic");
        GrinderRecipeBuilder.build().input(cItemTag("ender_pearls")).result(ItemContent.ENDERIC_COMPOUND, 12).export(exporter, "pearl_enderic");
        GrinderRecipeBuilder.build().input(Blocks.END_STONE).result(ItemContent.ENDERIC_COMPOUND).export(exporter, "stone_enderic");
        
        // clay beads
        offerBeadsRecipe(exporter,ItemContent.CLAY_CATALYST_BEADS, 8, of(Items.CLAY_BALL), of(ItemTags.SAND), of(Items.REDSTONE), "claybeads");
        AssemblerRecipeBuilder.build().input(Items.CLAY_BALL).input(Items.CLAY_BALL).input(ItemTags.SAND).input(Items.REDSTONE).result(ItemContent.CLAY_CATALYST_BEADS, 32).timeMultiplier(1f).export(exporter, "claybeads");
        
        // magnetic coils
        offerInsulatedCableRecipe(exporter, new ItemStack(ItemContent.MAGNETIC_COIL, 4), of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), "magnet");
        AssemblerRecipeBuilder.build().input(TagContent.STEEL_INGOTS).input(TagContent.NICKEL_INGOTS).input(TagContent.NICKEL_INGOTS).input(cItemTag("ingots/copper")).result(ItemContent.MAGNETIC_COIL, 6).timeMultiplier(0.4f).export(exporter, "magnet");
        
        // motor
        offerMotorRecipe(exporter, ItemContent.MOTOR, of(TagContent.NICKEL_INGOTS), of(ItemContent.MAGNETIC_COIL), of(TagContent.STEEL_INGOTS), "motorcraft");
        AssemblerRecipeBuilder.build().input(TagContent.NICKEL_INGOTS).input(TagContent.STEEL_INGOTS).input(ItemContent.MAGNETIC_COIL).input(ItemContent.MAGNETIC_COIL).result(ItemContent.MOTOR, 2).timeMultiplier(0.4f).export(exporter, "motor");
        
        // machine plating variants
        offerMachinePlatingRecipe(exporter, BlockContent.MACHINE_PLATING_BLOCK.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(cItemTag("ingots/copper")), 2, "plating");
        AssemblerRecipeBuilder.build().input(TagContent.STEEL_INGOTS).input(TagContent.STEEL_INGOTS).input(cItemTag("ingots/copper")).input(TagContent.PLASTIC_PLATES).result(BlockContent.MACHINE_PLATING_BLOCK.asItem(), 8).timeMultiplier(0.8f).export(exporter, "plating");
        offerMachinePlatingRecipe(exporter, BlockContent.IRON_PLATING_BLOCK.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(cItemTag("ingots/iron")), 2, "iron");
        AssemblerRecipeBuilder.build().input(TagContent.STEEL_INGOTS).input(TagContent.STEEL_INGOTS).input(cItemTag("ingots/iron")).input(TagContent.PLASTIC_PLATES).result(BlockContent.IRON_PLATING_BLOCK.asItem(), 8).timeMultiplier(0.8f).export(exporter, "platingiron");
        offerMachinePlatingRecipe(exporter, BlockContent.NICKEL_PLATING_BLOCK.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(TagContent.NICKEL_INGOTS), 2, "nickel");
        AssemblerRecipeBuilder.build().input(TagContent.STEEL_INGOTS).input(TagContent.STEEL_INGOTS).input(TagContent.NICKEL_INGOTS).input(TagContent.PLASTIC_PLATES).result(BlockContent.NICKEL_PLATING_BLOCK.asItem(), 8).timeMultiplier(0.8f).export(exporter, "platingnickel");
        offerMachinePlatingRecipe(exporter, BlockContent.CARBON_PLATING_BLOCK.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(ItemContent.REINFORCED_CARBON_SHEET), 2, "carbon");
        AssemblerRecipeBuilder.build().input(TagContent.STEEL_INGOTS).input(TagContent.STEEL_INGOTS).input(ItemContent.REINFORCED_CARBON_SHEET).input(TagContent.PLASTIC_PLATES).result(BlockContent.CARBON_PLATING_BLOCK.asItem(), 8).timeMultiplier(0.8f).export(exporter, "platingcarbon");
        
        // basic battery
        offerMotorRecipe(exporter, ItemContent.BASIC_BATTERY, of(TagContent.STEEL_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(TagContent.PLASTIC_PLATES), "manualbattery");
        AssemblerRecipeBuilder.build().input(TagContent.PLASTIC_PLATES).input(TagContent.ELECTRUM_INGOTS).input(TagContent.ELECTRUM_INGOTS).input(TagContent.STEEL_INGOTS).result(ItemContent.BASIC_BATTERY).timeMultiplier(0.4f).export(exporter, "battery");
        AssemblerRecipeBuilder.build().input(TagContent.PLASTIC_PLATES).input(ItemContent.FLUXITE).input(ItemContent.FLUXITE).input(TagContent.STEEL_INGOTS).result(ItemContent.BASIC_BATTERY, 2).timeMultiplier(0.8f).export(exporter, "batterybetter");
        
        // silicon
        offerManualAlloyRecipe(exporter, ItemContent.RAW_SILICON, of(TagContent.QUARTZ_DUSTS), of(ItemTags.SAND), 3, "rawsilicon");
        oreSmelting(exporter, List.of(ItemContent.RAW_SILICON), RecipeCategory.MISC, ItemContent.SILICON, 0.5f, 60, "siliconfurnace");
        
        // plastic
        twoByTwoPacker(exporter, RecipeCategory.MISC, ItemContent.PACKED_WHEAT, Items.WHEAT);
        CentrifugeFluidRecipeBuilder.build().input(ItemContent.PACKED_WHEAT).result(ItemContent.RAW_BIOPOLYMER).fluidInput(Fluids.WATER, 0.25f).export(exporter, "biopolymer");
        CentrifugeFluidRecipeBuilder.build().input(ItemContent.SOLID_BIOFUEL).result(ItemContent.RAW_BIOPOLYMER).fluidInput(Fluids.WATER, 0.25f).export(exporter, "biopolymer_biomass");
        CentrifugeFluidRecipeBuilder.build().input(TagContent.BIOMASS_BLOCK).result(ItemContent.RAW_BIOPOLYMER).fluidInput(Fluids.WATER, 0.25f).export(exporter, "biopolymer_bioblock");
        CentrifugeFluidRecipeBuilder.build().input(ItemTags.SAND).result(ItemContent.POLYMER_RESIN).fluidInput(cFluidTag("biodiesel"), 0.1f).time(100).export(exporter, "polymerresin");
        CentrifugeFluidRecipeBuilder.build().input(ItemContent.RAW_BIOPOLYMER).result(ItemContent.PLASTIC_SHEET, 1).fluidInput(Fluids.WATER, 0.5f).time(120).export(exporter, "plasticoil");
        CentrifugeFluidRecipeBuilder.build().input(ItemContent.RAW_BIOPOLYMER).result(ItemContent.PLASTIC_SHEET, 2).fluidInput(FluidContent.STILL_MINERAL_SLURRY.get(), 0.25f).time(120).export(exporter, "plasticoilbetter");
        CentrifugeFluidRecipeBuilder.build().input(ItemContent.POLYMER_RESIN).result(ItemContent.PLASTIC_SHEET, 2).fluidInput(Fluids.WATER, 0.5f).time(40).export(exporter, "plasticbio");
        CentrifugeFluidRecipeBuilder.build().input(ItemContent.POLYMER_RESIN).result(ItemContent.PLASTIC_SHEET, 4).fluidInput(FluidContent.STILL_MINERAL_SLURRY.get(), 0.25f).time(40).export(exporter, "plasticbiobetter");
        oreSmelting(exporter, List.of(ItemContent.POLYMER_RESIN), RecipeCategory.MISC, ItemContent.PLASTIC_SHEET, 0.5f, 10, "plastic_manual");
        oreBlasting(exporter, List.of(ItemContent.POLYMER_RESIN), RecipeCategory.MISC, ItemContent.PLASTIC_SHEET, 0.5f, 10, "plastic_manual_blast");
        
        // processing unit
        AssemblerRecipeBuilder.build().input(TagContent.PLASTIC_PLATES).input(TagContent.CARBON_FIBRE).input(TagContent.ELECTRUM_INGOTS).input(cItemTag("dusts/redstone")).result(ItemContent.PROCESSING_UNIT).timeMultiplier(0.8f).export(exporter, "processingunit");
        // enderic lens
        AssemblerRecipeBuilder.build().input(ItemContent.ADAMANT_INGOT).input(TagContent.CARBON_FIBRE).input(ItemContent.ENDERIC_COMPOUND).input(ItemContent.ENDERIC_COMPOUND).result(ItemContent.ENDERIC_LENS).timeMultiplier(1.2f).export(exporter, "enderlens");
        // flux gate
        AssemblerRecipeBuilder.build().input(ItemContent.PROCESSING_UNIT).input(ItemContent.FLUXITE).input(ItemContent.FLUXITE).input(TagContent.PLATINUM_INGOTS).result(ItemContent.FLUX_GATE).timeMultiplier(1.2f).export(exporter, "fluxgate");
        
        // ai processor tree
        AtomicForgeRecipeBuilder.build().input(TagContent.CARBON_FIBRE).input(TagContent.SILICON).input(TagContent.SILICON).result(ItemContent.SILICON_WAFER).time(5).export(exporter, "wafer");
        AtomicForgeRecipeBuilder.build().input(ItemContent.PROCESSING_UNIT).input(ItemContent.SILICON_WAFER).input(ItemContent.SILICON_WAFER).result(ItemContent.ADVANCED_COMPUTING_ENGINE).time(5).export(exporter, "advcomputer");
        AtomicForgeRecipeBuilder.build().input(ItemContent.DURATIUM_INGOT).input(ItemContent.ADVANCED_COMPUTING_ENGINE).input(ItemContent.ADVANCED_COMPUTING_ENGINE).result(ItemContent.SUPER_AI_CHIP).time(50).export(exporter, "aicomputer");
        
        // dubios container
        offerMotorRecipe(exporter, ItemContent.DUBIOS_CONTAINER, of(TagContent.PLASTIC_PLATES), of(ItemContent.ADAMANT_INGOT), of(ItemContent.ENDERIC_COMPOUND), "dubios");
        // adv battery
        offerMotorRecipe(exporter, ItemContent.ADVANCED_BATTERY, of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENERGITE_INGOT), of(TagContent.STEEL_INGOTS), "advbattery");
        
        // ion thruster
        AssemblerRecipeBuilder.build().input(ItemContent.REINFORCED_CARBON_SHEET).input(ItemContent.REINFORCED_CARBON_SHEET).input(ItemContent.ADVANCED_BATTERY).input(ItemContent.FLUX_GATE).result(ItemContent.ION_THRUSTER, 2).timeMultiplier(2f).export(exporter, "ionthruster");
        
        // biosteel
        FoundryRecipeBuilder.build().input(ItemContent.RAW_BIOPOLYMER).input(cItemTag("ingots/iron")).result(ItemContent.BIOSTEEL_INGOT).export(exporter, "biosteel");
        
        // endgame components
        AtomicForgeRecipeBuilder.build().input(ItemContent.SUPER_AI_CHIP).input(ItemContent.ADAMANT_INGOT).input(ItemContent.ADAMANT_INGOT).result(ItemContent.HEISENBERG_COMPENSATOR).time(60).export(exporter, "compensator");
        AtomicForgeRecipeBuilder.build().input(ItemContent.UNHOLY_INTELLIGENCE).input(ItemContent.ADAMANT_INGOT).input(ItemContent.ADAMANT_INGOT).result(ItemContent.HEISENBERG_COMPENSATOR).time(60).export(exporter, "compensatoralt");
        offerMotorRecipe(exporter, ItemContent.OVERCHARGED_CRYSTAL, of(Items.AMETHYST_BLOCK), of(ItemContent.ADVANCED_BATTERY), of(BlockContent.SUPERCONDUCTOR.asItem()), "overchargedcrystal");
        AssemblerRecipeBuilder.build().input(ItemContent.FLUX_GATE).input(TagContent.ELECTRUM_INGOTS).input(ItemContent.DUBIOS_CONTAINER).input(ItemContent.ENERGITE_INGOT).result(BlockContent.SUPERCONDUCTOR.asItem(), 4).timeMultiplier(1.6f).export(exporter, "superconductor");
        AtomicForgeRecipeBuilder.build().input(ItemContent.HEISENBERG_COMPENSATOR).input(ItemContent.OVERCHARGED_CRYSTAL).input(ItemContent.OVERCHARGED_CRYSTAL).result(ItemContent.PROMETHEUM_INGOT).time(240).export(exporter, "prometheum");
        
        // ice in cooler
        CoolerRecipeBuilder.build().fluidInput(Fluids.WATER).result(Items.ICE, 3).export(exporter, "ice");
        
        // snow from steam in cooler
        CoolerRecipeBuilder.build().fluidInput(FluidContent.STILL_STEAM.get()).result(Items.SNOW_BLOCK, 3).export(exporter, "snow");
        
        // obsidian from lava
        CoolerRecipeBuilder.build().fluidInput(Fluids.LAVA).result(Items.OBSIDIAN, 2).export(exporter, "obsidian");
    }
    
    private void addCompactingRecipes(RecipeOutput exporter) {
        addCompactingRecipe(exporter, BlockContent.STEEL_BLOCK, ItemContent.STEEL_INGOT, of(TagContent.STEEL_INGOTS), of(getStorageBlockTag("steel")));
        addCompactingRecipe(exporter, BlockContent.ENERGITE_BLOCK, ItemContent.ENERGITE_INGOT, of(getIngotTag("energite")), of(getStorageBlockTag("energite")));
        addCompactingRecipe(exporter, BlockContent.NICKEL_BLOCK, ItemContent.NICKEL_INGOT, of(getIngotTag("nickel")), of(getStorageBlockTag("nickel")));
        addCompactingRecipe(exporter, BlockContent.BIOSTEEL_BLOCK, ItemContent.BIOSTEEL_INGOT, of(ItemContent.BIOSTEEL_INGOT), of(getStorageBlockTag("biosteel")));
        addCompactingRecipe(exporter, BlockContent.PLATINUM_BLOCK, ItemContent.PLATINUM_INGOT, of(getIngotTag("platinum")), of(getStorageBlockTag("platinum")));
        addCompactingRecipe(exporter, BlockContent.ADAMANT_BLOCK, ItemContent.ADAMANT_INGOT, of(getIngotTag("adamant")), of(getStorageBlockTag("adamant")));
        addCompactingRecipe(exporter, BlockContent.ELECTRUM_BLOCK, ItemContent.ELECTRUM_INGOT, of(getIngotTag("electrum")), of(getStorageBlockTag("electrum")));
        addCompactingRecipe(exporter, BlockContent.DURATIUM_BLOCK, ItemContent.DURATIUM_INGOT, of(getIngotTag("duratium")), of(getStorageBlockTag("duratium")));
        addCompactingRecipe(exporter, BlockContent.BIOMASS_BLOCK, ItemContent.BIOMASS, of(ItemContent.BIOMASS), of(getStorageBlockTag("biomass")));
        addCompactingRecipe(exporter, BlockContent.PLASTIC_BLOCK, ItemContent.PLASTIC_SHEET, of(TagContent.PLASTIC_PLATES), of(getStorageBlockTag("plastic")));
        addCompactingRecipe(exporter, BlockContent.FLUXITE_BLOCK, ItemContent.FLUXITE, of(ItemContent.FLUXITE), of(getStorageBlockTag("fluxite")));
        addCompactingRecipe(exporter, BlockContent.SILICON_BLOCK, ItemContent.SILICON, of(TagContent.SILICON), of(getStorageBlockTag("silicon")));
        addCompactingRecipe(exporter, BlockContent.RAW_NICKEL_BLOCK, ItemContent.RAW_NICKEL, of(TagContent.NICKEL_RAW_MATERIALS), of(getStorageBlockTag("raw_nickel")));
        addCompactingRecipe(exporter, BlockContent.RAW_PLATINUM_BLOCK, ItemContent.RAW_PLATINUM, of(TagContent.PLATINUM_RAW_MATERIALS), of(getStorageBlockTag("raw_platinum")));
        
    }

    // offerSmelting, offerBlasting, and offerMultipleOptions copied from RecipeProvider, and altered to force Oritech id onto recipes
    // I don't really like this, but any other way I found to get these recipes to have the oritech namespace in Neoforge wasn't working.
    public static void oreSmelting(RecipeOutput exporter, List<ItemLike> inputs, RecipeCategory category, ItemLike output, float experience, int cookingTime, String group) {
      oreCooking(exporter, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, inputs, category, output, experience, cookingTime, group, "_from_smelting");
    }

    public static void oreBlasting(RecipeOutput exporter, List<ItemLike> inputs, RecipeCategory category, ItemLike output, float experience, int cookingTime, String group) {
      oreCooking(exporter, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, inputs, category, output, experience, cookingTime, group, "_from_blasting");
    }

    public static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput exporter, RecipeSerializer<T> serializer, AbstractCookingRecipe.Factory<T> recipeFactory, List<ItemLike> inputs, RecipeCategory category, ItemLike output, float experience, int cookingTime, String group, String suffix) {
        
        for (var itemConvertible : inputs) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemConvertible), category, output, experience, cookingTime, serializer, recipeFactory).group(group).unlockedBy(getHasName(itemConvertible), has(itemConvertible)).save(exporter, Oritech.id(getItemName(output) + suffix + "_" + getItemName(itemConvertible)));
        }
    }
    
    private void addOreChains(RecipeOutput exporter) {
        
        // basic smelting for nickel + platinum
        oreSmelting(exporter, List.of(ItemContent.RAW_NICKEL), RecipeCategory.MISC, ItemContent.NICKEL_INGOT, 1f, 200, "nickelsmelting");
        oreSmelting(exporter, List.of(ItemContent.RAW_PLATINUM), RecipeCategory.MISC, ItemContent.PLATINUM_INGOT, 1f, 200, "platinumsmelting");
        oreBlasting(exporter, List.of(ItemContent.RAW_NICKEL), RecipeCategory.MISC, ItemContent.NICKEL_INGOT, 1f, 100, "nickelblasting");
        oreBlasting(exporter, List.of(ItemContent.RAW_PLATINUM), RecipeCategory.MISC, ItemContent.PLATINUM_INGOT, 1f, 100, "platinumblasting");
        
        // iron chain
        MetalProcessingChainBuilder.build("iron")
          .ore(ItemTags.IRON_ORES)
          .rawOre(cItemTag("raw_materials/iron"), Items.RAW_IRON)
          .rawOreByproduct(ItemContent.RAW_NICKEL)
          .ingot(cItemTag("ingots/iron"), Items.IRON_INGOT).nugget(cItemTag("nuggets/iron"), Items.IRON_NUGGET)
          .clump(TagContent.IRON_CLUMPS, ItemContent.IRON_CLUMP).smallClump(ItemContent.SMALL_IRON_CLUMP).clumpByproduct(ItemContent.SMALL_NICKEL_CLUMP)
          .dust(ItemContent.IRON_DUST).smallDust(ItemContent.SMALL_IRON_DUST).dustByproduct(ItemContent.SMALL_NICKEL_DUST)
          .gem(ItemContent.IRON_GEM).gemCatalyst(ItemContent.FLUXITE)
          .vanillaProcessing()
          .skipCompacting()
          .export(exporter);
        // copper chain
        MetalProcessingChainBuilder.build("copper")
          .ore(ItemTags.COPPER_ORES)
          .rawOre(cItemTag("raw_materials/copper"), Items.RAW_COPPER).rawOreByproduct(Items.RAW_GOLD)
          .ingot(cItemTag("ingots/copper"), Items.COPPER_INGOT).nugget(TagContent.COPPER_NUGGETS, ItemContent.COPPER_NUGGET)
          .clump(TagContent.COPPER_CLUMPS, ItemContent.COPPER_CLUMP).smallClump(ItemContent.SMALL_COPPER_CLUMP).clumpByproduct(ItemContent.SMALL_GOLD_CLUMP)
          .dust(ItemContent.COPPER_DUST).smallDust(ItemContent.SMALL_COPPER_DUST).dustByproduct(ItemContent.SMALL_GOLD_DUST)
          .gem(ItemContent.COPPER_GEM).gemCatalyst(ItemContent.FLUXITE)
          .vanillaProcessing()
          .skipCompacting()
          .export(exporter);
        // gold chain
        MetalProcessingChainBuilder.build("gold")
          .ore(ItemTags.GOLD_ORES)
          .rawOre(cItemTag("raw_materials/gold"), Items.RAW_GOLD).rawOreByproduct(Items.RAW_COPPER)
          .ingot(cItemTag("ingots/gold"), Items.GOLD_INGOT).nugget(cItemTag("nuggets/gold"), Items.GOLD_NUGGET)
          .clump(TagContent.GOLD_CLUMPS, ItemContent.GOLD_CLUMP).smallClump(ItemContent.SMALL_GOLD_CLUMP).clumpByproduct(ItemContent.SMALL_COPPER_CLUMP)
          .dust(ItemContent.GOLD_DUST).smallDust(ItemContent.SMALL_GOLD_DUST).dustByproduct(ItemContent.SMALL_COPPER_DUST)
          .gem(ItemContent.GOLD_GEM).gemCatalyst(ItemContent.FLUXITE)
          .vanillaProcessing()
          .skipCompacting()
          .export(exporter);
        // nickel chain
        MetalProcessingChainBuilder.build("nickel")
          .ore(TagContent.NICKEL_ORES)
          .rawOre(TagContent.NICKEL_RAW_MATERIALS, ItemContent.RAW_NICKEL).rawOreByproduct(ItemContent.RAW_PLATINUM)
          .ingot(TagContent.NICKEL_INGOTS, ItemContent.NICKEL_INGOT).nugget(TagContent.NICKEL_NUGGETS, ItemContent.NICKEL_NUGGET)
          .clump(TagContent.NICKEL_CLUMPS, ItemContent.NICKEL_CLUMP).smallClump(ItemContent.SMALL_NICKEL_CLUMP).clumpByproduct(ItemContent.SMALL_PLATINUM_CLUMP)
          .dust(ItemContent.NICKEL_DUST).smallDust(ItemContent.SMALL_NICKEL_DUST).dustByproduct(ItemContent.SMALL_PLATINUM_DUST)
          .byproductAmount(2)
          .gem(ItemContent.NICKEL_GEM).gemCatalyst(ItemContent.FLUXITE)
          .vanillaProcessing()
          .export(exporter);
        // platinum chain
        MetalProcessingChainBuilder.build("platinum")
          .ore(TagContent.PLATINUM_ORES)
          .rawOre(TagContent.PLATINUM_RAW_MATERIALS, ItemContent.RAW_PLATINUM).rawOreByproduct(ItemContent.FLUXITE)
          .ingot(TagContent.PLATINUM_INGOTS, ItemContent.PLATINUM_INGOT).nugget(TagContent.PLATINUM_NUGGETS, ItemContent.PLATINUM_NUGGET)
          .clump(TagContent.PLATINUM_CLUMPS, ItemContent.PLATINUM_CLUMP).smallClump(ItemContent.SMALL_PLATINUM_CLUMP).clumpByproduct(ItemContent.FLUXITE)
          .dust(ItemContent.PLATINUM_DUST).smallDust(ItemContent.SMALL_PLATINUM_DUST).dustByproduct(ItemContent.FLUXITE)
          .byproductAmount(1)
          .gem(ItemContent.PLATINUM_GEM).gemCatalyst(ItemContent.FLUXITE)
          .timeMultiplier(1.5f)
          .vanillaProcessing()
          .export(exporter);
        
    }
    
    private void addAlloys(RecipeOutput exporter) {
        FoundryRecipeBuilder.build().input(TagContent.PLATINUM_INGOTS).input(cItemTag("ingots/netherite")).result(ItemContent.DURATIUM_INGOT).export(exporter, "duratium");
        FoundryRecipeBuilder.build().input(cItemTag("ingots/gold")).input(cItemTag("dusts/redstone")).result(ItemContent.ELECTRUM_INGOT).export(exporter, "electrum");
        FoundryRecipeBuilder.build().input(cItemTag("gems/diamond")).input(TagContent.NICKEL_INGOTS).result(ItemContent.ADAMANT_INGOT).export(exporter, "adamant");
        FoundryRecipeBuilder.build().input(TagContent.NICKEL_INGOTS).input(ItemContent.FLUXITE).result(ItemContent.ENERGITE_INGOT).export(exporter, "energite");
        FoundryRecipeBuilder.build().input(cItemTag("ingots/iron")).input(TagContent.COAL_DUSTS).result(ItemContent.STEEL_INGOT).timeMultiplier(0.3333f).export(exporter, "steel");
        AtomicForgeRecipeBuilder.build().input(TagContent.PLATINUM_INGOTS).input(ItemContent.REINFORCED_CARBON_SHEET).input(ItemContent.REINFORCED_CARBON_SHEET).result(ItemContent.DURATIUM_INGOT).export(exporter, "duratium");
    }
    
    private void addParticleCollisions(RecipeOutput exporter) {
        // diamond from coal dust
        ParticleCollisionRecipeBuilder.build().input(TagContent.COAL_DUSTS).input(TagContent.COAL_DUSTS).result(Items.DIAMOND).time(500).export(exporter, "diamond");
        // overcharged crystal from fluxite and energite dust
        ParticleCollisionRecipeBuilder.build().input(ItemContent.FLUXITE).input(ItemContent.ENERGITE_DUST).result(ItemContent.OVERCHARGED_CRYSTAL).time(5000).export(exporter, "overcharged_crystal");
        // platinum from gold dust
        ParticleCollisionRecipeBuilder.build().input(TagContent.GOLD_DUSTS).input(TagContent.GOLD_DUSTS).result(ItemContent.PLATINUM_DUST).time(500).export(exporter, "platinum_dust");
        // enderic compound from redstone and flesh
        ParticleCollisionRecipeBuilder.build().input(cItemTag("dusts/redstone")).input(Items.ROTTEN_FLESH).result(ItemContent.ENDERIC_COMPOUND).time(500).export(exporter, "enderic_compound");
        // fluxite from electrum dust and redstone
        ParticleCollisionRecipeBuilder.build().input(TagContent.ELECTRUM_DUSTS).input(cItemTag("dusts/redstone")).result(ItemContent.FLUXITE).time(1000).export(exporter, "fluxite");
        // netherite scrap from adamant dust and netherrack
        ParticleCollisionRecipeBuilder.build().input(ItemContent.ADAMANT_DUST).input(Items.NETHERRACK).result(Items.NETHERITE_SCRAP).time(2500).export(exporter, "netherite");
        // elytra from feather and saddle
        ParticleCollisionRecipeBuilder.build().input(cItemTag("feathers")).input(Items.SADDLE).result(Items.ELYTRA).time(10000).export(exporter, "elytra");
        // nether star from overcharged crystal and netherite
        ParticleCollisionRecipeBuilder.build().input(ItemContent.OVERCHARGED_CRYSTAL).input(cItemTag("ingots/netherite")).result(Items.NETHER_STAR).time(15000).export(exporter, "nether_star");
        // echo shard from ender pearl and amethyst shard
        ParticleCollisionRecipeBuilder.build().input(cItemTag("ender_pearls")).input(cItemTag("gems/amethyst")).result(Items.ECHO_SHARD).time(1000).export(exporter, "echo_shard");
        // heavy core from reinforced deepslate block and duration dust
        ParticleCollisionRecipeBuilder.build().input(Items.REINFORCED_DEEPSLATE).input(ItemContent.DURATIUM_DUST).result(Items.HEAVY_CORE).time(8000).export(exporter, "heavy_core");
    }
    
    private void addDusts(RecipeOutput exporter) {
        addDustRecipe(exporter, of(ItemContent.BIOSTEEL_INGOT), ItemContent.BIOSTEEL_DUST, ItemContent.BIOSTEEL_INGOT, "biosteel");
        addDustRecipe(exporter, of(ItemContent.DURATIUM_INGOT), ItemContent.DURATIUM_DUST, ItemContent.DURATIUM_INGOT, "duratium");
        addDustRecipe(exporter, of(TagContent.ELECTRUM_INGOTS), ItemContent.ELECTRUM_DUST, ItemContent.ELECTRUM_INGOT, "electrum");
        addDustRecipe(exporter, of(ItemContent.ADAMANT_INGOT), ItemContent.ADAMANT_DUST, ItemContent.ADAMANT_INGOT, "adamant");
        addDustRecipe(exporter, of(ItemContent.ENERGITE_INGOT), ItemContent.ENERGITE_DUST, ItemContent.ENERGITE_INGOT, "energite");
        addDustRecipe(exporter, of(TagContent.STEEL_INGOTS), ItemContent.STEEL_DUST, ItemContent.STEEL_INGOT, "steel");
        addDustRecipe(exporter, of(ItemTags.COALS), ItemContent.COAL_DUST, "coal");
        addDustRecipe(exporter, of(cItemTag("gems/quartz")), ItemContent.QUARTZ_DUST, "quartz");
        
        // raw ores without processing chains
        // coal
        GrinderRecipeBuilder.build().input(ItemTags.COAL_ORES).result(Items.COAL, 3).export(exporter, "coalore");
        PulverizerRecipeBuilder.build().input(ItemTags.COAL_ORES).result(Items.COAL, 2).export(exporter, "coalore");
        // redstone
        GrinderRecipeBuilder.build().input(ItemTags.REDSTONE_ORES).result(Items.REDSTONE, 12).export(exporter, "redstoneore");
        PulverizerRecipeBuilder.build().input(ItemTags.REDSTONE_ORES).result(Items.REDSTONE, 8).export(exporter, "redstoneore");
        // diamond
        GrinderRecipeBuilder.build().input(ItemTags.DIAMOND_ORES).result(Items.DIAMOND, 2).export(exporter, "diamondore");
        PulverizerRecipeBuilder.build().input(ItemTags.DIAMOND_ORES).result(Items.DIAMOND).export(exporter, "diamondore");
        // quartz
        GrinderRecipeBuilder.build().input(Blocks.NETHER_QUARTZ_ORE).result(Items.QUARTZ, 3).export(exporter, "quartzore");
        PulverizerRecipeBuilder.build().input(Blocks.NETHER_QUARTZ_ORE).result(Items.QUARTZ, 2).export(exporter, "quartzore");
        // glowstone
        GrinderRecipeBuilder.build().input(Blocks.GLOWSTONE).result(Items.GLOWSTONE_DUST, 4).export(exporter, "glowstoneore");
        PulverizerRecipeBuilder.build().input(Blocks.GLOWSTONE).result(Items.GLOWSTONE_DUST, 3).export(exporter, "glowstoneore");
        // lapis
        GrinderRecipeBuilder.build().input(ItemTags.LAPIS_ORES).result(Items.LAPIS_LAZULI, 8).export(exporter, "lapisore");
        PulverizerRecipeBuilder.build().input(ItemTags.LAPIS_ORES).result(Items.LAPIS_LAZULI, 6).export(exporter, "lapisore");
        // bone
        GrinderRecipeBuilder.build().input(Items.BONE).result(Items.BONE_MEAL, 8).export(exporter, "bone");
        PulverizerRecipeBuilder.build().input(Items.BONE).result(Items.BONE_MEAL, 6).export(exporter, "bone");
        // blaze powder
        GrinderRecipeBuilder.build().input(Items.BLAZE_ROD).result(Items.BLAZE_POWDER, 4).export(exporter, "blaze");
        PulverizerRecipeBuilder.build().input(Items.BLAZE_ROD).result(Items.BLAZE_POWDER, 3).export(exporter, "blaze");
        // wool
        GrinderRecipeBuilder.build().input(ItemTags.WOOL).result(Items.STRING, 4).export(exporter, "string");
        PulverizerRecipeBuilder.build().input(ItemTags.WOOL).result(Items.STRING, 3).export(exporter, "string");
        // ancient debris
        GrinderRecipeBuilder.build().input(Items.ANCIENT_DEBRIS).result(Items.NETHERITE_SCRAP, 2).export(exporter, "netheritescrap");
    }
    
    private void addUraniumProcessing(RecipeOutput exporter) {
        // uranium order is:
        // raw ore -> dust/gem, dust -> gem, gem -> pellets
        
        // plutonium can be made via either ender laser on crystals (manually, usually low amount)
        // or via the particle accelerator
        
        // small uranium dust from redstone
        CentrifugeRecipeBuilder.build().input(cItemTag("dusts/redstone")).result(ItemContent.SMALL_URANIUM_DUST).export(exporter, "redstoneuran");
        
        // uranium ore blocks
        GrinderRecipeBuilder.build().input(BlockContent.DEEPSLATE_URANIUM_ORE).result(ItemContent.RAW_URANIUM, 3).result(ItemContent.SMALL_PLUTONIUM_DUST).export(exporter, "uraniumore");
        PulverizerRecipeBuilder.build().input(BlockContent.DEEPSLATE_URANIUM_ORE).result(ItemContent.RAW_URANIUM, 2).export(exporter, "uraniumore");
        
        // uranium crystal blocks
        GrinderRecipeBuilder.build().input(BlockContent.URANIUM_CRYSTAL).result(ItemContent.RAW_URANIUM, 5).result(ItemContent.SMALL_PLUTONIUM_DUST).export(exporter, "uraniumcrystal");
        PulverizerRecipeBuilder.build().input(BlockContent.URANIUM_CRYSTAL).result(ItemContent.RAW_URANIUM, 4).export(exporter, "uraniumcrystal");
        
        // raw uranium in grinder
        GrinderRecipeBuilder.build().input(TagContent.URANIUM_RAW_MATERIALS).result(ItemContent.URANIUM_DUST, 2).result(ItemContent.SMALL_PLUTONIUM_DUST).export(exporter, "uranium");
        PulverizerRecipeBuilder.build().input(TagContent.URANIUM_RAW_MATERIALS).result(ItemContent.URANIUM_DUST, 2).export(exporter, "uranium");
        
        // uranium gem from raw uranium / uranium dust in atomic forge
        AtomicForgeRecipeBuilder.build().input(TagContent.COPPER_DUSTS).input(TagContent.URANIUM_RAW_MATERIALS).input(TagContent.URANIUM_RAW_MATERIALS).result(ItemContent.URANIUM_GEM).time(5).export(exporter, "urandust");
        AtomicForgeRecipeBuilder.build().input(TagContent.COPPER_DUSTS).input(TagContent.URANIUM_DUSTS).input(TagContent.URANIUM_DUSTS).result(ItemContent.URANIUM_GEM).time(5).export(exporter, "urandustgem");
        
        // uranium pellets in assembler
        AssemblerRecipeBuilder.build().input(ItemContent.URANIUM_GEM).input(ItemContent.URANIUM_GEM).input(TagContent.PLASTIC_PLATES).input(TagContent.NICKEL_INGOTS).result(ItemContent.URANIUM_PELLET, 2).timeMultiplier(0.8f).export(exporter, "uranpelletbasic");
        AssemblerRecipeBuilder.build().input(ItemContent.URANIUM_GEM).input(ItemContent.URANIUM_GEM).input(TagContent.PLASTIC_PLATES).input(ItemContent.ADAMANT_INGOT).result(ItemContent.URANIUM_PELLET, 3).timeMultiplier(0.8f).export(exporter, "uranpelletbetter");
        AssemblerRecipeBuilder.build().input(ItemContent.URANIUM_GEM).input(ItemContent.URANIUM_GEM).input(TagContent.PLASTIC_PLATES).input(ItemContent.DURATIUM_INGOT).result(ItemContent.URANIUM_PELLET, 4).timeMultiplier(0.8f).export(exporter, "uranpelletult");
        
        // plutonium pellets in assembler
        AssemblerRecipeBuilder.build().input(ItemContent.PLUTONIUM_DUST).input(ItemContent.PLUTONIUM_DUST).input(TagContent.PLASTIC_PLATES).input(TagContent.NICKEL_INGOTS).result(ItemContent.PLUTONIUM_PELLET, 2).timeMultiplier(0.8f).export(exporter, "plutoniumpelletbasic");
        AssemblerRecipeBuilder.build().input(ItemContent.PLUTONIUM_DUST).input(ItemContent.PLUTONIUM_DUST).input(TagContent.PLASTIC_PLATES).input(ItemContent.ADAMANT_INGOT).result(ItemContent.PLUTONIUM_PELLET, 3).timeMultiplier(0.8f).export(exporter, "plutoniumpelletbetter");
        AssemblerRecipeBuilder.build().input(ItemContent.PLUTONIUM_DUST).input(ItemContent.PLUTONIUM_DUST).input(TagContent.PLASTIC_PLATES).input(ItemContent.DURATIUM_INGOT).result(ItemContent.PLUTONIUM_PELLET, 4).timeMultiplier(0.8f).export(exporter, "plutoniumpelletult");
        
        // dust compacting
        addCompactingRecipe(exporter, ItemContent.URANIUM_DUST, ItemContent.SMALL_URANIUM_DUST, of(ItemContent.SMALL_URANIUM_DUST), of(TagContent.URANIUM_DUSTS));
        addCompactingRecipe(exporter, ItemContent.PLUTONIUM_DUST, ItemContent.SMALL_PLUTONIUM_DUST, of(ItemContent.SMALL_PLUTONIUM_DUST), of(TagContent.PLUTONIUM_DUSTS));
        
        // uranium to plutonium
        ParticleCollisionRecipeBuilder.build().input(TagContent.URANIUM_DUSTS).input(ItemContent.FLUXITE).result(ItemContent.PLUTONIUM_DUST).time(2500).export(exporter, "plutonium");
        
        // pellet compacting
        addCompactingRecipe(exporter, ItemContent.URANIUM_PELLET, ItemContent.SMALL_URANIUM_PELLET, of(ItemContent.SMALL_URANIUM_PELLET), of(ItemContent.URANIUM_PELLET));
        addCompactingRecipe(exporter, ItemContent.PLUTONIUM_PELLET, ItemContent.SMALL_PLUTONIUM_PELLET, of(ItemContent.SMALL_PLUTONIUM_PELLET), of(ItemContent.PLUTONIUM_PELLET));
    }
    
    private void addAugmentRecipes(RecipeOutput exporter) {
        
        var SIMPLE_AUGMENT_STATION_ID = BuiltInRegistries.BLOCK.getKey(BlockContent.SIMPLE_AUGMENT_STATION);
        var ADVANCED_AUGMENT_STATION_ID = BuiltInRegistries.BLOCK.getKey(BlockContent.ADVANCED_AUGMENT_STATION);
        var ARCANE_AUGMENT_STATION_ID = BuiltInRegistries.BLOCK.getKey(BlockContent.ARCANE_AUGMENT_STATION);
        
        AugmentRecipeBuilder.build()
          .researchCost(TagContent.MACHINE_PLATING, 64)
          .researchCost(TagContent.COAL_DUSTS, 32)
          .researchCost(ItemContent.BIOSTEEL_INGOT, 8)
          .applyCost(TagContent.MACHINE_PLATING, 8)
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(5).uiY(70).time(400).rfCost(10_000_000)
          .modifierDefinition(Attributes.MAX_HEALTH, 6, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "hpboost");
        
        AugmentRecipeBuilder.build()
          .researchCost(TagContent.CARBON_FIBRE, 32)
          .researchCost(ItemContent.BIOSTEEL_INGOT, 16)
          .researchCost(Items.DIAMOND, 4)
          .applyCost(TagContent.CARBON_FIBRE, 8)
          .requirement(Oritech.id("augment/armor"))
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(80).uiY(70).time(800).rfCost(50_000_000)
          .modifierDefinition(Attributes.MAX_HEALTH, 4, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "hpboostmore");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.ENERGITE_INGOT, 64)
          .researchCost(ItemContent.REINFORCED_CARBON_SHEET, 32)
          .researchCost(Items.NETHER_STAR)
          .applyCost(ItemContent.ENERGITE_INGOT, 4)
          .requirement(Oritech.id("augment/ultimatearmor"))
          .requiredStation(ADVANCED_AUGMENT_STATION_ID)
          .uiX(165).uiY(70).time(1600).rfCost(200_000_000)
          .modifierDefinition(Attributes.MAX_HEALTH, 10, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "hpboostultra");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.ADAMANT_INGOT, 32)
          .researchCost(Items.NETHER_STAR, 4)
          .researchCost(ItemContent.URANIUM_PELLET, 64)
          .researchCost(BlockContent.FLUXITE_BLOCK, 64)
          .applyCost(ItemContent.ADAMANT_INGOT, 4)
          .requirement(Oritech.id("augment/hpboostultra"))
          .requirement(Oritech.id("augment/gravity"))
          .requiredStation(ADVANCED_AUGMENT_STATION_ID)
          .uiX(205).uiY(40).time(2400).rfCost(500_000_000)
          .modifierDefinition(Attributes.MAX_HEALTH, 10, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "hpboostultimate");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.MOTOR, 16)
          .researchCost(ItemContent.BIOSTEEL_INGOT, 32)
          .researchCost(Items.REDSTONE, 64)
          .applyCost(ItemContent.MOTOR, 4)
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(5).uiY(30).time(600).rfCost(30_000_000)
          .modifierDefinition(Attributes.MOVEMENT_SPEED, 0.25f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
          .export(exporter, "speedboost");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.ENERGITE_INGOT, 32)
          .researchCost(ItemContent.ION_THRUSTER, 16)
          .researchCost(ItemContent.FLUX_GATE, 16)
          .applyCost(ItemContent.ENERGITE_INGOT, 4)
          .requirement(Oritech.id("augment/speedboost"))
          .requirement(Oritech.id("augment/armor"))
          .requiredStation(ADVANCED_AUGMENT_STATION_ID)
          .uiX(55).uiY(50).time(1800).rfCost(150_000_000)
          .modifierDefinition(Attributes.MOVEMENT_SPEED, 0.25f, AttributeModifier.Operation.ADD_VALUE)
          .toggleable(true)
          .export(exporter, "superspeedboost");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.MOTOR, 32)
          .researchCost(TagContent.STEEL_INGOTS, 64)
          .applyCost(ItemContent.MOTOR, 4)
          .requirement(Oritech.id("augment/superspeedboost"))
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(80).uiY(50).time(800).rfCost(75_000_000)
          .modifierDefinition(Attributes.STEP_HEIGHT, 0.6f, AttributeModifier.Operation.ADD_VALUE)
          .toggleable()
          .export(exporter, "stepassist");
        
        AugmentRecipeBuilder.build()
          .researchCost(Items.COPPER_INGOT, 64)
          .researchCost(ItemContent.PROCESSING_UNIT, 8)
          .researchCost(Items.GOLD_INGOT, 32)
          .applyCost(TagContent.SILICON, 4)
          .requirement(Oritech.id("augment/hpboost"))
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(30).uiY(90).time(400).rfCost(20_000_000)
          .modifierDefinition(Attributes.SCALE, -0.5f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
          .toggleable()
          .export(exporter, "dwarf");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.RAW_BIOPOLYMER, 64)
          .researchCost(ItemContent.SMALL_URANIUM_DUST, 4)
          .applyCost(ItemContent.RAW_BIOPOLYMER, 8)
          .requirement(Oritech.id("augment/dwarf"))
          .requirement(Oritech.id("augment/armor"))
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(55).uiY(90).time(1600).rfCost(80_000_000)
          .modifierDefinition(Attributes.SCALE, 1f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
          .toggleable()
          .export(exporter, "giant");
        
        AugmentRecipeBuilder.build()
          .researchCost(TagContent.STEEL_INGOTS, 64)
          .researchCost(ItemContent.DURATIUM_INGOT, 8)
          .researchCost(Items.DIAMOND, 16)
          .applyCost(ItemContent.DURATIUM_INGOT, 4)
          .applyCost(cItemTag("ingots/iron"), 32)
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(30).uiY(50).time(800).rfCost(80_000_000)
          .modifierDefinition(Attributes.ARMOR, 4, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "armor");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.ENERGITE_INGOT, 64)
          .researchCost(ItemContent.MAGNETIC_COIL, 32)
          .researchCost(Items.DIAMOND, 8)
          .applyCost(ItemContent.MAGNETIC_COIL, 4)
          .requirement(Oritech.id("augment/autofeeder"))
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(105).uiY(50).time(1600).rfCost(180_000_000)
          .modifierDefinition(Attributes.ARMOR, 6, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "betterarmor");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.FLUXITE, 64)
          .researchCost(ItemContent.HEISENBERG_COMPENSATOR, 32)
          .researchCost(ItemContent.PLUTONIUM_PELLET, 64)
          .researchCost(Items.NETHER_STAR, 4)
          .applyCost(ItemContent.HEISENBERG_COMPENSATOR, 1)
          .requirement(Oritech.id("augment/betterarmor"))
          .requiredStation(ADVANCED_AUGMENT_STATION_ID)
          .uiX(155).uiY(50).time(2400).rfCost(500_000_000)
          .modifierDefinition(Attributes.ARMOR, 8, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "ultimatearmor");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.MAGNETIC_COIL, 32)
          .researchCost(TagContent.ELECTRUM_INGOTS, 48)
          .researchCost(Items.ENDER_PEARL, 4)
          .applyCost(ItemContent.MAGNETIC_COIL, 4)
          .requirement(Oritech.id("augment/blockreach"))
          .requiredStation(ADVANCED_AUGMENT_STATION_ID)
          .uiX(140).uiY(70).time(1600).rfCost(150_000_000)
          .modifierDefinition(Attributes.ENTITY_INTERACTION_RANGE, 0.3f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
          .export(exporter, "weaponreach");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.MOTOR, 64)
          .researchCost(TagContent.STEEL_INGOTS, 48)
          .researchCost(Items.ENDER_PEARL, 4)
          .applyCost(ItemContent.MOTOR, 4)
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(115).uiY(90).time(900).rfCost(100_000_000)
          .modifierDefinition(Attributes.BLOCK_INTERACTION_RANGE, 0.3f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
          .export(exporter, "blockreach");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.ENDERIC_LENS, 64)
          .researchCost(Items.ENDER_PEARL, 16)
          .applyCost(ItemContent.ENDERIC_LENS, 4)
          .requirement(Oritech.id("augment/blockreach"))
          .requiredStation(ADVANCED_AUGMENT_STATION_ID)
          .uiX(140).uiY(90).time(800).rfCost(200_000_000)
          .modifierDefinition(Attributes.BLOCK_INTERACTION_RANGE, 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
          .toggleable()
          .export(exporter, "farblockreach");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.MAGNETIC_COIL, 48)
          .researchCost(Items.QUARTZ, 64)
          .researchCost(ItemContent.BASIC_BATTERY, 32)
          .applyCost(ItemContent.MAGNETIC_COIL, 4)
          .requirement(Oritech.id("augment/attackdamage"))
          .requirement(Oritech.id("augment/speedboost"))
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(30).uiY(10).time(1200).rfCost(50_000_000)
          .modifierDefinition(Attributes.BLOCK_BREAK_SPEED, 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
          .export(exporter, "miningspeed");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.ENERGITE_INGOT, 64)
          .researchCost(ItemContent.FLUX_GATE, 48)
          .researchCost(ItemContent.DURATIUM_INGOT, 8)
          .applyCost(ItemContent.ENERGITE_INGOT, 4)
          .requirement(Oritech.id("augment/miningspeed"))
          .requirement(Oritech.id("augment/superspeedboost"))
          .requiredStation(ADVANCED_AUGMENT_STATION_ID)
          .uiX(80).uiY(10).time(2400).rfCost(250_000_000)
          .modifierDefinition(Attributes.BLOCK_BREAK_SPEED, 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
          .toggleable()
          .export(exporter, "superminingspeed");
        
        AugmentRecipeBuilder.build()
          .researchCost(TagContent.STEEL_INGOTS, 32)
          .researchCost(Items.DIAMOND, 8)
          .researchCost(ItemContent.FLUXITE, 64)
          .applyCost(TagContent.STEEL_INGOTS, 4)
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(5).uiY(10).time(1600).rfCost(150_000_000)
          .modifierDefinition(Attributes.ATTACK_DAMAGE, 4, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "attackdamage");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.ENDERIC_COMPOUND, 64)
          .researchCost(ItemContent.FLUXITE, 64)
          .applyCost(ItemContent.ENDERIC_COMPOUND, 4)
          .requirement(Oritech.id("augment/hpboostultra"))
          .requirement(Oritech.id("augment/ultimatearmor"))
          .requiredStation(ARCANE_AUGMENT_STATION_ID)
          .uiX(180).uiY(50).time(2800).rfCost(500_000_000)
          .modifierDefinition(Attributes.ATTACK_DAMAGE, 6, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "superattackdamage");
        
        AugmentRecipeBuilder.build()
          .researchCost(TagContent.ELECTRUM_INGOTS, 64)
          .researchCost(cItemTag("storage_blocks/lapis"), 32)
          .researchCost(cItemTag("storage_blocks/gold"), 24)
          .applyCost(Items.LAPIS_LAZULI, 16)
          .requiredStation(ARCANE_AUGMENT_STATION_ID)
          .uiX(55).uiY(30).time(1800).rfCost(200_000_000)
          .modifierDefinition(Attributes.LUCK, 5, AttributeModifier.Operation.ADD_VALUE)
          .export(exporter, "luck");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.MAGNETIC_COIL, 64)
          .researchCost(ItemContent.FLUXITE, 48)
          .researchCost(Items.PHANTOM_MEMBRANE, 8)
          .applyCost(ItemContent.MAGNETIC_COIL, 4)
          .requirement(Oritech.id("augment/flight"))
          .requiredStation(ARCANE_AUGMENT_STATION_ID)
          .uiX(180).uiY(10).time(2200).rfCost(300_000_000)
          .modifierDefinition(Attributes.GRAVITY, -0.5f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
          .toggleable()
          .export(exporter, "gravity");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.ION_THRUSTER, 64)
          .researchCost(Items.WIND_CHARGE, 16)
          .researchCost(ItemContent.PROMETHEUM_INGOT, 16)
          .researchCost(ItemContent.PLUTONIUM_PELLET, 32)
          .applyCost(ItemContent.ION_THRUSTER, 4)
          .requirement(Oritech.id("augment/betterarmor"))
          .requirement(Oritech.id("augment/portal"))
          .requiredStation(ARCANE_AUGMENT_STATION_ID)
          .uiX(155).uiY(30).time(3600).rfCost(500_000_000)
          .customAugmentDefinition(CustomAugmentsCollection.flight.id)
          .toggleable()
          .export(exporter, "flight");
        
        AugmentRecipeBuilder.build()
          .researchCost(Items.ENDER_EYE, 8)
          .researchCost(ItemContent.ENDERIC_LENS, 16)
          .researchCost(Items.DIAMOND, 8)
          .applyCost(ItemContent.ENDERIC_LENS, 4)
          .requirement(Oritech.id("augment/orefinder"))
          .requiredStation(ARCANE_AUGMENT_STATION_ID)
          .uiX(155).uiY(10).time(3200).rfCost(100_000_000)
          .effectDefinition(MobEffects.INVISIBILITY, 0)
          .toggleable()
          .export(exporter, "cloak");
        
        AugmentRecipeBuilder.build()
          .researchCost(cItemTag("ender_pearls"), 16)
          .researchCost(Items.OBSIDIAN, 64)
          .researchCost(ItemContent.UNHOLY_INTELLIGENCE)
          .researchCost(ItemContent.ADAMANT_INGOT, 32)
          .applyCost(cItemTag("ender_pearls"), 4)
          .requiredStation(ARCANE_AUGMENT_STATION_ID)
          .uiX(130).uiY(30).time(3000).rfCost(250_000_000)
          .customAugmentDefinition(CustomAugmentsCollection.portal.id)
          .toggleable()
          .export(exporter, "portal");
        
        AugmentRecipeBuilder.build()
          .researchCost(Items.GOLD_INGOT, 64)
          .researchCost(ItemContent.ENDERIC_LENS, 48)
          .researchCost(Items.GLOWSTONE_DUST, 64)
          .applyCost(Items.GLOWSTONE_DUST, 8)
          .requiredStation(ADVANCED_AUGMENT_STATION_ID)
          .uiX(105).uiY(30).time(2400).rfCost(50_000_000)
          .effectDefinition(MobEffects.NIGHT_VISION, 0)
          .toggleable()
          .export(exporter, "nightvision");
        
        AugmentRecipeBuilder.build()
          .researchCost(Items.PRISMARINE_CRYSTALS, 8)
          .researchCost(ItemContent.BIOSTEEL_INGOT, 32)
          .researchCost(Items.HEART_OF_THE_SEA)
          .applyCost(ItemContent.BIOSTEEL_INGOT, 4)
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(5).uiY(90).time(800).rfCost(50_000_000)
          .effectDefinition(MobEffects.WATER_BREATHING, 0)
          .export(exporter, "waterbreath");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.PROCESSING_UNIT, 16)
          .researchCost(TagContent.BIOMATTER, 64)
          .researchCost(Items.GOLDEN_CARROT, 64)
          .applyCost(TagContent.BIOMATTER, 4)
          .requirement(Oritech.id("augment/armor"))
          .requirement(Oritech.id("augment/hpboostmore"))
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(90).uiY(90).time(500).rfCost(30_000_000)
          .customAugmentDefinition(CustomAugmentsCollection.feeder.id)
          .toggleable()
          .export(exporter, "autofeeder");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.MAGNETIC_COIL, 32)
          .researchCost(ItemContent.ENERGITE_INGOT, 32)
          .researchCost(Items.LODESTONE, 2)
          .applyCost(ItemContent.MAGNETIC_COIL, 4)
          .requirement(Oritech.id("augment/superminingspeed"))
          .requiredStation(SIMPLE_AUGMENT_STATION_ID)
          .uiX(105).uiY(10).time(2400).rfCost(300_000_000)
          .customAugmentDefinition(CustomAugmentsCollection.magnet.id)
          .toggleable()
          .export(exporter, "magnet");
        
        AugmentRecipeBuilder.build()
          .researchCost(ItemContent.ENDERIC_LENS, 32)
          .researchCost(Items.SPYGLASS, 1)
          .researchCost(ItemContent.PROMETHEUM_INGOT, 1)
          .researchCost(Items.SCULK_SENSOR, 1)
          .applyCost(ItemContent.ENDERIC_LENS, 4)
          .requirement(Oritech.id("augment/nightvision"))
          .requirement(Oritech.id("augment/magnet"))
          .requiredStation(ARCANE_AUGMENT_STATION_ID)
          .uiX(130).uiY(10).time(3200).rfCost(200_000_000)
          .customAugmentDefinition(CustomAugmentsCollection.oreFinder.id)
          .toggleable()
          .export(exporter, "orefinder");
    }
    
    private void addReactorBlocks(RecipeOutput exporter) {
        
        // single rod
        offerRodRecipe(exporter, BlockContent.REACTOR_ROD.asItem(), of(TagContent.PLASTIC_PLATES), of(ItemContent.ENERGITE_INGOT), "singlerod");
        // dual rod
        offerRodCombinationRecipe(exporter, BlockContent.REACTOR_DOUBLE_ROD.asItem(), of(BlockContent.REACTOR_REFLECTOR), of(BlockContent.REACTOR_ROD), "doublerod");
        // quad rod
        offerRodCombinationRecipe(exporter, BlockContent.REACTOR_QUAD_ROD.asItem(), of(BlockContent.REACTOR_REFLECTOR), of(BlockContent.REACTOR_DOUBLE_ROD), "quadrod");
        
        // reactor plating: steel and machine plating in crafting table / assembler
        offerMachinePlatingRecipe(exporter, BlockContent.REACTOR_WALL.asItem(), of(TagContent.MACHINE_PLATING), of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), 4, "reactorplatingcrafting");
        AssemblerRecipeBuilder.build().input(TagContent.MACHINE_PLATING).input(TagContent.MACHINE_PLATING).input(TagContent.STEEL_INGOTS).input(TagContent.NICKEL_INGOTS).result(BlockContent.REACTOR_WALL.asItem(), 3).timeMultiplier(0.8f).export(exporter, "reactorplatingalt");
        
        // neutron reflectors: expensive, needs duratium core, adamant frame and reactor walls
        offerMachinePlatingRecipe(exporter, BlockContent.REACTOR_REFLECTOR.asItem(), of(BlockContent.REACTOR_WALL), of(ItemContent.ADAMANT_INGOT), of(ItemContent.DURATIUM_INGOT), 1, "reflector");
        
        // reactor controller: reactor wall, processing unit
        offerRodCombinationRecipe(exporter, BlockContent.REACTOR_CONTROLLER.asItem(), of(BlockContent.REACTOR_WALL), of(ItemContent.PROCESSING_UNIT), "controller");
        
        // reactor energy port: reactor wall, storage unit, electrum
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_ENERGY_PORT.asItem(), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.ENERGY_PIPE), of(BlockContent.REACTOR_WALL), of(cItemTag("ingots/iron")), "energyport");
        
        // reactor redstone port: wall, processing unit, repeater, torch
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_REDSTONE_PORT.asItem(), of(ItemContent.PROCESSING_UNIT), of(Items.REPEATER), of(BlockContent.REACTOR_WALL), of(Items.REDSTONE_TORCH), "redstoneport");
        
        // reactor fuel port: wall, hopper, motor, item pipe
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_FUEL_PORT.asItem(), of(BlockContent.ITEM_PIPE), of(Items.HOPPER), of(BlockContent.REACTOR_WALL), of(cItemTag("chests")), "fuelport");
        
        // reactor absorber port: wall, ice, motor, item pipe
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_ABSORBER_PORT.asItem(), of(BlockContent.ITEM_PIPE), of(Items.HOPPER), of(BlockContent.REACTOR_WALL), of(Blocks.ICE), "absorberport");
        
        // reactor absorber : wall, steel, ice
        offerBatteryRecipe(exporter, BlockContent.REACTOR_CONDENSER.asItem(), of(Items.ICE), of(cItemTag("glass_blocks")), of(TagContent.STEEL_INGOTS), "condenser");
        
        // reactor vent: motor, carbon fibre
        offerStarRecipe(exporter, BlockContent.REACTOR_VENT.asItem(), of(ItemContent.MOTOR), of(TagContent.CARBON_FIBRE), "reactorvent");
        
        // reactor heat pipe: electrum, gold
        offerStarRecipe(exporter, BlockContent.REACTOR_HEAT_PIPE.asItem(), of(TagContent.ELECTRUM_INGOTS), of(cItemTag("ingots/gold")), "reactorheatpipe");
        
        // explosives
        offerMachinePlatingRecipe(exporter, BlockContent.LOW_YIELD_NUKE.asItem(), of(ItemContent.DUBIOS_CONTAINER), of(ItemContent.URANIUM_PELLET), of(Items.TNT), 1, "nuke");
        offerMachinePlatingRecipe(exporter, BlockContent.NUKE.asItem(), of(ItemContent.HEISENBERG_COMPENSATOR), of(ItemContent.PLUTONIUM_PELLET), of(Items.TNT), 1, "nukebetter");
    }
    
    private void addReactorFuels(RecipeOutput exporter) {
        ReactorGeneratorRecipeBuilder.build().input(ItemContent.SMALL_URANIUM_PELLET).time(400).export(exporter, "smallpellet");
        ReactorGeneratorRecipeBuilder.build().input(ItemContent.URANIUM_PELLET).time(4000).export(exporter, "pellet");
        ReactorGeneratorRecipeBuilder.build().input(ItemContent.SMALL_PLUTONIUM_PELLET).time(4000).export(exporter, "smallplutoniumpellet");
        ReactorGeneratorRecipeBuilder.build().input(ItemContent.PLUTONIUM_PELLET).time(40000).export(exporter, "plutoniumpellet");
    }
    
    private void addLaserTransformations(RecipeOutput exporter) {
        LaserRecipeBuilder.build().input(Items.AMETHYST_CLUSTER).result(ItemContent.FLUXITE).export(exporter, "fluxite");
        LaserRecipeBuilder.build().input(BlockContent.URANIUM_CRYSTAL).result(ItemContent.PLUTONIUM_DUST).export(exporter, "plutoniumdust");
    }
    
    private void addCompactingRecipe(RecipeOutput exporter, ItemLike resBlock, ItemLike resItem, Ingredient itemIng, Ingredient blockIng) {
        ShapelessRecipeBuilder
          .shapeless(RecipeCategory.MISC, resItem, 9)
          .requires(blockIng)
          .unlockedBy(getHasName(resBlock), has(resBlock))
          .save(exporter, Oritech.id(RecipeProvider.getSimpleRecipeName(resBlock) + "blockinv"));
        ShapedRecipeBuilder
          .shaped(RecipeCategory.MISC, resBlock)
          .define('#', itemIng)
          .pattern("###")
          .pattern("###")
          .pattern("###")
          .unlockedBy(getHasName(resItem), has(resItem))
          .save(exporter, Oritech.id(RecipeProvider.getSimpleRecipeName(resBlock) + "block"));
    }
    
    // crafting shapes
    public void offerCableRecipe(RecipeOutput exporter, ItemStack output, Ingredient input, String suffix) {
        var item = output.getItem();
        createCableRecipe(RecipeCategory.MISC, output.getItem(), output.getCount(), input).unlockedBy(getHasName(item), has(item)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerInsulatedCableRecipe(RecipeOutput exporter, ItemStack output, Ingredient input, Ingredient insulation, String suffix) {
        var item = output.getItem();
        createInsulatedCableRecipe(RecipeCategory.MISC, output.getItem(), output.getCount(), input, insulation).unlockedBy(getHasName(item), has(item)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerRotatedCableRecipe(RecipeOutput exporter, ItemStack output, Ingredient input, Ingredient insulation, String suffix) {
        var item = output.getItem();
        createRotatedCableRecipe(RecipeCategory.MISC, output.getItem(), output.getCount(), input, insulation).unlockedBy(getHasName(item), has(item)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerFramedCableRecipe(RecipeOutput exporter, ItemStack output, Ingredient input, String suffix) {
        var item = output.getItem();
        createFramedCableRecipe(RecipeCategory.MISC, output.getItem(), output.getCount(), input).unlockedBy(getHasName(item), has(item)).save(exporter, Oritech.id("crafting/frame_" + suffix));
    }
    
    public void offerCableFromFrameRecipe(RecipeOutput exporter, ItemStack output, Ingredient frame, String suffix) {
        var item = output.getItem();
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item, output.getCount()).requires(frame).unlockedBy(getHasName(item), has(item)).save(exporter, Oritech.id("crafting/unframe_" + suffix));
    }
    
    public void offerCableDuctRecipe(RecipeOutput exporter, ItemStack output, Ingredient input, String suffix) {
        var item = output.getItem();
        createCableDuctRecipe(RecipeCategory.MISC, item, output.getCount(), input).unlockedBy(getHasName(item), has(item)).save(exporter, Oritech.id("crafting/duct_" + suffix));
    }
    
    public void offerCableFromDuctRecipe(RecipeOutput exporter, ItemStack output, Ingredient duct, String suffix) {
        var item = output.getItem();
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item, output.getCount()).requires(duct).unlockedBy(getHasName(item), has(item)).save(exporter, Oritech.id("crafting/unduct_" + suffix));
    }
    
    public RecipeBuilder createCableRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return ShapedRecipeBuilder.shaped(category, output, count).define('#', input).pattern("   ").pattern("###");
    }
    
    public RecipeBuilder createFramedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return ShapedRecipeBuilder.shaped(category, output, count).define('c', input).define('p', Ingredient.of(TagContent.MACHINE_PLATING)).pattern("ccc").pattern("cpc").pattern("ccc");
    }
    
    public RecipeBuilder createCableDuctRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return ShapedRecipeBuilder.shaped(category, output, count).define('c', input).define('p', Ingredient.of(TagContent.MACHINE_PLATING)).define('s', of(Blocks.STONE)).pattern("csc").pattern("sps").pattern("csc");
    }
    
    public void offerMotorRecipe(RecipeOutput exporter, Item output, Ingredient shaft, Ingredient core, Ingredient wall, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('s', shaft).define('c', core).define('w', wall).pattern(" s ").pattern("wcw").pattern("wcw");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("motor/" + suffix));
    }
    
    public void offerManualAlloyRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        offerManualAlloyRecipe(exporter, output, A, B, 1, suffix);
    }
    
    public void offerManualAlloyRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, int count, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, count).define('a', A).define('b', B).pattern("aa ").pattern("bb ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/alloy/" + suffix));
    }
    
    public void offerGeneratorRecipe(RecipeOutput exporter, Item output, Ingredient base, Ingredient sides, Ingredient core, Ingredient frame, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('s', sides).define('c', core).define('f', frame).define('b', base)
                        .pattern("fff")
                        .pattern("fcf")
                        .pattern("sbs");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerFurnaceRecipe(RecipeOutput exporter, Item output, Ingredient bottom, Ingredient botSides, Ingredient middleSides, Ingredient core, Ingredient top, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('s', botSides).define('c', core).define('f', top).define('b', bottom).define('m', middleSides)
                        .pattern("fff")
                        .pattern("mcm")
                        .pattern("sbs");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerAtomicForgeRecipe(RecipeOutput exporter, Item output, Ingredient base, Ingredient middleSides, Ingredient core, Ingredient top, Ingredient frame, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('s', top).define('c', core).define('f', frame).define('b', base).define('m', middleSides)
                        .pattern("fsf")
                        .pattern("mcm")
                        .pattern("bbb");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerBatteryRecipe(RecipeOutput exporter, Item output, Ingredient inner, Ingredient sides, Ingredient top, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('t', top).define('c', inner).define('f', sides)
                        .pattern(" t ")
                        .pattern("fcf")
                        .pattern("fcf");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerMachineFrameRecipe(RecipeOutput exporter, Item output, Ingredient base, Ingredient alt, int count, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, count).define('s', base).define('c', alt)
                        .pattern(" s ")
                        .pattern("csc")
                        .pattern(" s ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerMachineCoreRecipe(RecipeOutput exporter, Item output, Ingredient base, Ingredient alt, String suffix) {
        offerMachineCoreRecipe(exporter, output, 1, base, alt, suffix);
    }
    
    public void offerMachineCoreRecipe(RecipeOutput exporter, Item output, int count, Ingredient base, Ingredient alt, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, count).define('s', base).define('c', alt)
                        .pattern("sss")
                        .pattern("scs")
                        .pattern("sss");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerManualFluidApplication(RecipeOutput exporter, Item output, Ingredient fluid, Ingredient base, String suffix) {
        offerManualFluidApplication(exporter, output, 1, fluid, base, suffix);
    }
    
    public void offerManualFluidApplication(RecipeOutput exporter, Item output, int count, Ingredient fluid, Ingredient base, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, count).define('f', fluid).define('b', base)
                        .pattern("bb ")
                        .pattern("bf ")
                        .pattern("   ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerBeadsRecipe(RecipeOutput exporter, Item output, int count, Ingredient fluid, Ingredient base, Ingredient catalyst, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, count).define('f', fluid).define('b', base).define('c', catalyst)
                        .pattern("bb ")
                        .pattern("cf ")
                        .pattern("   ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerDrillRecipe(RecipeOutput exporter, Item output, Ingredient doubleBase, Ingredient motor, Ingredient outer, Ingredient head, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('s', doubleBase).define('m', motor).define('a', outer).define('e', head)
                        .pattern(" a ")
                        .pattern("aea")
                        .pattern("mss");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerWrenchRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('a', A).define('b', B)
                        .pattern(" a ")
                        .pattern(" ba")
                        .pattern("a  ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerChainsawRecipe(RecipeOutput exporter, Item output, Ingredient core, Ingredient motor, Ingredient center, Ingredient head, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('s', core).define('m', motor).define('a', center).define('e', head)
                        .pattern("aa ")
                        .pattern("ae ")
                        .pattern("mss");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerAxeRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                        .pattern("pp ")
                        .pattern("pc ")
                        .pattern(" c ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerPickaxeRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                        .pattern("ppp")
                        .pattern(" c ")
                        .pattern(" c ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerHelmetRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                        .pattern("ppp")
                        .pattern("pcp")
                        .pattern("   ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerChestplateRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                        .pattern("p p")
                        .pattern("ppp")
                        .pattern("pcp");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerLegsRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                        .pattern("ppp")
                        .pattern("pcp")
                        .pattern("p p");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerFeetRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                        .pattern("   ")
                        .pattern("p p")
                        .pattern("c c");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerRodRecipe(RecipeOutput exporter, Item output, Ingredient cap, Ingredient rod, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('c', cap).define('r', rod)
                        .pattern(" c ")
                        .pattern(" r ")
                        .pattern(" r ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerRodCombinationRecipe(RecipeOutput exporter, Item output, Ingredient cap, Ingredient rod, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('c', cap).define('r', rod)
                        .pattern("   ")
                        .pattern("rcr")
                        .pattern("   ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerStarRecipe(RecipeOutput exporter, Item output, Ingredient inner, Ingredient outer, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('c', inner).define('o', outer)
                        .pattern(" o ")
                        .pattern("oco")
                        .pattern(" o ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerTankRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, Ingredient sides, String suffix) {
        offerTankRecipe(exporter, output, 1, plating, core, sides, suffix);
    }
    
    public void offerTankRecipe(RecipeOutput exporter, Item output, int count, Ingredient plating, Ingredient core, Ingredient sides, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, count).define('p', plating).define('s', sides).define('c', core)
                        .pattern("ppp")
                        .pattern("scs")
                        .pattern("ppp");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerTwoComponentRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('a', A).define('b', B)
                        .pattern("ab ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerLeverRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('a', A).define('b', B)
                        .pattern("a  ")
                        .pattern("b  ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerParticleMotorRecipe(RecipeOutput exporter, Item output, Ingredient rail, Ingredient top, Ingredient baseInner, Ingredient baseOuter, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, 1).define('r', rail).define('t', top).define('i', baseInner).define('o', baseOuter)
                        .pattern(" t ")
                        .pattern("rrr")
                        .pattern("oio");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerMachinePlatingRecipe(RecipeOutput exporter, Item output, Ingredient side, Ingredient edge, Ingredient core, int count, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, output, count).define('a', side).define('e', edge).define('c', core)
                        .pattern("eae")
                        .pattern("aca")
                        .pattern("eae");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerDoorRecipe(RecipeOutput exporter, Item output, Ingredient A, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, output, 1).define('a', A)
                        .pattern("aa ")
                        .pattern("aa ")
                        .pattern("aa ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/" + suffix));
    }
    
    public void offerSlabRecipe(RecipeOutput exporter, Item output, Ingredient A, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, output, 6).define('a', A)
                        .pattern("aaa");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/slab/" + suffix));
    }
    
    public void offerStairsRecipe(RecipeOutput exporter, Item output, Ingredient A, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, output, 4).define('a', A)
                        .pattern("a  ")
                        .pattern("aa ")
                        .pattern("aaa");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/stairs/" + suffix));
    }
    
    public void offerPressurePlateRecipe(RecipeOutput exporter, Item output, Ingredient A, String suffix) {
        var builder = ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, output, 1).define('a', A)
                        .pattern("aa");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, Oritech.id("crafting/pressureplate/" + suffix));
    }
}
