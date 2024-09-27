package rearth.oritech.init.recipes;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.util.FluidStack;

import java.util.List;

public class OritechRecipe implements Recipe<RecipeInput> {
    
    private final OritechRecipeType type;
    private final List<Ingredient> inputs;
    private final List<ItemStack> results;
    @Nullable
    private final FluidStack fluidInput;
    @Nullable
    private final FluidStack fluidOutput;
    private final int time;

    public static final OritechRecipe DUMMY = new OritechRecipe(-1, DefaultedList.ofSize(1, Ingredient.ofStacks(Items.IRON_INGOT.getDefaultStack())), DefaultedList.ofSize(1, Items.IRON_BLOCK.getDefaultStack()), RecipeContent.PULVERIZER, null, null);

    public OritechRecipe(int time, List<Ingredient> inputs, List<ItemStack> results, OritechRecipeType type, @Nullable FluidStack fluidInput, @Nullable FluidStack fluidOutput) {
        this.type = type;
        this.results = results;
        this.inputs = inputs;
        this.time = time;
        this.fluidInput = fluidInput;
        this.fluidOutput = fluidOutput;
    }
    
    @Override
    public boolean matches(RecipeInput input, World world) {
        
        if (world.isClient) return false;
        
        if (input.getSize() < inputs.size()) return false;
        
        var ingredients = getInputs();
        for (int i = 0; i < ingredients.size(); i++) {
            var entry = ingredients.get(i);
            if (!entry.test(input.getStackInSlot(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public ItemStack craft(RecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return null;
    }
    
    @Override
    public boolean fits(int width, int height) {
        return true;
    }
    
    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return type;
    }
    
    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }
    
    @Override
    public RecipeType<?> getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return "OritechRecipe{" +
                 "type=" + type +
                 ", inputs=" + inputs +
                 ", results=" + results +
                 ", fluidInput=" + fluidInput +
                 ", fluidOutput=" + fluidOutput +
                 ", time=" + time +
                 '}';
    }
    
    public int getTime() {
        return time;
    }

    public List<Ingredient> getInputs() {
        return inputs;
    }

    // do not use this one, use getInputs if applicable to avoid unnecessary copy
    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return DefaultedList.copyOf(Ingredient.EMPTY, inputs.toArray(Ingredient[]::new));
    }

    public List<ItemStack> getResults() {
        return results;
    }

    public OritechRecipeType getOriType() {
        return type;
    }
    
    public @Nullable FluidStack getFluidInput() {
        return fluidInput;
    }
    
    public @Nullable FluidStack getFluidOutput() {
        return fluidOutput;
    }
}
