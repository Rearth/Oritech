package rearth.oritech.init.datagen.compat;

import java.util.List;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import rearth.oritech.Oritech;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.datagen.RecipeGenerator;
import rearth.oritech.init.datagen.data.TagContent;
import rearth.oritech.util.FluidStack;
import reborncore.common.crafting.SizedIngredient;
import reborncore.common.fluid.FluidValue;
import reborncore.common.fluid.container.FluidInstance;
import reborncore.common.crafting.RebornRecipe;
import techreborn.init.ModFluids;
import techreborn.init.ModRecipes;
import techreborn.init.TRContent;
import techreborn.items.DynamicCellItem;
import techreborn.recipe.recipes.IndustrialGrinderRecipe;

public class TechRebornRecipeGenerator {

    public static void generateRecipes(RecipeExporter exporter) {
        addCraftRecipes(exporter);
        addOritechAlloys(exporter);
        addTechRebornAlloys(exporter);
        addOritechGrinderRecipes(exporter);
        addOritechIndustrialGrinderRecipes(exporter);
        addTechRebornPulverizerRecipes(exporter);
        addTechRebornFragmentRecipes(exporter);
        addTechRebornFuels(exporter);
        addDistillation(exporter);
    }

    public static void addCraftRecipes(RecipeExporter exporter) {
        var output = TRContent.Parts.CARBON_MESH.asItem();   
        var shapelessBuilder = ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, output, 1).input(Ingredient.fromTag(TagContent.CARBON_FIBRE)).input(Ingredient.fromTag(TagContent.CARBON_FIBRE));
        shapelessBuilder.criterion(RecipeGenerator.hasItem(output), RecipeGenerator.conditionsFromItem(output)).offerTo(exporter, RecipeGenerator.getItemPath(output) + "_tr_carbon_mesh");
        
