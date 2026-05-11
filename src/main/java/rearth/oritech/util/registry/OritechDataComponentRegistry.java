package rearth.oritech.util.registry;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.UnaryOperator;

public class OritechDataComponentRegistry extends OritechDeferredRegistry<DataComponentType<?>> {

    public OritechDataComponentRegistry() {
      super(Registries.DATA_COMPONENT_TYPE);
    }

    public <T> RegistrySupplier<DataComponentType<T>> registerComponent(
      String path,
      UnaryOperator<DataComponentType.Builder<T>> builder
    ) {
        return register(path, () -> builder.apply(DataComponentType.<T>builder()).build());
    }

    public <T> RegistrySupplier<DataComponentType<T>> registerPersistent(String path, Codec<T> codec) {
        return registerComponent(path, builder -> builder.persistent(codec));
    }

    public <T> RegistrySupplier<DataComponentType<T>> registerSynchronized(
      String path,
      StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
    ) {
        return registerComponent(path, builder -> builder.networkSynchronized(streamCodec));
    }

    public <T> RegistrySupplier<DataComponentType<T>> registerPersistentSynchronized(
      String path,
      Codec<T> codec,
      StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
    ) {
        return registerComponent(path, builder -> builder.persistent(codec).networkSynchronized(streamCodec));
    }
}