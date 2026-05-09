package rearth.oritech.init.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.util.FluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record OritechRecipe(List<Ingredient> itemInputs, List<ItemStackTemplate> itemResults,
                            FluidIngredient fluidInput, List<FluidStack> fluidOutputs,
                            int time, RecipeType<OritechRecipe> recipeType) implements Recipe<OritechRecipeInput> {
    
    public static final MapCodec<OritechRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Ingredient.CODEC.listOf().fieldOf("itemInputs").forGetter(OritechRecipe::itemInputs),
      ItemStackTemplate.CODEC.listOf().fieldOf("itemResults").forGetter(OritechRecipe::itemResults),
      FluidIngredient.CODEC.optionalFieldOf("fluidInput", FluidIngredient.EMPTY).forGetter(OritechRecipe::fluidInput),
      NetworkManager.FLUID_STACK_CODEC.listOf().optionalFieldOf("fluidOutputs", List.of()).forGetter(OritechRecipe::fluidOutputs),
      Codec.INT.optionalFieldOf("time", 60).forGetter(OritechRecipe::time),
      Identifier.CODEC.xmap(OritechRecipe::recipeTypeFromId, OritechRecipe::idFromRecipeType).fieldOf("recipeType").forGetter(OritechRecipe::recipeType)
    ).apply(instance, OritechRecipe::new));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, OritechRecipe> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::itemInputs,
      ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::itemResults,
      FluidIngredient.PACKET_CODEC, OritechRecipe::fluidInput,
      NetworkManager.FLUID_STACK_STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::fluidOutputs,
      ByteBufCodecs.INT, OritechRecipe::time,
      StreamCodec.of(
        (buf, recipeType) -> Identifier.STREAM_CODEC.encode(buf, idFromRecipeType(recipeType)),
        buf -> recipeTypeFromId(Identifier.STREAM_CODEC.decode(buf))
      ), OritechRecipe::recipeType,
      OritechRecipe::new
    );
    
    @SuppressWarnings("unchecked")
    private static RecipeType<OritechRecipe> recipeTypeFromId(Identifier id) {
        return (RecipeType<OritechRecipe>) BuiltInRegistries.RECIPE_TYPE.get(id).orElseThrow().value();
    }
    
    private static Identifier idFromRecipeType(RecipeType<?> recipeType) {
        return Objects.requireNonNull(BuiltInRegistries.RECIPE_TYPE.getKey(recipeType));
    }
    
    @Override
    public boolean matches(OritechRecipeInput input, Level level) {
        // compare items and fluids
        
        var itemsMatching = itemInputs.isEmpty() || itemsMatch(itemInputs, input);
        var fluidsMatching = fluidInput.isEmpty() || fluidsMatch(input, level);
        
        return itemsMatching && fluidsMatching;
    }
    
    public static boolean itemsMatch(List<Ingredient> itemInputs, RecipeInput input) {
        if (itemInputs.isEmpty()) return true;
        
        if (input.isEmpty()) return false;
        
        // multiple inputs require fuzzy matching
        if (itemInputs.size() > 1) {
            return fuzzyItemMatches(itemInputs, input);
        }
        
        // if we have just one input, just test that one
        return itemInputs.getFirst().test(input.getItem(0));
    }
    
    private boolean fluidsMatch(OritechRecipeInput input, Level level) {
        if (input.fluidEmpty()) return false;
        return fluidInput.test(input.fluidStack());
    }
    
    private static boolean fuzzyItemMatches(List<Ingredient> itemInputs, RecipeInput input) {
        
        // Input does not need to be in the correct slots / split into different slots.
        // We just check if we can remove all ingredients from the inventory, and fail is any input is not able to be removed.
        
        var sourceItems = new ArrayList<ItemStack>();
        for (int slot = 0; slot < input.size(); slot++) {
            var stack = input.getItem(slot);
            if (!stack.isEmpty()) sourceItems.add(stack.copy());
        }
        
        for (var ingredient : itemInputs) {
            var found = false;
            
            for (var heldStack : sourceItems) {
                if (ingredient.test(heldStack)) {
                    heldStack.shrink(1);
                    found = true;
                    break;
                }
            }
            
            if (!found) return false;
        }
        
        return true;
    }
    
    // not used since we often have multiple outputs or fluid outputs
    @Override
    public ItemStack assemble(OritechRecipeInput oritechRecipeInput) {
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
    public RecipeSerializer<? extends Recipe<OritechRecipeInput>> getSerializer() {
        return RecipeContent.ORITECH_SERIALIZER.get();
    }
    
    @Override
    public RecipeType<? extends Recipe<OritechRecipeInput>> getType() {
        return recipeType;
    }
    
    // recipe book stuff, not really relevant here I think
    @Override
    public String group() {
        return "";
    }
    
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }
    
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