        output = TRContent.Machine.LAMP_INCANDESCENT.asItem();
        var shapedBuilder = ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, output, 1).input('G', ConventionalItemTags.GLASS_PANES_COLORLESS).input('C', TRContent.Cables.COPPER).input('F', TagContent.CARBON_FIBRE).pattern("GGG").pattern("CFC").pattern("GGG");
        shapedBuilder.criterion(RecipeGenerator.hasItem(output), RecipeGenerator.conditionsFromItem(output)).offerTo(exporter, RecipeGenerator.getItemPath(output) + "_tr_lamp_incandescent");
    }

    public static void addOritechAlloys(RecipeExporter exporter) {
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_INGOTS)), new SizedIngredient(1, Ingredient.ofItems(Items.DIAMOND)), new ItemStack(ItemContent.ADAMANT_INGOT.asItem()), 6, 200, "_tr_adamant");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(Items.IRON_INGOT)), new SizedIngredient(1, Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER.asItem())), new ItemStack(ItemContent.BIOSTEEL_INGOT.asItem()), 6, 200, "_tr_biosteel");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.PLATINUM_INGOTS)), new SizedIngredient(1, Ingredient.ofItems(Items.NETHERITE_INGOT)), new ItemStack(ItemContent.DURATIUM_INGOT), 60, 200, "_tr_duratium");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(Items.GOLD_INGOT)), new SizedIngredient(1, Ingredient.ofItems(Items.REDSTONE)), new ItemStack(ItemContent.ELECTRUM_INGOT.asItem()), 6, 200, "_tr_oritech_electrum");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_INGOTS)), new SizedIngredient(1, Ingredient.ofItems(ItemContent.FLUXITE.asItem())), new ItemStack(ItemContent.ENERGITE_INGOT.asItem()), 6, 200, "_tr_energite");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.COPPER_GEM.asItem())), new SizedIngredient(1, Ingredient.ofItems(ItemContent.COPPER_GEM.asItem())), new ItemStack(Items.COPPER_INGOT, 4), 6, 200, "_tr_copper_gems");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.IRON_GEM.asItem())), new SizedIngredient(1, Ingredient.ofItems(ItemContent.IRON_GEM.asItem())), new ItemStack(Items.IRON_INGOT, 4), 6, 200, "_tr_iron_gems");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.NICKEL_GEM.asItem())), new SizedIngredient(1, Ingredient.ofItems(ItemContent.NICKEL_GEM.asItem())), new ItemStack(ItemContent.NICKEL_INGOT, 4), 6, 200, "_tr_nickel_gems");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.PLATINUM_GEM.asItem())), new SizedIngredient(1, Ingredient.ofItems(ItemContent.PLATINUM_GEM.asItem())), new ItemStack(ItemContent.PLATINUM_INGOT, 4), 6, 200, "_tr_platinum_gems");
    }

    public static void addTechRebornAlloys(RecipeExporter exporter) {
        RecipeGenerator.addAlloyRecipe(exporter, Ingredient.ofItems(Items.IRON_INGOT), Ingredient.fromTag(TagContent.NICKEL_INGOTS), TRContent.Ingots.INVAR.asItem(), 2, "_tr_invar");
        RecipeGenerator.addAlloyRecipe(exporter, Ingredient.ofItems(Items.GOLD_INGOT), Ingredient.fromTag(TRContent.Ingots.SILVER.asTag()), TRContent.Ingots.ELECTRUM.asItem(), 2, "_tr_electrum");
        RecipeGenerator.addAlloyRecipe(exporter, Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.fromTag(TRContent.Ingots.TIN.asTag()), TRContent.Ingots.BRONZE.asItem(), 2, "_tr_bronze");
        RecipeGenerator.addAlloyRecipe(exporter, Ingredient.ofItems(Items.COPPER_INGOT), Ingredient.fromTag(TRContent.Ingots.ZINC.asTag()), TRContent.Ingots.BRASS.asItem(), 2, "_tr_brass");
    }

    public static void addOritechGrinderRecipes(RecipeExporter exporter) {
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_ORES)), new ItemStack(ItemContent.RAW_NICKEL, 2), 5, 200, "_nickel_ore");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_RAW_ORES)), new ItemStack(ItemContent.NICKEL_DUST), 5, 200, "_raw_nickel");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.ADAMANT_INGOT)), new ItemStack(ItemContent.ADAMANT_DUST), 5, 200, "_adamant_ingot");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT)), new ItemStack(ItemContent.BIOSTEEL_DUST), 5, 200, "_biosteel_ingot");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.DURATIUM_INGOT)), new ItemStack(ItemContent.DURATIUM_DUST), 5, 200, "_duratium_ingot");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.ELECTRUM_INGOT)), new ItemStack(ItemContent.ELECTRUM_DUST), 5, 200, "_electrum_ingot");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.ENERGITE_INGOT)), new ItemStack(ItemContent.ENERGITE_DUST), 5, 200, "_energite_ingot");
    }

    public static void addOritechIndustrialGrinderRecipes(RecipeExporter exporter) {
        offerTRIndustrialGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_ORES)), List.of(new ItemStack(ItemContent.RAW_NICKEL, 2), new ItemStack(ItemContent.RAW_PLATINUM)), 5, 200, new FluidInstance(Fluids.WATER, FluidValue.BUCKET), "_nickel_ore");
        offerTRIndustrialGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_RAW_ORES)), List.of(new ItemStack(ItemContent.NICKEL_DUST), new ItemStack(ItemContent.SMALL_NICKEL_DUST, 3), new ItemStack(ItemContent.SMALL_PLATINUM_DUST, 2)), 5, 200, new FluidInstance(Fluids.WATER, FluidValue.BUCKET), "_nickel");
        offerTRIndustrialGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.PLATINUM_ORES)), List.of(new ItemStack(ItemContent.RAW_PLATINUM, 2)), 5, 200, new FluidInstance(Fluids.WATER, FluidValue.BUCKET), "_tr_platinum_ore");
        offerTRIndustrialGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.PLATINUM_RAW_ORES)), List.of(new ItemStack(ItemContent.PLATINUM_DUST), new ItemStack(ItemContent.SMALL_PLATINUM_DUST, 3)), 5, 200, new FluidInstance(Fluids.WATER, FluidValue.BUCKET), "_tr_platinum");
    }

    public static void addTechRebornPulverizerRecipes(RecipeExporter exporter) {
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ingots.ALUMINUM.asTag()), TRContent.Ingots.ALUMINUM.getDust().asItem(), "_tr_aluminum");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.BAUXITE.asTag()), TRContent.Dusts.BAUXITE.asItem(), "_tr_bauxite");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ingots.BRASS.asTag()), TRContent.Ingots.BRASS.getDust().asItem(), "_tr_brass");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ingots.BRONZE.asTag()), TRContent.Ingots.BRONZE.getDust().asItem(), "_tr_bronze");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ingots.CHROME.asTag()), TRContent.Ingots.CHROME.getDust().asItem(), "_tr_chrome");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.CINNABAR.asTag()), TRContent.Dusts.CINNABAR.asItem(), "_tr_cinnabar");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ingots.ELECTRUM.asTag()), TRContent.Ingots.ELECTRUM.getDust().asItem(), "_tr_electrum");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.GALENA.asTag()), TRContent.Dusts.GALENA.asItem(), "_tr_galena");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ingots.INVAR.asTag()), TRContent.Ingots.INVAR.getDust().asItem(), "_tr_invar");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.LEAD.asTag()), TRContent.RawMetals.LEAD.asItem(), 2, "_tr_lead_ore");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.PYRITE.asTag()), TRContent.Dusts.PYRITE.asItem(), "_tr_pyrite_ore");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.SILVER.asTag()), TRContent.RawMetals.SILVER.asItem(), 2, "_tr_silver_ore");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.TIN.asTag()), TRContent.RawMetals.TIN.asItem(), 2, "_tr_tin_ore");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ingots.TITANIUM.asTag()), TRContent.Ingots.TITANIUM.getDust().asItem(), "_tr_titanium");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Gems.PERIDOT.asTag()), TRContent.Gems.PERIDOT.getDust().asItem(), "_tr_peridot");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.PERIDOT.asTag()), TRContent.Gems.PERIDOT.getDust().asItem(), "_tr_peridot_ore");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Gems.RED_GARNET.asTag()), TRContent.Gems.RED_GARNET.getDust().asItem(), "_tr_red_garnet");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Gems.RUBY.asTag()), TRContent.Gems.RUBY.getDust().asItem(), "_tr_ruby");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.RUBY.asTag()), TRContent.Gems.RUBY.getDust().asItem(), 2, "_tr_ruby_ore");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Gems.SAPPHIRE.asTag()), TRContent.Gems.SAPPHIRE.getDust().asItem(), "_tr_sapphire");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.SAPPHIRE.asTag()), TRContent.Gems.SAPPHIRE.getDust().asItem(), 2, "_tr_sapphire_ore");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Gems.YELLOW_GARNET.asTag()), TRContent.Gems.YELLOW_GARNET.getDust().asItem(), "_tr_yellow_garnet");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.SODALITE.asTag()), TRContent.Dusts.SODALITE.asItem(), "_tr_sodalite");
        RecipeGenerator.addPulverizerRecipe(exporter, Ingredient.fromTag(TRContent.Ores.SPHALERITE.asTag()), TRContent.Dusts.SPHALERITE.asItem(), "_tr_sphalerite");
    }

    public static void addTechRebornFragmentRecipes(RecipeExporter exporter) {
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.SODALITE.asTag()), List.of(new ItemStack(TRContent.Dusts.SODALITE.asItem(), 12), new ItemStack(TRContent.Dusts.ALUMINUM.asItem(), 3)), "_tr_sodalite_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.SPHALERITE.asTag()), List.of(new ItemStack(TRContent.Dusts.SPHALERITE.asItem(), 6), new ItemStack(TRContent.Dusts.ZINC.asItem()), new ItemStack(TRContent.SmallDusts.YELLOW_GARNET.asItem())), "_tr_sphalerite_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.PERIDOT.asTag()), List.of(new ItemStack(TRContent.Gems.PERIDOT.asItem()), new ItemStack(TRContent.SmallDusts.PERIDOT.asItem(), 6), new ItemStack(TRContent.SmallDusts.EMERALD.asItem(), 2)), "_tr_peridot_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.RUBY.asTag()), List.of(new ItemStack(TRContent.Gems.RUBY.asItem()), new ItemStack(TRContent.SmallDusts.RUBY.asItem(), 6), new ItemStack(TRContent.SmallDusts.RED_GARNET.asItem(), 2)), "_tr_ruby_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.SAPPHIRE.asTag()), List.of(new ItemStack(TRContent.Gems.SAPPHIRE.asItem()), new ItemStack(TRContent.SmallDusts.SAPPHIRE.asItem(), 6), new ItemStack(TRContent.SmallDusts.PERIDOT.asItem(), 2)), "_tr_sapphire_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.BAUXITE.asTag()), List.of(new ItemStack(TRContent.Dusts.BAUXITE.asItem(), 4), new ItemStack(TRContent.Dusts.ALUMINUM.asItem())), "_tr_bauxite_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.CINNABAR.asTag()), List.of(new ItemStack(TRContent.Dusts.CINNABAR.asItem(), 5), new ItemStack(TRContent.SmallDusts.REDSTONE.asItem(), 2), new ItemStack(TRContent.SmallDusts.GLOWSTONE)), "_tr_cinnabar_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.GALENA.asTag()), List.of(new ItemStack(TRContent.Dusts.GALENA.asItem(), 2), new ItemStack(TRContent.Dusts.SULFUR.asItem())), "_tr_galena_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.IRIDIUM.asTag()), List.of(new ItemStack(TRContent.RawMetals.IRIDIUM.asItem()), new ItemStack(TRContent.SmallDusts.PLATINUM.asItem(), 2)), "_tr_iridium_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.LEAD.asTag()), List.of(new ItemStack(TRContent.RawMetals.LEAD.asItem(), 2), new ItemStack(TRContent.SmallDusts.GALENA.asItem(), 2)), "_tr_lead_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.PYRITE.asTag()), List.of(new ItemStack(TRContent.Dusts.PYRITE.asItem(), 5), new ItemStack(TRContent.Dusts.SULFUR.asItem(), 2)), "_tr_pyrite_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.SHELDONITE.asTag()), List.of(new ItemStack(TRContent.Dusts.PLATINUM.asItem(), 2), new ItemStack(TRContent.Dusts.NICKEL.asItem()), new ItemStack(TRContent.Nuggets.IRIDIUM.asItem(), 2)), "_tr_sheldonite_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.SILVER.asTag()), List.of(new ItemStack(TRContent.RawMetals.SILVER.asItem(), 2), new ItemStack(TRContent.SmallDusts.GALENA.asItem(), 2)), "_tr_silver_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.TIN.asTag()), List.of(new ItemStack(TRContent.RawMetals.TIN.asItem(), 2), new ItemStack(Items.IRON_NUGGET, 3), new ItemStack(TRContent.Nuggets.ZINC.asItem(), 3)), "_tr_tin_ore");
        RecipeGenerator.addGrinderRecipe(exporter, Ingredient.fromTag(TRContent.Ores.TUNGSTEN.asTag()), List.of(new ItemStack(TRContent.RawMetals.TUNGSTEN.asItem(), 2), new ItemStack(Items.IRON_NUGGET, 7), new ItemStack(TRContent.SmallDusts.MANGANESE.asItem(), 3)), "_tr_tungsten_ore");
    }

    public static void addTechRebornFuels(RecipeExporter exporter) {
        RecipeGenerator.addBioGenRecipe(exporter, Ingredient.ofItems(TRContent.Parts.COMPRESSED_PLANTBALL.item), 140, "_compressedplantball");

        RecipeGenerator.addFuelGenRecipe(exporter, new FluidStack(ModFluids.OIL.getFluid(), FluidConstants.BUCKET / 10), 8, "_tr_oil");
        RecipeGenerator.addFuelGenRecipe(exporter, new FluidStack(ModFluids.NITROFUEL.getFluid(), FluidConstants.BUCKET / 10), 24, "_tr_nitrofuel");
        RecipeGenerator.addFuelGenRecipe(exporter, new FluidStack(ModFluids.NITROCOAL_FUEL.getFluid(), FluidConstants.BUCKET / 10), 48, "_tr_nitrocoalfuel");
        RecipeGenerator.addFuelGenRecipe(exporter, new FluidStack(ModFluids.DIESEL.getFluid(), FluidConstants.BUCKET / 10), 128, "_tr_diesel");
        RecipeGenerator.addFuelGenRecipe(exporter, new FluidStack(ModFluids.NITROCOAL_FUEL.getFluid(), FluidConstants.BUCKET / 10), 400, "_tr_nitrodiesel");
    }

    public static void addDistillation(RecipeExporter exporter) {
        exporter.accept(Oritech.id("tr_distillation_oil"), new RebornRecipe.Default(ModRecipes.DISTILLATION_TOWER, List.of(cellIngredient(Fluids.EMPTY, 16), cellIngredient(FluidContent.STILL_OIL, 16)), List.of(cellStack(ModFluids.DIESEL, 16), cellStack(ModFluids.SULFURIC_ACID, 15), cellStack(ModFluids.GLYCERYL, 1)), 20, 400), null);
        RecipeGenerator.addCentrifugeFluidRecipe(exporter, Ingredient.ofItems(ItemContent.FLUXITE), null, ModFluids.OIL.getFluid(), 1f, FluidContent.STILL_FUEL, 1f, 1f, "_tr_fuel");
    }

    private static ItemStack cellStack(ModFluids fluid, int count) {
        return cellStack(fluid.getFluid(), count);
    }

    private static ItemStack cellStack(Fluid fluid, int count) {
        return DynamicCellItem.getCellWithFluid(fluid, count);
    }

    private static SizedIngredient cellIngredient(Fluid fluid, int count) {
        return new SizedIngredient(count, Ingredient.ofStacks(cellStack(fluid, count)));
    }

    public static void offerTRAlloySmelterRecipe(RecipeExporter exporter, SizedIngredient A, SizedIngredient B, ItemStack output, int power, int time, String suffix) {
        exporter.accept(Oritech.id("tr_alloysmelter" + suffix), new RebornRecipe.Default(ModRecipes.ALLOY_SMELTER, List.of(A, B), List.of(output), power, time), null);
    }

    public static void offerTRGrinderRecipe(RecipeExporter exporter, SizedIngredient input, ItemStack output, int power, int time, String suffix) {
        exporter.accept(Oritech.id("tr_grinder" + suffix), new RebornRecipe.Default(ModRecipes.GRINDER, List.of(input), List.of(output), power, time), null);
    }

    public static void offerTRIndustrialGrinderRecipe(RecipeExporter exporter, SizedIngredient input, List<ItemStack> outputs, int power, int time, FluidInstance fluid, String suffix) {
        exporter.accept(Oritech.id("tr_industrial_grinder" + suffix), new IndustrialGrinderRecipe(ModRecipes.INDUSTRIAL_GRINDER, List.of(input), outputs, power, time, fluid), null);
    }
}
