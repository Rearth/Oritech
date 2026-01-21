package rearth.oritech.api.recipe;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.AugmentDataRecipe;
import rearth.oritech.init.recipes.AugmentDataRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.SizedIngredient;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class AugmentRecipeBuilder {
    private final AugmentDataRecipeType type;
    private final String resourcePath;
    
    private boolean toggleable;
    
    private List<SizedIngredient> researchCosts;
    private List<SizedIngredient> applyCosts;
    private List<ResourceLocation> requirements;
    private ResourceLocation requiredStation;
    private int uiX;
    private int uiY;
    private int time;
    private long rfCost;
    
    // 2 of these 3 should always be null
    private @Nullable AugmentDataRecipe.EffectDefinition effectDefinition;
    private @Nullable AugmentDataRecipe.ModifierDefinition modifierDefinition;
    private @Nullable AugmentDataRecipe.CustomAugmentDefinition customAugmentDefinition;
    
    private AugmentRecipeBuilder(AugmentDataRecipeType type, String resourcePath) {
        this.type = type;
        this.resourcePath = resourcePath;
    }
    
    public static AugmentRecipeBuilder build() {
        return new AugmentRecipeBuilder(RecipeContent.AUGMENT_DATA, "augment");
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
        return researchCost(new SizedIngredient(count, researchCost));
    }
    
    public AugmentRecipeBuilder researchCost(Ingredient researchCost) {
        return researchCost(researchCost, 1);
    }
    
    public AugmentRecipeBuilder researchCost(TagKey<Item> researchCostTag, int count) {
        return researchCost(Ingredient.of(researchCostTag), count);
    }
    
    public AugmentRecipeBuilder researchCost(TagKey<Item> researchCostTag) {
        return researchCost(researchCostTag, 1);
    }
    
    public AugmentRecipeBuilder researchCost(ItemLike researchCost, int count) {
        return researchCost(Ingredient.of(researchCost), count);
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
        return applyCost(new SizedIngredient(count, applyCost));
    }
    
    public AugmentRecipeBuilder applyCost(Ingredient applyCost) {
        return applyCost(applyCost, 1);
    }
    
    public AugmentRecipeBuilder applyCost(TagKey<Item> applyCostTag, int count) {
        return applyCost(Ingredient.of(applyCostTag), count);
    }
    
    public AugmentRecipeBuilder applyCost(TagKey<Item> applyCostTag) {
        return applyCost(applyCostTag, 1);
    }
    
    public AugmentRecipeBuilder applyCost(ItemLike applyCost, int count) {
        return applyCost(Ingredient.of(applyCost), count);
    }
    
    public AugmentRecipeBuilder applyCost(ItemLike applyCost) {
        return applyCost(applyCost, 1);
    }
    
    public AugmentRecipeBuilder requirement(List<ResourceLocation> requirements) {
        if (this.requirements == null)
            this.requirements = new ArrayList<>();
        this.requirements.addAll(requirements);
        return this;
    }
    
    public AugmentRecipeBuilder requirement(ResourceLocation requirement) {
        if (this.requirements == null)
            this.requirements = new ArrayList<>();
        this.requirements.add(requirement);
        return this;
    }
    
    public AugmentRecipeBuilder requiredStation(ResourceLocation requiredStation) {
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
        this.effectDefinition = new AugmentDataRecipe.EffectDefinition(BuiltInRegistries.MOB_EFFECT.getKey(entry.value()), amplifier);
        return this;
    }
    
    public AugmentRecipeBuilder modifierDefinition(Holder<Attribute> entry, float amount, AttributeModifier.Operation op) {
        this.modifierDefinition = new AugmentDataRecipe.ModifierDefinition(BuiltInRegistries.ATTRIBUTE.getKey(entry.value()), op.id(), amount);
        return this;
    }
    
    public AugmentRecipeBuilder customAugmentDefinition(ResourceLocation customAugmentId) {
        this.customAugmentDefinition = new AugmentDataRecipe.CustomAugmentDefinition(customAugmentId);
        return this;
    }
    
    private void validate(ResourceLocation id) throws IllegalStateException {
        if (researchCosts == null || researchCosts.isEmpty())
            throw new IllegalStateException("Research costs expected for recipe " + id + " (type " + type + ")");
        if (applyCosts == null || applyCosts.isEmpty())
            throw new IllegalStateException("Apply costs expected for recipe " + id + " (type " + type + ")");
        if (requiredStation == null)
            throw new IllegalStateException("required station expected for recipe " + id + " (type " + type + ")");
        
        // ensure exactly one type is set
        if ((effectDefinition != null ? 1 : 0) + (modifierDefinition != null ? 1 : 0) + (customAugmentDefinition != null ? 1 : 0) != 1) {
            throw new IllegalStateException("Exactly one of effectDefinition, modifierDefinition, or customAugmentDefinition must be set for recipe " + id + " (type " + type + ")");
        }
    }
    
    public void export(RecipeOutput exporter, String suffix) {
        var id = Oritech.id(resourcePath + "/" + suffix);
        validate(id);
        
        exporter.accept(id, new AugmentDataRecipe(
          type,
          toggleable,
          researchCosts,
          applyCosts,
          requirements != null ? requirements : List.of(),
          requiredStation,
          uiX,
          uiY,
          time,
          rfCost,
          effectDefinition,
          modifierDefinition,
          customAugmentDefinition
        ), null);
    }
}
