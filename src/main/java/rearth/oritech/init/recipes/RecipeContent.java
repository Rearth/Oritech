package rearth.oritech.init.recipes;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;

import java.util.function.Supplier;

public class RecipeContent {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Oritech.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Oritech.MOD_ID);

    public static final Supplier<RecipeSerializer<OritechRecipe>> ORITECH_SERIALIZER =
            RECIPE_SERIALIZERS.register("machine_recipe", () -> new RecipeSerializer<>(OritechRecipe.CODEC, OritechRecipe.STREAM_CODEC));

    public static final Supplier<RecipeType<OritechRecipe>> PULVERIZER = RECIPE_TYPES.register("pulverizer", () -> recipeType("pulverizer"));
    public static final Supplier<RecipeType<OritechRecipe>> GRINDER = RECIPE_TYPES.register("grinder", () -> recipeType("grinder"));
    public static final Supplier<RecipeType<OritechRecipe>> ASSEMBLER = RECIPE_TYPES.register("assembler", () -> recipeType("assembler"));
    public static final Supplier<RecipeType<OritechRecipe>> REFINERY = RECIPE_TYPES.register("refinery", () -> recipeType("refinery"));
    public static final Supplier<RecipeType<OritechRecipe>> FOUNDRY = RECIPE_TYPES.register("foundry", () -> recipeType("foundry"));
    public static final Supplier<RecipeType<OritechRecipe>> CENTRIFUGE = RECIPE_TYPES.register("centrifuge", () -> recipeType("centrifuge"));
    public static final Supplier<RecipeType<OritechRecipe>> CENTRIFUGE_FLUID = RECIPE_TYPES.register("centrifuge_fluid", () -> recipeType("centrifuge_fluid"));
    public static final Supplier<RecipeType<OritechRecipe>> ATOMIC_FORGE = RECIPE_TYPES.register("atomic_forge", () -> recipeType("atomic_forge"));
    public static final Supplier<RecipeType<OritechRecipe>> BIO_GENERATOR = RECIPE_TYPES.register("bio_generator", () -> recipeType("bio_generator"));
    public static final Supplier<RecipeType<OritechRecipe>> FUEL_GENERATOR = RECIPE_TYPES.register("fuel_generator", () -> recipeType("fuel_generator"));
    public static final Supplier<RecipeType<OritechRecipe>> LAVA_GENERATOR = RECIPE_TYPES.register("lava_generator", () -> recipeType("lava_generator"));
    public static final Supplier<RecipeType<OritechRecipe>> STEAM_ENGINE = RECIPE_TYPES.register("steam_engine", () -> recipeType("steam_engine"));
    public static final Supplier<RecipeType<OritechRecipe>> BEDROCK_EXTRACTOR = RECIPE_TYPES.register("bedrock_extractor", () -> recipeType("bedrock_extractor"));
    public static final Supplier<RecipeType<OritechRecipe>> PARTICLE_COLLISION = RECIPE_TYPES.register("particle_collision", () -> recipeType("particle_collision"));
    public static final Supplier<RecipeType<OritechRecipe>> INDUSTRIAL_CHILLER = RECIPE_TYPES.register("industrial_chiller", () -> recipeType("industrial_chiller"));
    public static final Supplier<RecipeType<OritechRecipe>> REACTOR = RECIPE_TYPES.register("reactor", () -> recipeType("reactor"));
    public static final Supplier<RecipeType<OritechRecipe>> LASER = RECIPE_TYPES.register("laser", () -> recipeType("laser"));
    public static final Supplier<RecipeType<OritechRecipe>> FURNACE_ADAPTER = RECIPE_TYPES.register("furnace_adapter", () -> recipeType("furnace_adapter"));

    private static <T extends Recipe<?>> RecipeType<T> recipeType(String path) {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return Oritech.id(path).toString();
            }
        };
    }

}
