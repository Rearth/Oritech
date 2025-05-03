package rearth.oritech.neoforgegen.datagen.compat;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;
import static rearth.oritech.init.TagContent.cItemTag;

import java.util.List;

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
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.CentrifugeRecipeBuilder;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.api.recipe.FuelGeneratorRecipeBuilder;
import rearth.oritech.api.recipe.util.MetalProcessingChainBuilder;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

public class ImmersiveEngineeringRecipeGenerator {
    private static final String PATH = "compat/immersiveengineering/";

    public static void generateRecipes(RecipeOutput exporter) {
        addAlloying(exporter);
        addIEAlloying(exporter);
        addCentrifuging(exporter);
        addGeneratorFuels(exporter);
        addMetalProcessing(exporter);
    }

    private static void addAlloying(RecipeOutput exporter) {
        FoundryRecipeBuilder.build().input(Tags.Items.INGOTS_COPPER).input(TagContent.NICKEL_INGOTS).result(IEItems.Metals.INGOTS.get(EnumMetals.CONSTANTAN).get(), 2).export(exporter, "compat/immersiveengineering/constantan");
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

        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/coppergem"), new AlloyRecipe(new TagOutput(Items.COPPER_INGOT, 3), new IngredientWithSize(of(ItemContent.COPPER_GEM)), new IngredientWithSize(of(ItemContent.COPPER_GEM)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/coppergem"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(Items.COPPER_INGOT, 4))), TagOutput.EMPTY, List.of(), time, base_energy, new IngredientWithSize(of(ItemContent.COPPER_GEM)), List.of(new IngredientWithSize(of(ItemContent.COPPER_GEM)))), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/irongem"), new AlloyRecipe(new TagOutput(Items.IRON_INGOT, 3), new IngredientWithSize(of(ItemContent.IRON_GEM)), new IngredientWithSize(of(ItemContent.IRON_GEM)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/irongem"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(Items.IRON_INGOT, 4))), TagOutput.EMPTY, List.of(), time, base_energy, new IngredientWithSize(of(ItemContent.IRON_GEM)), List.of(new IngredientWithSize(of(ItemContent.IRON_GEM)))), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/goldgem"), new AlloyRecipe(new TagOutput(Items.GOLD_INGOT, 3), new IngredientWithSize(of(ItemContent.GOLD_GEM)), new IngredientWithSize(of(ItemContent.GOLD_GEM)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/goldgem"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(Items.GOLD_INGOT, 4))), TagOutput.EMPTY, List.of(), time, base_energy, new IngredientWithSize(of(ItemContent.GOLD_GEM)), List.of(new IngredientWithSize(of(ItemContent.GOLD_GEM)))), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/nickelgem"), new AlloyRecipe(new TagOutput(ItemContent.NICKEL_INGOT, 3), new IngredientWithSize(of(ItemContent.NICKEL_GEM)), new IngredientWithSize(of(ItemContent.NICKEL_GEM)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/nickelgem"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(ItemContent.NICKEL_INGOT, 4))), TagOutput.EMPTY, List.of(), time, base_energy, new IngredientWithSize(of(ItemContent.NICKEL_GEM)), List.of(new IngredientWithSize(of(ItemContent.NICKEL_GEM)))), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/alloying/platinumgem"), new AlloyRecipe(new TagOutput(ItemContent.PLATINUM_INGOT, 3), new IngredientWithSize(of(ItemContent.PLATINUM_GEM)), new IngredientWithSize(of(ItemContent.PLATINUM_GEM)), 200), null);
        exporter.accept(Oritech.id("compat/immersiveengineering/arcalloying/platinumgem"), new ArcFurnaceRecipe(new TagOutputList(List.of(new TagOutput(ItemContent.PLATINUM_INGOT, 4))), TagOutput.EMPTY, List.of(), time, base_energy, new IngredientWithSize(of(ItemContent.PLATINUM_GEM)), List.of(new IngredientWithSize(of(ItemContent.PLATINUM_GEM)))), null);
        
    }

    private static void addCentrifuging(RecipeOutput exporter) {
        CentrifugeFluidRecipeBuilder.build().input(ItemTags.PLANKS).result(IEBlocks.WoodenDecoration.TREATED_WOOD.get(TreatedWoodStyles.HORIZONTAL).get().asItem()).fluidInput(IEFluids.CREOSOTE.still().get(), 0.125f).export(exporter, "compat/immersiveengineering/treated_planks");
        CentrifugeRecipeBuilder.build().input(IEItems.Ingredients.DUST_HOP_GRAPHITE.get()).result(ItemContent.CARBON_FIBRE_STRANDS).export(exporter, "compat/immersiveengineering/carbon_fibre_strands");
    }

    private static void addGeneratorFuels(RecipeOutput exporter) {
        FuelGeneratorRecipeBuilder.build().fluidInput(IEFluids.BIODIESEL.still().get(), 0.1f).timeInSeconds(3).export(exporter, PATH + "biodiesel");
        FuelGeneratorRecipeBuilder.build().fluidInput(IEFluids.HIGH_POWER_BIODIESEL.still().get(), 0.1f).timeInSeconds(12).export(exporter, PATH + "highpowerbiodiesel");
    }

    private static void addMetalProcessing(RecipeOutput exporter) {
        // bauxite/aluminum
        MetalProcessingChainBuilder.build("aluminum").resourcePath(PATH)
            .ore(cItemTag("ores/aluminum"))
            .rawOre(IETags.getTagsFor(EnumMetals.ALUMINUM).rawOre, IEItems.Metals.RAW_ORES.get(EnumMetals.ALUMINUM).get()).rawOreByproduct(ItemContent.QUARTZ_DUST)
            .ingot(IETags.getTagsFor(EnumMetals.ALUMINUM).ingot, IEItems.Metals.INGOTS.get(EnumMetals.ALUMINUM).get())
            .nugget(IETags.getTagsFor(EnumMetals.ALUMINUM).nugget, IEItems.Metals.NUGGETS.get(EnumMetals.ALUMINUM).get())
            .dust(IEItems.Metals.DUSTS.get(EnumMetals.ALUMINUM).get()).dustByproduct(ItemContent.QUARTZ_DUST).byproductAmount(1)
            .export(exporter);
        // silver
        MetalProcessingChainBuilder.build("silver").resourcePath(PATH)
            .ore(cItemTag("ores/silver"))
            .rawOre(IETags.getTagsFor(EnumMetals.SILVER).rawOre, IEItems.Metals.RAW_ORES.get(EnumMetals.SILVER).get()).rawOreByproduct(ItemContent.COPPER_DUST)
            .ingot(IETags.getTagsFor(EnumMetals.SILVER).ingot, IEItems.Metals.INGOTS.get(EnumMetals.SILVER).get())
            .nugget(IETags.getTagsFor(EnumMetals.SILVER).nugget, IEItems.Metals.NUGGETS.get(EnumMetals.SILVER).get())
            .dust(IEItems.Metals.DUSTS.get(EnumMetals.SILVER).get()).dustByproduct(ItemContent.SMALL_COPPER_DUST)
            .export(exporter);
        // lead
        MetalProcessingChainBuilder.build("lead").resourcePath(PATH)
            .ore(cItemTag("ores/lead"))
            .rawOre(IETags.getTagsFor(EnumMetals.LEAD).rawOre, IEItems.Metals.RAW_ORES.get(EnumMetals.LEAD).get()).rawOreByproduct(Items.RAW_GOLD)
            .ingot(IETags.getTagsFor(EnumMetals.LEAD).ingot, IEItems.Metals.INGOTS.get(EnumMetals.LEAD).get())
            .nugget(IETags.getTagsFor(EnumMetals.LEAD).nugget, IEItems.Metals.NUGGETS.get(EnumMetals.LEAD).get())
            .dust(IEItems.Metals.DUSTS.get(EnumMetals.LEAD).get()).dustByproduct(ItemContent.SMALL_GOLD_DUST)
            .export(exporter);
    }
}
