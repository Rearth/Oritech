package rearth.oritech.api.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.AugmentRecipe;
import rearth.oritech.init.recipes.AugmentRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.SizedIngredient;

public class AugmentRecipeBuilder {
    private AugmentRecipeType type;
    private List<SizedIngredient> researchCosts;
    private List<SizedIngredient> applyCosts;
    private List<Identifier> requirements;
    private Identifier requiredStation;
    private int uiX;
    private int uiY;
    private int time;
    private long rfCost;
    private String resourcePath;

    private AugmentRecipeBuilder(AugmentRecipeType type, String resourcePath) {
        this.type = type;
        this.resourcePath = resourcePath;
    }

    public static AugmentRecipeBuilder build () {
        return new AugmentRecipeBuilder(RecipeContent.AUGMENT, "augment");
    }

    public AugmentRecipeBuilder researchCost(List<SizedIngredient> researchCosts) {
        if (this.researchCosts == null)
            this.researchCosts = new ArrayList<SizedIngredient>();
        this.researchCosts.addAll(researchCosts);
        return this;
    }

    public AugmentRecipeBuilder researchCost(SizedIngredient researchCost) {
        if (this.researchCosts == null)
            this.researchCosts = new ArrayList<SizedIngredient>();
        this.researchCosts.add(researchCost);
        return this;
    }

    public AugmentRecipeBuilder researchCost(Ingredient researchCost, int count) {
        return researchCost(new SizedIngredient(count, researchCost));
    }

    public AugmentRecipeBuilder researchCost(Ingredient researchCost) {
        return researchCost(researchCost, 1);
    }

    public AugmentRecipeBuilder researchCost(TagKey<Item> researchCostTag, int count) {
        return researchCost(Ingredient.fromTag(researchCostTag), count);
    }

    public AugmentRecipeBuilder researchCost(TagKey<Item> researchCostTag) {
        return researchCost(researchCostTag, 1);
    }

    public AugmentRecipeBuilder researchCost(ItemConvertible researchCost, int count) {
        return researchCost(Ingredient.ofItems(researchCost), count);
    }

    public AugmentRecipeBuilder researchCost(ItemConvertible researchCost) {
        return researchCost(researchCost, 1);
    }

    public AugmentRecipeBuilder applyCost(List<SizedIngredient> applyCost) {
        if (this.applyCosts == null)
            this.applyCosts = new ArrayList<SizedIngredient>();
        this.applyCosts.addAll(applyCost);
        return this;
    }

    public AugmentRecipeBuilder applyCost(SizedIngredient applyCost) {
        if (this.applyCosts == null)
            this.applyCosts = new ArrayList<SizedIngredient>();
        this.applyCosts.add(applyCost);
        return this;
    }

    public AugmentRecipeBuilder applyCost(Ingredient applyCost, int count) {
        return applyCost(new SizedIngredient(count, applyCost));
    }

    public AugmentRecipeBuilder applyCost(Ingredient applyCost) {
        return applyCost(applyCost, 1);
    }

    public AugmentRecipeBuilder applyCost(TagKey<Item> applyCostTag, int count) {
        return applyCost(Ingredient.fromTag(applyCostTag), count);
    }

    public AugmentRecipeBuilder applyCost(TagKey<Item> applyCostTag) {
        return applyCost(applyCostTag, 1);
    }

    public AugmentRecipeBuilder applyCost(ItemConvertible applyCost, int count) {
        return applyCost(Ingredient.ofItems(applyCost), count);
    }

    public AugmentRecipeBuilder applyCost(ItemConvertible applyCost) {
        return applyCost(applyCost, 1);
    }

    public AugmentRecipeBuilder requirement(List<Identifier> requirements) {
        if (this.requirements == null)
            this.requirements = new ArrayList<Identifier>();
        this.requirements.addAll(requirements);
        return this;
    }

    public AugmentRecipeBuilder requirement(Identifier requirement) {
        if (this.requirements == null)
            this.requirements = new ArrayList<Identifier>();
        this.requirements.add(requirement);
        return this;
    }

    public AugmentRecipeBuilder requiredStation(Identifier requiredStation) {
        this.requiredStation = requiredStation;
        return this;
    }

    public AugmentRecipeBuilder uiX(int uiX) {
        this.uiX = uiX;
        return this;
    }

    public AugmentRecipeBuilder uiY(int uiY) {
        this.uiY = uiY;
        return this;
    }

    public AugmentRecipeBuilder time(int time) {
        this.time = time;
        return this;
    }

    public AugmentRecipeBuilder rfCost(long rfCost) {
        this.rfCost = rfCost;
        return this;
    }

    private void validate(Identifier id) throws IllegalStateException {
        if (researchCosts == null || researchCosts.size() == 0)
            throw new IllegalStateException("Research costs expected for recipe " + id + " (type " + type + ")");
        if (applyCosts == null || applyCosts.size() == 0)
            throw new IllegalStateException("Apply costs expected for recipe " + id + " (type " + type + ")");
        if (requiredStation == null)
            throw new IllegalStateException("required station expected for recipe " + id + " (type " + type + ")");
    }

    public void export(RecipeExporter exporter, String suffix) {
        var id = Oritech.id(resourcePath + "/" + suffix);
        validate(id);

        exporter.accept(id, new AugmentRecipe(type, researchCosts, applyCosts, requirements != null ? requirements : List.of(), requiredStation, uiX, uiY, time, rfCost), null);
    }
}
