package rearth.oritech.util.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import rearth.oritech.Oritech;

import java.lang.reflect.Field;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public interface ArchitecturyRecipeRegistryContainer extends ArchitecturyRegistryContainer<RecipeType<?>> {
    
    DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTRY = DeferredRegister.create(Oritech.MOD_ID, Registries.RECIPE_SERIALIZER);
    
    @Override
    default ResourceKey<Registry<RecipeType<?>>> getRegistryType() {
        return Registries.RECIPE_TYPE;
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
