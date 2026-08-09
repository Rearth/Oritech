package rearth.oritech.datagen.builders.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import rearth.oritech.Oritech;
import rearth.oritech.datagen.RecipeGenerator;
import rearth.oritech.datagen.builders.*;
import rearth.oritech.init.FluidContent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class MetalProcessingChainBuilder {
    private final String metalName;
    private String prefix = "";
    // ingredient should generally be used for recipe inputs and item for recipe output
    // wherever possible, use Tags.Items for ingredients
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
    private final HolderLookup.Provider registryAccess;

    public MetalProcessingChainBuilder(String metalName, HolderLookup.Provider registryAccess) {
        this.metalName = metalName;
        this.registryAccess = registryAccess;
    }

    public MetalProcessingChainBuilder prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public MetalProcessingChainBuilder ore(Ingredient ore) {
        this.ore = ore;
        return this;
    }

    public MetalProcessingChainBuilder ore(TagKey<Item> oreTag) {
        return ore(of(oreTag));
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
        return rawOre(of(rawOreTag), rawOre);
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
        return ingot(of(ingotTag), ingot);
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
        return nugget(of(nuggetTag), nugget);
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
        return clump(of(clumpTag), clump);
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
        return gem(of(gemTag), gem);
    }

    public MetalProcessingChainBuilder gem(Item gem) {
        return gem(Ingredient.of(gem), gem);
    }

    public MetalProcessingChainBuilder gemCatalyst(Ingredient gemCatalyst) {
        this.gemCatalyst = gemCatalyst;
        return this;
    }

    public MetalProcessingChainBuilder gemCatalyst(TagKey<Item> gemCatalyst) {
        return gemCatalyst(of(gemCatalyst));
    }

    public MetalProcessingChainBuilder gemCatalyst(Item gemCatalyst) {
        return gemCatalyst(Ingredient.of(gemCatalyst));
    }

    public MetalProcessingChainBuilder rawOre(Ingredient rawOreIngredient, Supplier<? extends Item> rawOre) {
        return rawOre(rawOreIngredient, rawOre.get());
    }
    public MetalProcessingChainBuilder rawOre(TagKey<Item> rawOreTag, Supplier<? extends Item> rawOre) {
        return rawOre(of(rawOreTag), rawOre.get());
    }
    public MetalProcessingChainBuilder rawOre(Supplier<? extends Item> rawOre) {
        return rawOre(Ingredient.of(rawOre.get()), rawOre.get());
    }

    public MetalProcessingChainBuilder rawOreByproduct(Supplier<? extends Item> byproduct) {
        this.rawOreByproduct = byproduct.get();
        return this;
    }

    public MetalProcessingChainBuilder ingot(Ingredient ingotIngredient, Supplier<? extends Item> ingot) {
        return ingot(ingotIngredient, ingot.get());
    }
    public MetalProcessingChainBuilder ingot(TagKey<Item> ingotTag, Supplier<? extends Item> ingot) {
        return ingot(of(ingotTag), ingot.get());
    }
    public MetalProcessingChainBuilder ingot(Supplier<? extends Item> ingot) {
        return ingot(Ingredient.of(ingot.get()), ingot.get());
    }

    public MetalProcessingChainBuilder nugget(Ingredient nuggetIngredient, Supplier<? extends Item> nugget) {
        return nugget(nuggetIngredient, nugget.get());
    }
    public MetalProcessingChainBuilder nugget(TagKey<Item> nuggetTag, Supplier<? extends Item> nugget) {
        return nugget(of(nuggetTag), nugget.get());
    }
    public MetalProcessingChainBuilder nugget(Supplier<? extends Item> nugget) {
        return nugget(Ingredient.of(nugget.get()), nugget.get());
    }

    public MetalProcessingChainBuilder clump(Ingredient clumpIngredient, Supplier<? extends Item> clump) {
        return clump(clumpIngredient, clump.get());
    }
    public MetalProcessingChainBuilder clump(TagKey<Item> clumpTag, Supplier<? extends Item> clump) {
        return clump(of(clumpTag), clump.get());
    }
    public MetalProcessingChainBuilder clump(Supplier<? extends Item> clump) {
        return clump(Ingredient.of(clump.get()), clump.get());
    }

    public MetalProcessingChainBuilder smallClump(Supplier<? extends Item> smallClump) {
        this.smallClumpItem = smallClump.get();
        return this;
    }

    public MetalProcessingChainBuilder centrifugeResult(Supplier<? extends Item> result, int amount) {
        this.centrifugeResult = result.get();
        this.centrifugeAmount = amount;
        return this;
    }
    public MetalProcessingChainBuilder centrifugeResult(Supplier<? extends Item> result) {
        return centrifugeResult(result, 1);
    }

    public MetalProcessingChainBuilder clumpByproduct(Supplier<? extends Item> byproduct) {
        this.clumpByproduct = byproduct.get();
        return this;
    }

    public MetalProcessingChainBuilder dustByproduct(Supplier<? extends Item> byproduct) {
        this.dustByproduct = byproduct.get();
        return this;
    }

    public MetalProcessingChainBuilder dust(Supplier<? extends Item> dust) {
        this.dustItem = dust.get();
        return this;
    }

    public MetalProcessingChainBuilder smallDust(Supplier<? extends Item> smallDust) {
        this.smallDustItem = smallDust.get();
        return this;
    }

    public MetalProcessingChainBuilder gem(Ingredient gemIngredient, Supplier<? extends Item> gem) {
        return gem(gemIngredient, gem.get());
    }
    public MetalProcessingChainBuilder gem(TagKey<Item> gemTag, Supplier<? extends Item> gem) {
        return gem(of(gemTag), gem.get());
    }
    public MetalProcessingChainBuilder gem(Supplier<? extends Item> gem) {
        return gem(Ingredient.of(gem.get()), gem.get());
    }

    public MetalProcessingChainBuilder gemCatalyst(Supplier<? extends Item> gemCatalyst) {
        return gemCatalyst(Ingredient.of(gemCatalyst.get()));
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
        if (rawOreItem == null)
            throw new IllegalStateException("raw ore is required for metal processing chain " + path);
        if ((dustItem != null || vanillaProcessing) && ingotItem == null)
            throw new IllegalStateException("ingot is required if dust is provided or vanilla processing is required for metal processing chain " + path);
        if ((smallClumpItem != null || smallDustItem != null) && nuggetItem == null)
            throw new IllegalStateException("nugget item is required if small clump or small dust are provided for metal processing chain " + path);
        if (centrifugeResult != null && centrifugeAmount < 1)
            throw new IllegalStateException("centrifugeAmount must be >= 1 if centrifugeOutput is provided for metal processing chain " + path);
        if (clumpItem != null && (centrifugeResult == null && gemItem == null))
            throw new IllegalStateException("either centrifugeResult or gemItem is required if clump is provided for metal processing chain " + path);
    }

    public void export(RecipeOutput exporter) {
        validate("ore/" + metalName);

        // ore block -> raw ores
        if (ore != null) {
            new PulverizerRecipeBuilder(registryAccess).input(ore).result(rawOreItem, 2).timeMultiplier(timeMultiplier).export(exporter, prefix, "ore/" + metalName, Oritech.MOD_ID);
            var grinderOreRecipe = new GrinderRecipeBuilder(registryAccess).input(ore).result(rawOreItem, 2).timeMultiplier(timeMultiplier);
            if (rawOreByproduct != null)
                grinderOreRecipe.result(rawOreByproduct);
            grinderOreRecipe.export(exporter, prefix, "ore/" + metalName, Oritech.MOD_ID);
        }

        // raw ores -> dusts in pulverizer
        if (dustItem != null) {
            new PulverizerRecipeBuilder(registryAccess)
                    .input(rawOreIngredient)
                    .result(dustItem)
                    .result(firstNonNullOptional(smallDustItem, nuggetItem), 3)
                    .timeMultiplier(timeMultiplier)
                    .export(exporter, prefix, "raw/" + metalName, Oritech.MOD_ID);
        }

        // raw ores -> clumps (falling back to dusts) in grinder
        if (clumpItem != null || dustItem != null) {
            new GrinderRecipeBuilder(registryAccess)
                    .input(rawOreIngredient)
                    .result(firstNonNull(clumpItem, dustItem))
                    .result(firstNonNullOptional(smallClumpItem, smallDustItem, nuggetItem), 3)
                    .result(Optional.ofNullable(clumpByproduct), byproductAmount)
                    .timeMultiplier(timeMultiplier)
                    .export(exporter, prefix, "raw/" + metalName, Oritech.MOD_ID);
        }

        // raw ores -> clumps (falling back to dusts) in refinery with sheol fire
        if (clumpItem != null || dustItem != null) {
            new RefineryRecipeBuilder(registryAccess)
                    .input(rawOreIngredient)
                    .fluidInput(FluidContent.STILL_SHEOL_FIRE.get(), 0.25f)
                    .result(firstNonNull(clumpItem, dustItem), 2)
                    .fluidOutput(Fluids.LAVA, 0.1f)
                    .timeMultiplier(timeMultiplier)
                    .export(exporter, prefix, "rawsheol/" + metalName, Oritech.MOD_ID);
        }

        // clump processing into gems in centrifuge
        if (clumpItem != null) {
            // dry variant
            new CentrifugeRecipeBuilder(registryAccess)
                    .input(clumpIngredient)
                    .result(firstNonNull(centrifugeResult, gemItem), centrifugeResult != null ? centrifugeAmount : 1)
                    .result(Optional.ofNullable(dustByproduct), byproductAmount)
                    .timeMultiplier(timeMultiplier)
                    .export(exporter, prefix, "clump/" + metalName, Oritech.MOD_ID);
            // water washed
            new CentrifugeFluidRecipeBuilder(registryAccess)
                    .input(clumpIngredient)
                    .fluidInput(Fluids.WATER)
                    .result(firstNonNull(centrifugeResult, gemItem), centrifugeResult != null ? centrifugeAmount * 2 : 2)
                    .timeMultiplier(timeMultiplier * 1.5f)
                    .export(exporter, prefix, "clump/" + metalName, Oritech.MOD_ID);
            // sulfuric acid washing
            new CentrifugeFluidRecipeBuilder(registryAccess)
                    .input(clumpIngredient)
                    .fluidInput(FluidContent.STILL_SULFURIC_ACID.get())
                    .result(firstNonNull(centrifugeResult, gemItem), centrifugeResult != null ? centrifugeAmount * 3 : 3)
                    .fluidOutput(FluidContent.STILL_MINERAL_SLURRY.get(), 0.25f)
                    .timeMultiplier(timeMultiplier * 1.5f)
                    .export(exporter, prefix, "clumpacid/" + metalName, Oritech.MOD_ID);
        }

        // gems to dust (doubling)
        if (gemIngredient != null) {
            // atomic forge: 1 gem -> 2 ingots
            new AtomicForgeRecipeBuilder(registryAccess).input(gemIngredient).input(gemCatalyst).input(gemCatalyst).result(dustItem, 2).time(20).export(exporter, prefix, "dust/" + metalName, Oritech.MOD_ID);

            // foundry alternative: 2 gems -> 3 ingots
            new FoundryRecipeBuilder(registryAccess).input(gemIngredient).input(gemIngredient).result(ingotItem, 3).export(exporter, prefix, "gem/" + metalName, Oritech.MOD_ID);
        }

        // ingots/nuggets to dust
        if (dustItem != null)
            RecipeHelpers.addDustRecipe(exporter, ingotIngredient, dustItem, null, prefix, "dust/" + metalName, registryAccess);
        if (smallDustItem != null)
            RecipeHelpers.addDustRecipe(exporter, nuggetIngredient, smallDustItem, null, prefix, "smalldust/" + metalName, registryAccess);

        // smelting/compacting
        // Using item instead of ingredient for recipe inputs, as that's what the offerSmelting/offerBlasting methods accept
        // This should be fine, because any mod that adds ores, dusts, etc. will provide their own smelting/blasting recipes
        if (vanillaProcessing) {
            if (dustItem != null) {
                RecipeGenerator.oreSmelting(exporter, List.of(dustItem), RecipeCategory.MISC, ingotItem, 1f, 200, Oritech.MOD_ID);
                RecipeGenerator.oreBlasting(exporter, List.of(dustItem), RecipeCategory.MISC, ingotItem, 1f, 100, Oritech.MOD_ID);
                RecipeGenerator.threeByThreePacker(exporter, RecipeCategory.MISC, dustItem, smallDustItem);
            }
            if (smallDustItem != null) {
                RecipeGenerator.oreSmelting(exporter, List.of(smallDustItem), RecipeCategory.MISC, nuggetItem, 0.5f, 50, Oritech.MOD_ID);
                RecipeGenerator.oreBlasting(exporter, List.of(smallDustItem), RecipeCategory.MISC, nuggetItem, 0.5f, 25, Oritech.MOD_ID);
            }
            if (gemItem != null) {
                RecipeGenerator.oreSmelting(exporter, List.of(gemItem), RecipeCategory.MISC, ingotItem, 1f, 200, Oritech.MOD_ID);
                RecipeGenerator.oreBlasting(exporter, List.of(gemItem), RecipeCategory.MISC, ingotItem, 1f, 100, Oritech.MOD_ID);
            }
            if (clumpItem != null && smallClumpItem != null)
                RecipeGenerator.threeByThreePacker(exporter, RecipeCategory.MISC, clumpItem, smallClumpItem);
            if (nuggetItem != null && !skipCompactingRecipes) {    // to avoid duplicate vanilla nugget -> item recipes
                RecipeGenerator.threeByThreePacker(exporter, RecipeCategory.MISC, ingotItem, nuggetItem);
                var inputName = BuiltInRegistries.ITEM.getKey(ingotItem).getPath();
                ShapelessRecipeBuilder.shapeless(BuiltInRegistries.ITEM, RecipeCategory.MISC, nuggetItem, 9)
                        .requires(ingotItem, 1)
                        .unlockedBy("has_" + inputName, InventoryChangeTrigger.TriggerInstance.hasItems(ingotItem))
                        .save(exporter);
            }
        }
    }

    private Item firstNonNull(Item... items) {
        return Iterables.find(Arrays.asList(items), Predicates.notNull());
    }

    private Optional<Item> firstNonNullOptional(Item... items) {
        return Arrays.stream(items).filter(Objects::nonNull).findFirst();
    }

    private Ingredient of(TagKey<Item> item) {
        return Ingredient.of(this.registryAccess.get(item).orElseThrow());
    }
}
