package rearth.oritech.neoforgegen.datagen.compat;

import rearth.oritech.Oritech;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.datagen.RecipeGeneratorUtil;
import rearth.oritech.util.datagen.OreTransform;

import mekanism.api.datagen.recipe.builder.ItemStackChemicalToItemStackRecipeBuilder;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.registries.MekanismItems;
import mekanism.common.resource.PrimaryResource;
import mekanism.common.resource.ResourceType;
import mekanism.common.resource.ore.OreType;
import mekanism.common.tags.MekanismTags;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.common.Tags;

import java.util.List;

public class MekanismRecipeGenerator {
    public static void generateRecipes(RecipeOutput exporter) {
        addAlloying(exporter);
        addAtomicForging(exporter);
        addDustGrinding(exporter);
        addMetalProcessing(exporter);
        addMekInfusing(exporter);
    }

    private static void addAlloying(RecipeOutput exporter) {
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.of(Tags.Items.INGOTS_COPPER), Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.TIN)), MekanismItems.BRONZE_INGOT.asItem(), 2, "compat/mekanism/bronze");
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.of(Tags.Items.INGOTS_COPPER), Ingredient.of(Tags.Items.DUSTS_REDSTONE), MekanismItems.INFUSED_ALLOY.asItem(), 1, "compat/mekanism/infused_alloy");
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.of(MekanismTags.Items.ALLOYS_INFUSED), Ingredient.of(MekanismTags.Items.DUSTS_DIAMOND), MekanismItems.REINFORCED_ALLOY.asItem(), 1, "compat/mekanism/reinforced_alloy");
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.of(MekanismTags.Items.ALLOYS_REINFORCED), Ingredient.of(MekanismTags.Items.DUSTS_REFINED_OBSIDIAN), MekanismItems.ATOMIC_ALLOY.asItem(), 1, "compat/mekanism/atomic_alloy");
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.of(MekanismTags.Items.DUSTS_OBSIDIAN), Ingredient.of(MekanismTags.Items.DUSTS_DIAMOND), MekanismItems.REFINED_OBSIDIAN_DUST.asItem(), 1, "compat/mekanism/refined_obsidian_dust");
    }

    private static void addAtomicForging(RecipeOutput exporter) {
        RecipeGeneratorUtil.addAtomicForgeRecipe(exporter, Ingredient.of(Tags.Items.DUSTS_REDSTONE), Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.OSMIUM)), MekanismItems.BASIC_CONTROL_CIRCUIT.asItem(), 5, "compat/mekanism/basic_control_circuit");
        RecipeGeneratorUtil.addAtomicForgeRecipe(exporter, Ingredient.of(Tags.Items.DUSTS_REDSTONE), Ingredient.of(MekanismTags.Items.CIRCUITS_BASIC), MekanismItems.ADVANCED_CONTROL_CIRCUIT.asItem(), 5, "compat/mekanism/advanced_control_circuit");
        RecipeGeneratorUtil.addAtomicForgeRecipe(exporter, Ingredient.of(MekanismTags.Items.DUSTS_DIAMOND), Ingredient.of(MekanismTags.Items.CIRCUITS_ADVANCED), MekanismItems.ELITE_CONTROL_CIRCUIT.asItem(), 5, "compat/mekanism/elite_control_circuit");
        RecipeGeneratorUtil.addAtomicForgeRecipe(exporter, Ingredient.of(MekanismTags.Items.DUSTS_REFINED_OBSIDIAN), Ingredient.of(MekanismTags.Items.CIRCUITS_ELITE), MekanismItems.ULTIMATE_CONTROL_CIRCUIT.asItem(), 5, "compat/mekanism/ultimate_control_circuit");
    }

    private static void addDustGrinding(RecipeOutput exporter) {
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(TagContent.BRONZE_INGOTS), MekanismItems.BRONZE_DUST.asItem(), "compat/mekanism/dust/bronze");
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(Items.CHARCOAL), MekanismItems.CHARCOAL_DUST.asItem(), "compat/mekanism/dust/charcoal");
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(Tags.Items.GEMS_DIAMOND), MekanismItems.DIAMOND_DUST.asItem(), "compat/mekanism/dust/diamond");
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(Tags.Items.GEMS_EMERALD), MekanismItems.EMERALD_DUST.asItem(), "compat/mekanism/dust/emerald");
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(MekanismTags.Items.GEMS_FLUORITE), MekanismItems.FLUORITE_DUST.asItem(), "compat/mekanism/dust/fluorite");
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(Tags.Items.GEMS_LAPIS), MekanismItems.LAPIS_LAZULI_DUST.asItem(), "compat/mekanism/dust/lapis");
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(Tags.Items.INGOTS_NETHERITE), MekanismItems.NETHERITE_DUST.asItem(), "compat/mekanism/dust/netherite");
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(MekanismTags.Items.INGOTS_REFINED_OBSIDIAN), MekanismItems.REFINED_OBSIDIAN_DUST.asItem(), "compat/mekanism/dust/refined_obsidian");
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(TagContent.STEEL_INGOTS), MekanismItems.STEEL_DUST.asItem(), "compat/mekanism/dust/steel");
        RecipeGeneratorUtil.addDustRecipe(exporter, Ingredient.of(Tags.Items.OBSIDIANS), MekanismItems.OBSIDIAN_DUST.asItem(), "compat/mekanism/dust/obsidian");
    }
    
    private static void addMetalProcessing(RecipeOutput exporter) {
        var oreTransforms = List.of(
        // osmium
        new OreTransform(
            Ingredient.of(MekanismTags.Items.ORES.get(OreType.OSMIUM)),
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.OSMIUM)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.OSMIUM).asItem(), ItemContent.RAW_PLATINUM,
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.OSMIUM)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.OSMIUM).asItem(),
            null, null, null,
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.OSMIUM)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.OSMIUM).asItem(),
            null, null, MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.OSMIUM).asItem(),
            // Adding dust as "gemItem" to give dust as an output from the centrifuge recipes
            null, MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.OSMIUM).asItem(),
            null,
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.OSMIUM)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.OSMIUM).asItem(),
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.OSMIUM)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.OSMIUM).asItem(),
            1.5f, "compat/mekanism/osmium", 1, false),
        // tin
        new OreTransform(
            Ingredient.of(MekanismTags.Items.ORES.get(OreType.TIN)),
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.TIN)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.TIN).asItem(), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.LEAD).asItem(),
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.TIN)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.TIN).asItem(),
            null, null, null,
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.TIN)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.TIN).asItem(),
            null, null, MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.TIN).asItem(),
            // Adding dust as "gemItem" to give dust as an output from the centrifuge recipes
            null, MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.TIN).asItem(),
            null,
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.TIN)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.TIN).asItem(),
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.TIN)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.TIN).asItem(),
            1.5f, "compat/mekanism/tin", 2, false),
        // lead
        new OreTransform(
            Ingredient.of(MekanismTags.Items.ORES.get(OreType.LEAD)),
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.LEAD)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.LEAD).asItem(), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.TIN).asItem(),
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.LEAD)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.LEAD).asItem(),
            null, null, null,
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.LEAD)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.LEAD).asItem(),
            null, null, MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.LEAD).asItem(),
            // Adding dust as "gemItem" to give dust as an output from the centrifuge recipes
            null, MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.LEAD).asItem(),
            null,
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.LEAD)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.LEAD).asItem(),
            Ingredient.of(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.LEAD)), MekanismItems.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.LEAD).asItem(),
            1.5f, "compat/mekanism/lead", 2, false));
        oreTransforms.forEach(ore -> RecipeGeneratorUtil.addMetalProcessingChain(exporter, ore));
    }

    private static void addMekInfusing(RecipeOutput exporter) {
        ItemStackChemicalToItemStackRecipeBuilder.metallurgicInfusing(IngredientCreatorAccess.item().from(TagContent.NICKEL_INGOTS), IngredientCreatorAccess.chemicalStack().from(MekanismChemicals.DIAMOND, 10), new ItemStack(ItemContent.ADAMANT_DUST), false).build(exporter, Oritech.id("compat/mekanism/infusing/adamant_dust"));
        ItemStackChemicalToItemStackRecipeBuilder.metallurgicInfusing(IngredientCreatorAccess.item().from(Tags.Items.INGOTS_IRON), IngredientCreatorAccess.chemicalStack().from(MekanismChemicals.BIO, 10), new ItemStack(ItemContent.BIOSTEEL_DUST), false).build(exporter, Oritech.id("compat/mekanism/infusing/biosteel_dust"));
        ItemStackChemicalToItemStackRecipeBuilder.metallurgicInfusing(IngredientCreatorAccess.item().from(Tags.Items.INGOTS_GOLD), IngredientCreatorAccess.chemicalStack().from(MekanismChemicals.REDSTONE, 10), new ItemStack(ItemContent.ELECTRUM_DUST), false).build(exporter, Oritech.id("compat/mekanism/infusing/electrum_dust"));
    }
}
