package rearth.oritech.neoforgegen.datagen.compat;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;
import static rearth.oritech.util.TagUtils.cItemTag;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.data.recipe.CrushingRecipeGen;
import com.simibubi.create.foundation.data.recipe.MixingRecipeGen;
import com.simibubi.create.foundation.data.recipe.WashingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.api.recipe.GrinderRecipeBuilder;
import rearth.oritech.api.recipe.util.MetalProcessingChainBuilder;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

public class CreateRecipeGenerator {
    private static final String PATH = "compat/create/";

    public static void generateRecipes(IConditionBuilder conditionBuilder, PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, RecipeOutput exporter) { 
        addAlloying(exporter);
        addBlasting(exporter);
        addCentrifuging(exporter);
        addMetalProcessing(conditionBuilder, exporter);

        CreateCrushingRecipeGen.registerAll(packOutput, registries, exporter);
        CreateMixingRecipeGen.registerAll(packOutput, registries, exporter);
        CreateWashingRecipeGen.registerAll(packOutput, registries, exporter);
    }

    private static void addAlloying(RecipeOutput exporter) {
        FoundryRecipeBuilder.build().input(Tags.Items.INGOTS_COPPER).input(AllItems.ZINC_INGOT.asItem()).result(AllItems.BRASS_INGOT.asItem(), 2).export(exporter, "compat/create/brass");
    }

    // Create lets you blast other crushed ores, so add that for Oritech ores
    private static void addBlasting(RecipeOutput exporter) {
        offerBlasting(exporter, AllItems.CRUSHED_NICKEL.asItem(), ItemContent.NICKEL_INGOT, 1f, 100, "crushed_nickel_to_nickel_ingot");
        offerBlasting(exporter, AllItems.CRUSHED_PLATINUM.asItem(), ItemContent.PLATINUM_INGOT, 1f, 100, "crushed_platinum_to_platinum_ingot");
    }

    private static void offerBlasting(RecipeOutput exporter, Item input, Item result, float xp, int cookTime, String suffix) {
        SimpleCookingRecipeBuilder.blasting(of(input), RecipeCategory.MISC, result, xp, cookTime)
            .unlockedBy(RecipeProvider.getHasName(input), RecipeProvider.has(input))
            .save(exporter, ResourceLocation.fromNamespaceAndPath(Oritech.MOD_ID, "blasting/" + PATH + suffix));
    }

    private static void addCentrifuging(RecipeOutput exporter) {
        CentrifugeFluidRecipeBuilder.build().input(AllItems.WHEAT_FLOUR.asItem()).result(AllItems.DOUGH.asItem()).fluidInput(Fluids.WATER).export(exporter, PATH + "dough");
    }

    private static void addMetalProcessing(IConditionBuilder conditionBuilder, RecipeOutput exporter) {
        MetalProcessingChainBuilder.build("zinc").resourcePath(PATH)
            .ore(cItemTag("ores/zinc"))
            .rawOre(cItemTag("raw_materials/zinc"), AllItems.RAW_ZINC.asItem()).rawOreByproduct(Items.GUNPOWDER)
            .ingot(cItemTag("ingots/zinc"), AllItems.ZINC_INGOT.asItem())
            .nugget(cItemTag("nuggets/zinc"), AllItems.ZINC_NUGGET.asItem())
            .clump(cItemTag("clumps/zinc"), AllItems.CRUSHED_ZINC.asItem()).clumpByproduct(Items.GUNPOWDER).byproductAmount(1)
            .centrifugeResult(AllItems.ZINC_NUGGET.asItem(), 9)
            .export(exporter.withConditions(conditionBuilder.not(conditionBuilder.modLoaded("jaopca"))));
    }

    private static class CreateCrushingRecipeGen extends CrushingRecipeGen {
        public CreateCrushingRecipeGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        public List<GeneratedRecipe> all = List.of(
            create(Oritech.id("compat/create/quartz"), b -> b.require(Items.QUARTZ).output(ItemContent.QUARTZ_DUST)),
            create(Oritech.id("compat/create/coal"), b -> b.require(ItemTags.COALS).output(ItemContent.COAL_DUST)),
            create(Oritech.id("compat/create/copperingot"), b -> b.require(Tags.Items.INGOTS_COPPER).output(ItemContent.COPPER_DUST)),
            create(Oritech.id("compat/create/ironingot"), b -> b.require(Tags.Items.INGOTS_IRON).output(ItemContent.IRON_DUST)),
            create(Oritech.id("compat/create/goldingot"), b -> b.require(Tags.Items.INGOTS_GOLD).output(ItemContent.GOLD_DUST)),
            create(Oritech.id("compat/create/nickelingot"), b -> b.require(TagContent.NICKEL_INGOTS).output(ItemContent.NICKEL_DUST)),
            create(Oritech.id("compat/create/platinumingot"), b -> b.require(TagContent.PLATINUM_INGOTS).output(ItemContent.PLATINUM_DUST)));

