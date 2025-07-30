package rearth.oritech.init.recipes;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.EndecRecipeSerializer;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import rearth.oritech.util.SizedIngredient;

public class AugmentDataRecipeType extends EndecRecipeSerializer<AugmentDataRecipe> implements RecipeType<AugmentDataRecipe> {
    
    public static final Endec<AugmentDataRecipe> AUGMENT_DATA_RECIPE_ENDEC = StructEndecBuilder.of(
      MinecraftEndecs.IDENTIFIER.xmap(identifier1 -> (AugmentDataRecipeType) BuiltInRegistries.RECIPE_TYPE.get(identifier1), AugmentDataRecipeType::getIdentifier).fieldOf("type", AugmentDataRecipe::getOriType),
      Endec.BOOLEAN.fieldOf("toggleable", AugmentDataRecipe::isToggleable),
      CodecUtils.toEndec(SizedIngredient.CODEC.codec()).listOf().fieldOf("researchCost", AugmentDataRecipe::getResearchCost),
      CodecUtils.toEndec(SizedIngredient.CODEC.codec()).listOf().fieldOf("applyCost", AugmentDataRecipe::getApplyCost),
      MinecraftEndecs.IDENTIFIER.listOf().fieldOf("requirements", AugmentDataRecipe::getRequirements),
      MinecraftEndecs.IDENTIFIER.fieldOf("requiredStation", AugmentDataRecipe::getRequiredStation),
      Endec.INT.fieldOf("uiX", AugmentDataRecipe::getUiX),
      Endec.INT.fieldOf("uiY", AugmentDataRecipe::getUiY),
      Endec.INT.fieldOf("time", AugmentDataRecipe::getTime),
      Endec.LONG.fieldOf("rfCost", AugmentDataRecipe::getRfCost),
      CodecUtils.eitherEndec(CodecUtils.eitherEndec(AugmentDataRecipe.EffectDefinition.ENDEC, AugmentDataRecipe.ModifierDefinition.ENDEC), AugmentDataRecipe.CustomAugmentDefinition.ENDEC).fieldOf("effect", AugmentDataRecipe::getDefinition),
      AugmentDataRecipe::new
    );
    
    private final ResourceLocation identifier;
    
    public ResourceLocation getIdentifier() {
        return identifier;
    }
    
    public AugmentDataRecipeType(ResourceLocation identifier) {
        super((StructEndec<AugmentDataRecipe>) AUGMENT_DATA_RECIPE_ENDEC);
        this.identifier = identifier;
    }
}
