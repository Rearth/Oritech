package rearth.oritech.init.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.block.entity.augmenter.api.CustomAugmentsCollection;
import rearth.oritech.block.entity.augmenter.api.EffectAugment;
import rearth.oritech.block.entity.augmenter.api.ModifierAugment;

import java.util.List;

public record AugmentData(boolean toggleable, List<SizedIngredient> researchCost, List<SizedIngredient> applyCost,
                          List<Identifier> requirements, Identifier requiredStation, int uiX, int uiY, int time,
                          long rfCost, AugmentDefinition definition) {
    
    public static final Codec<AugmentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.BOOL.fieldOf("toggleable").forGetter(AugmentData::toggleable),
      SizedIngredient.NESTED_CODEC.listOf().fieldOf("researchCost").forGetter(AugmentData::researchCost),
      SizedIngredient.NESTED_CODEC.listOf().fieldOf("applyCost").forGetter(AugmentData::applyCost),
      Identifier.CODEC.listOf().fieldOf("requirements").forGetter(AugmentData::requirements),
      Identifier.CODEC.fieldOf("requiredStation").forGetter(AugmentData::requiredStation),
      Codec.INT.fieldOf("uiX").forGetter(AugmentData::uiX),
      Codec.INT.fieldOf("uiY").forGetter(AugmentData::uiY),
      Codec.INT.fieldOf("time").forGetter(AugmentData::time),
      Codec.LONG.fieldOf("rfCost").forGetter(AugmentData::rfCost),
      AugmentDefinition.CODEC.fieldOf("effect").forGetter(AugmentData::definition)
    ).apply(instance, AugmentData::new));
    
    public Augment createAugment(Identifier augmentId) {
        switch (definition) {
            case CustomAugmentDefinition customAugmentDefinition -> {
                var customId = customAugmentDefinition.customAugmentId;
                var augment = CustomAugmentsCollection.getById(customId);
                if (augment == null) {
                    throw new IllegalStateException("No custom augment registered for " + customId + " while creating " + augmentId);
                }
                return augment;
            }
            case EffectDefinition effectDefinition -> {
                return new EffectAugment(
                  augmentId,
                  this.toggleable,
                  BuiltInRegistries.MOB_EFFECT.get(effectDefinition.potionEffectId).orElseThrow(),
                  effectDefinition.effectStrength);
            }
            case ModifierDefinition modifierDefinition -> {
                return new ModifierAugment(
                  augmentId,
                  BuiltInRegistries.ATTRIBUTE.get(modifierDefinition.entityAttributeId).orElseThrow(),
                  AttributeModifier.Operation.BY_ID.apply(modifierDefinition.attributeOperationType()),
                  modifierDefinition.amount(),
                  this.toggleable);
            }
            case null, default -> throw new IllegalStateException("No augment definition for " + augmentId);
        }
    }
    
    public sealed interface AugmentDefinition permits EffectDefinition, ModifierDefinition, CustomAugmentDefinition {
        Codec<AugmentDefinition> CODEC = Codec.STRING.dispatch("type", AugmentDefinition::type, AugmentDefinition::codecFor);
        
        String type();
        
        private static MapCodec<? extends AugmentDefinition> codecFor(String type) {
            return switch (type) {
                case "effect" -> EffectDefinition.MAP_CODEC;
                case "modifier" -> ModifierDefinition.MAP_CODEC;
                case "custom" -> CustomAugmentDefinition.MAP_CODEC;
                default -> throw new IllegalArgumentException("Unknown augment definition type: " + type);
            };
        }
    }
    
    // used to apply an effect, similar to potion effects
    public record EffectDefinition(Identifier potionEffectId, int effectStrength) implements AugmentDefinition {
        public static final MapCodec<EffectDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          Identifier.CODEC.fieldOf("potionEffectId").forGetter(EffectDefinition::potionEffectId),
          Codec.INT.fieldOf("effectStrength").forGetter(EffectDefinition::effectStrength)
        ).apply(instance, EffectDefinition::new));
        public static final Codec<EffectDefinition> CODEC = MAP_CODEC.codec();
        
        @Override
        public String type() {
            return "effect";
        }
    }
    
    // apply a stat modification. The attributeOperationType type can be either "add_value=0", "add_multiplied_base=1" or "add_multiplied_total=2"
    public record ModifierDefinition(Identifier entityAttributeId, int attributeOperationType,
                                     float amount) implements AugmentDefinition {
        public static final MapCodec<ModifierDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          Identifier.CODEC.fieldOf("entityAttributeId").forGetter(ModifierDefinition::entityAttributeId),
          Codec.INT.fieldOf("attributeOperationType").forGetter(ModifierDefinition::attributeOperationType),
          Codec.FLOAT.fieldOf("amount").forGetter(ModifierDefinition::amount)
        ).apply(instance, ModifierDefinition::new));
        public static final Codec<ModifierDefinition> CODEC = MAP_CODEC.codec();
        
        @Override
        public String type() {
            return "modifier";
        }
    }
    
    // apply a custom modification, that implements custom functionality.
    public record CustomAugmentDefinition(Identifier customAugmentId) implements AugmentDefinition {
        public static final MapCodec<CustomAugmentDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          Identifier.CODEC.fieldOf("customAugmentId").forGetter(CustomAugmentDefinition::customAugmentId)
        ).apply(instance, CustomAugmentDefinition::new));
        public static final Codec<CustomAugmentDefinition> CODEC = MAP_CODEC.codec();
        
        @Override
        public String type() {
            return "custom";
        }
    }
}

