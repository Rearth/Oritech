package rearth.oritech.init.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import rearth.oritech.Oritech;

import java.util.List;
import java.util.Objects;

public record OritechRecipe(List<Ingredient> itemInputs, List<ItemStackTemplate> itemResults,
                            SizedFluidIngredient fluidInput, List<FluidStack> fluidOutputs,
                            int time, RecipeType<OritechRecipe> recipeType) implements Recipe<OritechRecipeInput> {

    public static final OritechRecipe EMPTY = new OritechRecipe(List.of(), List.of(), SizedFluidIngredient.of(Fluids.EMPTY, 0), List.of(), 0, RecipeContent.PULVERIZER.get());


    public static final MapCodec<OritechRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.listOf().fieldOf("itemInputs").forGetter(OritechRecipe::itemInputs),
            ItemStackTemplate.CODEC.listOf().fieldOf("itemResults").forGetter(OritechRecipe::itemResults),
            SizedFluidIngredient.CODEC.optionalFieldOf("fluidInput", SizedFluidIngredient.of(Fluids.EMPTY, 0)).forGetter(OritechRecipe::fluidInput),
            FluidStack.OPTIONAL_CODEC.listOf().optionalFieldOf("fluidOutputs", List.of()).forGetter(OritechRecipe::fluidOutputs),
            Codec.INT.optionalFieldOf("time", 60).forGetter(OritechRecipe::time),
            Identifier.CODEC.xmap(OritechRecipe::recipeTypeFromId, OritechRecipe::idFromRecipeType).fieldOf("recipeType").forGetter(OritechRecipe::recipeType)
    ).apply(instance, OritechRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, OritechRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::itemInputs,
            ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::itemResults,
            SizedFluidIngredient.STREAM_CODEC, OritechRecipe::fluidInput,
            FluidStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), OritechRecipe::fluidOutputs,
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
        // compare items and fluids. This will not modify any inputs.

        var itemsMatching = itemInputs.isEmpty() || itemsMatch(itemInputs, input);
        var fluidsMatching = fluidInput.amount() <= 0 || fluidsMatch(input, level);

        return itemsMatching && fluidsMatching;
    }

    public static boolean itemsMatch(List<Ingredient> recipeIngredients, OritechRecipeItemInput input) {
        if (recipeIngredients.isEmpty()) return true;

        if (input.isEmpty()) return false;

        // multiple inputs require fuzzy matching
        if (recipeIngredients.size() > 1) {
            return fuzzyItemMatches(recipeIngredients, input);
        }

        // if we have just one input, just test that one
        return recipeIngredients.getFirst().test(input.getItem(0));
    }

    private boolean fluidsMatch(OritechRecipeInput input, Level level) {
        if (input.fluidEmpty()) return false;
        return fluidInput.test(input.fluidStack());
    }

    private static boolean fuzzyItemMatches(List<Ingredient> itemIngredients, OritechRecipeItemInput input) {

        // Input does not need to be in the correct slots / split into different slots.
        // We just check if we can remove all ingredients from the inventory, and fail if any input is not able to be removed.
        var sourceItems = input.getStacks().stream().filter(stack -> !stack.isEmpty()).toList();

        for (var ingredient : itemIngredients) {
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

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }
}
