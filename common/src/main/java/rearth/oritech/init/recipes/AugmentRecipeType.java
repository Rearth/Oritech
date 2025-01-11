package rearth.oritech.init.recipes;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.EndecRecipeSerializer;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import rearth.oritech.util.SizedIngredient;

public class AugmentRecipeType extends EndecRecipeSerializer<AugmentRecipe> implements RecipeType<AugmentRecipe> {
    
    
    public static final Endec<AugmentRecipe> AUGMENT_RECIPE_ENDEC = StructEndecBuilder.of(
      MinecraftEndecs.IDENTIFIER.xmap(identifier1 -> (AugmentRecipeType) Registries.RECIPE_TYPE.get(identifier1), AugmentRecipeType::getIdentifier).fieldOf("type", AugmentRecipe::getOriType),
      CodecUtils.toEndec(SizedIngredient.CODEC.codec()).listOf().fieldOf("researchCost", AugmentRecipe::getResearchCost),
      CodecUtils.toEndec(SizedIngredient.CODEC.codec()).listOf().fieldOf("applyCost", AugmentRecipe::getApplyCost),
      Endec.INT.optionalFieldOf("time", AugmentRecipe::getTime, 60),
      AugmentRecipe::new
    );
    
    private final Identifier identifier;
    
    public Identifier getIdentifier() {
        return identifier;
    }
    
    protected AugmentRecipeType(Identifier identifier) {
        super((StructEndec<AugmentRecipe>) AUGMENT_RECIPE_ENDEC);
        this.identifier = identifier;
    }
}
