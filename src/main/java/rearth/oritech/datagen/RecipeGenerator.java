package rearth.oritech.datagen;

import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import rearth.oritech.Oritech;
import rearth.oritech.datagen.builders.*;
import rearth.oritech.datagen.builders.util.MetalProcessingChainBuilder;
import rearth.oritech.init.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static rearth.oritech.datagen.builders.util.RecipeHelpers.*;
import static rearth.oritech.util.TagUtils.*;

public class RecipeGenerator extends RecipeProvider {

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new RecipeGenerator(registries, output);
        }

        @Override
        public String getName() {
            return "Oritech Recipes";
        }
    }

    public RecipeGenerator(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    private static ResourceKey<Recipe<?>> recipeKey(String path) {
        return ResourceKey.create(Registries.RECIPE, Oritech.id(path));
    }

    @Override
    protected void buildRecipes() {
        RecipeOutput exporter = this.output;

        addBedrockExtractorOres(exporter);
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
        addPaintRecipes(exporter);
    }

    private void addVanillaAdditions(RecipeOutput exporter) {

        // slimeball from honey and biomass
        new AssemblerRecipeBuilder(this.registries).input(Items.HONEYCOMB).input(TagContent.BIOMASS).input(TagContent.BIOMASS).input(TagContent.BIOMASS).result(Items.SLIME_BALL).timeMultiplier(0.8f).export(exporter, "slime");
        // fireball in assembler (gunpowder, blaze powder + coal) = 5 charges
        new AssemblerRecipeBuilder(this.registries).input(Items.GUNPOWDER).input(Items.BLAZE_POWDER).input(ItemTags.COALS).input(ItemTags.COALS).result(Items.FIRE_CHARGE, 4).timeMultiplier(0.8f).export(exporter, "fireball");
        // blaze rod (4 powder in assembler)
        new AssemblerRecipeBuilder(this.registries).input(Items.BLAZE_POWDER).input(Items.BLAZE_POWDER).input(Items.BLAZE_POWDER).input(Items.BLAZE_POWDER).result(Items.BLAZE_ROD).timeMultiplier(0.8f).export(exporter, "blazerod");
        // enderic compound from sculk
        new CentrifugeRecipeBuilder(this.registries).input(Items.SCULK).result(ItemContent.ENDERIC_COMPOUND.get()).timeMultiplier(4f).export(exporter, "endericsculk");
        // budding amethyst (amethyst shard x2, enderic compound, overcharged crystal)
        new AssemblerRecipeBuilder(this.registries).input(cItemTag("gems/amethyst")).input(cItemTag("gems/amethyst")).input(ItemContent.ENDERIC_COMPOUND).input(ItemContent.OVERCHARGED_CRYSTAL).result(Items.BUDDING_AMETHYST).time(160).export(exporter, "amethystbud");
        // netherite alloying (yes this is pretty OP)
        new FoundryRecipeBuilder(this.registries).input(cItemTag("ingots/gold")).input(Items.NETHERITE_SCRAP).result(Items.NETHERITE_INGOT).export(exporter, "netherite");
        // books
        new AssemblerRecipeBuilder(this.registries).input(Items.PAPER).input(Items.PAPER).input(Items.PAPER).input(cItemTag("leathers")).result(Items.BOOK, 2).timeMultiplier(0.8f).export(exporter, "book");
        // reinforced deepslate
        new AtomicForgeRecipeBuilder(this.registries).input(Items.DEEPSLATE).input(ItemContent.DURATIUM_INGOT).input(ItemContent.DURATIUM_INGOT).result(Items.REINFORCED_DEEPSLATE).time(100).export(exporter, "reinfdeepslate");
        // cobblestone to gravel
        new PulverizerRecipeBuilder(this.registries).input(cItemTag("cobblestones")).result(Items.GRAVEL).addToGrinder().export(exporter, "gravel");
        // gravel to sand
        new PulverizerRecipeBuilder(this.registries).input(Items.GRAVEL).result(Items.SAND).addToGrinder().export(exporter, "sand");
        // sandstone to sand
        new PulverizerRecipeBuilder(this.registries).input(cItemTag("sandstone/blocks")).result(Items.SAND).addToGrinder().export(exporter, "sand_from_sandstone");
        // red sandstone to red sand
        new PulverizerRecipeBuilder(this.registries).input(cItemTag("sandstone/red_blocks")).result(Items.RED_SAND).addToGrinder().export(exporter, "red_sand");
        // centrifuge dirt into clay
        new CentrifugeFluidRecipeBuilder(this.registries).input(ItemTags.DIRT).result(Items.CLAY).fluidInput(Fluids.WATER, 0.25f).export(exporter, "clay");
        // create dirt from sand + biomass
        this.shaped(RecipeCategory.MISC, Items.DIRT, 2).define('s', ItemTags.SAND).define('b', TagContent.BIOMASS).pattern("sb").pattern("bs").unlockedBy("has_biomass", has(TagContent.BIOMASS)).save(exporter, recipeKey("dirt_from_sand_and_biomass"));
        // dripstone from dripstone block
        new PulverizerRecipeBuilder(this.registries).input(Items.DRIPSTONE_BLOCK).result(Items.POINTED_DRIPSTONE, 4).addToGrinder().export(exporter, "dripstone");
        // shroomlight from logs and 3 glowstone
        new AssemblerRecipeBuilder(this.registries).input(ItemTags.LOGS).input(Items.GLOWSTONE).input(Items.GLOWSTONE).input(Items.GLOWSTONE).result(Items.SHROOMLIGHT).timeMultiplier(0.8f).export(exporter, "shroomlight");
        // prismarine shards to crystals
        new PulverizerRecipeBuilder(this.registries).input(Items.PRISMARINE_SHARD).result(Items.PRISMARINE_CRYSTALS, 2).addToGrinder().export(exporter, "prismarine");

        // recyclables
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_NETHERITE_SCRAP).result(Items.NETHERITE_SCRAP).addToGrinder().export(exporter, "recycle/netherite_scrap");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_DIAMOND).result(Items.DIAMOND).addToGrinder().export(exporter, "recycle/diamond");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_IRON_DUST).result(ItemContent.IRON_DUST).addToGrinder().export(exporter, "recycle/iron_dust");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_SMALL_IRON_DUST).result(ItemContent.SMALL_IRON_DUST).export(exporter, "recycle/small_iron_dust");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_GOLD_DUST).result(ItemContent.GOLD_DUST).export(exporter, "recycle/gold_dust");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_SMALL_GOLD_DUST).result(ItemContent.SMALL_GOLD_DUST).export(exporter, "recycle/small_gold_dust");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_COPPER_DUST).result(ItemContent.COPPER_DUST).export(exporter, "recycle/copper_dust");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_SMALL_COPPER_DUST).result(ItemContent.SMALL_COPPER_DUST).export(exporter, "recycle/small_copper_dust");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_2_QUARTZ_DUST).result(ItemContent.QUARTZ_DUST, 2).export(exporter, "recycle/2_quartz_dust");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_4_QUARTZ_DUST).result(ItemContent.QUARTZ_DUST, 4).export(exporter, "recycle/4_quartz_dust");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_REDSTONE_DUST).result(Items.REDSTONE).export(exporter, "recycle/redstone_dust");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_GRAVEL).result(Items.GRAVEL).export(exporter, "recycle/gravel");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_SAND).result(Items.SAND).export(exporter, "recycle/sand");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_RED_SAND).result(Items.RED_SAND).export(exporter, "recycle/red_sand");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_STRING).result(Items.STRING, 2).export(exporter, "recycle/string");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RECYCLES_TO_BIOMASS).result(ItemContent.BIOMASS).export(exporter, "recycle/biomass");
    }

    private void addDyes(RecipeOutput exporter) {
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_WHITE_DYE).result(Items.WHITE_DYE).addToGrinder().export(exporter, "dyes/white");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_LIGHT_GRAY_DYE).result(Items.LIGHT_GRAY_DYE).addToGrinder().export(exporter, "dyes/light_gray");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_BLACK_DYE).result(Items.BLACK_DYE).addToGrinder().export(exporter, "dyes/black");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_RED_DYE).result(Items.RED_DYE).addToGrinder().export(exporter, "dyes/red");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_ORANGE_DYE).result(Items.ORANGE_DYE).addToGrinder().export(exporter, "dyes/orange");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_YELLOW_DYE).result(Items.YELLOW_DYE).addToGrinder().export(exporter, "dyes/yellow");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_CYAN_DYE).result(Items.CYAN_DYE).addToGrinder().export(exporter, "dyes/cyan");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_BLUE_DYE).result(Items.BLUE_DYE).addToGrinder().export(exporter, "dyes/blue");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_MAGENTA_DYE).result(Items.MAGENTA_DYE).addToGrinder().export(exporter, "dyes/magenta");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.RAW_PINK_DYE).result(Items.PINK_DYE).addToGrinder().export(exporter, "dyes/pink");
    }

    private void addBedrockExtractorOres(RecipeOutput exporter) {
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.REDSTONE_RESOURCE_NODE).result(Items.REDSTONE).export(exporter, "redstone");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.RESOURCE_NODE_LAPIS).result(Items.LAPIS_LAZULI).export(exporter, "lapis");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.IRON_RESOURCE_NODE).result(Items.RAW_IRON).export(exporter, "iron");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.COAL_RESOURCE_NODE).result(Items.COAL).export(exporter, "coal");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.COPPER_RESOURCE_NODE).result(Items.RAW_COPPER).export(exporter, "copper");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.GOLD_RESOURCE_NODE).result(Items.RAW_GOLD).export(exporter, "gold");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.EMERALD_RESOURCE_NODE).result(Items.EMERALD).export(exporter, "emerald");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.DIAMOND_RESOURCE_NODE).result(Items.DIAMOND).export(exporter, "diamond");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.NICKEL_RESOURCE_NODE).result(ItemContent.RAW_NICKEL).export(exporter, "nickel");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.PLATINUM_RESOURCE_NODE).result(ItemContent.RAW_PLATINUM).export(exporter, "platinum");
        new BedrockExtractorRecipeBuilder(this.registries).input(BlockContent.URANIUM_RESOURCE_NODE).result(ItemContent.RAW_URANIUM).export(exporter, "uranium");
    }

    private void addFuels(RecipeOutput exporter) {

        // bio
        new BioGeneratorRecipeBuilder(this.registries).input(TagContent.BIOMATTER).timeInSeconds(15).export(exporter, "rawbio");
        new BioGeneratorRecipeBuilder(this.registries).input(ItemContent.PACKED_WHEAT).timeInSeconds(200).export(exporter, "packedwheat");
        new BioGeneratorRecipeBuilder(this.registries).input(TagContent.BIOMASS).timeInSeconds(25).export(exporter, "biomass");
        new BioGeneratorRecipeBuilder(this.registries).input(ItemContent.SOLID_BIOFUEL).timeInSeconds(160).export(exporter, "solidbiomass");
        new BioGeneratorRecipeBuilder(this.registries).input(TagContent.BIOMASS_BLOCK).timeInSeconds(270).export(exporter, "biomassblock");
        new BioGeneratorRecipeBuilder(this.registries).input(ItemContent.RAW_BIOPOLYMER).timeInSeconds(300).export(exporter, "polymer");
        new BioGeneratorRecipeBuilder(this.registries).input(ItemContent.UNHOLY_INTELLIGENCE).timeInSeconds(3000).export(exporter, "vex");
        // lava
        new LavaGeneratorRecipeBuilder(this.registries).fluidInput(Fluids.LAVA, 0.1f).timeInSeconds(6).export(exporter, "lava");
        new LavaGeneratorRecipeBuilder(this.registries).fluidInput(FluidContent.STILL_SHEOL_FIRE.get(), 0.1f).timeInSeconds(40).export(exporter, "sheolfire");
        // fuel
        new FuelGeneratorRecipeBuilder(this.registries).fluidInput(cFluidTag("oil"), 0.1f).timeInSeconds(1).export(exporter, "crude");
        new FuelGeneratorRecipeBuilder(this.registries).fluidInput(FluidContent.STILL_HEAVY_OIL.get(), 0.1f).timeInSeconds(2).export(exporter, "heavyoil");
        new FuelGeneratorRecipeBuilder(this.registries).fluidInput(TagContent.DIESEL, 0.1f).timeInSeconds(4).export(exporter, "diesel");
        new FuelGeneratorRecipeBuilder(this.registries).fluidInput(TagContent.NAPHTHA, 0.1f).timeInSeconds(2).export(exporter, "naptha");
        new FuelGeneratorRecipeBuilder(this.registries).fluidInput(TagContent.TURBOFUEL, 0.1f).timeInSeconds(16).export(exporter, "fuel");

        //steam
        // 32 fabric droplets / 32 neoforge mb (yes this will works, as we produce 2 millis per RF in the generator boilers, and then consume it at a 1:1 ratio)
        new SteamGeneratorRecipeBuilder(this.registries).specificFluidInput(TagContent.STEAM, 32).time(1).export(exporter, "steameng");
    }

    private void addFluidProcessing(RecipeOutput exporter) {

        // crude oil processing
        new RefineryRecipeBuilder(this.registries)
                .fluidInput(cFluidTag("oil"))
                .fluidOutput(FluidContent.STILL_HEAVY_OIL.get(), 0.5f)
                .fluidOutput(FluidContent.STILL_NAPHTHA.get(), 0.25f)
                .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 0.25f)
                .export(exporter, "oilbase");

        new RefineryRecipeBuilder(this.registries)
                .input(ItemContent.CLAY_CATALYST_BEADS)
                .fluidInput(cFluidTag("oil"))
                .fluidOutput(FluidContent.STILL_DIESEL.get(), 0.5f)
                .fluidOutput(FluidContent.STILL_NAPHTHA.get(), 0.5f)
                .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 0.5f)
                .timeMultiplier(1.6f)
                .export(exporter, "oilalt");

        // heavy oil
        new RefineryRecipeBuilder(this.registries)
                .input(ItemTags.SAND)
                .fluidInput(FluidContent.STILL_HEAVY_OIL.get())
                .fluidOutput(FluidContent.STILL_DIESEL.get(), 1f)
                .fluidOutput(FluidContent.STILL_NAPHTHA.get(), 0.25f)
                .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 0.25f)
                .export(exporter, "heavyoil");

        // lava
        new RefineryRecipeBuilder(this.registries)
                .fluidInput(Fluids.LAVA)
                .fluidOutput(FluidContent.STILL_STEAM.get(), 4f)
                .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 0.1f)
                .fluidOutput(FluidContent.STILL_SHEOL_FIRE.get(), 0.2f)
                .export(exporter, "lava");

        new RefineryRecipeBuilder(this.registries)
                .input(ItemContent.ENDERIC_COMPOUND)
                .fluidInput(Fluids.LAVA)
                .fluidOutput(FluidContent.STILL_SULFURIC_ACID.get(), 1f)
                .fluidOutput(FluidContent.STILL_SHEOL_FIRE.get(), 0.25f)
                .fluidOutput(FluidContent.STILL_STRANGE_MATTER.get(), 0.1f)
                .timeMultiplier(1.6f)
                .export(exporter, "lavaalt");

        // biodiesel
        new RefineryRecipeBuilder(this.registries)
                .input(ItemContent.CLAY_CATALYST_BEADS)
                .fluidInput(TagContent.BIOFUEL)
                .fluidOutput(FluidContent.STILL_DIESEL.get(), 0.5f)
                .fluidOutput(FluidContent.STILL_NAPHTHA.get(), 0.2f)
                .export(exporter, "biodiesel");

        // centrifuge turbofuel
        new CentrifugeFluidRecipeBuilder(this.registries)
                .input(ItemContent.FLUXITE)
                .fluidInput(TagContent.DIESEL)
                .fluidOutput(FluidContent.STILL_FUEL.get())
                .export(exporter, "fuel");

        // centrifuge biofuel
        new CentrifugeFluidRecipeBuilder(this.registries)
                .input(TagContent.BIOMASS)
                .fluidInput(Fluids.WATER, 0.25f)
                .fluidOutput(FluidContent.STILL_BIOFUEL.get(), 0.1f)
                .timeMultiplier(0.2f)
                .export(exporter, "biofuel");

        // silicon wash from naphtha in centrifuge
        new CentrifugeFluidRecipeBuilder(this.registries)
                .input(TagContent.QUARTZ_DUSTS)
                .fluidInput(TagContent.NAPHTHA)
                .fluidOutput(FluidContent.STILL_SILICON_WASH.get(), 1f)
                .export(exporter, "siliconwash");

        new CentrifugeFluidRecipeBuilder(this.registries)
                .input(Items.GRAVEL)
                .fluidInput(TagContent.NAPHTHA)
                .fluidOutput(FluidContent.STILL_SILICON_WASH.get(), 0.05f)
                .timeMultiplier(1.6f)
                .export(exporter, "siliconwashbad");

        // polymer resin from naphtha (manual)
        offerManualFluidApplication(exporter, ItemContent.POLYMER_RESIN.get(), of(FluidContent.STILL_NAPHTHA_BUCKET.get()), of(ItemTags.SAND), "manualresin");

        // polymer resin from naphtha in centrifuge
        new CentrifugeFluidRecipeBuilder(this.registries)
                .input(ItemTags.SAND)
                .fluidInput(TagContent.NAPHTHA, 0.1f)
                .result(ItemContent.POLYMER_RESIN, 2)
                .export(exporter, "naptharesin");

        // basic battery in centrifuge with sulfuric acid
        new CentrifugeFluidRecipeBuilder(this.registries)
                .input(TagContent.STEEL_INGOTS)
                .fluidInput(TagContent.SULFURIC_ACID)
                .result(ItemContent.BASIC_BATTERY, 2)
                .export(exporter, "batteryacid");

        // adv battery in centrifuge with sulfuric acid
        new CentrifugeFluidRecipeBuilder(this.registries)
                .input(ItemContent.DUBIOS_CONTAINER)
                .fluidInput(TagContent.SULFURIC_ACID)
                .result(ItemContent.ADVANCED_BATTERY, 8)
                .timeMultiplier(2f)
                .export(exporter, "advbatteryacid");

        // silicon from silicon wash + sand in refinery
        new RefineryRecipeBuilder(this.registries)
                .input(ItemTags.SAND)
                .fluidInput(FluidContent.STILL_SILICON_WASH.get())
                .result(ItemContent.SILICON, 4)
                .timeMultiplier(2f)
                .export(exporter, "siliconwashing");

        // silicon wafer in centrifuge
        new CentrifugeFluidRecipeBuilder(this.registries)
                .input(ItemContent.CARBON_FIBRE_STRANDS)
                .fluidInput(FluidContent.STILL_SILICON_WASH.get())
                .result(ItemContent.SILICON_WAFER, 4)
                .timeMultiplier(2f)
                .export(exporter, "siliconwafers");

        // quartz from mineral wash in refinery
        new RefineryRecipeBuilder(this.registries)
                .input(ItemContent.CLAY_CATALYST_BEADS)
                .fluidInput(FluidContent.STILL_MINERAL_SLURRY.get(), 0.25f)
                .result(Items.QUARTZ)
                .timeMultiplier(2f)
                .export(exporter, "quartz");

        // reinforced carbon sheeting
        new RefineryRecipeBuilder(this.registries)
                .input(ItemContent.CARBON_FIBRE_STRANDS)
                .fluidInput(TagContent.NAPHTHA, 0.5f)
                .result(ItemContent.REINFORCED_CARBON_SHEET)
                .timeMultiplier(3f)
                .export(exporter, "carbonsheet");

        // dubious container and strange matter in centrifuge
        new CentrifugeFluidRecipeBuilder(this.registries)
                .input(ItemContent.DUBIOS_CONTAINER)
                .fluidInput(FluidContent.STILL_STRANGE_MATTER.get())
                .result(ItemContent.UNHOLY_INTELLIGENCE, 1)
                .timeMultiplier(8f)
                .export(exporter, "unholyai");
    }

    private void addBiomass(RecipeOutput exporter) {
        // biomass
        new PulverizerRecipeBuilder(this.registries).input(TagContent.BIOMATTER).result(ItemContent.BIOMASS).addToGrinder().export(exporter, "biobasic");
        new PulverizerRecipeBuilder(this.registries).input(ItemContent.PACKED_WHEAT).result(ItemContent.BIOMASS, 16).addToGrinder().export(exporter, "packagedwheatbio");
        new PulverizerRecipeBuilder(this.registries).input(cItemTag("storage_blocks/wheat")).result(ItemContent.BIOMASS, 16).addToGrinder().export(exporter, "hay_block");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.BIOMASS).input(TagContent.BIOMASS).input(TagContent.BIOMASS).input(ItemTags.PLANKS).result(ItemContent.SOLID_BIOFUEL).timeMultiplier(0.8f).export(exporter, "solidbiofuel");
    }

    private void addEquipment(RecipeOutput exporter) {
        offerDrillRecipe(exporter, ToolsContent.HAND_DRILL.get(), of(TagContent.STEEL_INGOTS), of(ItemContent.MOTOR), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.ADAMANT_INGOT), "handdrill");
        offerChainsawRecipe(exporter, ToolsContent.CHAINSAW.get(), of(TagContent.STEEL_INGOTS), of(ItemContent.MOTOR), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.ADAMANT_INGOT), "chainsaw");
        offerAxeRecipe(exporter, ToolsContent.PROMETHIUM_AXE.get(), of(ItemContent.PROMETHEUM_INGOT), of(BlockContent.DESTROYER.asItem()), "promaxe");
        offerPickaxeRecipe(exporter, ToolsContent.PROMETHIUM_PICKAXE.get(), of(ItemContent.PROMETHEUM_INGOT), of(BlockContent.DESTROYER.asItem()), "prompick");

        // enderic laser / portable laser
        offerChainsawRecipe(exporter, ToolsContent.PORTABLE_LASER.get(), of(ItemContent.ADVANCED_BATTERY), of(BlockContent.ACCELERATOR_MOTOR), of(ItemContent.ADAMANT_INGOT), of(BlockContent.ENDERIC_LASER), "portablelaser");

        // electric mace
        offerDrillRecipe(exporter, ToolsContent.ELECTRIC_MACE.get(), of(ItemContent.ADVANCED_BATTERY), of(ItemContent.CARBON_FIBRE_STRANDS), of(ItemContent.ADAMANT_INGOT), of(Items.HEAVY_CORE), "_emace");

        // designator
        offerDrillRecipe(exporter, ItemContent.TARGET_DESIGNATOR.get(), of(TagContent.STEEL_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.PROCESSING_UNIT), of(TagContent.PLASTIC_PLATES), "designator");
        // weed killer
        offerDrillRecipe(exporter, ItemContent.WEED_KILLER.get(), of(cItemTag("foods/food_poisoning")), of(cItemTag("foods/food_poisoning")), of(ItemContent.RAW_BIOPOLYMER), of(Items.GLASS_BOTTLE), "weedex");
        // wrench
        offerWrenchRecipe(exporter, ItemContent.WRENCH.get(), of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), "wrench");

        // helmet (enderic lens + machine plating)
        offerHelmetRecipe(exporter, ToolsContent.EXO_HELMET.get(), of(TagContent.PLATING_BLOCKS), of(ItemContent.ENDERIC_LENS), "exohelm");
        // chestplate (advanced battery + machine plating)
        offerChestplateRecipe(exporter, ToolsContent.EXO_CHESTPLATE.get(), of(TagContent.PLATING_BLOCKS), of(ItemContent.ADVANCED_BATTERY), "exochest");
        // legs (motor + plating)
        offerLegsRecipe(exporter, ToolsContent.EXO_LEGGINGS.get(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), "exolegs");
        // feet (silicon + plating)
        offerFeetRecipe(exporter, ToolsContent.EXO_BOOTS.get(), of(TagContent.PLATING_BLOCKS), of(TagContent.SILICON), "exoboots");

        // basic jetpack main
        offerParticleMotorRecipe(exporter, ToolsContent.JETPACK.get(), of(TagContent.STEEL_INGOTS), of(cItemTag("leathers")), of(ItemContent.ADVANCED_BATTERY), of(Items.GUNPOWDER), "basicjetpack");
        // jetpack alt
        offerParticleMotorRecipe(exporter, ToolsContent.JETPACK.get(), of(TagContent.STEEL_INGOTS), of(cItemTag("leathers")), of(Items.REDSTONE_BLOCK), of(Items.BLAZE_POWDER), "basicjetpackalt");
        // exo jetpack
        offerGeneratorRecipe(exporter, ToolsContent.EXO_JETPACK.get(), of(ToolsContent.JETPACK), of(BlockContent.PORTABLE_TANK), of(ToolsContent.EXO_CHESTPLATE), of(ItemContent.ION_THRUSTER), "exojetpack");
        // boosted elytra
        offerGeneratorRecipe(exporter, ToolsContent.JETPACK_ELYTRA.get(), of(Items.ELYTRA), of(ItemContent.PROCESSING_UNIT), of(ToolsContent.JETPACK), of(Items.GUNPOWDER), "boostedelytra");
        // exo elytra (exo jetpack + elytra)
        offerGeneratorRecipe(exporter, ToolsContent.JETPACK_EXO_ELYTRA.get(), of(ToolsContent.EXO_JETPACK), of(ItemContent.PROCESSING_UNIT), of(Items.ELYTRA), of(Items.GUNPOWDER), "exoboostedelytra");
        // exo elytra (boosted elytra + exo chestplate)
        offerGeneratorRecipe(exporter, ToolsContent.JETPACK_EXO_ELYTRA.get(), of(ToolsContent.EXO_CHESTPLATE), of(BlockContent.PORTABLE_TANK), of(ToolsContent.JETPACK_ELYTRA), of(ItemContent.ION_THRUSTER), "exoboostedelytraalt");

    }

    private void addDecorative(RecipeOutput exporter) {
        // ceiling light
        offerInsulatedCableRecipe(exporter, new ItemStackTemplate(BlockContent.INDUSTRIAL_LIGHT.asItem(), 6), of(Items.GLOWSTONE_DUST), of(TagContent.STEEL_INGOTS), "ceilightlight");
        // hanging light
        offerTwoComponentRecipe(exporter, BlockContent.INDUSTRIAL_LIGHT_HANGING.asItem(), of(cItemTag("chains")), of(BlockContent.INDUSTRIAL_LIGHT.asItem()), "hanginglight");
        // tech button
        offerLeverRecipe(exporter, BlockContent.INDUSTRIAL_BUTTON.asItem(), of(cItemTag("ingots/copper")), of(TagContent.STEEL_INGOTS), "techbutton");
        // tech lever
        offerLeverRecipe(exporter, BlockContent.INDUSTRIAL_LEVER.asItem(), of(TagContent.CARBON_FIBRE), of(TagContent.STEEL_INGOTS), "techlever");
        // tech door
        offerDoorRecipe(exporter, BlockContent.INDUSTRIAL_DOOR.asItem(), of(TagContent.STEEL_INGOTS), "techdoor");
        // hangar door
        this.shaped(RecipeCategory.REDSTONE, BlockContent.HANGAR_DOOR)
                .define('s', TagContent.STEEL_INGOTS)
                .define('m', ItemContent.MOTOR)
                .pattern("sms")
                .pattern("sss")
                .pattern("sms")
                .unlockedBy("has_motor", has(ItemContent.MOTOR))
                .save(exporter);
        // metal beam
        offerRotatedCableRecipe(exporter, new ItemStackTemplate(BlockContent.INDUSTRIAL_SUPPORT_BEAM.asItem(), 6), of(TagContent.CARBON_FIBRE), of(TagContent.STEEL_INGOTS), "metalbeams");
        // metal girder
        offerInsulatedCableRecipe(exporter, new ItemStackTemplate(BlockContent.INDUSTRIAL_SUPPORT_GIRDER.asItem(), 6), of(TagContent.CARBON_FIBRE), of(TagContent.STEEL_INGOTS), "metalgirder");
        // tech glass
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.INDUSTRIAL_GLASS.asItem(), of(TagContent.STEEL_INGOTS), of(cItemTag("glass_blocks")), of(TagContent.PLATING_BLOCKS), 4, "industrialglass");
        // machine plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.COPPER_REINFORCED_PLATING_SLAB.asItem(), of(BlockContent.COPPER_REINFORCED_PLATING.asItem()), "machine");
        offerStairsRecipe(exporter, BlockContent.COPPER_REINFORCED_PLATING_STAIRS.asItem(), of(BlockContent.COPPER_REINFORCED_PLATING.asItem()), "machine");
        offerPressurePlateRecipe(exporter, BlockContent.COPPER_REINFORCED_PLATING_PRESSURE_PLATE.asItem(), of(BlockContent.COPPER_REINFORCED_PLATING.asItem()), "machine");
        // iron plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.IRON_PLATING_SLAB.asItem(), of(BlockContent.IRON_PLATING.asItem()), "iron");
        offerStairsRecipe(exporter, BlockContent.IRON_PLATING_STAIRS.asItem(), of(BlockContent.IRON_PLATING.asItem()), "iron");
        offerPressurePlateRecipe(exporter, BlockContent.IRON_PLATING_PRESSURE_PLATE.asItem(), of(BlockContent.IRON_PLATING.asItem()), "iron");
        // nickel plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.NICKEL_PLATING_SLAB.asItem(), of(BlockContent.NICKEL_PLATING.asItem()), "nickel");
        offerStairsRecipe(exporter, BlockContent.NICKEL_PLATING_STAIRS.asItem(), of(BlockContent.NICKEL_PLATING.asItem()), "nickel");
        offerPressurePlateRecipe(exporter, BlockContent.NICKEL_PLATING_PRESSURE_PLATE.asItem(), of(BlockContent.NICKEL_PLATING.asItem()), "nickel");
        // carbon plated stairs, slabs, pressure plates
        offerSlabRecipe(exporter, BlockContent.CARBON_PLATING_SLAB.asItem(), of(BlockContent.CARBON_PLATING.asItem()), "carbon");
        offerStairsRecipe(exporter, BlockContent.CARBON_PLATING_STAIRS.asItem(), of(BlockContent.CARBON_PLATING.asItem()), "carbon");
        offerPressurePlateRecipe(exporter, BlockContent.CARBON_PLATING_PRESSURE_PLATE.asItem(), of(BlockContent.CARBON_PLATING.asItem()), "carbon");
    }

    private void addMachines(RecipeOutput exporter) {
        // basic generator
        offerGeneratorRecipe(exporter, BlockContent.BASIC_GENERATOR.asItem(), of(cItemTag("player_workstations/furnaces")), of(ItemContent.MAGNETIC_COIL), of(cItemTag("ingots/copper")), of(TagContent.NICKEL_INGOTS), "basicgen");
        // pulverizer
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER.asItem(), of(cItemTag("storage_blocks/copper")), of(ItemContent.MOTOR), of(TagContent.NICKEL_INGOTS), of(Items.IRON_INGOT), "pulverizer");
        offerGeneratorRecipe(exporter, BlockContent.PULVERIZER.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(TagContent.NICKEL_INGOTS), of(Items.IRON_INGOT), "pulverizeralt");
        // electric furnace
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE.asItem(), of(cItemTag("player_workstations/furnaces")), of(ItemContent.MAGNETIC_COIL), of(TagContent.SILICON), of(TagContent.ELECTRUM_INGOTS), of(cItemTag("ingots/copper")), "electricfurnace");
        offerFurnaceRecipe(exporter, BlockContent.POWERED_FURNACE.asItem(), of(cItemTag("player_workstations/furnaces")), of(ItemContent.MAGNETIC_COIL), of(TagContent.PLATINUM_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(cItemTag("ingots/copper")), "electricfurnacealt");
        // assembler
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER.asItem(), of(Blocks.BLAST_FURNACE.asItem()), of(ItemContent.MOTOR), of(Items.CRAFTER), of(ItemContent.ADAMANT_INGOT), of(cItemTag("ingots/copper")), "assembler");
        offerFurnaceRecipe(exporter, BlockContent.ASSEMBLER.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(Items.CRAFTER), of(ItemContent.ADAMANT_INGOT), of(cItemTag("ingots/copper")), "assembleralt");
        // foundry
        offerGeneratorRecipe(exporter, BlockContent.FOUNDRY.asItem(), of(Blocks.CAULDRON.asItem()), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.MOTOR), of(cItemTag("ingots/copper")), "foundry");
        // refinery
        offerParticleMotorRecipe(exporter, BlockContent.REFINERY.asItem(), of(BlockContent.REFINERY_CHAMBER_MODULE.asItem()), of(ItemContent.MOTOR), of(Items.CAULDRON), of(cItemTag("ingots/steel")), "refinery");
        // refinery module
        offerGeneratorRecipe(exporter, BlockContent.REFINERY_CHAMBER_MODULE.asItem(), of(BlockContent.PORTABLE_TANK.asItem()), of(Items.SLIME_BALL), of(BlockContent.INDUSTRIAL_SUPPORT_BEAM), of(cItemTag("ingots/copper")), "refinerymodule");
        offerGeneratorRecipe(exporter, BlockContent.REFINERY_CHAMBER_MODULE.asItem(), of(BlockContent.PORTABLE_TANK.asItem()), of(TagContent.SILICON), of(BlockContent.INDUSTRIAL_SUPPORT_BEAM), of(cItemTag("ingots/copper")), "refinerymodulealt");
        // industrial_chiller
        offerGeneratorRecipe(exporter, BlockContent.INDUSTRIAL_CHILLER.asItem(), of(Blocks.CAULDRON.asItem()), of(Blocks.ICE.asItem()), of(ItemContent.MOTOR), of(cItemTag("ingots/iron")), "industrial_chiller");
        // centrifuge
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.PROCESSING_UNIT), of(ItemContent.MOTOR), of(TagContent.STEEL_INGOTS), of(Items.GLASS_BOTTLE), "centrifuge");
        offerFurnaceRecipe(exporter, BlockContent.CENTRIFUGE.asItem(), of(ItemContent.MOTOR), of(cItemTag("storage_blocks/iron")), of(cItemTag("ingots/copper")), of(ItemContent.MOTOR), of(Items.GLASS_BOTTLE), "centrifugealt");
        // laser arm
        offerAtomicForgeRecipe(exporter, BlockContent.ENDERIC_LASER.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENDERIC_LENS), of(TagContent.CARBON_FIBRE), "laserarm");
        // crusher
        offerGeneratorRecipe(exporter, BlockContent.FRAGMENT_FORGE.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(ItemContent.FLUX_GATE), of(TagContent.PLASTIC_PLATES), "crusher");
        // atomic forge
        offerAtomicForgeRecipe(exporter, BlockContent.ATOMIC_FORGE.asItem(), of(TagContent.PLATING_BLOCKS), of(TagContent.PLASTIC_PLATES), of(ItemContent.ENDERIC_COMPOUND), of(ItemContent.DURATIUM_INGOT), of(ItemContent.FLUX_GATE), "atomicforge");

        // biofuel generator
        offerGeneratorRecipe(exporter, BlockContent.BIO_GENERATOR.asItem(), of(BlockContent.BASIC_GENERATOR.asItem()), of(ItemContent.MAGNETIC_COIL), of(ItemContent.FLUX_GATE), of(ItemContent.BIOSTEEL_INGOT), "biogen");
        // lava generator
        offerGeneratorRecipe(exporter, BlockContent.LAVA_GENERATOR.asItem(), of(BlockContent.BASIC_GENERATOR.asItem()), of(TagContent.PLATING_BLOCKS), of(ItemContent.MAGNETIC_COIL), of(TagContent.ELECTRUM_INGOTS), "lavagen");
        // steam engine
        offerGeneratorRecipe(exporter, BlockContent.STEAM_ENGINE.asItem(), of(BlockContent.BASIC_GENERATOR.asItem()), of(cItemTag("ingots/copper")), of(ItemContent.MAGNETIC_COIL), of(TagContent.ELECTRUM_INGOTS), "steamgen");
        // diesel generator
        offerGeneratorRecipe(exporter, BlockContent.FUEL_GENERATOR.asItem(), of(TagContent.PLATING_BLOCKS), of(BlockContent.BASIC_GENERATOR), of(ItemContent.ENDERIC_LENS), of(TagContent.STEEL_INGOTS), "fuelgen");
        // large solar
        offerGeneratorRecipe(exporter, BlockContent.BIG_SOLAR_PANEL.asItem(), of(BlockContent.BASIC_GENERATOR.asItem()), of(ItemContent.FLUX_GATE), of(ItemContent.ADVANCED_BATTERY), of(ItemContent.FLUXITE), "solar");

        // equipmentCharger
        offerAtomicForgeRecipe(exporter, BlockContent.EQUIPMENT_CHARGER.asItem(), of(cItemTag("chests/wooden")), of(BlockContent.ENERGY_PIPE), of(cItemTag("storage_blocks/redstone")), of(Items.DISPENSER), of(TagContent.STEEL_INGOTS), "equipmentcharger");
        offerAtomicForgeRecipe(exporter, BlockContent.EQUIPMENT_CHARGER.asItem(), of(cItemTag("chests/wooden")), of(BlockContent.ENERGY_PIPE), of(ItemContent.PROCESSING_UNIT), of(Items.DISPENSER), of(TagContent.STEEL_INGOTS), "equipmentchargeralt");

        // small storage
        offerAtomicForgeRecipe(exporter, BlockContent.PORTABLE_ENERGY_STORAGE.asItem(), of(ItemContent.BASIC_BATTERY), of(TagContent.SILICON), of(ItemContent.MAGNETIC_COIL), of(TagContent.NICKEL_INGOTS), of(TagContent.NICKEL_INGOTS), "smallstorage");
        // large storage
        offerAtomicForgeRecipe(exporter, BlockContent.LARGE_STORAGE.asItem(), of(ItemContent.ADVANCED_BATTERY), of(TagContent.STEEL_INGOTS), of(ItemContent.DUBIOS_CONTAINER), of(ItemContent.FLUX_GATE), of(ItemContent.MAGNETIC_COIL), "bigstorage");
        // unstable container
        offerAtomicForgeRecipe(exporter, ItemContent.SCHRODINGERS_SAFE.asItem(), of(ItemContent.FLUXITE), of(ItemContent.DURATIUM_INGOT), of(BlockContent.LARGE_STORAGE), of(ItemContent.FLUX_GATE), of(ItemContent.SUPER_AI_CHIP), "unstablecontainer");

        // fluid tank
        offerTankRecipe(exporter, BlockContent.PORTABLE_TANK.asItem(), of(cItemTag("ingots/copper")), of(cItemTag("glass_blocks")), of(BlockContent.FLUID_PIPE.asItem()), "stank");
        // pump
        offerGeneratorRecipe(exporter, BlockContent.PUMP.asItem(), of(TagContent.PLATING_BLOCKS), of(TagContent.SILICON), of(ItemContent.MOTOR), of(cItemTag("ingots/copper")), "pump");
        // block placer
        offerFurnaceRecipe(exporter, BlockContent.PLACER.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(ItemContent.PROCESSING_UNIT), of(BlockContent.MACHINE_FRAME.asItem()), of(cItemTag("ingots/copper")), "placer");
        // block destroyer
        offerAtomicForgeRecipe(exporter, BlockContent.DESTROYER.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(BlockContent.PULVERIZER), of(BlockContent.ENDERIC_LASER), of(ItemContent.MOTOR), "destroyer");
        // fertilizer
        offerFurnaceRecipe(exporter, BlockContent.FERTILIZER.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(TagContent.SILICON), of(ItemContent.PROCESSING_UNIT), of(cItemTag("ingots/copper")), "fertilizer");
        // tree feller
        offerGeneratorRecipe(exporter, BlockContent.TREE_CUTTER.asItem(), of(TagContent.PLATING_BLOCKS), of(Items.IRON_AXE), of(ItemContent.MOTOR), of(TagContent.ELECTRUM_INGOTS), "tree_cutter");
        // pipe booster
        offerTankRecipe(exporter, BlockContent.PIPE_BOOSTER.asItem(), of(BlockContent.ITEM_PIPE), of(ItemContent.MOTOR), of(BlockContent.FLUID_PIPE), "booster");

        // machine frame
        offerMachineFrameRecipe(exporter, BlockContent.MACHINE_FRAME.asItem(), of(Items.IRON_BARS), of(TagContent.NICKEL_INGOTS), 16, "frame");
        // energy pipe
        offerCableRecipe(exporter, new ItemStackTemplate(BlockContent.ENERGY_PIPE.asItem(), 6), of(TagContent.ELECTRUM_INGOTS), "energy");
        // item pipe
        offerInsulatedCableRecipe(exporter, new ItemStackTemplate(BlockContent.ITEM_PIPE.asItem(), 6), of(TagContent.NICKEL_INGOTS), of(ItemTags.PLANKS), "item");
        // item filter
        offerGeneratorRecipe(exporter, BlockContent.ITEM_FILTER.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemTags.PLANKS), of(ItemContent.PROCESSING_UNIT), of(ItemTags.PLANKS), "itemfilter");
        // smart splitter
        this.shaped(RecipeCategory.MISC, BlockContent.SMART_SPLITTER)
                .define('p', BlockContent.ITEM_PIPE)
                .define('f', BlockContent.ITEM_FILTER)
                .pattern(" p ")
                .pattern("pfp")
                .pattern(" p ")
                .unlockedBy("has_item_filter", has(BlockContent.ITEM_FILTER))
                .save(exporter, recipeKey("crafting/smart_splitter"));
        // fluid pipe
        offerInsulatedCableRecipe(exporter, new ItemStackTemplate(BlockContent.FLUID_PIPE.asItem(), 6), of(TagContent.SILICON), of(cItemTag("ingots/copper")), "fluidpipe");

        // framed energy pipe
        offerFramedCableRecipe(exporter, new ItemStackTemplate(BlockContent.FRAMED_ENERGY_PIPE.asItem(), 8), of(BlockContent.ENERGY_PIPE), "energy");
        offerCableFromFrameRecipe(exporter, new ItemStackTemplate(BlockContent.ENERGY_PIPE.asItem(), 1), of(BlockContent.FRAMED_ENERGY_PIPE), "energy");
        // framed superconductor
        offerFramedCableRecipe(exporter, new ItemStackTemplate(BlockContent.FRAMED_SUPERCONDUCTOR.asItem(), 8), of(BlockContent.SUPERCONDUCTOR.asItem()), "superconductor");
        offerCableFromFrameRecipe(exporter, new ItemStackTemplate(BlockContent.SUPERCONDUCTOR.asItem(), 1), of(BlockContent.FRAMED_SUPERCONDUCTOR), "superconductor");
        // framed fluid pipe
        offerFramedCableRecipe(exporter, new ItemStackTemplate(BlockContent.FRAMED_FLUID_PIPE.asItem(), 8), of(BlockContent.FLUID_PIPE), "fluid");
        offerCableFromFrameRecipe(exporter, new ItemStackTemplate(BlockContent.FLUID_PIPE.asItem(), 1), of(BlockContent.FRAMED_FLUID_PIPE), "fluid");
        // framed item pipe
        offerFramedCableRecipe(exporter, new ItemStackTemplate(BlockContent.FRAMED_ITEM_PIPE.asItem(), 8), of(BlockContent.ITEM_PIPE), "item");
        offerCableFromFrameRecipe(exporter, new ItemStackTemplate(BlockContent.ITEM_PIPE.asItem(), 1), of(BlockContent.FRAMED_ITEM_PIPE), "item");

        // transparent pipe
        offerTankRecipe(exporter, BlockContent.TRANSPARENT_ITEM_PIPE.asItem(), 6, of(ItemTags.PLANKS), of(TagContent.NICKEL_INGOTS), of(cItemTag("glass_blocks")), "transparentitem");
        offerMachineCoreRecipe(exporter, BlockContent.TRANSPARENT_ITEM_PIPE.asItem(), 8, of(BlockContent.ITEM_PIPE), of(cItemTag("glass_blocks")), "totransparent");
        offerMachineCoreRecipe(exporter, BlockContent.ITEM_PIPE.asItem(), 8, of(BlockContent.TRANSPARENT_ITEM_PIPE), of(ItemTags.PLANKS), "fromtransparent");

        // energy pipe duct
        offerCableDuctRecipe(exporter, new ItemStackTemplate(BlockContent.ENERGY_PIPE_DUCT.asItem(), 4), of(BlockContent.ENERGY_PIPE), "energy");
        offerCableFromDuctRecipe(exporter, new ItemStackTemplate(BlockContent.ENERGY_PIPE.asItem(), 1), of(BlockContent.ENERGY_PIPE_DUCT), "energy");
        // superconductor duct
        offerCableDuctRecipe(exporter, new ItemStackTemplate(BlockContent.SUPERCONDUCTOR_DUCT.asItem(), 4), of(BlockContent.SUPERCONDUCTOR.asItem()), "superconductor");
        offerCableFromDuctRecipe(exporter, new ItemStackTemplate(BlockContent.SUPERCONDUCTOR.asItem(), 1), of(BlockContent.SUPERCONDUCTOR_DUCT), "superconductor");
        // fluid pipe duct
        offerCableDuctRecipe(exporter, new ItemStackTemplate(BlockContent.FLUID_PIPE_DUCT.asItem(), 4), of(BlockContent.FLUID_PIPE), "fluid");
        offerCableFromDuctRecipe(exporter, new ItemStackTemplate(BlockContent.FLUID_PIPE.asItem(), 1), of(BlockContent.FLUID_PIPE_DUCT), "fluid");
        // item pipe duct
        offerCableDuctRecipe(exporter, new ItemStackTemplate(BlockContent.ITEM_PIPE_DUCT.asItem(), 4), of(BlockContent.ITEM_PIPE), "item");
        offerCableFromDuctRecipe(exporter, new ItemStackTemplate(BlockContent.ITEM_PIPE.asItem(), 1), of(BlockContent.ITEM_PIPE_DUCT), "item");

        // deep drill
        offerAtomicForgeRecipe(exporter, BlockContent.BEDROCK_EXTRACTOR.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(ItemContent.HEISENBERG_COMPENSATOR), of(ItemContent.OVERCHARGED_CRYSTAL), of(ItemContent.DURATIUM_INGOT), "deepdrill");
        // drone port
        offerAtomicForgeRecipe(exporter, BlockContent.DRONE_PORT.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(BlockContent.SUPERCONDUCTOR.asItem()), of(ItemContent.UNHOLY_INTELLIGENCE), of(ItemContent.ADVANCED_COMPUTING_ENGINE), "droneport");
        offerAtomicForgeRecipe(exporter, BlockContent.DRONE_PORT.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(BlockContent.SUPERCONDUCTOR.asItem()), of(ItemContent.SUPER_AI_CHIP), of(ItemContent.ADVANCED_COMPUTING_ENGINE), "droneportalt");

        // arcane catalyst
        offerFurnaceRecipe(exporter, BlockContent.ARCANE_CATALYST.asItem(), of(Items.ENCHANTING_TABLE), of(ItemContent.ADAMANT_INGOT), of(cItemTag("obsidians/normal")), of(ItemContent.UNHOLY_INTELLIGENCE), of(ItemContent.FLUXITE), "catalyst");
        offerFurnaceRecipe(exporter, BlockContent.ARCANE_CATALYST.asItem(), of(Items.ENCHANTING_TABLE), of(ItemContent.ADAMANT_INGOT), of(cItemTag("obsidians/normal")), of(ItemContent.SUPER_AI_CHIP), of(ItemContent.FLUXITE), "catalyst_alt");
        // stabilized_enchanter
        offerGeneratorRecipe(exporter, BlockContent.STABILIZED_ENCHANTER.asItem(), of(ItemContent.DURATIUM_INGOT), of(ItemContent.ENERGITE_INGOT), of(BlockContent.ARCANE_CATALYST.asItem()), of(Items.BOOK), "stabilized_enchanter");
        // spawner
        offerTankRecipe(exporter, BlockContent.SPAWNER_CONTROLLER.asItem(), of(BlockContent.SPAWNER_CAGE), of(Blocks.RESPAWN_ANCHOR), of(BlockContent.ARCANE_CATALYST), "spawner");
        // spawner cage
        offerInsulatedCableRecipe(exporter, new ItemStackTemplate(BlockContent.SPAWNER_CAGE.asItem(), 2), of(TagContent.PLASTIC_PLATES), of(Items.IRON_BARS), "cage");
        // withered rose
        offerMachineFrameRecipe(exporter, BlockContent.SOUL_FLOWERS.asItem(), of(Items.WITHER_ROSE), of(ItemTags.FLOWERS), 1, "witherrose");

        // energy transmission pole
        offerEnergyTransmissionPoleRecipe(exporter, BlockContent.ENERGY_TRANSMISSION_POLE.asItem(), of(ItemContent.MAGNETIC_COIL), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.PORTABLE_ENERGY_STORAGE), of(ItemContent.CARBON_FIBRE_STRANDS), "_pole");

        // addon_splicer
        offerTankRecipe(exporter, BlockContent.ADDON_SPLICER.asItem(), of(ItemContent.DUBIOS_CONTAINER), of(FluidContent.STILL_STRANGE_MATTER_BUCKET.get()), of(BlockContent.SUPERCONDUCTOR), "addon_splicer");

        // particle accelerator
        // motor
        offerParticleMotorRecipe(exporter, BlockContent.ACCELERATOR_MOTOR.asItem(), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.SUPERCONDUCTOR.asItem()), of(ItemContent.DURATIUM_INGOT), of(ItemContent.ION_THRUSTER), "particlemotor");
        // ring
        offerDrillRecipe(exporter, BlockContent.ACCELERATOR_RING.asItem(), of(BlockContent.INDUSTRIAL_GLASS.asItem()), of(BlockContent.SUPERCONDUCTOR.asItem()), of(TagContent.STEEL_INGOTS), of(Items.REDSTONE_TORCH), "acceleratorring");
        // controller
        offerGeneratorRecipe(exporter, BlockContent.PARTICLE_ACCELERATOR.asItem(), of(BlockContent.ACCELERATOR_MOTOR.asItem()), of(ItemContent.FLUX_GATE), of(Items.DROPPER), of(ItemContent.DURATIUM_INGOT), "particlecontroller");
        // sensor
        offerTwoComponentRecipe(exporter, BlockContent.ACCELERATOR_SENSOR.asItem(), of(BlockContent.ACCELERATOR_RING.asItem()), of(Items.OBSERVER), "particlesensor");
        // collector
        offerTankRecipe(exporter, BlockContent.TACHYON_ABSORBER.asItem(), of(BlockContent.SUPERCONDUCTOR.asItem()), of(BlockContent.BIG_SOLAR_PANEL.asItem()), of(ItemContent.HEISENBERG_COMPENSATOR), "particlecollector");

        // addons
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_SPEED_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MAGNETIC_COIL), of(TagContent.STEEL_INGOTS), of(TagContent.PLASTIC_PLATES), "addon/speed");
        offerAtomicForgeRecipe(exporter, BlockContent.AUXILIARY_PROCESSING_CHAMBER_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.SUPER_AI_CHIP), of(ItemContent.FLUX_GATE), of(TagContent.PLATINUM_INGOTS), of(ItemContent.MOTOR), "addon/processing");
        offerAtomicForgeRecipe(exporter, BlockContent.AUXILIARY_PROCESSING_CHAMBER_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.UNHOLY_INTELLIGENCE), of(Items.COMPARATOR), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.MOTOR), "addon/processingalt");
        offerAtomicForgeRecipe(exporter, BlockContent.SYNERGY_MATRIX_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.HEISENBERG_COMPENSATOR), of(BlockContent.MACHINE_SPEED_ADDON), of(BlockContent.MACHINE_EFFICIENCY_ADDON), of(ItemContent.OVERCHARGED_CRYSTAL), "addon/ultimate");
        offerAtomicForgeRecipe(exporter, BlockContent.MACHINE_BURST_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.INDUSTRIAL_SUPPORT_GIRDER), of(TagContent.STEEL_INGOTS), of(Items.REDSTONE), "addon/burst");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_EFFICIENCY_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(TagContent.CARBON_FIBRE), of(TagContent.ELECTRUM_INGOTS), of(TagContent.PLASTIC_PLATES), "addon/eff");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_CAPACITOR_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.ENERGITE_INGOT), of(ItemContent.MAGNETIC_COIL), of(TagContent.PLASTIC_PLATES), "addon/capacitor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_ACCEPTOR_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENERGITE_INGOT), of(TagContent.PLASTIC_PLATES), "addon/acceptor");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_YIELD_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENDERIC_LENS), of(TagContent.PLASTIC_PLATES), "addon/yield");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_FLUID_ADDON.asItem(), of(TagContent.SILICON), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.FLUID_PIPE), of(TagContent.CARBON_FIBRE), "addon/fluid");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_INVENTORY_PROXY_ADDON.asItem(), of(ItemContent.MOTOR), of(cItemTag("chests")), of(ItemContent.PROCESSING_UNIT), of(TagContent.CARBON_FIBRE), "addon/invproxy");
        offerGeneratorRecipe(exporter, BlockContent.CROP_FILTER_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(ItemContent.PROCESSING_UNIT), of(TagContent.CARBON_FIBRE), "addon/cropfilter");
        offerGeneratorRecipe(exporter, BlockContent.QUARRY_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(Items.DIAMOND_PICKAXE), of(TagContent.PLASTIC_PLATES), "addon/quarry");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_HUNTER_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(Items.IRON_SWORD), of(TagContent.PLASTIC_PLATES), "_hunter");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.ADAMANT_INGOT), of(cItemTag("ingots/copper")), of(BlockContent.FLUID_PIPE), "addon/steamboiler");
        offerGeneratorRecipe(exporter, BlockContent.STEAM_BOILER_ADDON.asItem(), of(TagContent.SILICON), of(ItemContent.ADAMANT_INGOT), of(BlockContent.FLUID_PIPE), of(TagContent.COAL_DUSTS), "addon/steamboileralt");
        offerGeneratorRecipe(exporter, BlockContent.CONTROL_UNIT_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(Items.REPEATER), of(Items.COMPARATOR), of(cItemTag("dusts/redstone")), "addon/redstone");
        offerGeneratorRecipe(exporter, BlockContent.MACHINE_SILK_TOUCH_ADDON.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemTags.WOOL), of(Items.DIAMOND_PICKAXE), of(TagContent.PLASTIC_PLATES), "addon/silktouch");
        offerTwoComponentRecipe(exporter, BlockContent.POWER_BANK_ADDON_EXTENDER.asItem(), of(BlockContent.MACHINE_EXTENDER.asItem()), of(BlockContent.MACHINE_CAPACITOR_ADDON), "addon/capextender");

        // cores
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_1.asItem(), of(ItemTags.PLANKS), of(Items.CRAFTING_TABLE), "core1");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), of(cItemTag("ingots/copper")), of(cItemTag("gems/lapis")), "core2");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_2.asItem(), of(cItemTag("ingots/iron")), of(cItemTag("gems/lapis")), "core2alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), of(TagContent.CARBON_FIBRE), of(cItemTag("dusts/redstone")), "core3");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_3.asItem(), of(TagContent.NICKEL_INGOTS), of(cItemTag("dusts/redstone")), "core3alt");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_4.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.ENDERIC_COMPOUND), "core4");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_5.asItem(), of(ItemContent.ADAMANT_INGOT), of(ItemContent.ADVANCED_COMPUTING_ENGINE), "core5");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_6.asItem(), of(ItemContent.DURATIUM_INGOT), of(ItemContent.DUBIOS_CONTAINER), "core6");
        offerMachineCoreRecipe(exporter, BlockContent.MACHINE_CORE_7.asItem(), of(ItemContent.PROMETHEUM_INGOT), of(BlockContent.SUPERCONDUCTOR.asItem()), "core7");

        // machine extender
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.MACHINE_EXTENDER.asItem(), of(TagContent.PLATING_BLOCKS), of(BlockContent.MACHINE_CORE_2.asItem()), of(ItemContent.DURATIUM_INGOT), 1, "extender");

        // augmenter
        // machine itself
        offerAtomicForgeRecipe(exporter, BlockContent.CYBERNETIC_AUGMENTATION_CENTER.asItem(), of(TagContent.PLATING_BLOCKS), of(ItemContent.MOTOR), of(Items.CHEST), of(TagContent.CARBON_FIBRE), of(ItemContent.DUBIOS_CONTAINER), "augment/applicator");
        // basic station
        offerGeneratorRecipe(exporter, BlockContent.CYBERNETIC_RESEARCH_STATION.asItem(), of(Items.BREWING_STAND), of(TagContent.PLATING_BLOCKS), of(cItemTag("storage_blocks/redstone")), of(TagContent.ELECTRUM_INGOTS), "augment/basic");
        // adv station
        offerGeneratorRecipe(exporter, BlockContent.QUANTUM_RESEARCH_STATION.asItem(), of(BlockContent.CENTRIFUGE), of(TagContent.PLATING_BLOCKS), of(ItemContent.FLUX_GATE), of(ItemContent.DURATIUM_INGOT), "augment/advanced");
        // arcane station
        offerGeneratorRecipe(exporter, BlockContent.ARCANE_AUGMENT_STATION.asItem(), of(Items.ENDER_EYE), of(TagContent.PLATING_BLOCKS), of(ItemContent.ENDERIC_LENS), of(ItemContent.OVERCHARGED_CRYSTAL), "augment/arcane");

    }

    private void addComponents(RecipeOutput exporter) {
        // coal stuff (including basic steel)
        new CentrifugeRecipeBuilder(this.registries).input(TagContent.COAL_DUSTS).result(ItemContent.CARBON_FIBRE_STRANDS).timeMultiplier(0.5f).export(exporter, "carbon");
        offerManualAlloyRecipe(exporter, ItemContent.STEEL_INGOT.get(), of(cItemTag("ingots/iron")), of(ItemTags.COALS), "steel");

        // manual alloys
        offerManualAlloyRecipe(exporter, ItemContent.ELECTRUM_INGOT.get(), of(cItemTag("ingots/gold")), of(cItemTag("dusts/redstone")), "electrum");
        offerManualAlloyRecipe(exporter, ItemContent.ADAMANT_INGOT.get(), of(TagContent.NICKEL_INGOTS), of(cItemTag("gems/diamond")), "adamant");

        // enderic entry
        new PulverizerRecipeBuilder(this.registries).input(cItemTag("ender_pearls")).result(ItemContent.ENDERIC_COMPOUND, 8).export(exporter, "pearl_enderic");
        new GrinderRecipeBuilder(this.registries).input(cItemTag("ender_pearls")).result(ItemContent.ENDERIC_COMPOUND, 12).export(exporter, "pearl_enderic");
        new GrinderRecipeBuilder(this.registries).input(Blocks.END_STONE).result(ItemContent.ENDERIC_COMPOUND).export(exporter, "stone_enderic");

        // clay beads
        offerBeadsRecipe(exporter, ItemContent.CLAY_CATALYST_BEADS.get(), 8, of(Items.CLAY_BALL), of(ItemTags.SAND), of(Items.REDSTONE), "claybeads");
        new AssemblerRecipeBuilder(this.registries).input(Items.CLAY_BALL).input(Items.CLAY_BALL).input(ItemTags.SAND).input(Items.REDSTONE).result(ItemContent.CLAY_CATALYST_BEADS, 32).timeMultiplier(1f).export(exporter, "claybeads");

        // magnetic coils
        offerInsulatedCableRecipe(exporter, new ItemStackTemplate(ItemContent.MAGNETIC_COIL.get(), 4), of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), "magnet");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.STEEL_INGOTS).input(TagContent.NICKEL_INGOTS).input(TagContent.NICKEL_INGOTS).input(cItemTag("ingots/copper")).result(ItemContent.MAGNETIC_COIL, 6).timeMultiplier(0.4f).export(exporter, "magnet");

        // motor
        offerMotorRecipe(exporter, ItemContent.MOTOR.get(), of(TagContent.NICKEL_INGOTS), of(ItemContent.MAGNETIC_COIL), of(TagContent.STEEL_INGOTS), "motorcraft");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.NICKEL_INGOTS).input(TagContent.STEEL_INGOTS).input(ItemContent.MAGNETIC_COIL).input(ItemContent.MAGNETIC_COIL).result(ItemContent.MOTOR, 2).timeMultiplier(0.4f).export(exporter, "motor");

        // machine plating variants
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.COPPER_REINFORCED_PLATING.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(cItemTag("ingots/copper")), 2, "plating");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.STEEL_INGOTS).input(TagContent.STEEL_INGOTS).input(cItemTag("ingots/copper")).input(TagContent.PLASTIC_PLATES).result(BlockContent.COPPER_REINFORCED_PLATING.asItem(), 8).timeMultiplier(0.8f).export(exporter, "plating");
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.IRON_PLATING.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(cItemTag("ingots/iron")), 2, "iron");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.STEEL_INGOTS).input(TagContent.STEEL_INGOTS).input(cItemTag("ingots/iron")).input(TagContent.PLASTIC_PLATES).result(BlockContent.IRON_PLATING.asItem(), 8).timeMultiplier(0.8f).export(exporter, "platingiron");
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.NICKEL_PLATING.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(TagContent.NICKEL_INGOTS), 2, "nickel");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.STEEL_INGOTS).input(TagContent.STEEL_INGOTS).input(TagContent.NICKEL_INGOTS).input(TagContent.PLASTIC_PLATES).result(BlockContent.NICKEL_PLATING.asItem(), 8).timeMultiplier(0.8f).export(exporter, "platingnickel");
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.CARBON_PLATING.asItem(), of(TagContent.STEEL_INGOTS), of(Blocks.STONE.asItem()), of(ItemContent.REINFORCED_CARBON_SHEET), 2, "carbon");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.STEEL_INGOTS).input(TagContent.STEEL_INGOTS).input(ItemContent.REINFORCED_CARBON_SHEET).input(TagContent.PLASTIC_PLATES).result(BlockContent.CARBON_PLATING.asItem(), 8).timeMultiplier(0.8f).export(exporter, "platingcarbon");

        // basic battery
        offerMotorRecipe(exporter, ItemContent.BASIC_BATTERY.get(), of(TagContent.STEEL_INGOTS), of(TagContent.ELECTRUM_INGOTS), of(TagContent.PLASTIC_PLATES), "manualbattery");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.PLASTIC_PLATES).input(TagContent.ELECTRUM_INGOTS).input(TagContent.ELECTRUM_INGOTS).input(TagContent.STEEL_INGOTS).result(ItemContent.BASIC_BATTERY).timeMultiplier(0.4f).export(exporter, "battery");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.PLASTIC_PLATES).input(ItemContent.FLUXITE).input(ItemContent.FLUXITE).input(TagContent.STEEL_INGOTS).result(ItemContent.BASIC_BATTERY, 2).timeMultiplier(0.8f).export(exporter, "batterybetter");

        // silicon
        offerManualAlloyRecipe(exporter, ItemContent.RAW_SILICON.get(), of(TagContent.QUARTZ_DUSTS), of(ItemTags.SAND), 3, "rawsilicon");
        oreSmelting(exporter, List.of(ItemContent.RAW_SILICON), RecipeCategory.MISC, ItemContent.SILICON, 0.5f, 60, "siliconfurnace");

        // plastic
        twoByTwoPacker(exporter, RecipeCategory.MISC, ItemContent.PACKED_WHEAT, Items.WHEAT);
        new CentrifugeFluidRecipeBuilder(this.registries).input(ItemContent.PACKED_WHEAT).result(ItemContent.RAW_BIOPOLYMER).fluidInput(Fluids.WATER, 0.25f).export(exporter, "biopolymer");
        new CentrifugeFluidRecipeBuilder(this.registries).input(ItemContent.SOLID_BIOFUEL).result(ItemContent.RAW_BIOPOLYMER).fluidInput(Fluids.WATER, 0.25f).export(exporter, "biopolymer_biomass");
        new CentrifugeFluidRecipeBuilder(this.registries).input(TagContent.BIOMASS_BLOCK).result(ItemContent.RAW_BIOPOLYMER).fluidInput(Fluids.WATER, 0.25f).export(exporter, "biopolymer_bioblock");
        new CentrifugeFluidRecipeBuilder(this.registries).input(ItemTags.SAND).result(ItemContent.POLYMER_RESIN).fluidInput(cFluidTag("biodiesel"), 0.1f).time(100).export(exporter, "polymerresin");
        new CentrifugeFluidRecipeBuilder(this.registries).input(ItemContent.RAW_BIOPOLYMER).result(ItemContent.PLASTIC_SHEET, 1).fluidInput(Fluids.WATER, 0.5f).time(120).export(exporter, "plasticoil");
        new CentrifugeFluidRecipeBuilder(this.registries).input(ItemContent.RAW_BIOPOLYMER).result(ItemContent.PLASTIC_SHEET, 2).fluidInput(FluidContent.STILL_MINERAL_SLURRY.get(), 0.25f).time(120).export(exporter, "plasticoilbetter");
        new CentrifugeFluidRecipeBuilder(this.registries).input(ItemContent.POLYMER_RESIN).result(ItemContent.PLASTIC_SHEET, 2).fluidInput(Fluids.WATER, 0.5f).time(40).export(exporter, "plasticbio");
        new CentrifugeFluidRecipeBuilder(this.registries).input(ItemContent.POLYMER_RESIN).result(ItemContent.PLASTIC_SHEET, 4).fluidInput(FluidContent.STILL_MINERAL_SLURRY.get(), 0.25f).time(40).export(exporter, "plasticbiobetter");
        oreSmelting(exporter, List.of(ItemContent.POLYMER_RESIN), RecipeCategory.MISC, ItemContent.PLASTIC_SHEET, 0.5f, 10, "plastic_manual");
        oreBlasting(exporter, List.of(ItemContent.POLYMER_RESIN), RecipeCategory.MISC, ItemContent.PLASTIC_SHEET, 0.5f, 10, "plastic_manual_blast");

        // processing unit
        new AssemblerRecipeBuilder(this.registries).input(TagContent.PLASTIC_PLATES).input(TagContent.CARBON_FIBRE).input(TagContent.ELECTRUM_INGOTS).input(cItemTag("dusts/redstone")).result(ItemContent.PROCESSING_UNIT).timeMultiplier(0.8f).export(exporter, "processingunit");
        // enderic lens
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.ADAMANT_INGOT).input(TagContent.CARBON_FIBRE).input(ItemContent.ENDERIC_COMPOUND).input(ItemContent.ENDERIC_COMPOUND).result(ItemContent.ENDERIC_LENS).timeMultiplier(1.2f).export(exporter, "enderlens");
        // flux gate
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.PROCESSING_UNIT).input(ItemContent.FLUXITE).input(ItemContent.FLUXITE).input(TagContent.PLATINUM_INGOTS).result(ItemContent.FLUX_GATE).timeMultiplier(1.2f).export(exporter, "fluxgate");

        // ai processor tree
        new AtomicForgeRecipeBuilder(this.registries).input(TagContent.CARBON_FIBRE).input(TagContent.SILICON).input(TagContent.SILICON).result(ItemContent.SILICON_WAFER).time(5).export(exporter, "wafer");
        new AtomicForgeRecipeBuilder(this.registries).input(ItemContent.PROCESSING_UNIT).input(ItemContent.SILICON_WAFER).input(ItemContent.SILICON_WAFER).result(ItemContent.ADVANCED_COMPUTING_ENGINE).time(5).export(exporter, "advcomputer");
        new AtomicForgeRecipeBuilder(this.registries).input(ItemContent.DURATIUM_INGOT).input(ItemContent.ADVANCED_COMPUTING_ENGINE).input(ItemContent.ADVANCED_COMPUTING_ENGINE).result(ItemContent.SUPER_AI_CHIP).time(50).export(exporter, "aicomputer");

        // dubios container
        offerMotorRecipe(exporter, ItemContent.DUBIOS_CONTAINER.get(), of(TagContent.PLASTIC_PLATES), of(ItemContent.ADAMANT_INGOT), of(ItemContent.ENDERIC_COMPOUND), "dubios");
        // adv battery
        offerMotorRecipe(exporter, ItemContent.ADVANCED_BATTERY.get(), of(TagContent.ELECTRUM_INGOTS), of(ItemContent.ENERGITE_INGOT), of(TagContent.STEEL_INGOTS), "advbattery");

        // ion thruster
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.REINFORCED_CARBON_SHEET).input(ItemContent.REINFORCED_CARBON_SHEET).input(ItemContent.ADVANCED_BATTERY).input(ItemContent.FLUX_GATE).result(ItemContent.ION_THRUSTER, 2).timeMultiplier(2f).export(exporter, "ionthruster");

        // biosteel
        new FoundryRecipeBuilder(this.registries).input(ItemContent.RAW_BIOPOLYMER).input(cItemTag("ingots/iron")).result(ItemContent.BIOSTEEL_INGOT).export(exporter, "biosteel");

        // endgame components
        new AtomicForgeRecipeBuilder(this.registries).input(ItemContent.SUPER_AI_CHIP).input(ItemContent.ADAMANT_INGOT).input(ItemContent.ADAMANT_INGOT).result(ItemContent.HEISENBERG_COMPENSATOR).time(60).export(exporter, "compensator");
        new AtomicForgeRecipeBuilder(this.registries).input(ItemContent.UNHOLY_INTELLIGENCE).input(ItemContent.ADAMANT_INGOT).input(ItemContent.ADAMANT_INGOT).result(ItemContent.HEISENBERG_COMPENSATOR).time(60).export(exporter, "compensatoralt");
        offerMotorRecipe(exporter, ItemContent.OVERCHARGED_CRYSTAL.get(), of(Items.AMETHYST_BLOCK), of(ItemContent.ADVANCED_BATTERY), of(BlockContent.SUPERCONDUCTOR.asItem()), "overchargedcrystal");
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.FLUX_GATE).input(TagContent.ELECTRUM_INGOTS).input(ItemContent.DUBIOS_CONTAINER).input(ItemContent.ENERGITE_INGOT).result(BlockContent.SUPERCONDUCTOR.asItem(), 4).timeMultiplier(1.6f).export(exporter, "superconductor");
        new AtomicForgeRecipeBuilder(this.registries).input(ItemContent.HEISENBERG_COMPENSATOR).input(ItemContent.OVERCHARGED_CRYSTAL).input(ItemContent.OVERCHARGED_CRYSTAL).result(ItemContent.PROMETHEUM_INGOT).time(240).export(exporter, "prometheum");

        // ice in industrial_chiller
        new IndustrialChillerRecipeBuilder(this.registries).fluidInput(Fluids.WATER).result(Items.ICE, 3).export(exporter, "ice");

        // snow from steam in industrial_chiller
        new IndustrialChillerRecipeBuilder(this.registries).fluidInput(FluidContent.STILL_STEAM.get()).result(Items.SNOW_BLOCK, 3).export(exporter, "snow");

        // obsidian from lava
        new IndustrialChillerRecipeBuilder(this.registries).fluidInput(Fluids.LAVA).result(Items.OBSIDIAN, 2).export(exporter, "obsidian");
    }

    private void addCompactingRecipes(RecipeOutput exporter) {
        addCompactingRecipe(exporter, BlockContent.STEEL, ItemContent.STEEL_INGOT, of(TagContent.STEEL_INGOTS), of(getStorageBlockTag("steel")));
        addCompactingRecipe(exporter, BlockContent.ENERGITE, ItemContent.ENERGITE_INGOT, of(getIngotTag("energite")), of(getStorageBlockTag("energite")));
        addCompactingRecipe(exporter, BlockContent.NICKEL, ItemContent.NICKEL_INGOT, of(getIngotTag("nickel")), of(getStorageBlockTag("nickel")));
        addCompactingRecipe(exporter, BlockContent.BIOSTEEL, ItemContent.BIOSTEEL_INGOT, of(ItemContent.BIOSTEEL_INGOT), of(getStorageBlockTag("biosteel")));
        addCompactingRecipe(exporter, BlockContent.PLATINUM, ItemContent.PLATINUM_INGOT, of(getIngotTag("platinum")), of(getStorageBlockTag("platinum")));
        addCompactingRecipe(exporter, BlockContent.ADAMANT, ItemContent.ADAMANT_INGOT, of(getIngotTag("adamant")), of(getStorageBlockTag("adamant")));
        addCompactingRecipe(exporter, BlockContent.ELECTRUM, ItemContent.ELECTRUM_INGOT, of(getIngotTag("electrum")), of(getStorageBlockTag("electrum")));
        addCompactingRecipe(exporter, BlockContent.DURATIUM, ItemContent.DURATIUM_INGOT, of(getIngotTag("duratium")), of(getStorageBlockTag("duratium")));
        addCompactingRecipe(exporter, BlockContent.BIOMASS, ItemContent.BIOMASS, of(ItemContent.BIOMASS), of(getStorageBlockTag("biomass")));
        addCompactingRecipe(exporter, BlockContent.PLASTIC, ItemContent.PLASTIC_SHEET, of(TagContent.PLASTIC_PLATES), of(getStorageBlockTag("plastic")));
        addCompactingRecipe(exporter, BlockContent.FLUXITE, ItemContent.FLUXITE, of(ItemContent.FLUXITE), of(getStorageBlockTag("fluxite")));
        addCompactingRecipe(exporter, BlockContent.SILICON, ItemContent.SILICON, of(TagContent.SILICON), of(getStorageBlockTag("silicon")));
        addCompactingRecipe(exporter, BlockContent.RAW_NICKEL, ItemContent.RAW_NICKEL, of(TagContent.NICKEL_RAW_MATERIALS), of(getStorageBlockTag("raw_nickel")));
        addCompactingRecipe(exporter, BlockContent.RAW_PLATINUM, ItemContent.RAW_PLATINUM, of(TagContent.PLATINUM_RAW_MATERIALS), of(getStorageBlockTag("raw_platinum")));
        addCompactingRecipe(exporter, BlockContent.RAW_URANIUM, ItemContent.RAW_URANIUM, of(TagContent.URANIUM_RAW_MATERIALS), of(getStorageBlockTag("raw_uranium")));
        addCompactingRecipe(exporter, BlockContent.URANIUM, ItemContent.URANIUM_DUST, of(TagContent.URANIUM_DUSTS), of(getStorageBlockTag("uranium_dust")));

    }

    // offerSmelting, offerBlasting, and offerMultipleOptions copied from RecipeProvider, and altered to force Oritech id onto recipes
    // I don't really like this, but any other way I found to get these recipes to have the oritech namespace in Neoforge wasn't working.
    public static void oreSmelting(RecipeOutput exporter, List<ItemLike> inputs, RecipeCategory category, ItemLike output, float experience, int cookingTime, String group) {
        oreCooking(exporter, SmeltingRecipe.SERIALIZER, SmeltingRecipe::new, inputs, category, output, experience, cookingTime, group, "_from_smelting");
    }

    public static void oreBlasting(RecipeOutput exporter, List<ItemLike> inputs, RecipeCategory category, ItemLike output, float experience, int cookingTime, String group) {
        oreCooking(exporter, BlastingRecipe.SERIALIZER, BlastingRecipe::new, inputs, category, output, experience, cookingTime, group, "_from_blasting");
    }

    public static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput exporter, RecipeSerializer<T> serializer, AbstractCookingRecipe.Factory<T> recipeFactory, List<ItemLike> inputs, RecipeCategory category, ItemLike output, float experience, int cookingTime, String group, String suffix) {

        for (var itemConvertible : inputs) {
            var inputName = BuiltInRegistries.ITEM.getKey(itemConvertible.asItem()).getPath();
            var outputName = BuiltInRegistries.ITEM.getKey(output.asItem()).getPath();
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemConvertible), category, CookingBookCategory.MISC, output, experience, cookingTime, recipeFactory)
                    .group(group)
                    .unlockedBy("has_" + inputName, InventoryChangeTrigger.TriggerInstance.hasItems(itemConvertible))
                    .save(exporter, recipeKey(outputName + suffix + "_" + inputName));
        }
    }

    public static void threeByThreePacker(RecipeOutput exporter, RecipeCategory category, ItemLike packed, ItemLike unpacked) {
        var inputName = BuiltInRegistries.ITEM.getKey(unpacked.asItem()).getPath();
        var outputName = BuiltInRegistries.ITEM.getKey(packed.asItem()).getPath();
        ShapelessRecipeBuilder.shapeless(BuiltInRegistries.ITEM, category, unpacked, 9)
                .requires(packed)
                .unlockedBy("has_" + outputName, InventoryChangeTrigger.TriggerInstance.hasItems(packed))
                .save(exporter, recipeKey("crafting/" + inputName + "_from_unpacking"));

        ShapedRecipeBuilder.shaped(BuiltInRegistries.ITEM, category, packed)
                .define('#', unpacked)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy("has_" + inputName, InventoryChangeTrigger.TriggerInstance.hasItems(unpacked))
                .save(exporter, recipeKey("crafting/" + outputName + "_from_packing"));
    }

    public static void twoByTwoPacker(RecipeOutput exporter, RecipeCategory category, ItemLike packed, ItemLike unpacked) {
        var inputName = BuiltInRegistries.ITEM.getKey(unpacked.asItem()).getPath();
        var outputName = BuiltInRegistries.ITEM.getKey(packed.asItem()).getPath();
        ShapelessRecipeBuilder.shapeless(BuiltInRegistries.ITEM, category, unpacked, 4)
                .requires(packed)
                .unlockedBy("has_" + outputName, InventoryChangeTrigger.TriggerInstance.hasItems(packed))
                .save(exporter, recipeKey("crafting/" + inputName + "_from_unpacking"));

        ShapedRecipeBuilder.shaped(BuiltInRegistries.ITEM, category, packed)
                .define('#', unpacked)
                .pattern("##")
                .pattern("##")
                .unlockedBy("has_" + inputName, InventoryChangeTrigger.TriggerInstance.hasItems(unpacked))
                .save(exporter, recipeKey("crafting/" + outputName + "_from_packing"));
    }

    private void addOreChains(RecipeOutput exporter) {

        // basic smelting for nickel + platinum
        oreSmelting(exporter, List.of(ItemContent.RAW_NICKEL), RecipeCategory.MISC, ItemContent.NICKEL_INGOT, 1f, 200, "nickelsmelting");
        oreSmelting(exporter, List.of(ItemContent.RAW_PLATINUM), RecipeCategory.MISC, ItemContent.PLATINUM_INGOT, 1f, 200, "platinumsmelting");
        oreBlasting(exporter, List.of(ItemContent.RAW_NICKEL), RecipeCategory.MISC, ItemContent.NICKEL_INGOT, 1f, 100, "nickelblasting");
        oreBlasting(exporter, List.of(ItemContent.RAW_PLATINUM), RecipeCategory.MISC, ItemContent.PLATINUM_INGOT, 1f, 100, "platinumblasting");

        // iron chain
        new MetalProcessingChainBuilder("iron", registries)
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
        new MetalProcessingChainBuilder("copper", registries)
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
        new MetalProcessingChainBuilder("gold", registries)
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
        new MetalProcessingChainBuilder("nickel", registries)
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
        new MetalProcessingChainBuilder("platinum", registries)
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
        new FoundryRecipeBuilder(this.registries).input(TagContent.PLATINUM_INGOTS).input(cItemTag("ingots/netherite")).result(ItemContent.DURATIUM_INGOT).export(exporter, "duratium");
        new FoundryRecipeBuilder(this.registries).input(cItemTag("ingots/gold")).input(cItemTag("dusts/redstone")).result(ItemContent.ELECTRUM_INGOT).export(exporter, "electrum");
        new FoundryRecipeBuilder(this.registries).input(cItemTag("gems/diamond")).input(TagContent.NICKEL_INGOTS).result(ItemContent.ADAMANT_INGOT).export(exporter, "adamant");
        new FoundryRecipeBuilder(this.registries).input(TagContent.NICKEL_INGOTS).input(ItemContent.FLUXITE).result(ItemContent.ENERGITE_INGOT).export(exporter, "energite");
        new FoundryRecipeBuilder(this.registries).input(cItemTag("ingots/iron")).input(TagContent.COAL_DUSTS).result(ItemContent.STEEL_INGOT).timeMultiplier(0.3333f).export(exporter, "steel");
        new AtomicForgeRecipeBuilder(this.registries).input(TagContent.PLATINUM_INGOTS).input(ItemContent.REINFORCED_CARBON_SHEET).input(ItemContent.REINFORCED_CARBON_SHEET).result(ItemContent.DURATIUM_INGOT).export(exporter, "duratium");
    }

    private void addParticleCollisions(RecipeOutput exporter) {
        // diamond from coal dust
        new ParticleCollisionRecipeBuilder(this.registries).input(TagContent.COAL_DUSTS).input(TagContent.COAL_DUSTS).result(Items.DIAMOND).time(500).export(exporter, "diamond");
        // overcharged crystal from fluxite and energite dust
        new ParticleCollisionRecipeBuilder(this.registries).input(ItemContent.FLUXITE).input(ItemContent.ENERGITE_DUST).result(ItemContent.OVERCHARGED_CRYSTAL).time(5000).export(exporter, "overcharged_crystal");
        // platinum from gold dust
        new ParticleCollisionRecipeBuilder(this.registries).input(TagContent.GOLD_DUSTS).input(TagContent.GOLD_DUSTS).result(ItemContent.PLATINUM_DUST).time(500).export(exporter, "platinum_dust");
        // enderic compound from redstone and flesh
        new ParticleCollisionRecipeBuilder(this.registries).input(cItemTag("dusts/redstone")).input(Items.ROTTEN_FLESH).result(ItemContent.ENDERIC_COMPOUND).time(500).export(exporter, "enderic_compound");
        // fluxite from electrum dust and redstone
        new ParticleCollisionRecipeBuilder(this.registries).input(TagContent.ELECTRUM_DUSTS).input(cItemTag("dusts/redstone")).result(ItemContent.FLUXITE).time(1000).export(exporter, "fluxite");
        // netherite scrap from adamant dust and netherrack
        new ParticleCollisionRecipeBuilder(this.registries).input(ItemContent.ADAMANT_DUST).input(Items.NETHERRACK).result(Items.NETHERITE_SCRAP).time(2500).export(exporter, "netherite");
        // elytra from feather and saddle
        new ParticleCollisionRecipeBuilder(this.registries).input(cItemTag("feathers")).input(Items.SADDLE).result(Items.ELYTRA).time(10000).export(exporter, "elytra");
        // nether star from overcharged crystal and netherite
        new ParticleCollisionRecipeBuilder(this.registries).input(ItemContent.OVERCHARGED_CRYSTAL).input(cItemTag("ingots/netherite")).result(Items.NETHER_STAR).time(15000).export(exporter, "nether_star");
        // echo shard from ender pearl and amethyst shard
        new ParticleCollisionRecipeBuilder(this.registries).input(cItemTag("ender_pearls")).input(cItemTag("gems/amethyst")).result(Items.ECHO_SHARD).time(1000).export(exporter, "echo_shard");
        // heavy core from reinforced deepslate block and duration dust
        new ParticleCollisionRecipeBuilder(this.registries).input(Items.REINFORCED_DEEPSLATE).input(ItemContent.DURATIUM_DUST).result(Items.HEAVY_CORE).time(8000).export(exporter, "heavy_core");
    }

    private void addDusts(RecipeOutput exporter) {
        addDustRecipe(exporter, of(ItemContent.BIOSTEEL_INGOT), ItemContent.BIOSTEEL_DUST, ItemContent.BIOSTEEL_INGOT, "biosteel", registries);
        addDustRecipe(exporter, of(ItemContent.DURATIUM_INGOT), ItemContent.DURATIUM_DUST, ItemContent.DURATIUM_INGOT, "duratium", registries);
        addDustRecipe(exporter, of(TagContent.ELECTRUM_INGOTS), ItemContent.ELECTRUM_DUST, ItemContent.ELECTRUM_INGOT, "electrum", registries);
        addDustRecipe(exporter, of(ItemContent.ADAMANT_INGOT), ItemContent.ADAMANT_DUST, ItemContent.ADAMANT_INGOT, "adamant", registries);
        addDustRecipe(exporter, of(ItemContent.ENERGITE_INGOT), ItemContent.ENERGITE_DUST, ItemContent.ENERGITE_INGOT, "energite", registries);
        addDustRecipe(exporter, of(TagContent.STEEL_INGOTS), ItemContent.STEEL_DUST, ItemContent.STEEL_INGOT, "steel", registries);
        addDustRecipe(exporter, of(ItemTags.COALS), ItemContent.COAL_DUST, "coal", registries);
        addDustRecipe(exporter, of(cItemTag("gems/quartz")), ItemContent.QUARTZ_DUST, "quartz", registries);

        // raw ores without processing chains
        // coal
        new GrinderRecipeBuilder(this.registries).input(ItemTags.COAL_ORES).result(Items.COAL, 3).export(exporter, "coalore");
        new PulverizerRecipeBuilder(this.registries).input(ItemTags.COAL_ORES).result(Items.COAL, 2).export(exporter, "coalore");
        // redstone
        new GrinderRecipeBuilder(this.registries).input(ItemTags.REDSTONE_ORES).result(Items.REDSTONE, 12).export(exporter, "redstoneore");
        new PulverizerRecipeBuilder(this.registries).input(ItemTags.REDSTONE_ORES).result(Items.REDSTONE, 8).export(exporter, "redstoneore");
        // diamond
        new GrinderRecipeBuilder(this.registries).input(ItemTags.DIAMOND_ORES).result(Items.DIAMOND, 2).export(exporter, "diamondore");
        new PulverizerRecipeBuilder(this.registries).input(ItemTags.DIAMOND_ORES).result(Items.DIAMOND).export(exporter, "diamondore");
        // quartz
        new GrinderRecipeBuilder(this.registries).input(Blocks.NETHER_QUARTZ_ORE).result(Items.QUARTZ, 3).export(exporter, "quartzore");
        new PulverizerRecipeBuilder(this.registries).input(Blocks.NETHER_QUARTZ_ORE).result(Items.QUARTZ, 2).export(exporter, "quartzore");
        // glowstone
        new GrinderRecipeBuilder(this.registries).input(Blocks.GLOWSTONE).result(Items.GLOWSTONE_DUST, 4).export(exporter, "glowstoneore");
        new PulverizerRecipeBuilder(this.registries).input(Blocks.GLOWSTONE).result(Items.GLOWSTONE_DUST, 3).export(exporter, "glowstoneore");
        // lapis
        new GrinderRecipeBuilder(this.registries).input(ItemTags.LAPIS_ORES).result(Items.LAPIS_LAZULI, 8).export(exporter, "lapisore");
        new PulverizerRecipeBuilder(this.registries).input(ItemTags.LAPIS_ORES).result(Items.LAPIS_LAZULI, 6).export(exporter, "lapisore");
        // bone
        new GrinderRecipeBuilder(this.registries).input(Items.BONE).result(Items.BONE_MEAL, 8).export(exporter, "bone");
        new PulverizerRecipeBuilder(this.registries).input(Items.BONE).result(Items.BONE_MEAL, 6).export(exporter, "bone");
        // blaze powder
        new GrinderRecipeBuilder(this.registries).input(Items.BLAZE_ROD).result(Items.BLAZE_POWDER, 4).export(exporter, "blaze");
        new PulverizerRecipeBuilder(this.registries).input(Items.BLAZE_ROD).result(Items.BLAZE_POWDER, 3).export(exporter, "blaze");
        // wool
        new GrinderRecipeBuilder(this.registries).input(ItemTags.WOOL).result(Items.STRING, 4).export(exporter, "string");
        new PulverizerRecipeBuilder(this.registries).input(ItemTags.WOOL).result(Items.STRING, 3).export(exporter, "string");
        // ancient debris
        new GrinderRecipeBuilder(this.registries).input(Items.ANCIENT_DEBRIS).result(Items.NETHERITE_SCRAP, 2).export(exporter, "netheritescrap");
    }

    private void addUraniumProcessing(RecipeOutput exporter) {
        // uranium order is:
        // raw ore -> dust/gem, dust -> gem, gem -> pellets

        // plutonium can be made via either ender laser on crystals (manually, usually low amount)
        // or via the particle accelerator

        // small uranium dust from redstone
        new CentrifugeRecipeBuilder(this.registries).input(cItemTag("dusts/redstone")).result(ItemContent.SMALL_URANIUM_DUST).export(exporter, "redstoneuran");

        // uranium ore blocks
        new GrinderRecipeBuilder(this.registries).input(BlockContent.DEEPSLATE_URANIUM_ORE).result(ItemContent.RAW_URANIUM, 3).result(ItemContent.SMALL_PLUTONIUM_DUST).export(exporter, "uraniumore");
        new PulverizerRecipeBuilder(this.registries).input(BlockContent.DEEPSLATE_URANIUM_ORE).result(ItemContent.RAW_URANIUM, 2).export(exporter, "uraniumore");

        // uranium crystal blocks
        new GrinderRecipeBuilder(this.registries).input(BlockContent.URANITE_CRYSTAL).result(ItemContent.RAW_URANIUM, 5).result(ItemContent.SMALL_PLUTONIUM_DUST).export(exporter, "uraniumcrystal");
        new PulverizerRecipeBuilder(this.registries).input(BlockContent.URANITE_CRYSTAL).result(ItemContent.RAW_URANIUM, 4).export(exporter, "uraniumcrystal");

        // raw uranium in grinder
        new GrinderRecipeBuilder(this.registries).input(TagContent.URANIUM_RAW_MATERIALS).result(ItemContent.URANIUM_DUST, 2).result(ItemContent.SMALL_PLUTONIUM_DUST).export(exporter, "uranium");
        new PulverizerRecipeBuilder(this.registries).input(TagContent.URANIUM_RAW_MATERIALS).result(ItemContent.URANIUM_DUST, 2).export(exporter, "uranium");

        // uranium gem from raw uranium / uranium dust in atomic forge
        new AtomicForgeRecipeBuilder(this.registries).input(TagContent.COPPER_DUSTS).input(TagContent.URANIUM_RAW_MATERIALS).input(TagContent.URANIUM_RAW_MATERIALS).result(ItemContent.URANIUM_GEM).time(5).export(exporter, "urandust");
        new AtomicForgeRecipeBuilder(this.registries).input(TagContent.COPPER_DUSTS).input(TagContent.URANIUM_DUSTS).input(TagContent.URANIUM_DUSTS).result(ItemContent.URANIUM_GEM).time(5).export(exporter, "urandustgem");

        // uranium pellets in assembler
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.URANIUM_GEM).input(ItemContent.URANIUM_GEM).input(TagContent.PLASTIC_PLATES).input(TagContent.NICKEL_INGOTS).result(ItemContent.URANIUM_PELLET, 2).timeMultiplier(0.8f).export(exporter, "uranpelletbasic");
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.URANIUM_GEM).input(ItemContent.URANIUM_GEM).input(TagContent.PLASTIC_PLATES).input(ItemContent.ADAMANT_INGOT).result(ItemContent.URANIUM_PELLET, 3).timeMultiplier(0.8f).export(exporter, "uranpelletbetter");
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.URANIUM_GEM).input(ItemContent.URANIUM_GEM).input(TagContent.PLASTIC_PLATES).input(ItemContent.DURATIUM_INGOT).result(ItemContent.URANIUM_PELLET, 4).timeMultiplier(0.8f).export(exporter, "uranpelletult");

        // plutonium pellets in assembler
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.PLUTONIUM_DUST).input(ItemContent.PLUTONIUM_DUST).input(TagContent.PLASTIC_PLATES).input(TagContent.NICKEL_INGOTS).result(ItemContent.PLUTONIUM_PELLET, 2).timeMultiplier(0.8f).export(exporter, "plutoniumpelletbasic");
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.PLUTONIUM_DUST).input(ItemContent.PLUTONIUM_DUST).input(TagContent.PLASTIC_PLATES).input(ItemContent.ADAMANT_INGOT).result(ItemContent.PLUTONIUM_PELLET, 3).timeMultiplier(0.8f).export(exporter, "plutoniumpelletbetter");
        new AssemblerRecipeBuilder(this.registries).input(ItemContent.PLUTONIUM_DUST).input(ItemContent.PLUTONIUM_DUST).input(TagContent.PLASTIC_PLATES).input(ItemContent.DURATIUM_INGOT).result(ItemContent.PLUTONIUM_PELLET, 4).timeMultiplier(0.8f).export(exporter, "plutoniumpelletult");

        // dust compacting
        addCompactingRecipe(exporter, ItemContent.URANIUM_DUST, ItemContent.SMALL_URANIUM_DUST, of(ItemContent.SMALL_URANIUM_DUST), of(TagContent.URANIUM_DUSTS));
        addCompactingRecipe(exporter, ItemContent.PLUTONIUM_DUST, ItemContent.SMALL_PLUTONIUM_DUST, of(ItemContent.SMALL_PLUTONIUM_DUST), of(TagContent.PLUTONIUM_DUSTS));

        // uranium to plutonium
        new ParticleCollisionRecipeBuilder(this.registries).input(TagContent.URANIUM_DUSTS).input(ItemContent.FLUXITE).result(ItemContent.PLUTONIUM_DUST).time(2500).export(exporter, "plutonium");

        // pellet compacting
        addCompactingRecipe(exporter, ItemContent.URANIUM_PELLET, ItemContent.SMALL_URANIUM_PELLET, of(ItemContent.SMALL_URANIUM_PELLET), of(ItemContent.URANIUM_PELLET));
        addCompactingRecipe(exporter, ItemContent.PLUTONIUM_PELLET, ItemContent.SMALL_PLUTONIUM_PELLET, of(ItemContent.SMALL_PLUTONIUM_PELLET), of(ItemContent.PLUTONIUM_PELLET));
    }

    private void addPaintRecipes(RecipeOutput exporter) {

        offerPaintRecipe(exporter, ItemContent.DIAMOND_PAINT.get(), of(ItemContent.ADAMANT_DUST), of(Items.CYAN_DYE), of(TagContent.PLASTIC_PLATES), "_diamondpaint");
        offerPaintRecipe(exporter, ItemContent.CAMO_PAINT.get(), of(TagContent.BIOMASS), of(Items.GREEN_DYE), of(TagContent.PLASTIC_PLATES), "_camopaint");
        offerPaintRecipe(exporter, ItemContent.REDSTONE_PAINT.get(), of(Items.REDSTONE), of(Items.RED_DYE), of(TagContent.PLASTIC_PLATES), "_redstonepaint");
        offerPaintRecipe(exporter, ItemContent.ORANGE_PAINT.get(), of(TagContent.ELECTRUM_DUSTS), of(TagContent.COPPER_DUSTS), of(TagContent.PLASTIC_PLATES), "_orangepaint");
        offerPaintRecipe(exporter, ItemContent.WHITE_PAINT.get(), of(TagContent.QUARTZ_DUSTS), of(Items.WHITE_DYE), of(TagContent.PLASTIC_PLATES), "_whitepaint");
        offerPaintRecipe(exporter, ItemContent.FLUXITE_PAINT.get(), of(ItemContent.FLUXITE), of(Items.MAGENTA_DYE), of(TagContent.PLASTIC_PLATES), "_fluxitepaint");
        offerPaintRecipe(exporter, ItemContent.NETHERITE_PAINT.get(), of(ItemContent.CARBON_FIBRE_STRANDS), of(Items.NETHERITE_INGOT), of(TagContent.PLASTIC_PLATES), "_netheritepaint");
        offerPaintRecipe(exporter, ItemContent.SCULK_PAINT.get(), of(ItemContent.ENDERIC_COMPOUND), of(Items.SCULK), of(TagContent.PLASTIC_PLATES), "_sculkpaint");
        offerPaintRecipe(exporter, ItemContent.INDUSTRIAL_PAINT.get(), of(BlockContent.INDUSTRIAL_GLASS), of(Items.YELLOW_DYE), of(TagContent.PLASTIC_PLATES), "_industrialpaint");

    }

    private void addReactorBlocks(RecipeOutput exporter) {

        // single rod
        offerRodRecipe(exporter, BlockContent.REACTOR_ROD.asItem(), of(TagContent.PLASTIC_PLATES), of(ItemContent.ENERGITE_INGOT), "singlerod");
        // dual rod
        offerRodCombinationRecipe(exporter, BlockContent.REACTOR_DOUBLE_ROD.asItem(), of(BlockContent.REACTOR_NEUTRON_REFLECTOR), of(BlockContent.REACTOR_ROD), "doublerod");
        // quad rod
        offerRodCombinationRecipe(exporter, BlockContent.REACTOR_QUAD_ROD.asItem(), of(BlockContent.REACTOR_NEUTRON_REFLECTOR), of(BlockContent.REACTOR_DOUBLE_ROD), "quadrod");

        // reactor plating: steel and machine plating in crafting table / assembler
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.REACTOR_WALL.asItem(), of(TagContent.PLATING_BLOCKS), of(TagContent.STEEL_INGOTS), of(TagContent.NICKEL_INGOTS), 4, "reactorplatingcrafting");
        new AssemblerRecipeBuilder(this.registries).input(TagContent.PLATING_BLOCKS).input(TagContent.PLATING_BLOCKS).input(TagContent.STEEL_INGOTS).input(TagContent.NICKEL_INGOTS).result(BlockContent.REACTOR_WALL.asItem(), 3).timeMultiplier(0.8f).export(exporter, "reactorplatingalt");

        // neutron reflectors: expensive, needs duratium core, adamant frame and reactor walls
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.REACTOR_NEUTRON_REFLECTOR.asItem(), of(BlockContent.REACTOR_WALL), of(ItemContent.ADAMANT_INGOT), of(ItemContent.DURATIUM_INGOT), 1, "reflector");

        // reactor controller: reactor wall, processing unit
        offerRodCombinationRecipe(exporter, BlockContent.NUCLEAR_REACTOR_CONTROLLER.asItem(), of(BlockContent.REACTOR_WALL), of(ItemContent.PROCESSING_UNIT), "controller");

        // reactor energy port: reactor wall, storage unit, electrum
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_ENERGY_PORT.asItem(), of(TagContent.ELECTRUM_INGOTS), of(BlockContent.ENERGY_PIPE), of(BlockContent.REACTOR_WALL), of(cItemTag("ingots/iron")), "energyport");

        // reactor redstone port: wall, processing unit, repeater, torch
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_REDSTONE_PORT.asItem(), of(ItemContent.PROCESSING_UNIT), of(Items.REPEATER), of(BlockContent.REACTOR_WALL), of(Items.REDSTONE_TORCH), "redstoneport");

        // reactor fuel port: wall, hopper, motor, item pipe
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_FUEL_PORT.asItem(), of(BlockContent.ITEM_PIPE), of(Items.HOPPER), of(BlockContent.REACTOR_WALL), of(cItemTag("chests")), "fuelport");

        // reactor absorber port: wall, ice, motor, item pipe
        offerParticleMotorRecipe(exporter, BlockContent.REACTOR_COOLANT_ABSORBER_PORT.asItem(), of(BlockContent.ITEM_PIPE), of(Items.HOPPER), of(BlockContent.REACTOR_WALL), of(Blocks.ICE), "absorberport");

        // reactor absorber : wall, steel, ice
        offerBatteryRecipe(exporter, BlockContent.REACTOR_HEAT_ABSORBER.asItem(), of(Items.ICE), of(cItemTag("glass_blocks")), of(TagContent.STEEL_INGOTS), "condenser");

        // reactor vent: motor, carbon fibre
        offerStarRecipe(exporter, BlockContent.REACTOR_HEAT_VENT.asItem(), of(ItemContent.MOTOR), of(TagContent.CARBON_FIBRE), "reactorvent");

        // reactor heat pipe: electrum, gold
        offerStarRecipe(exporter, BlockContent.REACTOR_HEAT_PIPE.asItem(), of(TagContent.ELECTRUM_INGOTS), of(cItemTag("ingots/gold")), "reactorheatpipe");

        // explosives
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.LOW_YIELD_NUCLEAR_EXPLOSION_DEVICE.asItem(), of(ItemContent.DUBIOS_CONTAINER), of(ItemContent.URANIUM_PELLET), of(Items.TNT), 1, "manhattan_module");
        offerCopperReinforcedPlatingRecipe(exporter, BlockContent.MANHATTAN_MODULE.asItem(), of(ItemContent.HEISENBERG_COMPENSATOR), of(ItemContent.PLUTONIUM_PELLET), of(Items.TNT), 1, "manhattan_modulebetter");
    }

    private void addReactorFuels(RecipeOutput exporter) {
        new ReactorGeneratorRecipeBuilder(this.registries).input(ItemContent.SMALL_URANIUM_PELLET).time(400).export(exporter, "smallpellet");
        new ReactorGeneratorRecipeBuilder(this.registries).input(ItemContent.URANIUM_PELLET).time(4000).export(exporter, "pellet");
        new ReactorGeneratorRecipeBuilder(this.registries).input(ItemContent.SMALL_PLUTONIUM_PELLET).time(4000).export(exporter, "smallplutoniumpellet");
        new ReactorGeneratorRecipeBuilder(this.registries).input(ItemContent.PLUTONIUM_PELLET).time(40000).export(exporter, "plutoniumpellet");
    }

    private void addLaserTransformations(RecipeOutput exporter) {
        new LaserRecipeBuilder(this.registries).input(Items.AMETHYST_CLUSTER).result(ItemContent.FLUXITE.get()).export(exporter, "fluxite");
        new LaserRecipeBuilder(this.registries).input(BlockContent.URANITE_CRYSTAL).result(ItemContent.PLUTONIUM_DUST.get()).export(exporter, "plutoniumdust");
    }

    private void addCompactingRecipe(RecipeOutput exporter, ItemLike resBlock, ItemLike resItem, Ingredient itemIng, Ingredient blockIng) {
        this
                .shapeless(RecipeCategory.MISC, resItem, 9)
                .requires(blockIng)
                .unlockedBy(getHasName(resBlock), has(resBlock))
                .save(exporter, recipeKey(RecipeProvider.getSimpleRecipeName(resBlock) + "blockinv"));
        this
                .shaped(RecipeCategory.MISC, resBlock)
                .define('#', itemIng)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy(getHasName(resItem), has(resItem))
                .save(exporter, recipeKey(RecipeProvider.getSimpleRecipeName(resBlock) + "block"));
    }

    // crafting shapes
    public void offerCableRecipe(RecipeOutput exporter, ItemStackTemplate output, Ingredient input, String suffix) {
        var item = output.item().value();
        createCableRecipe(RecipeCategory.MISC, item, output.count(), input).unlockedBy(getHasName(item), has(item)).save(exporter, recipeKey("crafting/" + suffix));
    }

    public void offerInsulatedCableRecipe(RecipeOutput exporter, ItemStackTemplate output, Ingredient input, Ingredient insulation, String suffix) {
        var item = output.item().value();
        createInsulatedCableRecipe(RecipeCategory.MISC, item, output.count(), input, insulation).unlockedBy(getHasName(item), has(item)).save(exporter);
    }

    public void offerRotatedCableRecipe(RecipeOutput exporter, ItemStackTemplate output, Ingredient input, Ingredient insulation, String suffix) {
        var item = output.item().value();
        createRotatedCableRecipe(RecipeCategory.MISC, item, output.count(), input, insulation).unlockedBy(getHasName(item), has(item)).save(exporter);
    }

    public void offerFramedCableRecipe(RecipeOutput exporter, ItemStackTemplate output, Ingredient input, String suffix) {
        var item = output.item().value();
        createFramedCableRecipe(RecipeCategory.MISC, item, output.count(), input).unlockedBy(getHasName(item), has(item)).save(exporter);
    }

    public void offerCableFromFrameRecipe(RecipeOutput exporter, ItemStackTemplate output, Ingredient frame, String suffix) {
        var item = output.item().value();
        this.shapeless(RecipeCategory.MISC, item, output.count()).requires(frame).unlockedBy(getHasName(item), has(item)).save(exporter, recipeKey("crafting/unframe_" + suffix));
    }

    public void offerCableDuctRecipe(RecipeOutput exporter, ItemStackTemplate output, Ingredient input, String suffix) {
        var item = output.item().value();
        createCableDuctRecipe(RecipeCategory.MISC, item, output.count(), input).unlockedBy(getHasName(item), has(item)).save(exporter);
    }

    public void offerCableFromDuctRecipe(RecipeOutput exporter, ItemStackTemplate output, Ingredient duct, String suffix) {
        var item = output.item().value();
        this.shapeless(RecipeCategory.MISC, output).requires(duct).unlockedBy("has_" + suffix, this.has(item)).save(exporter, recipeKey("crafting/unduct_" + suffix));
    }

    public RecipeBuilder createCableRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return this.shaped(category, output, count).define('#', input).pattern("   ").pattern("###");
    }

    public RecipeBuilder createFramedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return this.shaped(category, output, count).define('c', input).define('p', Ingredient.of(registries.get(TagContent.PLATING_BLOCKS).orElseThrow())).pattern("ccc").pattern("cpc").pattern("ccc");
    }

    public RecipeBuilder createCableDuctRecipe(RecipeCategory category, Item output, int count, Ingredient input) {
        return this.shaped(category, output, count).define('c', input).define('p', Ingredient.of(registries.get(TagContent.PLATING_BLOCKS).orElseThrow())).define('s', of(Blocks.STONE)).pattern("csc").pattern("sps").pattern("csc");
    }

    public void offerMotorRecipe(RecipeOutput exporter, Item output, Ingredient shaft, Ingredient core, Ingredient wall, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('s', shaft).define('c', core).define('w', wall).pattern(" s ").pattern("wcw").pattern("wcw");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerManualAlloyRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        offerManualAlloyRecipe(exporter, output, A, B, 1, suffix);
    }

    public void offerManualAlloyRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, int count, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, count).define('a', A).define('b', B).pattern("aa ").pattern("bb ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerGeneratorRecipe(RecipeOutput exporter, Item output, Ingredient base, Ingredient sides, Ingredient core, Ingredient frame, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('s', sides).define('c', core).define('f', frame).define('b', base)
                .pattern("fff")
                .pattern("fcf")
                .pattern("sbs");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, recipeKey("crafting/" + suffix));
    }

    public void offerFurnaceRecipe(RecipeOutput exporter, Item output, Ingredient bottom, Ingredient botSides, Ingredient middleSides, Ingredient core, Ingredient top, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('s', botSides).define('c', core).define('f', top).define('b', bottom).define('m', middleSides)
                .pattern("fff")
                .pattern("mcm")
                .pattern("sbs");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, recipeKey("crafting/" + suffix));
    }

    public void offerEnergyTransmissionPoleRecipe(RecipeOutput exporter, Item output, Ingredient coil, Ingredient sides, Ingredient inner, Ingredient base, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('c', coil).define('s', sides).define('i', inner).define('b', base)
                .pattern("c c")
                .pattern("sis")
                .pattern("bbb");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerAtomicForgeRecipe(RecipeOutput exporter, Item output, Ingredient base, Ingredient middleSides, Ingredient core, Ingredient top, Ingredient frame, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('s', top).define('c', core).define('f', frame).define('b', base).define('m', middleSides)
                .pattern("fsf")
                .pattern("mcm")
                .pattern("bbb");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, recipeKey("crafting/" + suffix));
    }

    public void offerBatteryRecipe(RecipeOutput exporter, Item output, Ingredient inner, Ingredient sides, Ingredient top, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('t', top).define('c', inner).define('f', sides)
                .pattern(" t ")
                .pattern("fcf")
                .pattern("fcf");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerMachineFrameRecipe(RecipeOutput exporter, Item output, Ingredient base, Ingredient alt, int count, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, count).define('s', base).define('c', alt)
                .pattern(" s ")
                .pattern("csc")
                .pattern(" s ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerMachineCoreRecipe(RecipeOutput exporter, Item output, Ingredient base, Ingredient alt, String suffix) {
        offerMachineCoreRecipe(exporter, output, 1, base, alt, suffix);
    }

    public void offerMachineCoreRecipe(RecipeOutput exporter, Item output, int count, Ingredient base, Ingredient alt, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, count).define('s', base).define('c', alt)
                .pattern("sss")
                .pattern("scs")
                .pattern("sss");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, recipeKey("crafting/" + suffix));
    }

    public void offerManualFluidApplication(RecipeOutput exporter, Item output, Ingredient fluid, Ingredient base, String suffix) {
        offerManualFluidApplication(exporter, output, 1, fluid, base, suffix);
    }

    public void offerManualFluidApplication(RecipeOutput exporter, Item output, int count, Ingredient fluid, Ingredient base, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, count).define('f', fluid).define('b', base)
                .pattern("bb ")
                .pattern("bf ")
                .pattern("   ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerPaintRecipe(RecipeOutput exporter, Item output, Ingredient base, Ingredient sides, Ingredient plate, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 4).define('s', sides).define('p', plate).define('b', base)
                .pattern(" s ")
                .pattern("pbp")
                .pattern(" s ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerBeadsRecipe(RecipeOutput exporter, Item output, int count, Ingredient fluid, Ingredient base, Ingredient catalyst, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, count).define('f', fluid).define('b', base).define('c', catalyst)
                .pattern("bb ")
                .pattern("cf ")
                .pattern("   ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerDrillRecipe(RecipeOutput exporter, Item output, Ingredient doubleBase, Ingredient motor, Ingredient outer, Ingredient head, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('s', doubleBase).define('m', motor).define('a', outer).define('e', head)
                .pattern(" a ")
                .pattern("aea")
                .pattern("mss");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerWrenchRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('a', A).define('b', B)
                .pattern(" a ")
                .pattern(" ba")
                .pattern("a  ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerChainsawRecipe(RecipeOutput exporter, Item output, Ingredient core, Ingredient motor, Ingredient center, Ingredient head, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('s', core).define('m', motor).define('a', center).define('e', head)
                .pattern("aa ")
                .pattern("ae ")
                .pattern("mss");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerAxeRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                .pattern("pp ")
                .pattern("pc ")
                .pattern(" c ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerPickaxeRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                .pattern("ppp")
                .pattern(" c ")
                .pattern(" c ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerHelmetRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                .pattern("ppp")
                .pattern("pcp")
                .pattern("   ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerChestplateRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                .pattern("p p")
                .pattern("ppp")
                .pattern("pcp");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerLegsRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                .pattern("ppp")
                .pattern("pcp")
                .pattern("p p");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerFeetRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('p', plating).define('c', core)
                .pattern("   ")
                .pattern("p p")
                .pattern("c c");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerRodRecipe(RecipeOutput exporter, Item output, Ingredient cap, Ingredient rod, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('c', cap).define('r', rod)
                .pattern(" c ")
                .pattern(" r ")
                .pattern(" r ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerRodCombinationRecipe(RecipeOutput exporter, Item output, Ingredient cap, Ingredient rod, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('c', cap).define('r', rod)
                .pattern("   ")
                .pattern("rcr")
                .pattern("   ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerStarRecipe(RecipeOutput exporter, Item output, Ingredient inner, Ingredient outer, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('c', inner).define('o', outer)
                .pattern(" o ")
                .pattern("oco")
                .pattern(" o ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerTankRecipe(RecipeOutput exporter, Item output, Ingredient plating, Ingredient core, Ingredient sides, String suffix) {
        offerTankRecipe(exporter, output, 1, plating, core, sides, suffix);
    }

    public void offerTankRecipe(RecipeOutput exporter, Item output, int count, Ingredient plating, Ingredient core, Ingredient sides, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, count).define('p', plating).define('s', sides).define('c', core)
                .pattern("ppp")
                .pattern("scs")
                .pattern("ppp");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerTwoComponentRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('a', A).define('b', B)
                .pattern("ab ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerLeverRecipe(RecipeOutput exporter, Item output, Ingredient A, Ingredient B, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('a', A).define('b', B)
                .pattern("a  ")
                .pattern("b  ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerParticleMotorRecipe(RecipeOutput exporter, Item output, Ingredient rail, Ingredient top, Ingredient baseInner, Ingredient baseOuter, String suffix) {
        var builder = this.shaped(RecipeCategory.MISC, output, 1).define('r', rail).define('t', top).define('i', baseInner).define('o', baseOuter)
                .pattern(" t ")
                .pattern("rrr")
                .pattern("oio");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter, recipeKey("crafting/" + suffix));
    }

    public void offerCopperReinforcedPlatingRecipe(RecipeOutput exporter, Item output, Ingredient side, Ingredient edge, Ingredient core, int count, String suffix) {
        var builder = this.shaped(RecipeCategory.REDSTONE, output, count).define('a', side).define('e', edge).define('c', core)
                .pattern("eae")
                .pattern("aca")
                .pattern("eae");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerDoorRecipe(RecipeOutput exporter, Item output, Ingredient A, String suffix) {
        var builder = this.shaped(RecipeCategory.REDSTONE, output, 1).define('a', A)
                .pattern("aa ")
                .pattern("aa ")
                .pattern("aa ");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerSlabRecipe(RecipeOutput exporter, Item output, Ingredient A, String suffix) {
        var builder = this.shaped(RecipeCategory.BUILDING_BLOCKS, output, 6).define('a', A)
                .pattern("aaa");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerStairsRecipe(RecipeOutput exporter, Item output, Ingredient A, String suffix) {
        var builder = this.shaped(RecipeCategory.BUILDING_BLOCKS, output, 4).define('a', A)
                .pattern("a  ")
                .pattern("aa ")
                .pattern("aaa");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    public void offerPressurePlateRecipe(RecipeOutput exporter, Item output, Ingredient A, String suffix) {
        var builder = this.shaped(RecipeCategory.REDSTONE, output, 1).define('a', A)
                .pattern("aa");
        builder.unlockedBy(getHasName(output), has(output)).save(exporter);
    }

    private Ingredient of(TagKey<Item> item) {
        return Ingredient.of(this.registries.get(item).orElseThrow());
    }

    public static Ingredient of(ItemLike item) {
        return Ingredient.of(item);
    }

}
