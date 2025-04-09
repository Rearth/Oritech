package rearth.oritech.util.datagen;

import net.minecraft.item.Item;
import net.minecraft.recipe.Ingredient;

public record OreTransform(
    // ingredient should generally be used for recipe inputs and item for recipe output
    // wherever possible, use ConventionalItemTags for ingredients
    Ingredient ore,
    Ingredient rawOreIngredient, Item rawOreItem, Item rawOreByproduct,
    Ingredient clumpIngredient, Item clumpItem,
    Ingredient smallClumpIngredient, Item smallClumpItem, Item smallClumpByproduct,
    Ingredient dustIngredient, Item dustItem,
    Ingredient smallDustIngredient, Item smallDustItem, Item smallDustByproduct,
    Ingredient gemIngredient, Item gemItem,
    Ingredient gemCatalyst,
    Ingredient nuggetIngredient, Item nuggetItem,
    Ingredient ingotIngredient, Item ingotItem,
    float timeMultiplier, 
    String name,
    int byproductAmount) {}
