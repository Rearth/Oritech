package rearth.oritech.neoforgegen.datagen.compat;

import rearth.oritech.Oritech;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.datagen.OreTransform;
import rearth.oritech.util.datagen.RecipeGeneratorUtil;

import blusunrize.immersiveengineering.api.EnumMetals;
import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.api.crafting.AlloyRecipe;
import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import blusunrize.immersiveengineering.api.crafting.TagOutputList;
import blusunrize.immersiveengineering.common.blocks.wooden.TreatedWoodStyles;
import blusunrize.immersiveengineering.common.register.IEBlocks;
import blusunrize.immersiveengineering.common.register.IEFluids;
import blusunrize.immersiveengineering.common.register.IEItems;

import dev.architectury.fluid.FluidStack;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.common.Tags;

import java.util.List;

import static rearth.oritech.util.datagen.RecipeGeneratorUtil.of;
import static rearth.oritech.util.datagen.RecipeGeneratorUtil.cItemTag;

public class ImmersiveEngineeringRecipeGenerator {
    public static void generateRecipes(RecipeOutput exporter) {
        addAlloying(exporter);
        addIEAlloying(exporter);
        addCentrifuging(exporter);
        addGeneratorFuels(exporter);
        addMetalProcessing(exporter);
    }

    private static void addAlloying(RecipeOutput exporter) {
        RecipeGeneratorUtil.addAlloyRecipe(exporter, of(Tags.Items.INGOTS_COPPER), of(TagContent.NICKEL_INGOTS), IEItems.Metals.INGOTS.get(EnumMetals.CONSTANTAN).get(), 2, "compat/immersiveengineering/constantan");
    }

