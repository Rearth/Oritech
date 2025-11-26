package rearth.oritech.api.recipe.util;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.*;
import rearth.oritech.init.FluidContent;

import java.util.Arrays;
import java.util.List;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;

public class MetalProcessingChainBuilder {
    private String metalName;
    private String resourcePath = "";
    // ingredient should generally be used for recipe inputs and item for recipe output
    // wherever possible, use ConventionalItemTags (Fabric) or Tags.Items (Neoforge) for ingredients
    private Ingredient ore;
    private Ingredient rawOreIngredient;
    private Item rawOreItem;
    // should be a raw ore, secondary raw ore given when grinding ore blocks
    private Item rawOreByproduct;
    private Ingredient ingotIngredient;
    private Item ingotItem;
    private Ingredient nuggetIngredient;
    private Item nuggetItem;
    private Ingredient clumpIngredient;
    private Item clumpItem;
    private Item smallClumpItem;
    private Item dustItem;
    private Item smallDustItem;
    private Item centrifugeResult;
    private int centrifugeAmount;
    // usually a small dust (or nugget) given as a byproduct from the grinder or centrifuge
    private Item dustByproduct;
    private Item clumpByproduct;
    private int byproductAmount = 3;
    private Ingredient gemIngredient;
    private Item gemItem;
    private Ingredient gemCatalyst;
    private float timeMultiplier = 1f;
    // for compat use. no need to add vanilla processing for other mods' ores
    private boolean vanillaProcessing = false;
    private boolean skipCompactingRecipes = false;
    
    private MetalProcessingChainBuilder(String metalName) {
        this.metalName = metalName;
    }
    
    public static MetalProcessingChainBuilder build(String metalName) {
        return new MetalProcessingChainBuilder(metalName);
    }
    
