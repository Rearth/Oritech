package rearth.oritech.util.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.wispforest.owo.registration.reflect.FieldProcessingSubject;
import io.wispforest.owo.util.ReflectionUtils;
import java.lang.reflect.Field;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

@SuppressWarnings({"UnstableApiUsage"})
public interface ArchitecturyRegistryContainer<T> extends FieldProcessingSubject<T> {
    
    ResourceKey<Registry<T>> getRegistryType();
    
    default void postProcessField(String namespace, T value, String identifier, Field field, RegistrySupplier<T> supplier) {}
    
    static <T> void register(Class<? extends ArchitecturyRegistryContainer<T>> clazz, String namespace, boolean recurseIntoInnerClasses) {
        ArchitecturyRegistryContainer<T> container = ReflectionUtils.tryInstantiateWithNoArgs(clazz);
        
        var registry = DeferredRegister.create(namespace, container.getRegistryType());
        
        ReflectionUtils.iterateAccessibleStaticFields(clazz, container.getTargetFieldType(), createProcessor((fieldValue, identifier, field) -> {
            var supplier = registry.register(identifier, () -> fieldValue);
            
            container.postProcessField(namespace, fieldValue, identifier, field, supplier);
        }, container));
        
        registry.register();
        
        container.afterFieldProcessing();
    }
    
    private static <T> ReflectionUtils.FieldConsumer<T> createProcessor(ReflectionUtils.FieldConsumer<T> delegate, FieldProcessingSubject<T> handler) {
        return (value, name, field) -> {
            if (!handler.shouldProcessField(value, name, field)) return;
            delegate.accept(value, name, field);
        };
    }
}
