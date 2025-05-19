package rearth.oritech.neoforgegen.datagen.compat;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;

import java.util.List;

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
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.AtomicForgeRecipeBuilder;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.api.recipe.util.MetalProcessingChainBuilder;
import rearth.oritech.api.recipe.util.RecipeHelpers;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

public class MekanismRecipeGenerator {
    private static final String PATH = "compat/mekanism/";

    public static void generateRecipes(IConditionBuilder conditionBuilder, RecipeOutput exporter) {
        addAlloying(exporter);
        addAtomicForging(exporter);
        addDustGrinding(exporter);
        addMetalProcessing(conditionBuilder, exporter);
        addMekInfusing(exporter);
    }

    private static void addAlloying(RecipeOutput exporter) {
        FoundryRecipeBuilder.build()
            .input(Tags.Items.INGOTS_COPPER)
            .input(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.TIN))
            .result(MekanismItems.BRONZE_INGOT.asItem(), 2)
            .export(exporter, PATH + "bronze");
        FoundryRecipeBuilder.build()
            .input(Tags.Items.INGOTS_COPPER)
            .input(Tags.Items.DUSTS_REDSTONE)
            .result(MekanismItems.INFUSED_ALLOY.asItem())
            .export(exporter, PATH + "infused_alloy");
        FoundryRecipeBuilder.build()
            .input(MekanismTags.Items.ALLOYS_INFUSED)
            .input(MekanismTags.Items.DUSTS_DIAMOND)
            .result(MekanismItems.REINFORCED_ALLOY.asItem())
            .export(exporter, PATH + "reinforced_alloy");
        FoundryRecipeBuilder.build()
            .input(MekanismTags.Items.ALLOYS_REINFORCED)
            .input(MekanismTags.Items.DUSTS_REFINED_OBSIDIAN)
            .result(MekanismItems.ATOMIC_ALLOY.asItem())
            .export(exporter, PATH + "atomic_alloy");
        FoundryRecipeBuilder.build()
            .input(MekanismTags.Items.DUSTS_OBSIDIAN)
            .input(MekanismTags.Items.DUSTS_DIAMOND)
            .result(MekanismItems.REFINED_OBSIDIAN_DUST.asItem())
            .export(exporter, PATH + "refined_obsidian_dust");
    }

    private static void addAtomicForging(RecipeOutput exporter) {
        AtomicForgeRecipeBuilder.build()
            .input(Tags.Items.DUSTS_REDSTONE)
            .input(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.OSMIUM))
            .result(MekanismItems.BASIC_CONTROL_CIRCUIT.asItem())
            .time(5).export(exporter, PATH + "basic_control_circuit");
        AtomicForgeRecipeBuilder.build()
            .input(Tags.Items.DUSTS_REDSTONE)
            .input(MekanismTags.Items.CIRCUITS_BASIC)
            .result(MekanismItems.ADVANCED_CONTROL_CIRCUIT.asItem())
            .time(5).export(exporter, PATH + "advanced_control_circuit");
        AtomicForgeRecipeBuilder.build()
            .input(MekanismTags.Items.DUSTS_DIAMOND)
            .input(MekanismTags.Items.CIRCUITS_ADVANCED)
            .result(MekanismItems.ELITE_CONTROL_CIRCUIT.asItem())
            .time(5).export(exporter, PATH + "elite_control_circuit");
        AtomicForgeRecipeBuilder.build()
            .input(MekanismTags.Items.DUSTS_REFINED_OBSIDIAN)
            .input(MekanismTags.Items.CIRCUITS_ELITE)
            .result(MekanismItems.ULTIMATE_CONTROL_CIRCUIT.asItem())
            .time(5).export(exporter, PATH + "ultimate_control_circuit");
    }

    private static void addDustGrinding(RecipeOutput exporter) {
        RecipeHelpers.addDustRecipe(exporter, of(MekanismTags.Items.INGOTS_BRONZE), MekanismItems.BRONZE_DUST.asItem(), PATH + "dust/bronze");
        RecipeHelpers.addDustRecipe(exporter, of(Items.CHARCOAL), MekanismItems.CHARCOAL_DUST.asItem(), "compat/mekanism/dust/charcoal");
        RecipeHelpers.addDustRecipe(exporter, of(Tags.Items.GEMS_DIAMOND), MekanismItems.DIAMOND_DUST.asItem(), "compat/mekanism/dust/diamond");
        RecipeHelpers.addDustRecipe(exporter, of(Tags.Items.GEMS_EMERALD), MekanismItems.EMERALD_DUST.asItem(), "compat/mekanism/dust/emerald");
        RecipeHelpers.addDustRecipe(exporter, of(MekanismTags.Items.GEMS_FLUORITE), MekanismItems.FLUORITE_DUST.asItem(), "compat/mekanism/dust/fluorite");
        RecipeHelpers.addDustRecipe(exporter, of(Tags.Items.GEMS_LAPIS), MekanismItems.LAPIS_LAZULI_DUST.asItem(), "compat/mekanism/dust/lapis");
        RecipeHelpers.addDustRecipe(exporter, of(Tags.Items.INGOTS_NETHERITE), MekanismItems.NETHERITE_DUST.asItem(), "compat/mekanism/dust/netherite");
        RecipeHelpers.addDustRecipe(exporter, of(MekanismTags.Items.INGOTS_REFINED_OBSIDIAN), MekanismItems.REFINED_OBSIDIAN_DUST.asItem(), "compat/mekanism/dust/refined_obsidian");
        RecipeHelpers.addDustRecipe(exporter, of(TagContent.STEEL_INGOTS), MekanismItems.STEEL_DUST.asItem(), "compat/mekanism/dust/steel");
        RecipeHelpers.addDustRecipe(exporter, of(Tags.Items.OBSIDIANS), MekanismItems.OBSIDIAN_DUST.asItem(), "compat/mekanism/dust/obsidian");
    }
    
    private static void addMetalProcessing(IConditionBuilder conditionBuilder, RecipeOutput exporter) {
        var conditionExporter = exporter.withConditions(conditionBuilder.not(conditionBuilder.modLoaded("jaopca")));
        // osmium
        MetalProcessingChainBuilder.build("osmium").resourcePath(PATH)
            .ore(MekanismTags.Items.ORES.get(OreType.OSMIUM))
            .rawOre(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.OSMIUM),
                    MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.OSMIUM).asItem())
            .rawOreByproduct(ItemContent.RAW_PLATINUM)
            .ingot(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.OSMIUM),
                    MekanismItems.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.OSMIUM).asItem())
            .nugget(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.OSMIUM).asItem())
            .clump(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.OSMIUM),
                    MekanismItems.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.OSMIUM).asItem())
            .clumpByproduct(ItemContent.SMALL_PLATINUM_CLUMP)
            .centrifugeResult(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.OSMIUM).asItem())
            .dust(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.OSMIUM).asItem())
            .dustByproduct(ItemContent.SMALL_PLATINUM_DUST)
            .byproductAmount(2)
            .timeMultiplier(1.5f)
            .export(conditionExporter);
        // tin
        MetalProcessingChainBuilder.build("tin").resourcePath(PATH)
            .ore(MekanismTags.Items.ORES.get(OreType.TIN))
            .rawOre(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.TIN),
                    MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.TIN).asItem())
            .rawOreByproduct(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.LEAD).asItem())
            .ingot(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.TIN),
                    MekanismItems.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.TIN).asItem())
            .nugget(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.TIN).asItem())
            .clump(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.TIN),
                    MekanismItems.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.TIN).asItem())
            .clumpByproduct(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.LEAD).asItem())
            .centrifugeResult(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.TIN).asItem())
            .dust(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.TIN).asItem())
            .dustByproduct(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.LEAD).asItem())
            .export(conditionExporter);
        // lead
        MetalProcessingChainBuilder.build("lead").resourcePath(PATH)
            .ore(MekanismTags.Items.ORES.get(OreType.LEAD))
            .rawOre(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.LEAD),
                    MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.LEAD).asItem())
            .rawOreByproduct(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.RAW, PrimaryResource.TIN).asItem())
            .ingot(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.LEAD),
                    MekanismItems.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.LEAD).asItem())
            .nugget(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.LEAD).asItem())
            .clump(MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.LEAD),
                    MekanismItems.PROCESSED_RESOURCES.get(ResourceType.CLUMP, PrimaryResource.LEAD).asItem())
            .clumpByproduct(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.TIN).asItem())
            .centrifugeResult(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.LEAD).asItem())
            .dust(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.DUST, PrimaryResource.LEAD).asItem())
            .dustByproduct(MekanismItems.PROCESSED_RESOURCES.get(ResourceType.NUGGET, PrimaryResource.TIN).asItem())
            .export(conditionExporter);
    }

    private static void addMekInfusing(RecipeOutput exporter) {
        ItemStackChemicalToItemStackRecipeBuilder.metallurgicInfusing(IngredientCreatorAccess.item().from(TagContent.NICKEL_INGOTS), IngredientCreatorAccess.chemicalStack().from(MekanismChemicals.DIAMOND, 10), new ItemStack(ItemContent.ADAMANT_DUST), false).build(exporter, Oritech.id("compat/mekanism/infusing/adamant_dust"));
        ItemStackChemicalToItemStackRecipeBuilder.metallurgicInfusing(IngredientCreatorAccess.item().from(Tags.Items.INGOTS_IRON), IngredientCreatorAccess.chemicalStack().from(MekanismChemicals.BIO, 10), new ItemStack(ItemContent.BIOSTEEL_DUST), false).build(exporter, Oritech.id("compat/mekanism/infusing/biosteel_dust"));
        ItemStackChemicalToItemStackRecipeBuilder.metallurgicInfusing(IngredientCreatorAccess.item().from(Tags.Items.INGOTS_GOLD), IngredientCreatorAccess.chemicalStack().from(MekanismChemicals.REDSTONE, 10), new ItemStack(ItemContent.ELECTRUM_DUST), false).build(exporter, Oritech.id("compat/mekanism/infusing/electrum_dust"));
    }
}
