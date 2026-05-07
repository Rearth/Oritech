package rearth.oritech.util.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import rearth.oritech.Oritech;

import java.util.Iterator;
import java.util.function.Supplier;

public class OritechDeferredRegistry<T> implements Iterable<RegistrySupplier<T>> {

    private final ResourceKey<Registry<T>> registryKey;
    private final DeferredRegister<T> register;
    private boolean committed;

    protected OritechDeferredRegistry(ResourceKey<Registry<T>> registryKey) {
        this.registryKey = registryKey;
        this.register = DeferredRegister.create(Oritech.MOD_ID, registryKey);
    }

    public static <T> OritechDeferredRegistry<T> create(ResourceKey<Registry<T>> registryKey) {
        return new OritechDeferredRegistry<>(registryKey);
    }

    public <R extends T> RegistrySupplier<R> register(String path, Supplier<? extends R> supplier) {
        return register(id(path), supplier);
    }

    public <R extends T> RegistrySupplier<R> register(Identifier id, Supplier<? extends R> supplier) {
        if (committed) {
            throw new IllegalStateException("Cannot add registry entry after committing " + registryKey.identifier());
        }

        return register.register(id, supplier);
    }

    public Identifier id(String path) {
        return Oritech.id(path);
    }

    public ResourceKey<T> key(String path) {
        return ResourceKey.create(registryKey, id(path));
    }

    public ResourceKey<Registry<T>> registryKey() {
        return registryKey;
    }

    public DeferredRegister<T> backingRegister() {
        return register;
    }

    public void register() {
        if (committed) {
            throw new IllegalStateException("Registry already committed: " + registryKey.identifier());
        }

        committed = true;
        register.register();
    }

    @Override
    public Iterator<RegistrySupplier<T>> iterator() {
        return register.iterator();
    }
}