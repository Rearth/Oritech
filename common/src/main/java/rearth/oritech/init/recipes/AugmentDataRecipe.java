package rearth.oritech.init.recipes;

import com.mojang.datafixers.util.Either;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.block.entity.augmenter.api.CustomAugmentsCollection;
import rearth.oritech.block.entity.augmenter.api.EffectAugment;
import rearth.oritech.block.entity.augmenter.api.ModifierAugment;
import rearth.oritech.util.SizedIngredient;

import java.util.List;

public class AugmentDataRecipe implements Recipe<RecipeInput> {
    
    private final boolean toggleable;
    private final AugmentDataRecipeType type;
    
    private final List<SizedIngredient> researchCost;
    private final List<SizedIngredient> applyCost;
    private final List<Identifier> requirements;
    private final Identifier requiredStation;
    private final int uiX;
    private final int uiY;
    private final int time;
    private final long rfCost;
    
    // 2 of these 3 should always be null
    private final @Nullable EffectDefinition effectDefinition;
    private final @Nullable ModifierDefinition modifierDefinition;
    private final @Nullable CustomAugmentDefinition customAugmentDefinition;
    
    // this shitty either variant is needed because neoforge datagen wont work with the normal null values
    public AugmentDataRecipe(
      AugmentDataRecipeType type,
      boolean toggleable,
      List<SizedIngredient> researchCost,
      List<SizedIngredient> applyCost,
      List<Identifier> requirements,
      Identifier requiredStation,
      int uiX,
      int uiY,
      int time,
      long rfCost,
      Either<Either<EffectDefinition, ModifierDefinition>, CustomAugmentDefinition> effect) {
        
        this(type,
          toggleable,
          researchCost,
          applyCost,
          requirements,
          requiredStation,
          uiX,
          uiY,
          time,
          rfCost,
          effect.left().isPresent() ? effect.left().get().left().isPresent() ? effect.left().get().left().get() : null : null,
          effect.left().isPresent() ? effect.left().get().right().isPresent() ? effect.left().get().right().get() : null : null,
          effect.right().isPresent() ? effect.right().get() : null);
    }
    
    public AugmentDataRecipe(
      AugmentDataRecipeType type,
      boolean toggleable,
      List<SizedIngredient> researchCost,
      List<SizedIngredient> applyCost,
      List<Identifier> requirements,
      Identifier requiredStation,
      int uiX,
      int uiY,
      int time,
      long rfCost,
      @Nullable EffectDefinition effectDefinition,
      @Nullable ModifierDefinition modifierDefinition,
      @Nullable CustomAugmentDefinition customAugmentDefinition) {
        
        this.toggleable = toggleable;
        this.researchCost = researchCost;
        this.applyCost = applyCost;
        this.requirements = requirements;
        this.requiredStation = requiredStation;
        this.uiX = uiX;
        this.uiY = uiY;
        this.time = time;
        this.rfCost = rfCost;
        this.effectDefinition = effectDefinition;
        this.modifierDefinition = modifierDefinition;
        this.customAugmentDefinition = customAugmentDefinition;
        this.type = type;
    }
    
    @Override
    public boolean matches(RecipeInput input, World world) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public ItemStack craft(RecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean fits(int width, int height) {
        return false;
    }
    
    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public RecipeSerializer<?> getSerializer() {
        return type;
    }
    
    @Override
    public RecipeType<?> getType() {
        return type;
    }
    
    public AugmentDataRecipeType getOriType() {
        return type;
    }
    
    public boolean isToggleable() {
        return toggleable;
    }
    
    public Augment createAugment(Identifier recipeId) {
        if (customAugmentDefinition != null) {
            var customId = customAugmentDefinition.customAugmentId;
            return CustomAugmentsCollection.getById(customId);
        } else if (effectDefinition != null) {
            return new EffectAugment(
              recipeId,
              this.toggleable,
              Registries.STATUS_EFFECT.getEntry(effectDefinition.potionEffectId).orElseThrow(),
              effectDefinition.effectStrength);
        } else if (modifierDefinition != null) {
            return new ModifierAugment(
              recipeId,
              Registries.ATTRIBUTE.getEntry(modifierDefinition.entityAttributeId).orElseThrow(),
              EntityAttributeModifier.Operation.ID_TO_VALUE.apply(modifierDefinition.attributeOperationType()),
              modifierDefinition.amount(),
              this.toggleable);
        } else {
            throw new IllegalStateException("No augment definition for " + recipeId);
        }
    }
    
    public List<SizedIngredient> getResearchCost() {
        return researchCost;
    }
    
    public List<SizedIngredient> getApplyCost() {
        return applyCost;
    }
    
    public long getRfCost() {
        return rfCost;
    }
    
    public int getTime() {
        return time;
    }
    
    public Identifier getRequiredStation() {
        return requiredStation;
    }
    
    public List<Identifier> getRequirements() {
        return requirements;
    }
    
    public int getUiX() {
        return uiX;
    }
    
    public int getUiY() {
        return uiY;
    }
    
    public @Nullable EffectDefinition getEffectDefinition() {
        return effectDefinition;
    }
    
    public @Nullable CustomAugmentDefinition getCustomAugmentDefinition() {
        return customAugmentDefinition;
    }
    
    public @Nullable ModifierDefinition getModifierDefinition() {
        return modifierDefinition;
    }
    
    public Either<Either<EffectDefinition, ModifierDefinition>, CustomAugmentDefinition> getDefinition() {
        if (effectDefinition != null) {
            return Either.left(Either.left(effectDefinition));
        } else if (modifierDefinition != null) {
            return Either.left(Either.right(modifierDefinition));
        } else if (customAugmentDefinition != null) {
            return Either.right(customAugmentDefinition);
        }
        
        throw new IllegalStateException("Either effect, modifier or custom augment needs to be set!");
    }
    
    // used to apply an effect, similar to potion effects
    public record EffectDefinition(Identifier potionEffectId, int effectStrength) {
        public static Endec<EffectDefinition> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(EffectDefinition.class);
    }
    
    // apply a stat modification. The attributeOperationType type can be either "add_value=0", "add_multiplied_base=1" or "add_multiplied_total=2"
    public record ModifierDefinition(Identifier entityAttributeId, int attributeOperationType, float amount) {
        public static Endec<ModifierDefinition> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(ModifierDefinition.class);
    }
    
    // apply a custom modification, that implements custom functionality.
    public record CustomAugmentDefinition(Identifier customAugmentId) {
        public static Endec<CustomAugmentDefinition> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(CustomAugmentDefinition.class);
    }
    
}
