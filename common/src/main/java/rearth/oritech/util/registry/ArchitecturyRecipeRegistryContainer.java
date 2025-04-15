package rearth.oritech.util.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import rearth.oritech.Oritech;

import java.lang.reflect.Field;

public interface ArchitecturyRecipeRegistryContainer extends ArchitecturyRegistryContainer<RecipeType<?>> {
    
    DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTRY = DeferredRegister.create(Oritech.MOD_ID, RegistryKeys.RECIPE_SERIALIZER);
    
    @Override
    default RegistryKey<Registry<RecipeType<?>>> getRegistryType() {
        return RegistryKeys.RECIPE_TYPE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    default Class<RecipeType<?>> getTargetFieldType() {
        return (Class<RecipeType<?>>) (Object) RecipeType.class;
    }
    
    @Override
    default void postProcessField(String namespace, RecipeType<?> value, String identifier, Field field, RegistrySupplier<RecipeType<?>> supplier) {
        ArchitecturyRegistryContainer.super.postProcessField(namespace, value, identifier, field, supplier);
        SERIALIZER_REGISTRY.register(identifier, () -> (RecipeSerializer<?>) value);
    }
    
    static void finishSerializerRegister() {
        SERIALIZER_REGISTRY.register();
    }
    
}