    public MetalProcessingChainBuilder resourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }
    
    public MetalProcessingChainBuilder ore(Ingredient ore) {
        this.ore = ore;
        return this;
    }
    
    public MetalProcessingChainBuilder ore(TagKey<Item> oreTag) {
        return ore(Ingredient.of(oreTag));
    }
    
    public MetalProcessingChainBuilder ore(ItemLike ore) {
        return ore(Ingredient.of(ore));
    }
    
    public MetalProcessingChainBuilder rawOre(Ingredient rawOreIngredient, Item rawOre) {
        this.rawOreIngredient = rawOreIngredient;
        this.rawOreItem = rawOre;
        return this;
    }
    
    public MetalProcessingChainBuilder rawOre(TagKey<Item> rawOreTag, Item rawOre) {
        return rawOre(Ingredient.of(rawOreTag), rawOre);
    }
    
    public MetalProcessingChainBuilder rawOre(Item rawOre) {
        return rawOre(Ingredient.of(rawOre), rawOre);
    }
    
    public MetalProcessingChainBuilder rawOreByproduct(Item byproduct) {
        this.rawOreByproduct = byproduct;
        return this;
    }
    
    public MetalProcessingChainBuilder ingot(Ingredient ingotIngredient, Item ingot) {
        this.ingotIngredient = ingotIngredient;
        this.ingotItem = ingot;
        return this;
    }
    
    public MetalProcessingChainBuilder ingot(TagKey<Item> ingotTag, Item ingot) {
        return ingot(Ingredient.of(ingotTag), ingot);
    }
    
    public MetalProcessingChainBuilder ingot(Item ingot) {
        return ingot(Ingredient.of(ingot), ingot);
    }
    
    public MetalProcessingChainBuilder nugget(Ingredient nuggetIngredient, Item nugget) {
        this.nuggetIngredient = nuggetIngredient;
        this.nuggetItem = nugget;
        return this;
    }
    
    public MetalProcessingChainBuilder nugget(TagKey<Item> nuggetTag, Item nugget) {
        return nugget(Ingredient.of(nuggetTag), nugget);
    }
    
    public MetalProcessingChainBuilder nugget(Item nugget) {
        return nugget(Ingredient.of(nugget), nugget);
    }
    
    public MetalProcessingChainBuilder clump(Ingredient clumpIngredient, Item clump) {
        this.clumpIngredient = clumpIngredient;
        this.clumpItem = clump;
        return this;
    }
    
    public MetalProcessingChainBuilder clump(TagKey<Item> clumpTag, Item clump) {
        return clump(Ingredient.of(clumpTag), clump);
    }
    
    public MetalProcessingChainBuilder clump(Item clump) {
        return clump(Ingredient.of(clump), clump);
    }
    
    public MetalProcessingChainBuilder smallClump(Item smallClump) {
        this.smallClumpItem = smallClump;
        return this;
    }
    
    public MetalProcessingChainBuilder centrifugeResult(Item result, int amount) {
        this.centrifugeResult = result;
        this.centrifugeAmount = amount;
        return this;
    }
    
    public MetalProcessingChainBuilder centrifugeResult(Item result) {
        return centrifugeResult(result, 1);
    }
    
    public MetalProcessingChainBuilder clumpByproduct(Item byproduct) {
        this.clumpByproduct = byproduct;
        return this;
    }
    
    public MetalProcessingChainBuilder dustByproduct(Item byproduct) {
        this.dustByproduct = byproduct;
        return this;
    }
    
    public MetalProcessingChainBuilder byproductAmount(int amount) {
        this.byproductAmount = amount;
        return this;
    }
    
    public MetalProcessingChainBuilder dust(Item dust) {
        this.dustItem = dust;
        return this;
    }
    
    public MetalProcessingChainBuilder smallDust(Item smallDust) {
        this.smallDustItem = smallDust;
        return this;
    }
    
    public MetalProcessingChainBuilder gem(Ingredient gemIngredient, Item gem) {
        this.gemIngredient = gemIngredient;
        this.gemItem = gem;
        return this;
    }
    
    public MetalProcessingChainBuilder gem(TagKey<Item> gemTag, Item gem) {
        return gem(Ingredient.of(gemTag), gem);
    }
    
    public MetalProcessingChainBuilder gem(Item gem) {
        return gem(Ingredient.of(gem), gem);
    }
    
    public MetalProcessingChainBuilder gemCatalyst(Ingredient gemCatalyst) {
        this.gemCatalyst = gemCatalyst;
        return this;
    }
    
    public MetalProcessingChainBuilder gemCatalyst(TagKey<Item> gemCatalyst) {
        return gemCatalyst(Ingredient.of(gemCatalyst));
    }
    
    public MetalProcessingChainBuilder gemCatalyst(Item gemCatalyst) {
        return gemCatalyst(Ingredient.of(gemCatalyst));
    }
    
    public MetalProcessingChainBuilder timeMultiplier(float timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
        return this;
    }
    
    public MetalProcessingChainBuilder vanillaProcessing() {
        this.vanillaProcessing = true;
        return this;
    }
    
    public MetalProcessingChainBuilder skipCompacting() {
        this.skipCompactingRecipes = true;
        return this;
    }
    
    private void validate(String path) throws IllegalStateException {
        if (ore == null)
            throw new IllegalStateException("ore is required for metal processing chain " + path);
        if (rawOreItem == null)
            throw new IllegalStateException("raw ore is required for metal processing chain " + path);
        if ((dustItem != null || vanillaProcessing == true) && ingotItem == null)
            throw new IllegalStateException("ingot is required if dust is provided or vanilla processing is required for metal processing chain " + path);
        if ((smallClumpItem != null || smallDustItem != null) && nuggetItem == null)
            throw new IllegalStateException("nugget item is required if small clump or small dust are provided for metal processing chain " + path);
        if (centrifugeResult != null && centrifugeAmount < 1)
            throw new IllegalStateException("centrifugeAmount must be >= 1 if centrifugeOutput is provided for metal processing chain " + path);
        if (clumpItem != null && (centrifugeResult == null && gemItem == null))
            throw new IllegalStateException("either centrifugeResult or gemItem is required if clump is provided for metal processing chain " + path);
    }
    
    public void export(RecipeOutput exporter) {
        validate(resourcePath + "ore/" + metalName);
        
        // ore block -> raw ores
        PulverizerRecipeBuilder.build().input(ore).result(rawOreItem, 2).timeMultiplier(timeMultiplier).export(exporter, resourcePath + "ore/" + metalName);
        var grinderOreRecipe = GrinderRecipeBuilder.build().input(ore).result(rawOreItem, 2).time(140).timeMultiplier(timeMultiplier);
        if (rawOreByproduct != null)
            grinderOreRecipe.result(rawOreByproduct);
        grinderOreRecipe.export(exporter, resourcePath + "ore/" + metalName);
        
        // raw ores -> dusts in pulverizer
        if (dustItem != null) {
            PulverizerRecipeBuilder.build()
              .input(rawOreIngredient)
              .result(dustItem)
              .result(firstNonNullOptional(smallDustItem, nuggetItem), 3)
              .timeMultiplier(timeMultiplier)
              .export(exporter, resourcePath + "raw/" + metalName);
        }
        
        // raw ores -> clumps (falling back to dusts) in grinder
        if (clumpItem != null || dustItem != null) {
            GrinderRecipeBuilder.build()
              .input(rawOreIngredient)
              .result(firstNonNull(clumpItem, dustItem))
              .result(firstNonNullOptional(smallClumpItem, smallDustItem, nuggetItem), 3)
              .result(Optional.fromNullable(clumpByproduct), byproductAmount)
              .time(140).timeMultiplier(timeMultiplier)
              .export(exporter, resourcePath + "raw/" + metalName);
        }
        
        // raw ores -> clumps (falling back to dusts) in refinery with sheol fire
        if (clumpItem != null || dustItem != null) {
            RefineryRecipeBuilder.build()
              .input(rawOreIngredient)
              .fluidInput(FluidContent.STILL_SHEOL_FIRE.get(), 0.25f)
              .result(firstNonNull(clumpItem, dustItem), 2)
              .fluidOutput(Fluids.LAVA, 0.1f)
              .timeMultiplier(timeMultiplier)
              .export(exporter, resourcePath + "rawsheol/" + metalName);
        }
        
        // clump processing into gems in centrifuge
        if (clumpItem != null) {
            // dry variant
            CentrifugeRecipeBuilder.build()
              .input(clumpIngredient)
              .result(firstNonNull(centrifugeResult, gemItem), centrifugeResult != null ? centrifugeAmount : 1)
              .result(Optional.fromNullable(dustByproduct), byproductAmount)
              .timeMultiplier(timeMultiplier)
              .export(exporter, resourcePath + "clump/" + metalName);
            // water washed
            CentrifugeFluidRecipeBuilder.build()
              .input(clumpIngredient)
              .fluidInput(Fluids.WATER)
              .result(firstNonNull(centrifugeResult, gemItem), centrifugeResult != null ? centrifugeAmount * 2 : 2)
              .timeMultiplier(timeMultiplier * 1.5f)
              .export(exporter, resourcePath + "clump/" + metalName);
            // sulfuric acid washing
            CentrifugeFluidRecipeBuilder.build()
              .input(clumpIngredient)
              .fluidInput(FluidContent.STILL_SULFURIC_ACID.get())
              .result(firstNonNull(centrifugeResult, gemItem), centrifugeResult != null ? centrifugeAmount * 3 : 3)
              .fluidOutput(FluidContent.STILL_MINERAL_SLURRY.get(), 0.25f)
              .timeMultiplier(timeMultiplier * 1.5f)
              .export(exporter, resourcePath + "clumpacid/" + metalName);
        }
        
        // gems to dust (doubling)
        if (gemIngredient != null) {
            // atomic forge: 1 gem -> 2 ingots
            AtomicForgeRecipeBuilder.build().input(gemIngredient).input(gemCatalyst).input(gemCatalyst).result(dustItem, 2).time(20).export(exporter, resourcePath + "dust/" + metalName);
            
            // foundry alternative: 2 gems -> 3 ingots
            FoundryRecipeBuilder.build().input(gemIngredient).input(gemIngredient).result(ingotItem, 3).export(exporter, resourcePath + "gem/" + metalName);
        }
        
        // ingots/nuggets to dust
        if (dustItem != null)
            RecipeHelpers.addDustRecipe(exporter, ingotIngredient, dustItem, resourcePath + "dust/" + metalName);
        if (smallDustItem != null)
            RecipeHelpers.addDustRecipe(exporter, nuggetIngredient, smallDustItem, resourcePath + "smalldust/" + metalName);
        
        // smelting/compacting
        // Using item instead of ingredient for recipe inputs, as that's what the offerSmelting/offerBlasting methods accept
        // This should be fine, because any mod that adds ores, dusts, etc. will provide their own smelting/blasting recipes
        if (vanillaProcessing) {
            if (dustItem != null) {
                OritechRecipeGenerator.oreSmelting(exporter, List.of(dustItem), RecipeCategory.MISC, ingotItem, 1f, 200, Oritech.MOD_ID);
                OritechRecipeGenerator.oreBlasting(exporter, List.of(dustItem), RecipeCategory.MISC, ingotItem, 1f, 100, Oritech.MOD_ID);
                OritechRecipeGenerator.threeByThreePacker(exporter, RecipeCategory.MISC, dustItem, smallDustItem);
            }
            if (smallDustItem != null) {
                OritechRecipeGenerator.oreSmelting(exporter, List.of(smallDustItem), RecipeCategory.MISC, nuggetItem, 0.5f, 50, Oritech.MOD_ID);
                OritechRecipeGenerator.oreBlasting(exporter, List.of(smallDustItem), RecipeCategory.MISC, nuggetItem, 0.5f, 25, Oritech.MOD_ID);
            }
            if (gemItem != null) {
                OritechRecipeGenerator.oreSmelting(exporter, List.of(gemItem), RecipeCategory.MISC, ingotItem, 1f, 200, Oritech.MOD_ID);
                OritechRecipeGenerator.oreBlasting(exporter, List.of(gemItem), RecipeCategory.MISC, ingotItem, 1f, 100, Oritech.MOD_ID);
            }
            if (clumpItem != null && smallClumpItem != null)
                OritechRecipeGenerator.threeByThreePacker(exporter, RecipeCategory.MISC, clumpItem, smallClumpItem);
            if (nuggetItem != null && !skipCompactingRecipes) {    // to avoid duplicate vanilla nugget -> item recipes
                OritechRecipeGenerator.threeByThreePacker(exporter, RecipeCategory.MISC, ingotItem, nuggetItem);
                OritechRecipeGenerator.threeByThreePacker(exporter, RecipeCategory.MISC, nuggetItem, ingotItem);
            }
        }
    }
    
    private Item firstNonNull(Item... items) {
        return Iterables.find(Arrays.asList(items), Predicates.notNull());
    }
    
    private Optional<Item> firstNonNullOptional(Item... items) {
        return Iterables.tryFind(Arrays.asList(items), Predicates.notNull());
    }
}
