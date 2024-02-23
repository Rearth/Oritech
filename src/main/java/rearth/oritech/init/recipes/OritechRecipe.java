package rearth.oritech.init.recipes;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.List;

public class OritechRecipe implements Recipe<Inventory> {

    private final OritechRecipeType type;
    private final List<Ingredient> inputs;
    private final List<ItemStack> results;
    private final int time;

    public static final OritechRecipe DUMMY = new OritechRecipe(-1, DefaultedList.ofSize(1, Ingredient.ofStacks(Items.IRON_INGOT.getDefaultStack())), DefaultedList.ofSize(1, Items.IRON_BLOCK.getDefaultStack()), RecipeContent.PULVERIZER);

    public OritechRecipe(int time, List<Ingredient> inputs, List<ItemStack> results, OritechRecipeType type) {
        this.type = type;
        this.results = results;
        this.inputs = inputs;
        this.time = time;
    }

    @Override
    public boolean matches(Inventory machineInv, World world) {

        if (world.isClient) return false;

        if (machineInv.size() < inputs.size()) return false;

        var ingredients = getInputs();
        for (int i = 0; i < ingredients.size(); i++) {
            var entry = ingredients.get(i);
            if (!entry.test(machineInv.getStack(i))) {
                return false;
            }
        }

        return true;

    }
    @Override
    public ItemStack craft(Inventory inventory, DynamicRegistryManager registryManager) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResult(DynamicRegistryManager registryManager) {
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
    
}
