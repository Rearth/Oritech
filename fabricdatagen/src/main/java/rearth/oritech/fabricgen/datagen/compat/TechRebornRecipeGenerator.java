package rearth.oritech.fabricgen.datagen.compat;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
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
import rearth.oritech.api.recipe.BioGeneratorRecipeBuilder;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.api.recipe.FuelGeneratorRecipeBuilder;
import rearth.oritech.api.recipe.GrinderRecipeBuilder;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import rearth.oritech.fabricgen.datagen.RecipeGenerator;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import reborncore.common.crafting.RebornRecipe;
import reborncore.common.crafting.SizedIngredient;
import reborncore.common.fluid.FluidValue;
import reborncore.common.fluid.container.FluidInstance;
import techreborn.init.ModFluids;
import techreborn.init.ModRecipes;
import techreborn.init.TRContent;
import techreborn.items.DynamicCellItem;
import techreborn.recipe.recipes.FluidGeneratorRecipe;
import techreborn.recipe.recipes.IndustrialGrinderRecipe;

import java.util.List;

public class TechRebornRecipeGenerator {
    private static final String PATH = "compat/techreborn/";
    
    public static void generateRecipes(RecipeExporter exporter) {
        addPlantballPolymer(exporter);
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

    public static void addPlantballPolymer(RecipeExporter exporter) {
        CentrifugeFluidRecipeBuilder.build().input(TRContent.Parts.COMPRESSED_PLANTBALL.item).result(ItemContent.RAW_BIOPOLYMER).fluidInput(Fluids.WATER, 0.25f).export(exporter, PATH + "biopolymer");
    }
    
    public static void addCraftRecipes(RecipeExporter exporter) {
        var output = TRContent.Parts.CARBON_MESH.asItem();
        var shapelessBuilder = ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, output, 1).input(Ingredient.fromTag(TagContent.CARBON_FIBRE)).input(Ingredient.fromTag(TagContent.CARBON_FIBRE));
        shapelessBuilder.criterion(RecipeGenerator.hasItem(output), RecipeGenerator.conditionsFromItem(output)).offerTo(exporter, PATH + RecipeGenerator.getItemPath(output));
        
        output = TRContent.Machine.LAMP_INCANDESCENT.asItem();
        var shapedBuilder = ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, output, 1).input('G', ConventionalItemTags.GLASS_PANES_COLORLESS).input('C', TRContent.Cables.COPPER).input('F', TagContent.CARBON_FIBRE).pattern("GGG").pattern("CFC").pattern("GGG");
        shapedBuilder.criterion(RecipeGenerator.hasItem(output), RecipeGenerator.conditionsFromItem(output)).offerTo(exporter, PATH + RecipeGenerator.getItemPath(output));
    }
    
    public static void addOritechAlloys(RecipeExporter exporter) {
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_INGOTS)), new SizedIngredient(1, Ingredient.ofItems(Items.DIAMOND)), new ItemStack(ItemContent.ADAMANT_INGOT.asItem()), 6, 200, "adamant");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(Items.IRON_INGOT)), new SizedIngredient(1, Ingredient.ofItems(ItemContent.RAW_BIOPOLYMER.asItem())), new ItemStack(ItemContent.BIOSTEEL_INGOT.asItem()), 6, 200, "biosteel");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.PLATINUM_INGOTS)), new SizedIngredient(1, Ingredient.ofItems(Items.NETHERITE_INGOT)), new ItemStack(ItemContent.DURATIUM_INGOT), 60, 200, "duratium");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(Items.GOLD_INGOT)), new SizedIngredient(1, Ingredient.ofItems(Items.REDSTONE)), new ItemStack(ItemContent.ELECTRUM_INGOT.asItem()), 6, 200, "oritech_electrum");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_INGOTS)), new SizedIngredient(1, Ingredient.ofItems(ItemContent.FLUXITE.asItem())), new ItemStack(ItemContent.ENERGITE_INGOT.asItem()), 6, 200, "energite");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.COPPER_GEM.asItem())), new SizedIngredient(1, Ingredient.ofItems(ItemContent.COPPER_GEM.asItem())), new ItemStack(Items.COPPER_INGOT, 4), 6, 200, "copper_gems");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.IRON_GEM.asItem())), new SizedIngredient(1, Ingredient.ofItems(ItemContent.IRON_GEM.asItem())), new ItemStack(Items.IRON_INGOT, 4), 6, 200, "iron_gems");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.NICKEL_GEM.asItem())), new SizedIngredient(1, Ingredient.ofItems(ItemContent.NICKEL_GEM.asItem())), new ItemStack(ItemContent.NICKEL_INGOT, 4), 6, 200, "nickel_gems");
        offerTRAlloySmelterRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.PLATINUM_GEM.asItem())), new SizedIngredient(1, Ingredient.ofItems(ItemContent.PLATINUM_GEM.asItem())), new ItemStack(ItemContent.PLATINUM_INGOT, 4), 6, 200, "platinum_gems");
    }
    
    public static void addTechRebornAlloys(RecipeExporter exporter) {
        FoundryRecipeBuilder.build().input(ConventionalItemTags.IRON_INGOTS).input(TagContent.NICKEL_INGOTS).result(TRContent.Ingots.INVAR.asItem(), 2).export(exporter, PATH + "invar");
        FoundryRecipeBuilder.build().input(ConventionalItemTags.GOLD_INGOTS).input(TRContent.Ingots.SILVER.asTag()).result(TRContent.Ingots.ELECTRUM.asItem(), 2).export(exporter, PATH + "electrum");
        FoundryRecipeBuilder.build().input(ConventionalItemTags.COPPER_INGOTS).input(TRContent.Ingots.TIN.asTag()).result(TRContent.Ingots.BRONZE.asItem(), 2).export(exporter, PATH +  "bronze");
        FoundryRecipeBuilder.build().input(ConventionalItemTags.COPPER_INGOTS).input(TRContent.Ingots.ZINC.asTag()).result(TRContent.Ingots.BRASS.asItem(), 2).export(exporter, PATH + "brass");
    }
    
    public static void addOritechGrinderRecipes(RecipeExporter exporter) {
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_ORES)), new ItemStack(ItemContent.RAW_NICKEL, 2), 5, 200, "nickel_ore");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_RAW_MATERIALS)), new ItemStack(ItemContent.NICKEL_DUST), 5, 200, "raw_nickel");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.ADAMANT_INGOT)), new ItemStack(ItemContent.ADAMANT_DUST), 5, 200, "adamant_ingot");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT)), new ItemStack(ItemContent.BIOSTEEL_DUST), 5, 200, "biosteel_ingot");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.DURATIUM_INGOT)), new ItemStack(ItemContent.DURATIUM_DUST), 5, 200, "duratium_ingot");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.ELECTRUM_INGOT)), new ItemStack(ItemContent.ELECTRUM_DUST), 5, 200, "electrum_ingot");
        offerTRGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.ofItems(ItemContent.ENERGITE_INGOT)), new ItemStack(ItemContent.ENERGITE_DUST), 5, 200, "energite_ingot");
    }
    
    public static void addOritechIndustrialGrinderRecipes(RecipeExporter exporter) {
        offerTRIndustrialGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_ORES)), List.of(new ItemStack(ItemContent.RAW_NICKEL, 2), new ItemStack(ItemContent.RAW_PLATINUM)), 5, 200, new FluidInstance(Fluids.WATER, FluidValue.BUCKET), "nickel_ore");
        offerTRIndustrialGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_RAW_MATERIALS)), List.of(new ItemStack(ItemContent.NICKEL_DUST), new ItemStack(ItemContent.SMALL_NICKEL_DUST, 3), new ItemStack(ItemContent.SMALL_PLATINUM_DUST, 2)), 5, 200, new FluidInstance(Fluids.WATER, FluidValue.BUCKET), "nickel");
        offerTRIndustrialGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.PLATINUM_ORES)), List.of(new ItemStack(ItemContent.RAW_PLATINUM, 2)), 5, 200, new FluidInstance(Fluids.WATER, FluidValue.BUCKET), "platinum_ore");
        offerTRIndustrialGrinderRecipe(exporter, new SizedIngredient(1, Ingredient.fromTag(TagContent.PLATINUM_RAW_MATERIALS)), List.of(new ItemStack(ItemContent.PLATINUM_DUST), new ItemStack(ItemContent.SMALL_PLATINUM_DUST, 3)), 5, 200, new FluidInstance(Fluids.WATER, FluidValue.BUCKET), "platinum");
    }
    
    public static void addTechRebornPulverizerRecipes(RecipeExporter exporter) {
        PulverizerRecipeBuilder.build().input(TRContent.Ingots.ALUMINUM.asTag()).result(TRContent.Ingots.ALUMINUM.getDust().asItem()).export(exporter, PATH + "aluminum");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.BAUXITE.asTag()).result(TRContent.Dusts.BAUXITE.asItem()).export(exporter, PATH + "bauxite");
        PulverizerRecipeBuilder.build().input(TRContent.Ingots.BRASS.asTag()).result(TRContent.Ingots.BRASS.getDust().asItem()).export(exporter, PATH + "brass");
        PulverizerRecipeBuilder.build().input(TRContent.Ingots.BRONZE.asTag()).result(TRContent.Ingots.BRONZE.getDust().asItem()).export(exporter, PATH + "bronze");
        PulverizerRecipeBuilder.build().input(TRContent.Ingots.CHROME.asTag()).result(TRContent.Ingots.CHROME.getDust().asItem()).export(exporter, PATH + "chrome");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.CINNABAR.asTag()).result(TRContent.Dusts.CINNABAR.asItem()).export(exporter, PATH + "cinnabar");
        PulverizerRecipeBuilder.build().input(TRContent.Ingots.ELECTRUM.asTag()).result(TRContent.Ingots.ELECTRUM.getDust().asItem()).export(exporter, PATH + "electrum");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.GALENA.asTag()).result(TRContent.Dusts.GALENA.asItem()).export(exporter, PATH + "galena");
        PulverizerRecipeBuilder.build().input(TRContent.Ingots.INVAR.asTag()).result(TRContent.Ingots.INVAR.getDust().asItem()).export(exporter, PATH + "invar");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.LEAD.asTag()).result(TRContent.RawMetals.LEAD.asItem(), 2).export(exporter, PATH + "lead_ore");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.PYRITE.asTag()).result(TRContent.Dusts.PYRITE.asItem()).export(exporter, PATH + "pyrite_ore");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.SILVER.asTag()).result(TRContent.RawMetals.SILVER.asItem(), 2).export(exporter, PATH + "silver_ore");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.TIN.asTag()).result(TRContent.RawMetals.TIN.asItem(), 2).export(exporter, PATH + "tin_ore");
        PulverizerRecipeBuilder.build().input(TRContent.Ingots.TITANIUM.asTag()).result(TRContent.Ingots.TITANIUM.getDust().asItem()).export(exporter, PATH + "titanium");
        PulverizerRecipeBuilder.build().input(TRContent.Gems.PERIDOT.asTag()).result(TRContent.Gems.PERIDOT.getDust().asItem()).export(exporter, PATH + "peridot");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.PERIDOT.asTag()).result(TRContent.Gems.PERIDOT.getDust().asItem()).export(exporter, PATH + "peridot_ore");
        PulverizerRecipeBuilder.build().input(TRContent.Gems.RED_GARNET.asTag()).result(TRContent.Gems.RED_GARNET.getDust().asItem()).export(exporter, PATH + "red_garnet");
        PulverizerRecipeBuilder.build().input(TRContent.Gems.RUBY.asTag()).result(TRContent.Gems.RUBY.getDust().asItem()).export(exporter, PATH + "ruby");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.RUBY.asTag()).result(TRContent.Gems.RUBY.getDust().asItem(), 2).export(exporter, PATH + "ruby_ore");
        PulverizerRecipeBuilder.build().input(TRContent.Gems.SAPPHIRE.asTag()).result(TRContent.Gems.SAPPHIRE.getDust().asItem()).export(exporter, PATH + "sapphire");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.SAPPHIRE.asTag()).result(TRContent.Gems.SAPPHIRE.getDust().asItem(), 2).export(exporter, PATH + "sapphire_ore");
        PulverizerRecipeBuilder.build().input(TRContent.Gems.YELLOW_GARNET.asTag()).result(TRContent.Gems.YELLOW_GARNET.getDust().asItem()).export(exporter, PATH + "yellow_garnet");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.SODALITE.asTag()).result(TRContent.Dusts.SODALITE.asItem()).export(exporter, PATH + "sodalite");
        PulverizerRecipeBuilder.build().input(TRContent.Ores.SPHALERITE.asTag()).result(TRContent.Dusts.SPHALERITE.asItem()).export(exporter, PATH + "sphalerite");
    }
    
    public static void addTechRebornFragmentRecipes(RecipeExporter exporter) {
        GrinderRecipeBuilder.build().input(TRContent.Ores.SODALITE.asTag()).result(TRContent.Dusts.SODALITE.asItem(), 12).result(TRContent.Dusts.ALUMINUM.asItem(), 3).export(exporter, PATH + "sodalite_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.SPHALERITE.asTag()).result(TRContent.Dusts.SPHALERITE.asItem(), 6).result(TRContent.Dusts.ZINC.asItem()).result(TRContent.SmallDusts.YELLOW_GARNET.asItem()).export(exporter, PATH + "sphalerite_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.PERIDOT.asTag()).result(TRContent.Gems.PERIDOT.asItem()).result(TRContent.SmallDusts.PERIDOT.asItem(), 6).result(TRContent.SmallDusts.EMERALD.asItem(), 2).export(exporter, PATH + "peridot_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.RUBY.asTag()).result(TRContent.Gems.RUBY.asItem()).result(TRContent.SmallDusts.RUBY.asItem(), 6).result(TRContent.SmallDusts.RED_GARNET.asItem(), 2).export(exporter, PATH + "ruby_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.SAPPHIRE.asTag()).result(TRContent.Gems.SAPPHIRE.asItem()).result(TRContent.SmallDusts.SAPPHIRE.asItem(), 6).result(TRContent.SmallDusts.PERIDOT.asItem(), 2).export(exporter, PATH + "sapphire_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.BAUXITE.asTag()).result(TRContent.Dusts.BAUXITE.asItem(), 4).result(TRContent.Dusts.ALUMINUM.asItem()).export(exporter, PATH + "bauxite_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.CINNABAR.asTag()).result(TRContent.Dusts.CINNABAR.asItem(), 5).result(TRContent.SmallDusts.REDSTONE.asItem(), 2).result(TRContent.SmallDusts.GLOWSTONE.asItem()).export(exporter, PATH + "cinnabar_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.GALENA.asTag()).result(TRContent.Dusts.GALENA.asItem(), 2).result(TRContent.Dusts.SULFUR.asItem()).export(exporter, PATH + "galena_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.IRIDIUM.asTag()).result(TRContent.RawMetals.IRIDIUM.asItem()).result(TRContent.SmallDusts.PLATINUM.asItem(), 2).export(exporter, PATH + "iridium_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.LEAD.asTag()).result(TRContent.RawMetals.LEAD.asItem(), 2).result(TRContent.SmallDusts.GALENA.asItem(), 2).export(exporter, PATH + "lead_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.PYRITE.asTag()).result(TRContent.Dusts.PYRITE.asItem(), 5).result(TRContent.Dusts.SULFUR.asItem(), 2).export(exporter, PATH + "pyrite_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.SHELDONITE.asTag()).result(TRContent.Dusts.PLATINUM.asItem(), 2).result(TRContent.Dusts.NICKEL.asItem()).result(TRContent.Nuggets.IRIDIUM.asItem(), 2).export(exporter, PATH + "sheldonite_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.SILVER.asTag()).result(TRContent.RawMetals.SILVER.asItem(), 2).result(TRContent.SmallDusts.GALENA.asItem(), 2).export(exporter, PATH + "silver_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.TIN.asTag()).result(TRContent.RawMetals.TIN.asItem(), 2).result(Items.IRON_NUGGET, 3).result(TRContent.Nuggets.ZINC.asItem(), 3).export(exporter, PATH + "tin_ore");
        GrinderRecipeBuilder.build().input(TRContent.Ores.TUNGSTEN.asTag()).result(TRContent.RawMetals.TUNGSTEN.asItem(), 2).result(Items.IRON_NUGGET, 7).result(TRContent.SmallDusts.MANGANESE.asItem(), 3).export(exporter, PATH + "tungsten_ore");
    }

    public static void addOritechFuels(RecipeExporter exporter) {
        offerTRFluidGeneratorRecipe(exporter, 1000, FluidContent.STILL_OIL.get(), "oil");
        offerTRFluidGeneratorRecipe(exporter, 1000, FluidContent.STILL_BIOFUEL.get(), "biofuel");
        offerTRFluidGeneratorRecipe(exporter, 5000, FluidContent.STILL_FUEL.get(), "fuel");
    }
    
    public static void addTechRebornFuels(RecipeExporter exporter) {
        BioGeneratorRecipeBuilder.build().input(TRContent.Parts.COMPRESSED_PLANTBALL.item).timeInSeconds(140).export(exporter, PATH + "compressedplantball");
        
        FuelGeneratorRecipeBuilder.build().fluidInput(ModFluids.OIL.getFluid(), 0.1f).timeInSeconds(3).export(exporter, PATH + "oil");
        FuelGeneratorRecipeBuilder.build().fluidInput(ModFluids.NITROFUEL.getFluid(), 0.1f).timeInSeconds(10).export(exporter, PATH + "nitrofuel");
        FuelGeneratorRecipeBuilder.build().fluidInput(ModFluids.NITROCOAL_FUEL.getFluid(), 0.1f).timeInSeconds(12).export(exporter, PATH + "nitrocoalfuel");
        FuelGeneratorRecipeBuilder.build().fluidInput(ModFluids.DIESEL.getFluid(), 0.1f).timeInSeconds(14).export(exporter, PATH + "diesel");
        FuelGeneratorRecipeBuilder.build().fluidInput(ModFluids.NITRO_DIESEL.getFluid(), 0.1f).timeInSeconds(16).export(exporter, PATH + "nitrodiesel");
    }
    
    public static void addDistillation(RecipeExporter exporter) {
        exporter.accept(Oritech.id(PATH + "distillation/oil"), new RebornRecipe.Default(ModRecipes.DISTILLATION_TOWER, List.of(cellIngredient(Fluids.EMPTY, 16), cellIngredient(FluidContent.STILL_OIL.get(), 16)), List.of(cellStack(ModFluids.DIESEL, 16), cellStack(ModFluids.SULFURIC_ACID, 15), cellStack(ModFluids.GLYCERYL, 1)), 20, 400), null);
        CentrifugeFluidRecipeBuilder.build().input(ItemContent.FLUXITE).fluidInput(ModFluids.OIL.getFluid()).fluidOutput(FluidContent.STILL_FUEL.get()).export(exporter, PATH + "fuel");
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
        exporter.accept(Oritech.id(PATH + "alloysmelter/" + suffix), new RebornRecipe.Default(ModRecipes.ALLOY_SMELTER, List.of(A, B), List.of(output), power, time), null);
    }

    public static void offerTRFluidGeneratorRecipe(RecipeExporter exporter, int power, Fluid fluid, String suffix) {
        exporter.accept(Oritech.id(PATH + "fluidgenerator/" + suffix), new FluidGeneratorRecipe(ModRecipes.SEMI_FLUID_GENERATOR, power, fluid), null);
    }
    
    public static void offerTRGrinderRecipe(RecipeExporter exporter, SizedIngredient input, ItemStack output, int power, int time, String suffix) {
        exporter.accept(Oritech.id(PATH + "grinder/" + suffix), new RebornRecipe.Default(ModRecipes.GRINDER, List.of(input), List.of(output), power, time), null);
    }
    
    public static void offerTRIndustrialGrinderRecipe(RecipeExporter exporter, SizedIngredient input, List<ItemStack> outputs, int power, int time, FluidInstance fluid, String suffix) {
        exporter.accept(Oritech.id(PATH + "industrial_grinder/" + suffix), new IndustrialGrinderRecipe(ModRecipes.INDUSTRIAL_GRINDER, List.of(input), outputs, power, time, fluid), null);
    }
}