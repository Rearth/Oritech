package rearth.oritech.util.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

@SuppressWarnings({"UnstableApiUsage"})
public interface ArchitecturyRegistryContainer<T> {
    
    ResourceKey<Registry<T>> getRegistryType();
    
    Class<T> getTargetFieldType();
    
    default boolean shouldProcessField(T value, String identifier, Field field) {
        return true;
    }
    
    default void afterFieldProcessing() {}
    
    default void postProcessField(String namespace, T value, String identifier, Field field, RegistrySupplier<T> supplier) {}
    
    @SuppressWarnings("unchecked")
    static <T> void register(Class<? extends ArchitecturyRegistryContainer<T>> clazz, String namespace, boolean recurseIntoInnerClasses) {
        ArchitecturyRegistryContainer<T> container;
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            container = constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate registry container: " + clazz.getName(), e);
        }
        
        var registry = DeferredRegister.create(namespace, container.getRegistryType());
        var targetType = container.getTargetFieldType();
        
        for (var field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!Modifier.isPublic(field.getModifiers())) continue;
            if (!targetType.isAssignableFrom(field.getType())) continue;
            
            try {
                field.setAccessible(true);
                T value = (T) field.get(null);
                var identifier = field.getName().toLowerCase(java.util.Locale.ROOT);
                
                if (!container.shouldProcessField(value, identifier, field)) continue;
                
                var supplier = registry.register(identifier, () -> value);
                container.postProcessField(namespace, value, identifier, field, supplier);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + field.getName(), e);
            }
        }
        
        registry.register();
        container.afterFieldProcessing();
    }
}
