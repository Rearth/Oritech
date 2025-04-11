package rearth.oritech.api.recipe;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.Item;
import net.minecraft.recipe.Ingredient;

public record OreTransform(
    // ingredient should generally be used for recipe inputs and item for recipe output
    // wherever possible, use ConventionalItemTags for ingredients
    Ingredient ore,
    Ingredient rawOreIngredient, Item rawOreItem, Item rawOreByproduct,
    @Nullable Ingredient clumpIngredient, @Nullable Item clumpItem,
    @Nullable Ingredient smallClumpIngredient, @Nullable Item smallClumpItem, Item smallClumpByproduct,
    @Nullable Ingredient dustIngredient, @Nullable Item dustItem,
    @Nullable Ingredient smallDustIngredient, @Nullable Item smallDustItem, @Nullable Item smallDustByproduct,
    @Nullable Ingredient gemIngredient, @Nullable Item gemItem,
    @Nullable Ingredient gemCatalyst,
    Ingredient nuggetIngredient, Item nuggetItem,
    Ingredient ingotIngredient, Item ingotItem,
    float timeMultiplier, 
    String name,
    int byproductAmount,
    // for compat use. no need to add vanilla processing for other mods' ores
    boolean addVanillaProcessing) {}
