package rearth.oritech.init.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.fluid.FluidStack;
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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class OritechRecipeType implements RecipeSerializer<OritechRecipe>, RecipeType<OritechRecipe> {
    
    public static final Codec<FluidStack> FLUID_STACK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidStack::getFluid),
        Codec.LONG.optionalFieldOf("amount", FluidStack.bucketAmount()).forGetter(FluidStack::getAmount)
    ).apply(instance, FluidStack::create));
    
    public static final MapCodec<OritechRecipe> ORI_RECIPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.INT.optionalFieldOf("time", 60).forGetter(OritechRecipe::getTime),
      Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(OritechRecipe::getInputs),
      ItemStack.CODEC.listOf().fieldOf("results").forGetter(OritechRecipe::getResults),
      ResourceLocation.CODEC.xmap(identifier1 -> (OritechRecipeType) BuiltInRegistries.RECIPE_TYPE.get(identifier1), OritechRecipeType::getIdentifier).fieldOf("type").forGetter(OritechRecipe::getOriType),
      FluidIngredient.CODEC.optionalFieldOf("fluidInput", FluidIngredient.EMPTY).forGetter(OritechRecipe::getFluidInput),
      FLUID_STACK_CODEC.listOf().optionalFieldOf("fluidOutputs", List.of()).forGetter(OritechRecipe::getFluidOutputs)
    ).apply(instance, OritechRecipe::new));
    
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
        this.identifier = identifier;
    }
    
    @Override
    public MapCodec<OritechRecipe> codec() {
        return ORI_RECIPE_CODEC;
    }
    
    @Override
    public StreamCodec<RegistryFriendlyByteBuf, OritechRecipe> streamCodec() {
        return PACKET_CODEC;
    }
    
    @Override
    public String toString() {
        return "OritechRecipeType{" +
                 "identifier=" + identifier +
                 '}';
    }
}
