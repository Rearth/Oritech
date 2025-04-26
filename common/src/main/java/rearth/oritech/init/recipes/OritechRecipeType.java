package rearth.oritech.init.recipes;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.EndecRecipeSerializer;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.fluid.Fluid;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class OritechRecipeType extends EndecRecipeSerializer<OritechRecipe> implements RecipeType<OritechRecipe> {
    
    public static final Endec<Fluid> FLUID_ENDEC = CodecUtils.toEndec(Registries.FLUID.getCodec());
    
    // this doesnt work on neoforge client sadly (client logs the errors only somehow)
    //public static final Endec<FluidStack> FLUID_STACK_ENDEC = Codec.either(CodecUtils.toEndecWithRegistries(FluidStack.CODEC, FluidStack.STREAM_CODEC), SIMPLE_FLUID.catchErrors(FALLBACK));
    
    public static final Endec<OritechRecipe> ORI_RECIPE_ENDEC = StructEndecBuilder.of(
      Endec.INT.optionalFieldOf("time", OritechRecipe::getTime, 60),
      CodecUtils.toEndec(Ingredient.DISALLOW_EMPTY_CODEC).listOf().fieldOf("ingredients", OritechRecipe::getInputs),
      MinecraftEndecs.ITEM_STACK.listOf().fieldOf("results", OritechRecipe::getResults),
      MinecraftEndecs.IDENTIFIER.xmap(identifier1 -> (OritechRecipeType) Registries.RECIPE_TYPE.get(identifier1), OritechRecipeType::getIdentifier).fieldOf("type", OritechRecipe::getOriType),
      FLUID_ENDEC.fieldOf("fluidInputVariant", elem -> elem.getFluidInput().getFluid()),
      Endec.LONG.fieldOf("fluidInputAmount", elem -> elem.getFluidInput().getAmount() * OritechRecipe.fluidDivider),
      FLUID_ENDEC.fieldOf("fluidOutputVariant", elem -> elem.getFluidOutput().getFluid()),
      Endec.LONG.fieldOf("fluidOutputAmount", elem -> elem.getFluidOutput().getAmount() * OritechRecipe.fluidDivider),
      OritechRecipe::new
    );
    
    private final Identifier identifier;
    
    public Identifier getIdentifier() {
        return identifier;
    }
    
    protected OritechRecipeType(Identifier identifier) {
        super((StructEndec<OritechRecipe>) ORI_RECIPE_ENDEC);
        this.identifier = identifier;
    }
    
    @Override
    public String toString() {
        return "OritechRecipeType{" +
                 "identifier=" + identifier +
                 '}';
    }
}
