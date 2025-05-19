package rearth.oritech.api.recipe;

import dev.architectury.platform.Platform;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.AugmentDataRecipe;
import rearth.oritech.init.recipes.AugmentDataRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

public class AugmentRecipeBuilder {
    private final AugmentDataRecipeType type;
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
    
    public AugmentRecipeBuilder effectDefinition(RegistryEntry<StatusEffect> entry, int amplifier) {
        this.effectDefinition = new AugmentDataRecipe.EffectDefinition(Registries.STATUS_EFFECT.getId(entry.value()), amplifier);
        return this;
    }
    
    public AugmentRecipeBuilder modifierDefinition(RegistryEntry<EntityAttribute> entry, float amount, EntityAttributeModifier.Operation op) {
        this.modifierDefinition = new AugmentDataRecipe.ModifierDefinition(Registries.ATTRIBUTE.getId(entry.value()), op.getId(), amount);
        return this;
    }
    
    public AugmentRecipeBuilder customAugmentDefinition(Identifier customAugmentId) {
        this.customAugmentDefinition = new AugmentDataRecipe.CustomAugmentDefinition(customAugmentId);
        return this;
    }
    
    private void validate(Identifier id) throws IllegalStateException {
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
    
    public void export(RecipeExporter exporter, String suffix) {
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
