package rearth.oritech.api.recipe;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import rearth.oritech.Oritech;
import rearth.oritech.init.datapack.AugmentData;

import java.util.ArrayList;
import java.util.List;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;

public class AugmentRecipeBuilder {
    private final String resourcePath;

    private boolean toggleable;

    private List<SizedIngredient> researchCosts;
    private List<SizedIngredient> applyCosts;
    private List<Identifier> requirements;
    private Identifier requiredStation;
    private int uiX;
    private int uiY;
    private int time;
    private long rfCost;

    private AugmentData.AugmentDefinition definition;

    private AugmentRecipeBuilder(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public static AugmentRecipeBuilder build() {
        return new AugmentRecipeBuilder("augment");
    }

    @FunctionalInterface
    public interface Output {
        void accept(Identifier id, AugmentData augmentData);
    }

    public AugmentRecipeBuilder researchCost(List<SizedIngredient> researchCosts) {
        if (this.researchCosts == null)
            this.researchCosts = new ArrayList<>();
        this.researchCosts.addAll(researchCosts);
        return this;
    }

    public AugmentRecipeBuilder researchCost(SizedIngredient researchCost) {
        if (this.researchCosts == null)
            this.researchCosts = new ArrayList<>();
        this.researchCosts.add(researchCost);
        return this;
    }

    public AugmentRecipeBuilder researchCost(Ingredient researchCost, int count) {
        return researchCost(new SizedIngredient(researchCost, count));
    }

    public AugmentRecipeBuilder researchCost(Ingredient researchCost) {
        return researchCost(researchCost, 1);
    }

    public AugmentRecipeBuilder researchCost(TagKey<Item> researchCostTag, int count) {
        return researchCost(of(researchCostTag), count);
    }

    public AugmentRecipeBuilder researchCost(TagKey<Item> researchCostTag) {
        return researchCost(researchCostTag, 1);
    }

    public AugmentRecipeBuilder researchCost(ItemLike researchCost, int count) {
        return researchCost(of(researchCost), count);
    }

    public AugmentRecipeBuilder researchCost(ItemLike researchCost) {
        return researchCost(researchCost, 1);
    }

    public AugmentRecipeBuilder applyCost(List<SizedIngredient> applyCost) {
        if (this.applyCosts == null)
            this.applyCosts = new ArrayList<>();
        this.applyCosts.addAll(applyCost);
        return this;
    }

    public AugmentRecipeBuilder applyCost(SizedIngredient applyCost) {
        if (this.applyCosts == null)
            this.applyCosts = new ArrayList<>();
        this.applyCosts.add(applyCost);
        return this;
    }

    public AugmentRecipeBuilder applyCost(Ingredient applyCost, int count) {
        return applyCost(new SizedIngredient(applyCost, count));
    }

    public AugmentRecipeBuilder applyCost(Ingredient applyCost) {
        return applyCost(applyCost, 1);
    }

    public AugmentRecipeBuilder applyCost(TagKey<Item> applyCostTag, int count) {
        return applyCost(of(applyCostTag), count);
    }

    public AugmentRecipeBuilder applyCost(TagKey<Item> applyCostTag) {
        return applyCost(applyCostTag, 1);
    }

    public AugmentRecipeBuilder applyCost(ItemLike applyCost, int count) {
        return applyCost(of(applyCost), count);
    }

    public AugmentRecipeBuilder applyCost(ItemLike applyCost) {
        return applyCost(applyCost, 1);
    }

    public AugmentRecipeBuilder requirement(List<Identifier> requirements) {
        if (this.requirements == null)
            this.requirements = new ArrayList<>();
        this.requirements.addAll(requirements);
        return this;
    }

    public AugmentRecipeBuilder requirement(Identifier requirement) {
        if (this.requirements == null)
            this.requirements = new ArrayList<>();
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

    public AugmentRecipeBuilder toggleable(boolean toggleable) {
        this.toggleable = toggleable;
        return this;
    }

    public AugmentRecipeBuilder toggleable() {
        this.toggleable = true;
        return this;
    }

    public AugmentRecipeBuilder effectDefinition(Holder<MobEffect> entry, int amplifier) {
        this.definition = new AugmentData.EffectDefinition(BuiltInRegistries.MOB_EFFECT.getKey(entry.value()), amplifier);
        return this;
    }

    public AugmentRecipeBuilder modifierDefinition(Holder<Attribute> entry, float amount, AttributeModifier.Operation op) {
        this.definition = new AugmentData.ModifierDefinition(BuiltInRegistries.ATTRIBUTE.getKey(entry.value()), op.id(), amount);
        return this;
    }

    public AugmentRecipeBuilder customAugmentDefinition(Identifier customAugmentId) {
        this.definition = new AugmentData.CustomAugmentDefinition(customAugmentId);
        return this;
    }

    private void validate(Identifier id) throws IllegalStateException {
        if (researchCosts == null || researchCosts.isEmpty())
            throw new IllegalStateException("Research costs expected for augment " + id);
        if (applyCosts == null || applyCosts.isEmpty())
            throw new IllegalStateException("Apply costs expected for augment " + id);
        if (requiredStation == null)
            throw new IllegalStateException("Required station expected for augment " + id);

        if (definition == null)
            throw new IllegalStateException("Augment definition expected for augment " + id);
    }

    public void export(Output exporter, String suffix) {
        var id = Oritech.id(resourcePath + "/" + suffix);
        validate(id);

        exporter.accept(id, new AugmentData(
                toggleable,
                researchCosts,
                applyCosts,
                requirements != null ? requirements : List.of(),
                requiredStation,
                uiX,
                uiY,
                time,
                rfCost,
                definition
        ));
    }
}
