package rearth.oritech.init.recipes;

import dev.architectury.fluid.FluidStack;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.EndecRecipeSerializer;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import rearth.oritech.util.FluidIngredient;

public class OritechRecipeType extends EndecRecipeSerializer<OritechRecipe> implements RecipeType<OritechRecipe> {
    
    public static final Endec<FluidStack> FLUID_STACK_ENDEC = StructEndecBuilder.of(
        MinecraftEndecs.ofRegistry(Registries.FLUID).fieldOf("fluid", FluidStack::getFluid),
        Endec.LONG.optionalFieldOf("amount", FluidStack::getAmount, FluidStack.bucketAmount()),
        FluidStack::create);
    
    public static final Endec<OritechRecipe> ORI_RECIPE_ENDEC = StructEndecBuilder.of(
      Endec.INT.optionalFieldOf("time", OritechRecipe::getTime, 60),
      CodecUtils.toEndec(Ingredient.DISALLOW_EMPTY_CODEC).listOf().fieldOf("ingredients", OritechRecipe::getInputs),
      MinecraftEndecs.ITEM_STACK.listOf().fieldOf("results", OritechRecipe::getResults),
      MinecraftEndecs.IDENTIFIER.xmap(identifier1 -> (OritechRecipeType) Registries.RECIPE_TYPE.get(identifier1), OritechRecipeType::getIdentifier).fieldOf("type", OritechRecipe::getOriType),
      FluidIngredient.FLUID_INGREDIENT_ENDEC.optionalFieldOf("fluidInput", OritechRecipe::getFluidInput, FluidIngredient.EMPTY),
      FLUID_STACK_ENDEC.optionalFieldOf("fluidOutput", OritechRecipe::getFluidOutput, FluidStack.empty()),
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