        public static void registerAll(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, RecipeOutput exporter) {
            var crushing = new CreateCrushingRecipeGen(packOutput, registries);
            crushing.all.forEach(recipe -> recipe.register(exporter));
        }
    }

    private static class CreateMixingRecipeGen extends MixingRecipeGen {
        public CreateMixingRecipeGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        public List<GeneratedRecipe> all = List.of(
            create(Oritech.id("compat/create/turbofuel"), b -> b.require(ItemContent.FLUXITE).require(FluidContent.STILL_OIL.get(), 1000).output(FluidContent.STILL_FUEL.get(), 1000)),

            // Alloys
            create(Oritech.id("compat/create/adamant"), b -> b.require(Tags.Items.GEMS_DIAMOND).require(TagContent.NICKEL_INGOTS).output(ItemContent.ADAMANT_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/biosteel"), b -> b.require(ItemContent.RAW_BIOPOLYMER).require(Tags.Items.INGOTS_IRON).output(ItemContent.BIOSTEEL_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/coppergem"), b -> b.require(ItemContent.COPPER_GEM).require(ItemContent.COPPER_GEM).output(Items.COPPER_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/duratium"), b -> b.require(ItemContent.ADAMANT_INGOT).require(Tags.Items.INGOTS_NETHERITE).output(ItemContent.DURATIUM_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/electrum"), b -> b.require(Tags.Items.DUSTS_REDSTONE).require(Tags.Items.INGOTS_GOLD).output(ItemContent.ELECTRUM_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/energite"), b -> b.require(ItemContent.FLUXITE).require(TagContent.NICKEL_INGOTS).output(ItemContent.ENERGITE_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/goldgem"), b -> b.require(ItemContent.GOLD_GEM).require(ItemContent.GOLD_GEM).output(Items.GOLD_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/irongem"), b -> b.require(ItemContent.IRON_GEM).require(ItemContent.IRON_GEM).output(Items.IRON_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/nickelgem"), b -> b.require(ItemContent.NICKEL_GEM).require(ItemContent.NICKEL_GEM).output(ItemContent.NICKEL_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/platinumgem"), b -> b.require(ItemContent.PLATINUM_GEM).require(ItemContent.PLATINUM_GEM).output(ItemContent.PLATINUM_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/steel"), b -> b.require(TagContent.COAL_DUSTS).require(Tags.Items.INGOTS_IRON).output(ItemContent.STEEL_INGOT).requiresHeat(HeatCondition.HEATED)));


        public static void registerAll(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, RecipeOutput exporter) {
            var mixing = new CreateMixingRecipeGen(packOutput, registries);
            mixing.all.forEach(recipe -> recipe.register(exporter));
        }
    }

    private static class CreateWashingRecipeGen extends WashingRecipeGen {
        public CreateWashingRecipeGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        public List<GeneratedRecipe> all = List.of(
            create(Oritech.id("compat/create/copperclump"), b -> b.require(TagContent.COPPER_CLUMPS).output(AllItems.COPPER_NUGGET.asItem(), 9).output(0.5f, ItemContent.SMALL_GOLD_DUST, 2)),
            create(Oritech.id("compat/create/ironclump"), b -> b.require(TagContent.IRON_CLUMPS).output(Items.IRON_NUGGET, 9).output(0.5f, ItemContent.SMALL_NICKEL_DUST, 2)),
            create(Oritech.id("compat/create/goldclump"), b -> b.require(TagContent.GOLD_CLUMPS).output(Items.GOLD_NUGGET, 9).output(0.5f, ItemContent.SMALL_COPPER_DUST, 2)),
            create(Oritech.id("compat/create/nickelclump"), b -> b.require(TagContent.NICKEL_CLUMPS).output(ItemContent.NICKEL_NUGGET, 9).output(0.5f, ItemContent.SMALL_PLATINUM_DUST, 2)),
            create(Oritech.id("compat/create/platinumclump"), b -> b.require(TagContent.PLATINUM_CLUMPS).output(ItemContent.PLATINUM_NUGGET, 9).output(0.25f, ItemContent.FLUXITE)),
            create(Oritech.id("compat/create/uraniumclump"), b -> b.require(TagContent.URANIUM_CLUMPS).output(ItemContent.URANIUM_DUST, 2).output(0.25f, ItemContent.SMALL_PLUTONIUM_DUST)),
            create(Oritech.id("compat/create/redstone"), b -> b.require(Tags.Items.DUSTS_REDSTONE).output(ItemContent.SMALL_URANIUM_DUST)));
        
        public static void registerAll(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, RecipeOutput exporter) {
            var washing = new CreateWashingRecipeGen(packOutput, registries);
            washing.all.forEach(recipe -> recipe.register(exporter));
        }
    }
}