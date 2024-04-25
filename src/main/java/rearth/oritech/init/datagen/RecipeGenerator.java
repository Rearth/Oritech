package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.datagen.data.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.FluidStack;

import java.util.List;

public class RecipeGenerator extends FabricRecipeProvider {
    
    public RecipeGenerator(FabricDataOutput output) {
        super(output);
    }
    
    @Override
    public void generate(RecipeExporter exporter) {
        
        // iron chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(ItemTags.IRON_ORES),
          Ingredient.ofItems(Items.RAW_IRON),
          Items.RAW_IRON,
          ItemContent.RAW_NICKEL,
          ItemContent.IRON_CLUMP,
          ItemContent.SMALL_IRON_CLUMP,
          ItemContent.SMALL_NICKEL_CLUMP,
          ItemContent.IRON_DUST,
          ItemContent.SMALL_IRON_DUST,
          ItemContent.SMALL_NICKEL_DUST,
          ItemContent.IRON_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          Items.IRON_NUGGET,
          Items.IRON_INGOT,
          1f,
          "_iron"
        );
        
        // copper chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(ItemTags.COPPER_ORES),
          Ingredient.ofItems(Items.RAW_COPPER),
          Items.RAW_COPPER,
          Items.RAW_GOLD,
          ItemContent.COPPER_CLUMP,
          ItemContent.SMALL_COPPER_CLUMP,
          ItemContent.SMALL_GOLD_CLUMP,
          ItemContent.COPPER_DUST,
          ItemContent.SMALL_COPPER_DUST,
          ItemContent.SMALL_GOLD_DUST,
          ItemContent.COPPER_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          ItemContent.COPPER_NUGGET,
          Items.COPPER_INGOT,
          1f,
          "_copper"
        );
        
        // gold chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(ItemTags.GOLD_ORES),
          Ingredient.ofItems(Items.RAW_GOLD),
          Items.RAW_GOLD,
          Items.RAW_COPPER,
          ItemContent.GOLD_CLUMP,
          ItemContent.SMALL_GOLD_CLUMP,
          ItemContent.SMALL_COPPER_CLUMP,
          ItemContent.GOLD_DUST,
          ItemContent.SMALL_GOLD_DUST,
          ItemContent.SMALL_COPPER_DUST,
          ItemContent.GOLD_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          Items.GOLD_NUGGET,
          Items.GOLD_INGOT,
          1f,
          "_gold"
        );
        
        // nickel chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(TagContent.NICKEL_ORES),
          Ingredient.ofItems(ItemContent.RAW_NICKEL),
          ItemContent.RAW_NICKEL,
          ItemContent.RAW_PLATINUM,
          ItemContent.NICKEL_CLUMP,
          ItemContent.SMALL_NICKEL_CLUMP,
          ItemContent.SMALL_PLATINUM_CLUMP,
          ItemContent.NICKEL_DUST,
          ItemContent.SMALL_NICKEL_DUST,
          ItemContent.SMALL_PLATINUM_DUST,
          ItemContent.NICKEL_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          ItemContent.NICKEL_NUGGET,
          ItemContent.NICKEL_INGOT,
          1f,
          "_nickel"
        );
        
        // platinum chain
        addMetalProcessingChain(exporter,
          Ingredient.fromTag(TagContent.PLATINUM_ORES),
          Ingredient.ofItems(ItemContent.RAW_PLATINUM),
          ItemContent.RAW_PLATINUM,
          ItemContent.FLUXITE,
          ItemContent.PLATINUM_CLUMP,
          ItemContent.SMALL_PLATINUM_CLUMP,
          Items.AMETHYST_SHARD,
          ItemContent.PLATINUM_DUST,
          ItemContent.SMALL_PLATINUM_DUST,
          Items.AMETHYST_SHARD,
          ItemContent.PLATINUM_GEM,
          Ingredient.ofItems(ItemContent.BANANA),
          ItemContent.PLATINUM_NUGGET,
          ItemContent.PLATINUM_INGOT,
          1.5f,
          "_platinum"
        );
        
        addAlloyRecipe(exporter, Ingredient.fromTag(TagContent.PLATINUM_INGOTS), Ingredient.ofItems(Items.NETHERITE_INGOT), ItemContent.DURATIUM_INGOT, "_duratium");
        addAlloyRecipe(exporter, Items.GOLD_INGOT, Items.REDSTONE, ItemContent.ELECTRUM_INGOT, "_electrum");
        addAlloyRecipe(exporter, Ingredient.ofItems(Items.DIAMOND), Ingredient.fromTag(TagContent.NICKEL_INGOTS), ItemContent.ADAMANT_INGOT, "_adamant");
        addAlloyRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), Ingredient.ofItems(ItemContent.FLUXITE), ItemContent.ENERGITE_INGOT, "_energite");
        addAlloyRecipe(exporter, Items.IRON_INGOT, Items.COAL, ItemContent.STEEL_INGOT, "_steel");
        
        addDustRecipe(exporter, Ingredient.ofItems(Items.COPPER_INGOT), ItemContent.COPPER_DUST, "_copper");
        addDustRecipe(exporter, Ingredient.ofItems(Items.IRON_INGOT), ItemContent.IRON_DUST, "_iron");
        addDustRecipe(exporter, Ingredient.ofItems(Items.GOLD_INGOT), ItemContent.GOLD_DUST, "_gold");
        addDustRecipe(exporter, Ingredient.fromTag(TagContent.NICKEL_INGOTS), ItemContent.NICKEL_DUST, "_nickel");
        addDustRecipe(exporter, Ingredient.fromTag(TagContent.PLATINUM_INGOTS), ItemContent.PLATINUM_DUST, "_platinum");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.BIOSTEEL_INGOT), ItemContent.BIOSTEEL_DUST, "_biosteel");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.PROMETHEUM_INGOT), ItemContent.PROMETHEUM_DUST, "_promethium");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.DURATIUM_INGOT), ItemContent.DURATIUM_DUST, "_duratium");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.ELECTRUM_INGOT), ItemContent.ELECTRUM_DUST, "_electrum");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.ADAMANT_INGOT), ItemContent.ADAMANT_DUST, "_adamant");
        addDustRecipe(exporter, Ingredient.ofItems(ItemContent.ENERGITE_INGOT), ItemContent.ENERGITE_DUST, "_energite");
        addDustRecipe(exporter, Ingredient.fromTag(TagContent.STEEL_INGOTS), ItemContent.STEEL_DUST, "_steel");
        
    }
    
    private void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        var pulverizerDefaultSpeed = 300;
        var grinderDefaultSpeed = 100;
        
        var pulverizer = new OritechRecipe(pulverizerDefaultSpeed, List.of(ingot), List.of(new ItemStack(dust)), RecipeContent.PULVERIZER, null, null);
        var grinder = new OritechRecipe(grinderDefaultSpeed, List.of(ingot), List.of(new ItemStack(dust)), RecipeContent.GRINDER, null, null);
        
        exporter.accept(new Identifier(Oritech.MOD_ID, "pulverizerdust" + suffix), pulverizer, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "grinderdust" + suffix), grinder, null);
    }
    
    private void addAlloyRecipe(RecipeExporter exporter, Item A, Item B, Item result, String suffix) {
        addAlloyRecipe(exporter, Ingredient.ofItems(A), Ingredient.ofItems(B), result, suffix);
    }
    
    private void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, String suffix) {
        var foundryDefaultSpeed = 300;
        var entry = new OritechRecipe(foundryDefaultSpeed, List.of(A, B), List.of(new ItemStack(result)), RecipeContent.FOUNDRY, null, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "foundryalloy" + suffix), entry, null);
    }
    
    private void addMetalProcessingChain(RecipeExporter exporter, Ingredient oreInput, Ingredient rawOre, Item rawMain, Item rawSecondary, Item clump, Item smallClump,
                                         Item smallSecondaryClump, Item dust, Item smallDust, Item smallSecondaryDust, Item gem, Ingredient gemCatalyst, Item nugget,
                                         Item ingot, float timeMultiplier, String suffix) {
                
        // ore block -> raw ores
        var pulverizerOre = new OritechRecipe((int) (400 * timeMultiplier), List.of(oreInput), List.of(new ItemStack(rawMain, 2)), RecipeContent.PULVERIZER, null, null);
        var grinderOre = new OritechRecipe((int) (400 * timeMultiplier), List.of(oreInput), List.of(new ItemStack(rawMain, 2), new ItemStack(rawSecondary, 1)), RecipeContent.GRINDER, null, null);
        
        // raw ores -> dusts / clumps
        var pulverizerRaw = new OritechRecipe((int) (500 * timeMultiplier), List.of(rawOre), List.of(new ItemStack(dust, 1), new ItemStack(smallDust, 3)), RecipeContent.PULVERIZER, null, null);
        var grinderRaw = new OritechRecipe((int) (800 * timeMultiplier), List.of(rawOre), List.of(new ItemStack(clump, 1), new ItemStack(smallClump, 3), new ItemStack(smallSecondaryClump, 3)), RecipeContent.GRINDER, null, null);
        
        // clump processing
        var centrifugeClumpDry = new OritechRecipe((int) (400 * timeMultiplier), List.of(Ingredient.ofItems(clump)), List.of(new ItemStack(dust, 1), new ItemStack(smallSecondaryDust, 3)), RecipeContent.CENTRIFUGE, null, null);
        var centrifugeClumpWet = new OritechRecipe((int) (600 * timeMultiplier), List.of(Ingredient.ofItems(clump)), List.of(new ItemStack(dust, 2)), RecipeContent.CENTRIFUGE_FLUID, new FluidStack(FluidVariant.of(Fluids.WATER), 81000), null);
        
        // gems
        var atomicForgeDust = new OritechRecipe(3, List.of(Ingredient.ofItems(dust), gemCatalyst), List.of(new ItemStack(gem, 1)), RecipeContent.ATOMIC_FORGE, null, null);
        var foundryGem = new OritechRecipe(800, List.of(Ingredient.ofItems(gem), Ingredient.ofItems(gem)), List.of(new ItemStack(ingot, 4)), RecipeContent.FOUNDRY, null, null);
        
        // smelting/compacting
        RecipeProvider.offerSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingot, 0.5f, 300, Oritech.MOD_ID);
        RecipeProvider.offerSmelting(exporter, List.of(smallDust), RecipeCategory.MISC, nugget, 0.5f, 300, Oritech.MOD_ID);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, clump, smallClump);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, dust, smallDust);
        RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, ingot, nugget);
        
        // registration
        exporter.accept(new Identifier(Oritech.MOD_ID, "pulverizerore" + suffix), pulverizerOre, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "grinderore" + suffix), grinderOre, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "pulverizerraw" + suffix), pulverizerRaw, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "grinderraw" + suffix), grinderRaw, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "centrifugeclumpdry" + suffix), centrifugeClumpDry, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "centrifugeclumpwet" + suffix), centrifugeClumpWet, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "atomicforgedust" + suffix), atomicForgeDust, null);
        exporter.accept(new Identifier(Oritech.MOD_ID, "foundrygem" + suffix), foundryGem, null);
        
    }
}
