package rearth.oritech.init.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.block.entity.augmenter.api.CustomAugmentsCollection;
import rearth.oritech.block.entity.augmenter.api.EffectAugment;
import rearth.oritech.block.entity.augmenter.api.ModifierAugment;
import rearth.oritech.util.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

public record AugmentDataRecipe(boolean toggleable, List<SizedIngredient> researchCost, List<SizedIngredient> applyCost,
                                List<Identifier> requirements, Identifier requiredStation, int uiX, int uiY, int time,
                                long rfCost,
                                rearth.oritech.init.recipes.AugmentDataRecipe.AugmentDefinition definition) implements Recipe<AugmentDataRecipeInput> {
    
    public static final MapCodec<AugmentDataRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.BOOL.fieldOf("toggleable").forGetter(AugmentDataRecipe::toggleable),
      SizedIngredient.CODEC.codec().listOf().fieldOf("researchCost").forGetter(AugmentDataRecipe::researchCost),
      SizedIngredient.CODEC.codec().listOf().fieldOf("applyCost").forGetter(AugmentDataRecipe::applyCost),
      Identifier.CODEC.listOf().fieldOf("requirements").forGetter(AugmentDataRecipe::requirements),
      Identifier.CODEC.fieldOf("requiredStation").forGetter(AugmentDataRecipe::requiredStation),
      Codec.INT.fieldOf("uiX").forGetter(AugmentDataRecipe::uiX),
      Codec.INT.fieldOf("uiY").forGetter(AugmentDataRecipe::uiY),
      Codec.INT.fieldOf("time").forGetter(AugmentDataRecipe::time),
      Codec.LONG.fieldOf("rfCost").forGetter(AugmentDataRecipe::rfCost),
      AugmentDefinition.CODEC.fieldOf("effect").forGetter(AugmentDataRecipe::definition)
    ).apply(instance, AugmentDataRecipe::new));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, AugmentDataRecipe> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, AugmentDataRecipe::toggleable,
      SizedIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()), AugmentDataRecipe::researchCost,
      SizedIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()), AugmentDataRecipe::applyCost,
      Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), AugmentDataRecipe::requirements,
      Identifier.STREAM_CODEC, AugmentDataRecipe::requiredStation,
      ByteBufCodecs.INT, AugmentDataRecipe::uiX,
      ByteBufCodecs.INT, AugmentDataRecipe::uiY,
      ByteBufCodecs.INT, AugmentDataRecipe::time,
      ByteBufCodecs.VAR_LONG, AugmentDataRecipe::rfCost,
      AugmentDefinition.STREAM_CODEC, AugmentDataRecipe::definition,
      AugmentDataRecipe::new
    );
    
    @Override
    public boolean matches(AugmentDataRecipeInput input, Level world) {
        return switch (input.mode()) {
            case RESEARCH -> matchesResearchCost(input);
            case APPLY -> matchesApplyCost(input);
        };
    }
    
    public boolean matchesResearchCost(AugmentDataRecipeInput input) {
        return OritechRecipe.itemsMatch(expandSizedIngredients(researchCost), input);
    }
    
    public boolean matchesApplyCost(AugmentDataRecipeInput input) {
        return OritechRecipe.itemsMatch(expandSizedIngredients(applyCost), input);
    }
    
    private static List<Ingredient> expandSizedIngredients(List<SizedIngredient> sizedIngredients) {
        var ingredients = new ArrayList<Ingredient>();
        for (var sizedIngredient : sizedIngredients) {
            for (int count = 0; count < sizedIngredient.count(); count++) {
                ingredients.add(sizedIngredient.ingredient());
            }
        }
        return ingredients;
    }
    
    @Override
    public ItemStack assemble(AugmentDataRecipeInput augmentDataRecipeInput) {
        Oritech.LOGGER.warn("Tried to assemble oritech recipe");
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean isSpecial() {
        return true;
    }
    
    @Override
    public boolean showNotification() {
        return false;
    }
    
    @Override
    public String group() {
        return "";
    }
    
    @Override
    public RecipeSerializer<? extends Recipe<AugmentDataRecipeInput>> getSerializer() {
        return RecipeContent.AUGMENT_DATA_SERIALIZER.get();
    }
    
    @Override
    public RecipeType<? extends Recipe<AugmentDataRecipeInput>> getType() {
        return RecipeContent.AUGMENT_DATA.get();
    }
    
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }
    
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
    
    public Augment createAugment(Identifier recipeId) {
        if (definition instanceof CustomAugmentDefinition customAugmentDefinition) {
            var customId = customAugmentDefinition.customAugmentId;
            return CustomAugmentsCollection.getById(customId);
        } else if (definition instanceof EffectDefinition effectDefinition) {
            return new EffectAugment(
              recipeId,
              this.toggleable,
              BuiltInRegistries.MOB_EFFECT.get(effectDefinition.potionEffectId).orElseThrow(),
              effectDefinition.effectStrength);
        } else if (definition instanceof ModifierDefinition modifierDefinition) {
            return new ModifierAugment(
              recipeId,
              BuiltInRegistries.ATTRIBUTE.get(modifierDefinition.entityAttributeId).orElseThrow(),
              AttributeModifier.Operation.BY_ID.apply(modifierDefinition.attributeOperationType()),
              modifierDefinition.amount(),
              this.toggleable);
        } else {
            throw new IllegalStateException("No augment definition for " + recipeId);
        }
    }
    
    public sealed
    interface AugmentDefinition permits EffectDefinition, ModifierDefinition, CustomAugmentDefinition {
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
        
        StreamCodec<RegistryFriendlyByteBuf, AugmentDefinition> STREAM_CODEC = StreamCodec.of(
          (buf, definition) -> {
              if (definition instanceof CustomAugmentDefinition customAugmentDefinition) {
                  buf.writeByte(2);
                  Identifier.STREAM_CODEC.encode(buf, customAugmentDefinition.customAugmentId());
              } else if (definition instanceof ModifierDefinition modifierDefinition) {
                  buf.writeByte(1);
                  Identifier.STREAM_CODEC.encode(buf, modifierDefinition.entityAttributeId());
                  buf.writeInt(modifierDefinition.attributeOperationType());
                  buf.writeFloat(modifierDefinition.amount());
              } else if (definition instanceof EffectDefinition effectDefinition) {
                  buf.writeByte(0);
                  Identifier.STREAM_CODEC.encode(buf, effectDefinition.potionEffectId());
                  buf.writeInt(effectDefinition.effectStrength());
              } else {
                  throw new IllegalStateException("Unknown augment definition type: " + definition.getClass());
              }
          },
          buf -> {
              var kind = buf.readByte();
              if (kind == 2) return new CustomAugmentDefinition(Identifier.STREAM_CODEC.decode(buf));
              if (kind == 1)
                  return new ModifierDefinition(Identifier.STREAM_CODEC.decode(buf), buf.readInt(), buf.readFloat());
              return new EffectDefinition(Identifier.STREAM_CODEC.decode(buf), buf.readInt());
          }
        );
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
