package rearth.oritech.init.recipes;

import com.mojang.datafixers.util.Either;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.block.entity.augmenter.api.CustomAugmentsCollection;
import rearth.oritech.block.entity.augmenter.api.EffectAugment;
import rearth.oritech.block.entity.augmenter.api.ModifierAugment;
import rearth.oritech.util.SizedIngredient;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class AugmentDataRecipe implements Recipe<RecipeInput> {
    
    private final boolean toggleable;
    private final AugmentDataRecipeType type;
    
    private final List<SizedIngredient> researchCost;
    private final List<SizedIngredient> applyCost;
    private final List<ResourceLocation> requirements;
    private final ResourceLocation requiredStation;
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
      List<ResourceLocation> requirements,
      ResourceLocation requiredStation,
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
      List<ResourceLocation> requirements,
      ResourceLocation requiredStation,
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
    public boolean matches(RecipeInput input, Level world) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider lookup) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }
    
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registriesLookup) {
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
    
    public Augment createAugment(ResourceLocation recipeId) {
        if (customAugmentDefinition != null) {
            var customId = customAugmentDefinition.customAugmentId;
            return CustomAugmentsCollection.getById(customId);
        } else if (effectDefinition != null) {
            return new EffectAugment(
              recipeId,
              this.toggleable,
              BuiltInRegistries.MOB_EFFECT.getHolder(effectDefinition.potionEffectId).orElseThrow(),
              effectDefinition.effectStrength);
        } else if (modifierDefinition != null) {
            return new ModifierAugment(
              recipeId,
              BuiltInRegistries.ATTRIBUTE.getHolder(modifierDefinition.entityAttributeId).orElseThrow(),
              AttributeModifier.Operation.BY_ID.apply(modifierDefinition.attributeOperationType()),
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
    
    public ResourceLocation getRequiredStation() {
        return requiredStation;
    }
    
    public List<ResourceLocation> getRequirements() {
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
    public record EffectDefinition(ResourceLocation potionEffectId, int effectStrength) {
        public static Endec<EffectDefinition> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(EffectDefinition.class);
    }
    
    // apply a stat modification. The attributeOperationType type can be either "add_value=0", "add_multiplied_base=1" or "add_multiplied_total=2"
    public record ModifierDefinition(ResourceLocation entityAttributeId, int attributeOperationType, float amount) {
        public static Endec<ModifierDefinition> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(ModifierDefinition.class);
    }
    
    // apply a custom modification, that implements custom functionality.
    public record CustomAugmentDefinition(ResourceLocation customAugmentId) {
        public static Endec<CustomAugmentDefinition> ENDEC = ReflectiveEndecBuilder.SHARED_INSTANCE.get(CustomAugmentDefinition.class);
    }
    
}
