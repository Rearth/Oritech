package rearth.oritech.init.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
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

    public static final Endec<OritechRecipe> ORITECH_ENDEC = StructEndecBuilder.of(
            Endec.INT.fieldOf("energyPerTick", OritechRecipe::getEnergyPerTick),
            Endec.INT.fieldOf("time", OritechRecipe::getTime),
            Endec.ofCodec(Ingredient.DISALLOW_EMPTY_CODEC).listOf().fieldOf("ingredients", OritechRecipe::getInputs),
            BuiltInEndecs.ITEM_STACK.listOf().fieldOf("results", OritechRecipe::getResults),
            BuiltInEndecs.IDENTIFIER.xmap(OritechRecipeType::new, OritechRecipeType::identifier).fieldOf("type", OritechRecipe::getOriType),
            OritechRecipe::new
    );

    public static final Codec<OritechRecipe> ORITECH_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("energyPerTick").forGetter(OritechRecipe::getEnergyPerTick),
            Codec.INT.fieldOf("time").forGetter(OritechRecipe::getTime),
            Ingredient.DISALLOW_EMPTY_CODEC.listOf().fieldOf("ingredients").forGetter(OritechRecipe::getInputs),
            ItemStack.CODEC.listOf().fieldOf("results").forGetter(OritechRecipe::getResults),
            Identifier.CODEC.xmap(OritechRecipeType::new, OritechRecipeType::identifier).fieldOf("type").forGetter(OritechRecipe::getOriType)
    ).apply(instance, OritechRecipe::new));

    public record OritechRecipeType(Identifier identifier) implements RecipeType<OritechRecipe>, RecipeSerializer<OritechRecipe> {

        @Override
        public Codec<OritechRecipe> codec() {
            return ORITECH_CODEC;
            
            // return ORITECH_ENDEC.codec(SerializationAttribute.HUMAN_READABLE);
        }

        @Override
        public OritechRecipe read(PacketByteBuf buf) {
            return buf.read(ORITECH_ENDEC);
        }

        @Override
        public void write(PacketByteBuf buf, OritechRecipe recipe) {
            buf.write(ORITECH_ENDEC, recipe);
        }
    }
}
