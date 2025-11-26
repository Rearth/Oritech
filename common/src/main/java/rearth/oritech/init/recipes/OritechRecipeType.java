package rearth.oritech.init.recipes;

import dev.architectury.fluid.FluidStack;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.EndecRecipeSerializer;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.util.FluidIngredient;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;

public class OritechRecipeType extends EndecRecipeSerializer<OritechRecipe> implements RecipeType<OritechRecipe> {
    
    public static final Endec<FluidStack> FLUID_STACK_ENDEC = StructEndecBuilder.of(
        MinecraftEndecs.ofRegistry(BuiltInRegistries.FLUID).fieldOf("fluid", FluidStack::getFluid),
        Endec.LONG.optionalFieldOf("amount", FluidStack::getAmount, FluidStack.bucketAmount()),
        FluidStack::create);
    
    public static final Endec<OritechRecipe> ORI_RECIPE_ENDEC = StructEndecBuilder.of(
      Endec.INT.optionalFieldOf("time", OritechRecipe::getTime, 60),
      CodecUtils.toEndec(Ingredient.CODEC_NONEMPTY).listOf().fieldOf("ingredients", OritechRecipe::getInputs),
      MinecraftEndecs.ITEM_STACK.listOf().fieldOf("results", OritechRecipe::getResults),
      MinecraftEndecs.IDENTIFIER.xmap(identifier1 -> (OritechRecipeType) BuiltInRegistries.RECIPE_TYPE.get(identifier1), OritechRecipeType::getIdentifier).fieldOf("type", OritechRecipe::getOriType),
      FluidIngredient.FLUID_INGREDIENT_ENDEC.optionalFieldOf("fluidInput", OritechRecipe::getFluidInput, FluidIngredient.EMPTY),
      FLUID_STACK_ENDEC.listOf().optionalFieldOf("fluidOutputs", OritechRecipe::getFluidOutputs, List.of()),
      OritechRecipe::new
    );
    
    public static final StreamCodec<RegistryFriendlyByteBuf, OritechRecipe> PACKET_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, OritechRecipe::getTime,
      Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::getInputs,
      ItemStack.OPTIONAL_LIST_STREAM_CODEC, OritechRecipe::getResults,
      ResourceLocation.STREAM_CODEC.map(identifier1 -> (OritechRecipeType) BuiltInRegistries.RECIPE_TYPE.get(identifier1), OritechRecipeType::getIdentifier), OritechRecipe::getOriType,
      FluidIngredient.PACKET_CODEC, OritechRecipe::getFluidInput,
      NetworkManager.FLUID_STACK_STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::getFluidOutputs,
      OritechRecipe::new
    );
    
    private final ResourceLocation identifier;
    
    public ResourceLocation getIdentifier() {
        return identifier;
    }
    
    public OritechRecipeType(ResourceLocation identifier) {
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
