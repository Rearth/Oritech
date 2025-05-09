package rearth.oritech.init.recipes;

import dev.architectury.fluid.FluidStack;
import dev.architectury.platform.Platform;
import net.minecraft.fluid.Fluid;
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
import rearth.oritech.util.FluidIngredient;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.util.SimpleCraftingInventory;

import java.util.List;

public class OritechRecipe implements Recipe<RecipeInput> {
    
    protected final OritechRecipeType type;
    protected final List<Ingredient> inputs;
    protected final List<ItemStack> results;
    protected final FluidIngredient fluidInput;
    protected final FluidStack fluidOutput;
    protected final int time;

    public static final OritechRecipe DUMMY = new OritechRecipe(-1, DefaultedList.ofSize(1, Ingredient.ofStacks(Items.IRON_INGOT.getDefaultStack())), DefaultedList.ofSize(1, Items.IRON_BLOCK.getDefaultStack()), RecipeContent.PULVERIZER, FluidIngredient.EMPTY, FluidStack.empty());
    
    public OritechRecipe(int time, List<Ingredient> inputs, List<ItemStack> results, OritechRecipeType type, @Nullable FluidIngredient fluidInput, @Nullable FluidStack fluidOutput) {
        this.type = type;
        this.results = results;
        this.inputs = inputs;
        this.time = time;
        if (fluidInput == null) fluidInput = FluidIngredient.EMPTY;
        this.fluidInput = fluidInput.withAmount(fluidInput.amount());
        if (fluidOutput == null) fluidOutput = FluidStack.empty();
        this.fluidOutput = fluidOutput;
        if (!fluidOutput.isEmpty())
            this.fluidOutput.setAmount(this.fluidOutput.getAmount());
    }

    public OritechRecipe(int time, List<Ingredient> inputs, List<ItemStack> results, OritechRecipeType type, @Nullable FluidIngredient fluidInput, Fluid outVariant, long outAmount) {
        this(time, inputs, results, type, fluidInput, FluidStack.create(outVariant, outAmount));
    }
    
    public OritechRecipe(int time, List<Ingredient> inputs, List<ItemStack> results, OritechRecipeType type, Fluid inVariant, long inAmount, Fluid outVariant, long outAmount) {
        this(time, inputs, results, type, new FluidIngredient().withContent(inVariant).withAmount(inAmount), FluidStack.create(outVariant, outAmount));
    }
    
    
    @Override
    public boolean matches(RecipeInput input, World world) {
        
        if (world.isClient) return false;
        
        if (inputs.size() > 1) {
            return complexMatch(input);
        }
        
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
    
    private boolean complexMatch(RecipeInput input) {
        
        if (!(input instanceof SimpleCraftingInventory simpleInventory)) return false;
        
        // Input does not need to be in the correct slots / split into different slots.
        // We just check if we can remove all ingredients from the inventory, and fail is any input is not able to be removed.
        var copiedInv = simpleInventory.getHeldStacks().stream().map(ItemStack::copy).toArray(ItemStack[]::new);
        
        for (var ingredient : getInputs()) {
            
            var found = false;
            
            for (var heldStack : copiedInv) {
                if (ingredient.test(heldStack)) {
                    heldStack.decrement(1);
                    found = true;
                    break;
                }
            }
            
            if (!found) return false;
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
    
    public @Nullable FluidIngredient getFluidInput() {
        return fluidInput;
    }
    
    public @Nullable FluidStack getFluidOutput() {
        return fluidOutput;
    }
}
