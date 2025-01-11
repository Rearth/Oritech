package rearth.oritech.init.recipes;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.SizedIngredient;

import java.util.List;

public class AugmentRecipe implements Recipe<RecipeInput> {
    
    private final AugmentRecipeType type;
    private final List<SizedIngredient> researchCost;
    private final List<SizedIngredient> applyCost;
    private final int time;

    public static final AugmentRecipe DUMMY = new AugmentRecipe(RecipeContent.AUGMENT, List.of(new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_DUSTS))), List.of(new SizedIngredient(1, Ingredient.fromTag(TagContent.NICKEL_DUSTS))), -1);
    
    public AugmentRecipe(AugmentRecipeType type, List<SizedIngredient> inputs, List<SizedIngredient> applyCost, int time) {
        this.type = type;
        this.researchCost = inputs;
        this.applyCost = applyCost;
        this.time = time;
    }
    
    @Override
    public boolean matches(RecipeInput input, World world) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public ItemStack craft(RecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean fits(int width, int height) {
        return false;
    }
    
    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public RecipeSerializer<?> getSerializer() {
        return type;
    }
    
    @Override
    public RecipeType<?> getType() {
        return type;
    }
    
    public AugmentRecipeType getOriType() {
        return type;
    }
    
    public List<SizedIngredient> getResearchCost() {
        return researchCost;
    }
    
    public List<SizedIngredient> getApplyCost() {
        return applyCost;
    }
    
    public int getTime() {
        return time;
    }
    
    @Override
    public String toString() {
        return "AugmentRecipe{" +
                 "type=" + type +
                 ", inputs=" + researchCost +
                 ", time=" + time +
                 '}';
    }
}
