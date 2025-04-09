package rearth.oritech.neoforgegen.datagen.compat;

import rearth.oritech.Oritech;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.datagen.RecipeGeneratorUtil;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.common.Tags;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CreateRecipeGenerator {
    public static void generateRecipes(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, RecipeOutput exporter) { 
        addAlloying(exporter);
        addBlasting(exporter);
        addCentrifuging(exporter);
        addPulverizing(exporter);

        CreateCrushingRecipeGen.registerAll(packOutput, registries, exporter);
        CreateMixingRecipeGen.registerAll(packOutput, registries, exporter);
        CreateWashingRecipeGen.registerAll(packOutput, registries, exporter);
    }

    private static void addAlloying(RecipeOutput exporter) {
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.of(Tags.Items.INGOTS_COPPER), Ingredient.of(AllItems.ZINC_INGOT.asItem()), AllItems.BRASS_INGOT.asItem(), 2, "compat/create/brass");
    }

    private static void addBlasting(RecipeOutput exporter) {
        offerBlasting(exporter, AllItems.CRUSHED_NICKEL.asItem(), ItemContent.NICKEL_INGOT, 1f, 100, "crushed_nickel_to_nickel_ingot");
        offerBlasting(exporter, AllItems.CRUSHED_PLATINUM.asItem(), ItemContent.PLATINUM_INGOT, 1f, 100, "crushed_platinum_to_platinum_ingot");
    }

    private static void offerBlasting(RecipeOutput exporter, Item input, Item result, float xp, int cookTime, String suffix) {
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, xp, cookTime)
            .unlockedBy(RecipeProvider.getHasName(input), RecipeProvider.has(input))
            .save(exporter, ResourceLocation.fromNamespaceAndPath(Oritech.MOD_ID, "blasting/compat/create/" + suffix));
    }

    private static void addCentrifuging(RecipeOutput exporter) {
        // Centrifuge crushed ores
        RecipeGeneratorUtil.addCentrifugeRecipe(exporter, Ingredient.of(AllItems.CRUSHED_COPPER.asItem()), List.of(new ItemStack(ItemContent.COPPER_GEM), new ItemStack(ItemContent.SMALL_GOLD_DUST, 3)), 0.5f, "compat/create/crushed_copper");
        RecipeGeneratorUtil.addCentrifugeRecipe(exporter, Ingredient.of(AllItems.CRUSHED_IRON.asItem()), List.of(new ItemStack(ItemContent.GOLD_GEM), new ItemStack(ItemContent.SMALL_COPPER_DUST, 3)), 0.5f, "compat/create/crushed_iron");
        RecipeGeneratorUtil.addCentrifugeRecipe(exporter, Ingredient.of(AllItems.CRUSHED_GOLD.asItem()), List.of(new ItemStack(ItemContent.IRON_GEM), new ItemStack(ItemContent.SMALL_NICKEL_DUST, 3)), 0.5f, "compat/create/crushed_gold");
        RecipeGeneratorUtil.addCentrifugeRecipe(exporter, Ingredient.of(AllItems.CRUSHED_NICKEL.asItem()), List.of(new ItemStack(ItemContent.NICKEL_GEM), new ItemStack(ItemContent.SMALL_PLATINUM_DUST, 2)), 0.5f, "compat/create/crushed_nickel");
        RecipeGeneratorUtil.addCentrifugeRecipe(exporter, Ingredient.of(AllItems.CRUSHED_PLATINUM.asItem()), List.of(new ItemStack(ItemContent.PLATINUM_GEM), new ItemStack(ItemContent.FLUXITE)), 0.5f, "compat/create/crushed_platinum");
        RecipeGeneratorUtil.addCentrifugeRecipe(exporter, Ingredient.of(AllItems.CRUSHED_URANIUM.asItem()), List.of(new ItemStack(ItemContent.URANIUM_DUST, 2), new ItemStack(ItemContent.SMALL_PLUTONIUM_DUST)), 0.5f, "compat/create/crushed_uranium");

        // Fluid centrifuge crushed ores
        RecipeGeneratorUtil.addCentrifugeFluidRecipe(exporter, Ingredient.of(AllItems.CRUSHED_COPPER.asItem()), ItemContent.COPPER_GEM, 2, Fluids.WATER, 1, null, 0, 0.5f, "compat/create/crushed_copper");
        RecipeGeneratorUtil.addCentrifugeFluidRecipe(exporter, Ingredient.of(AllItems.CRUSHED_IRON.asItem()), ItemContent.IRON_GEM, 2, Fluids.WATER, 1, null, 0, 0.5f, "compat/create/crushed_iron");
        RecipeGeneratorUtil.addCentrifugeFluidRecipe(exporter, Ingredient.of(AllItems.CRUSHED_GOLD.asItem()), ItemContent.GOLD_GEM, 2, Fluids.WATER, 1, null, 0, 0.5f, "compat/create/crushed_gold");
        RecipeGeneratorUtil.addCentrifugeFluidRecipe(exporter, Ingredient.of(AllItems.CRUSHED_NICKEL.asItem()), ItemContent.NICKEL_GEM, 2, Fluids.WATER, 1, null, 0, 0.5f, "compat/create/crushed_nickel");
        RecipeGeneratorUtil.addCentrifugeFluidRecipe(exporter, Ingredient.of(AllItems.CRUSHED_PLATINUM.asItem()), ItemContent.PLATINUM_GEM, 2, Fluids.WATER, 1, null, 0, 0.5f, "compat/create/crushed_platinum");
        RecipeGeneratorUtil.addCentrifugeFluidRecipe(exporter, Ingredient.of(AllItems.CRUSHED_URANIUM.asItem()), ItemContent.URANIUM_DUST, 3, Fluids.WATER, 1, null, 0, 0.5f, "compat/create/crushed_uranium");
    }

    private static void addPulverizing(RecipeOutput exporter) {
        RecipeGeneratorUtil.addPulverizerRecipe(exporter, Ingredient.of(Tags.Items.CROPS_WHEAT), AllItems.WHEAT_FLOUR.asItem(), "compat/create/wheat_flour");
        RecipeGeneratorUtil.addGrinderRecipe(exporter, Ingredient.of(Tags.Items.CROPS_WHEAT), AllItems.WHEAT_FLOUR.asItem(), "compat/create/wheat_flour");
    }

    private static class CreateCrushingRecipeGen extends CrushingRecipeGen {
        public CreateCrushingRecipeGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        public List<GeneratedRecipe> all = List.of(
            create(Oritech.id("compat/create/quartz"), b -> b.require(Items.QUARTZ).output(ItemContent.QUARTZ_DUST)),
            create(Oritech.id("compat/create/copper_ingot"), b -> b.require(Tags.Items.INGOTS_COPPER).output(ItemContent.COPPER_DUST)),
            create(Oritech.id("compat/create/iron_ingot"), b -> b.require(Tags.Items.INGOTS_IRON).output(ItemContent.IRON_DUST)),
            create(Oritech.id("compat/create/gold_ingot"), b -> b.require(Tags.Items.INGOTS_GOLD).output(ItemContent.GOLD_DUST)),
            create(Oritech.id("compat/create/nickel_ingot"), b -> b.require(TagContent.NICKEL_INGOTS).output(ItemContent.NICKEL_DUST)),
            create(Oritech.id("compat/create/platinum_ingot"), b -> b.require(TagContent.PLATINUM_INGOTS).output(ItemContent.PLATINUM_DUST)));

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
            create(Oritech.id("compat/create/adamant"), b -> b.require(Items.DIAMOND).require(TagContent.NICKEL_INGOTS).output(ItemContent.ADAMANT_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/biosteel"), b -> b.require(ItemContent.RAW_BIOPOLYMER).require(Tags.Items.INGOTS_IRON).output(ItemContent.BIOSTEEL_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/copper_from_gem"), b -> b.require(ItemContent.COPPER_GEM).require(ItemContent.COPPER_GEM).output(Items.COPPER_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/duratium"), b -> b.require(ItemContent.ADAMANT_INGOT).require(Items.NETHERITE_INGOT).output(ItemContent.DURATIUM_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/electrum"), b -> b.require(Items.REDSTONE).require(Tags.Items.INGOTS_GOLD).output(ItemContent.ELECTRUM_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/energite"), b -> b.require(ItemContent.FLUXITE).require(TagContent.NICKEL_INGOTS).output(ItemContent.ENERGITE_INGOT).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/gold_from_gem"), b -> b.require(ItemContent.GOLD_GEM).require(ItemContent.GOLD_GEM).output(Items.GOLD_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/iron_from_gem"), b -> b.require(ItemContent.IRON_GEM).require(ItemContent.IRON_GEM).output(Items.IRON_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/nickel_from_gem"), b -> b.require(ItemContent.NICKEL_GEM).require(ItemContent.NICKEL_GEM).output(ItemContent.NICKEL_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
            create(Oritech.id("compat/create/platinum_from_gem"), b -> b.require(ItemContent.PLATINUM_GEM).require(ItemContent.PLATINUM_GEM).output(ItemContent.PLATINUM_INGOT, 3).requiresHeat(HeatCondition.HEATED)),
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
            create(Oritech.id("compat/create/copper_clump"), b -> b.require(ItemContent.COPPER_CLUMP).output(AllItems.COPPER_NUGGET.asItem(), 9).output(0.5f, ItemContent.SMALL_GOLD_DUST, 2)),
            create(Oritech.id("compat/create/iron_clump"), b -> b.require(ItemContent.IRON_CLUMP).output(Items.IRON_NUGGET, 9).output(0.5f, ItemContent.SMALL_NICKEL_DUST, 2)),
            create(Oritech.id("compat/create/gold_clump"), b -> b.require(ItemContent.GOLD_CLUMP).output(Items.GOLD_NUGGET, 9).output(0.5f, ItemContent.SMALL_COPPER_DUST, 2)),
            create(Oritech.id("compat/create/nickel_clump"), b -> b.require(ItemContent.NICKEL_CLUMP).output(ItemContent.NICKEL_NUGGET, 9).output(0.5f, ItemContent.SMALL_PLATINUM_DUST, 2)),
            create(Oritech.id("compat/create/platinum_clump"), b -> b.require(ItemContent.PLATINUM_CLUMP).output(ItemContent.PLATINUM_NUGGET, 9).output(0.25f, ItemContent.FLUXITE)),
            create(Oritech.id("compat/create/crushed_nickel"), b -> b.require(AllItems.CRUSHED_NICKEL.asItem()).output(ItemContent.NICKEL_NUGGET, 9).output(0.5f, ItemContent.SMALL_PLATINUM_DUST, 2)),
            create(Oritech.id("compat/create/crushed_platinum"), b -> b.require(AllItems.CRUSHED_PLATINUM.asItem()).output(ItemContent.PLATINUM_NUGGET, 9).output(0.25f, ItemContent.FLUXITE)),
            create(Oritech.id("compat/create/crushed_uranium"), b -> b.require(AllItems.CRUSHED_URANIUM.asItem()).output(ItemContent.URANIUM_DUST, 2).output(0.25f, ItemContent.SMALL_PLUTONIUM_DUST)),
            create(Oritech.id("compat/create/redstone"), b -> b.require(Items.REDSTONE).output(ItemContent.SMALL_URANIUM_DUST)));
        
        public static void registerAll(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, RecipeOutput exporter) {
            var washing = new CreateWashingRecipeGen(packOutput, registries);
            washing.all.forEach(recipe -> recipe.register(exporter));
        }
    }
}