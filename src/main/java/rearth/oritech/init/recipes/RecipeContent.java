package rearth.oritech.init.recipes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class RecipeContent {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Oritech.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Oritech.MOD_ID);

    public static final Supplier<RecipeType<OritechRecipe>> PULVERIZER = CreateRecipeType("pulverizer");
    public static final Supplier<RecipeType<OritechRecipe>> FRAGMENT_FORGE = CreateRecipeType("grinder");
    public static final Supplier<RecipeType<OritechRecipe>> ASSEMBLER = CreateRecipeType("assembler");
    public static final Supplier<RecipeType<OritechRecipe>> REFINERY = CreateRecipeType("refinery");
    public static final Supplier<RecipeType<OritechRecipe>> FOUNDRY = CreateRecipeType("foundry");
    public static final Supplier<RecipeType<OritechRecipe>> CENTRIFUGE = CreateRecipeType("centrifuge");
    public static final Supplier<RecipeType<OritechRecipe>> CENTRIFUGE_FLUID = CreateRecipeType("centrifuge_fluid");
    public static final Supplier<RecipeType<OritechRecipe>> ATOMIC_FORGE = CreateRecipeType("atomic_forge");
    public static final Supplier<RecipeType<OritechRecipe>> BIO_GENERATOR = CreateRecipeType("bio_generator");
    public static final Supplier<RecipeType<OritechRecipe>> FUEL_GENERATOR = CreateRecipeType("fuel_generator");
    public static final Supplier<RecipeType<OritechRecipe>> LAVA_GENERATOR = CreateRecipeType("lava_generator");
    public static final Supplier<RecipeType<OritechRecipe>> STEAM_ENGINE = CreateRecipeType("steam_engine");
    public static final Supplier<RecipeType<OritechRecipe>> BEDROCK_EXTRACTOR = CreateRecipeType("bedrock_extractor");
    public static final Supplier<RecipeType<OritechRecipe>> PARTICLE_COLLISION = CreateRecipeType("particle_collision");
    public static final Supplier<RecipeType<OritechRecipe>> INDUSTRIAL_CHILLER = CreateRecipeType("industrial_chiller");
    public static final Supplier<RecipeType<OritechRecipe>> REACTOR = CreateRecipeType("reactor");
    public static final Supplier<RecipeType<OritechRecipe>> LASER = CreateRecipeType("laser");
    public static final Supplier<RecipeType<OritechRecipe>> FURNACE_ADAPTER = CreateRecipeType("furnace_adapter");

    private static Supplier<RecipeType<OritechRecipe>> CreateRecipeType(String path) {
        DeferredHolder<RecipeType<?>, RecipeType<OritechRecipe>> type = RECIPE_TYPES.register(path, () -> recipeType(path));
        RECIPE_SERIALIZERS.register(path, () -> OritechRecipe.CreateSerializerForType(type.get()));
        return type;
    }

    @SuppressWarnings("unchecked")
    public static RecipeSerializer<OritechRecipe> GetSerializerByType(RecipeType<OritechRecipe> type) {
        var id = requireNonNull(BuiltInRegistries.RECIPE_TYPE.getKey(type));
        var serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(id)
                .orElseThrow(() -> new IllegalStateException("No recipe serializer registered for " + id));
        return (RecipeSerializer<OritechRecipe>) serializer.value();
    }

    private static <T extends Recipe<?>> RecipeType<T> recipeType(String path) {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return Oritech.id(path).toString();
            }
        };
    }

}
