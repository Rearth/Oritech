package rearth.oritech.init.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import rearth.oritech.Oritech;

import java.util.List;
import java.util.Optional;

public record OritechRecipe(List<Ingredient> itemInputs, List<ItemStackTemplate> itemResults,
                            Optional<SizedFluidIngredient> fluidInput, List<FluidStackTemplate> fluidOutputs,
                            int time, RecipeType<OritechRecipe> recipeType) implements Recipe<OritechRecipeInput> {

    public static final Lazy<OritechRecipe> EMPTY = Lazy.of(() -> new OritechRecipe(List.of(), List.of(), Optional.empty(), List.of(), 0, RecipeContent.PULVERIZER.get()));


    public static RecipeSerializer<OritechRecipe> CreateSerializerForType(RecipeType<OritechRecipe> recipeType) {
        var codec = RecordCodecBuilder.<OritechRecipe>mapCodec(instance -> instance.group(
                Ingredient.CODEC.listOf().fieldOf("itemInputs").forGetter(OritechRecipe::itemInputs),
                ItemStackTemplate.CODEC.listOf().fieldOf("itemResults").forGetter(OritechRecipe::itemResults),
                SizedFluidIngredient.CODEC.optionalFieldOf("fluidInput").forGetter(OritechRecipe::fluidInput),
                FluidStackTemplate.CODEC.listOf().optionalFieldOf("fluidOutputs", List.of()).forGetter(OritechRecipe::fluidOutputs),
                Codec.INT.optionalFieldOf("time", 60).forGetter(OritechRecipe::time)
        ).apply(instance, (itemInputs, itemResults, fluidInput, fluidOutputs, time) ->
                new OritechRecipe(itemInputs, itemResults, fluidInput, fluidOutputs, time, recipeType)));
        return new RecipeSerializer<>(codec, ByteBufCodecs.fromCodecWithRegistries(codec.codec()));
    }

    @Override
    public boolean matches(OritechRecipeInput input, Level level) {
        // compare items and fluids. This will not modify any inputs.

        // iemless recipes must not shadow variants that use an item catalyst.
        var itemsMatching = itemInputs.isEmpty() ? input.itemsEmpty() : itemsMatch(itemInputs, input);
        var fluidsMatching = fluidInput.isEmpty() || fluidsMatch(input, level);

        return itemsMatching && fluidsMatching;
    }

    public static boolean itemsMatch(List<Ingredient> recipeIngredients, OritechRecipeInput input) {
        if (recipeIngredients.isEmpty()) return true;

        var contents = new StackedItemContents();
        input.getStacks().forEach(contents::accountStack);
        return contents.canCraft(recipeIngredients, null);
    }

    private boolean fluidsMatch(OritechRecipeInput input, Level level) {
        if (input.fluidEmpty()) return false;
        return fluidInput.get().test(input.fluidStack());
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
        return RecipeContent.GetSerializerByType(recipeType);
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
        return this.equals(EMPTY.get());
    }
}