    private static void addIEAlloying(RecipeOutput exporter) {
        var time = 100;
        var base_energy = 51200;
        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/adamant"), new AlloyRecipe(new TagOutput(ItemContent.ADAMANT_INGOT), new IngredientWithSize(of(TagContent.NICKEL_INGOTS)), new IngredientWithSize(of(Tags.Items.GEMS_DIAMOND)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/adamant"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(ItemContent.ADAMANT_INGOT))), TagOutput.EMPTY, List.of(), time, base_energy, new IngredientWithSize(of(TagContent.NICKEL_INGOTS)), List.of(new IngredientWithSize(of(Tags.Items.GEMS_DIAMOND)))), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/biosteel"), new AlloyRecipe(new TagOutput(ItemContent.BIOSTEEL_INGOT), new IngredientWithSize(of(Tags.Items.INGOTS_IRON)), new IngredientWithSize(of(ItemContent.RAW_BIOPOLYMER)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/biosteel"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(ItemContent.BIOSTEEL_INGOT))), TagOutput.EMPTY, List.of(), time, base_energy, new IngredientWithSize(of(Tags.Items.INGOTS_IRON)), List.of(new IngredientWithSize(of(ItemContent.RAW_BIOPOLYMER)))), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/duratium"), new AlloyRecipe(new TagOutput(ItemContent.DURATIUM_INGOT), new IngredientWithSize(of(TagContent.PLATINUM_INGOTS)), new IngredientWithSize(of(Tags.Items.INGOTS_NETHERITE)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/duration"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(ItemContent.DURATIUM_INGOT))), TagOutput.EMPTY, List.of(), (int)(time*2.5), (int)(base_energy*5), new IngredientWithSize(of(TagContent.PLATINUM_INGOTS)), List.of(new IngredientWithSize(of(Tags.Items.INGOTS_NETHERITE)))), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/electrum"), new AlloyRecipe(new TagOutput(ItemContent.ELECTRUM_INGOT), new IngredientWithSize(of(Tags.Items.INGOTS_GOLD)), new IngredientWithSize(of(Tags.Items.DUSTS_REDSTONE)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/electrum"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(ItemContent.ELECTRUM_INGOT))), TagOutput.EMPTY, List.of(), time, base_energy, new IngredientWithSize(of(Tags.Items.INGOTS_GOLD)), List.of(new IngredientWithSize(of(Tags.Items.DUSTS_REDSTONE)))), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/energite"), new AlloyRecipe(new TagOutput(ItemContent.ENERGITE_INGOT), new IngredientWithSize(of(TagContent.NICKEL_INGOTS)), new IngredientWithSize(of(ItemContent.FLUXITE)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/energite"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(ItemContent.ENERGITE_INGOT))), TagOutput.EMPTY, List.of(), time, base_energy, new IngredientWithSize(of(TagContent.NICKEL_INGOTS)), List.of(new IngredientWithSize(of(ItemContent.FLUXITE)))), null);
    }

    private static void addCentrifuging(RecipeOutput exporter) {
        RecipeGeneratorUtil.addCentrifugeFluidRecipe(exporter, of(ItemTags.PLANKS), IEBlocks.WoodenDecoration.TREATED_WOOD.get(TreatedWoodStyles.HORIZONTAL).get().asItem(), IEFluids.CREOSOTE.still().get(), 0.125f, null, 0, 1f, "compat/immersiveengineering/treated_planks");
    }

    private static void addGeneratorFuels(RecipeOutput exporter) {
        RecipeGeneratorUtil.addFuelGenRecipe(exporter, FluidStack.create(IEFluids.BIODIESEL.still().get(), 100), 8, "compat/immersiveengineering/biodiesel");
        RecipeGeneratorUtil.addFuelGenRecipe(exporter, FluidStack.create(IEFluids.HIGH_POWER_BIODIESEL.still().get(), 100), 24, "compat/immersiveengineering/highpowerbiodiesel");
    }

    private static void addMetalProcessing(RecipeOutput exporter) {
        var oreTransforms = List.of(
            // bauxite/aluminum
            new OreTransform(
                of(cItemTag("ores/aluminum")),
                of(IETags.getTagsFor(EnumMetals.ALUMINUM).rawOre), IEItems.Metals.INGOTS.get(EnumMetals.ALUMINUM).get(), ItemContent.QUARTZ_DUST,
                null, null,
                null, null, null,
                of(IETags.getTagsFor(EnumMetals.ALUMINUM).dust), IEItems.Metals.DUSTS.get(EnumMetals.ALUMINUM).get(),
                null, null, IEItems.Metals.NUGGETS.get(EnumMetals.ALUMINUM).get(),
                // Adding dust as "gemItem" to give dust as an output from the centrifuge recipes
                null, IEItems.Metals.DUSTS.get(EnumMetals.ALUMINUM).get(),
                null,
                of(IETags.getTagsFor(EnumMetals.ALUMINUM).nugget), IEItems.Metals.NUGGETS.get(EnumMetals.ALUMINUM).get(),
                of(IETags.getTagsFor(EnumMetals.ALUMINUM).ingot), IEItems.Metals.INGOTS.get(EnumMetals.ALUMINUM).get(),
                1.5f, "compat/immersiveengineering/aluminum", 2, false),
            // silver
            new OreTransform(
                of(cItemTag("ores/silver")),
                of(IETags.getTagsFor(EnumMetals.SILVER).rawOre), IEItems.Metals.INGOTS.get(EnumMetals.SILVER).get(), ItemContent.COPPER_DUST,
                null, null,
                null, null, null,
                of(IETags.getTagsFor(EnumMetals.SILVER).dust), IEItems.Metals.DUSTS.get(EnumMetals.SILVER).get(),
                null, null, IEItems.Metals.NUGGETS.get(EnumMetals.SILVER).get(),
                // Adding dust as "gemItem" to give dust as an output from the centrifuge recipes
                null, IEItems.Metals.DUSTS.get(EnumMetals.SILVER).get(),
                null,
                of(IETags.getTagsFor(EnumMetals.SILVER).nugget), IEItems.Metals.NUGGETS.get(EnumMetals.SILVER).get(),
                of(IETags.getTagsFor(EnumMetals.SILVER).ingot), IEItems.Metals.INGOTS.get(EnumMetals.SILVER).get(),
                1.5f, "compat/immersiveengineering/silver", 2, false),
            // lead
            new OreTransform(
                of(cItemTag("ores/lead")),
                of(IETags.getTagsFor(EnumMetals.LEAD).rawOre), IEItems.Metals.INGOTS.get(EnumMetals.LEAD).get(), ItemContent.GOLD_DUST,
                null, null,
                null, null, null,
                of(IETags.getTagsFor(EnumMetals.LEAD).dust), IEItems.Metals.DUSTS.get(EnumMetals.LEAD).get(),
                null, null, IEItems.Metals.NUGGETS.get(EnumMetals.LEAD).get(),
                // Adding dust as "gemItem" to give dust as an output from the centrifuge recipes
                null, IEItems.Metals.DUSTS.get(EnumMetals.LEAD).get(),
                null,
                of(IETags.getTagsFor(EnumMetals.LEAD).nugget), IEItems.Metals.NUGGETS.get(EnumMetals.LEAD).get(),
                of(IETags.getTagsFor(EnumMetals.LEAD).ingot), IEItems.Metals.INGOTS.get(EnumMetals.LEAD).get(),
                1.5f, "compat/immersiveengineering/lead", 2, false));
        oreTransforms.forEach(ore -> RecipeGeneratorUtil.addMetalProcessingChain(exporter, ore));
    }
}
