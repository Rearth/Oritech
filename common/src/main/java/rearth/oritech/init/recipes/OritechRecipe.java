package rearth.oritech.init.recipes;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import rearth.oritech.Oritech;
import rearth.oritech.util.FluidIngredient;

import java.util.List;

public class OritechRecipe implements Recipe<OritechRecipeInput> {
    
    protected final List<Ingredient> itemInputs;
    protected final List<ItemStackTemplate> itemResults;
    protected final FluidIngredient fluidInput;
    protected final List<FluidStack> fluidOutputs;
    protected final int time;
    
    public OritechRecipe(List<Ingredient> itemInputs, List<ItemStackTemplate> itemResults, FluidIngredient fluidInput, List<FluidStack> fluidOutputs, int time) {
        this.itemInputs = itemInputs;
        this.itemResults = itemResults;
        this.fluidInput = fluidInput;
        this.fluidOutputs = fluidOutputs;
        this.time = time;
    }
    
    @Override
    public boolean matches(OritechRecipeInput input, Level level) {
        // compare items and fluids
        
        var itemsMatching = itemInputs.isEmpty() || itemsMatch(input, level);
        var fluidsMatching = fluidInput.isEmpty() || fluidsMatch(input, level);
        
        return itemsMatching && fluidsMatching;
    }
    
    // this is only called if the recipe needs at least one item input
    private boolean itemsMatch(OritechRecipeInput input, Level level) {
        
        if (input.itemsEmpty()) return false;
        
        // multiple inputs require fuzzy matching
        if (itemInputs.size() > 1) {
            return fuzzyItemMatches(input);
        }
        
        // if we have just one input, just test that one
        return itemInputs.getFirst().test(input.getItem(0));
    }
    
    private boolean fluidsMatch(OritechRecipeInput input, Level level) {
        if (input.fluidEmpty()) return false;
        return fluidInput.test(input.fluidStack());
    }
    
    private boolean fuzzyItemMatches(OritechRecipeInput input) {
        
        // Input does not need to be in the correct slots / split into different slots.
        // We just check if we can remove all ingredients from the inventory, and fail is any input is not able to be removed.
        
        var sourceItems = input.itemStacks();
        
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
    public String group() {
        return "";
    }
    
    @Override
    public RecipeSerializer<? extends Recipe<OritechRecipeInput>> getSerializer() {
        return null;
    }
    
    @Override
    public RecipeType<? extends Recipe<OritechRecipeInput>> getType() {
        return null;
    }
    
    @Override
    public PlacementInfo placementInfo() {
        return null;
    }
    
    @Override
    public List<RecipeDisplay> display() {
        return Recipe.super.display();
    }
    
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return null;
    }
}
