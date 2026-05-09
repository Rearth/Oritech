package rearth.oritech.init.recipes;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import rearth.oritech.Oritech;
import rearth.oritech.util.registry.OritechDeferredRegistry;

public class RecipeContent {
    
    public static final OritechDeferredRegistry<RecipeType<?>> RECIPE_TYPES = OritechDeferredRegistry.create(Registries.RECIPE_TYPE);
    public static final OritechDeferredRegistry<RecipeSerializer<?>> RECIPE_SERIALIZERS = OritechDeferredRegistry.create(Registries.RECIPE_SERIALIZER);
    
    public static final RegistrySupplier<RecipeSerializer<OritechRecipe>> ORITECH_SERIALIZER = RECIPE_SERIALIZERS.register("machine_recipe", () -> new RecipeSerializer<>(OritechRecipe.CODEC, OritechRecipe.STREAM_CODEC));
    public static final RegistrySupplier<RecipeSerializer<AugmentDataRecipe>> AUGMENT_DATA_SERIALIZER = RECIPE_SERIALIZERS.register("augment_data", () -> new RecipeSerializer<>(AugmentDataRecipe.CODEC, AugmentDataRecipe.STREAM_CODEC));
    
    public static final RegistrySupplier<RecipeType<OritechRecipe>> PULVERIZER = RECIPE_TYPES.register("pulverizer", () -> recipeType("pulverizer"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> GRINDER = RECIPE_TYPES.register("grinder", () -> recipeType("grinder"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> ASSEMBLER = RECIPE_TYPES.register("assembler", () -> recipeType("assembler"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> REFINERY = RECIPE_TYPES.register("refinery", () -> recipeType("refinery"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> FOUNDRY = RECIPE_TYPES.register("foundry", () -> recipeType("foundry"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> CENTRIFUGE = RECIPE_TYPES.register("centrifuge", () -> recipeType("centrifuge"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> CENTRIFUGE_FLUID = RECIPE_TYPES.register("centrifuge_fluid", () -> recipeType("centrifuge_fluid"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> ATOMIC_FORGE = RECIPE_TYPES.register("atomic_forge", () -> recipeType("atomic_forge"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> BIO_GENERATOR = RECIPE_TYPES.register("bio_generator", () -> recipeType("bio_generator"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> FUEL_GENERATOR = RECIPE_TYPES.register("fuel_generator", () -> recipeType("fuel_generator"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> LAVA_GENERATOR = RECIPE_TYPES.register("lava_generator", () -> recipeType("lava_generator"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> STEAM_ENGINE = RECIPE_TYPES.register("steam_engine", () -> recipeType("steam_engine"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> DEEP_DRILL = RECIPE_TYPES.register("deep_drill", () -> recipeType("deep_drill"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> PARTICLE_COLLISION = RECIPE_TYPES.register("particle_collision", () -> recipeType("particle_collision"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> COOLER = RECIPE_TYPES.register("cooler", () -> recipeType("cooler"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> REACTOR = RECIPE_TYPES.register("reactor", () -> recipeType("reactor"));
    public static final RegistrySupplier<RecipeType<OritechRecipe>> LASER = RECIPE_TYPES.register("laser", () -> recipeType("laser"));
    
    public static final RegistrySupplier<RecipeType<AugmentDataRecipe>> AUGMENT_DATA = RECIPE_TYPES.register("augment_data", () -> recipeType("augment_data"));
    
    public static void registerTypes() {
        RECIPE_TYPES.register();
    }
    
    public static void registerSerializers() {
        RECIPE_SERIALIZERS.register();
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
