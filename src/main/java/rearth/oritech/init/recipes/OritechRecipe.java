package rearth.oritech.init.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;
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
        return findMatchingInputSlots(recipeIngredients, input.getStacks()) != null;
    }

    /**
     * Assigns one item from an input slot to every recipe ingredient.
     * <p>
     * Each ingredient entry consumes one item, so a stack with count {@code n} can satisfy up to {@code n}
     * entries, including repeated ingredients. Unrelated items in other input slots are allowed.
     * {@link Ingredient#test(ItemStack)} is intentionally used instead of {@code StackedItemContents}, which
     * compares item types without preserving custom ingredient checks such as the components on configurable
     * bee eggs.
     * <p>
     * Backtracking is required when ingredients overlap. For example, a broad ingot tag must not consume the
     * only item that can satisfy a later, more specific ingredient if another ingot is available.
     *
     * @return a slot index for each ingredient, or {@code null} when no complete assignment exists
     */
    @Nullable
    public static int[] findMatchingInputSlots(List<Ingredient> ingredients, List<ItemStack> inputStacks) {
        var matchedSlots = new int[ingredients.size()];
        var remaining = inputStacks.stream().mapToInt(ItemStack::getCount).toArray();
        return findMatchingInputSlots(ingredients, inputStacks, remaining, matchedSlots, 0) ? matchedSlots : null;
    }

    private static boolean findMatchingInputSlots(List<Ingredient> ingredients, List<ItemStack> inputStacks,
                                                  int[] remaining, int[] matchedSlots, int ingredientIndex) {
        if (ingredientIndex >= ingredients.size()) return true;

        var ingredient = ingredients.get(ingredientIndex);
        for (int i = 0; i < inputStacks.size(); i++) {
            // Rotating the first slot spreads repeated ingredients across slots, matching the previous behavior.
            var stackIndex = (i + ingredientIndex) % inputStacks.size();
            if (remaining[stackIndex] <= 0 || !ingredient.test(inputStacks.get(stackIndex))) continue;

            remaining[stackIndex]--;
            matchedSlots[ingredientIndex] = stackIndex;
            if (findMatchingInputSlots(ingredients, inputStacks, remaining, matchedSlots, ingredientIndex + 1)) return true;
            remaining[stackIndex]++;
        }

        return false;
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
