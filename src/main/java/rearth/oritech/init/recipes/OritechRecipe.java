package rearth.oritech.init.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.SerializationAttribute;
import io.wispforest.owo.serialization.StructEndec;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import io.wispforest.owo.serialization.util.EndecRecipeSerializer;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.List;

public class OritechRecipe implements Recipe<Inventory> {

    private final OritechRecipeType type;

    private final List<Ingredient> inputs;
    private final List<ItemStack> results;
    private final int energyPerTick;
    private final int time;

    public static final OritechRecipe DUMMY = new OritechRecipe(-1, 10, DefaultedList.ofSize(1, Ingredient.ofStacks(Items.IRON_INGOT.getDefaultStack())), DefaultedList.ofSize(1, Items.IRON_BLOCK.getDefaultStack()), RecipeContent.PULVERIZER);

    public OritechRecipe(int energy, int time, List<Ingredient> inputs, List<ItemStack> results, OritechRecipeType type) {
        this.type = type;
        this.results = results;
        this.inputs = inputs;
        this.energyPerTick = energy;
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
                ", energy=" + energyPerTick +
                ", time=" + time +
                '}';
    }

    public int getEnergyPerTick() {
        return energyPerTick;
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
    
    public static class OritechRecipeType extends EndecRecipeSerializer<OritechRecipe> implements RecipeType<OritechRecipe> {
        
        public static final Endec<OritechRecipe> ORI_RECIPE_ENDEC = StructEndecBuilder.of(
          Endec.INT.fieldOf("energyPerTick", OritechRecipe::getEnergyPerTick),
          Endec.INT.fieldOf("time", OritechRecipe::getTime),
          Endec.ofCodec(Ingredient.DISALLOW_EMPTY_CODEC).listOf().fieldOf("ingredients", OritechRecipe::getInputs),
          BuiltInEndecs.ITEM_STACK.listOf().fieldOf("results", OritechRecipe::getResults),
          BuiltInEndecs.IDENTIFIER.xmap(identifier1 -> (OritechRecipeType) Registries.RECIPE_TYPE.get(identifier1), OritechRecipeType::getIdentifier).fieldOf("type", OritechRecipe::getOriType),
          OritechRecipe::new
        );
        
        private final Identifier identifier;
        
        public Identifier getIdentifier() {
            return identifier;
        }
        
        protected OritechRecipeType(Identifier identifier) {
            super((StructEndec<OritechRecipe>) ORI_RECIPE_ENDEC);
            this.identifier = identifier;
        }
        
        @Override
        public String toString() {
            return "OritechRecipeType{" +
                     "identifier=" + identifier +
                     '}';
        }
    }
}
