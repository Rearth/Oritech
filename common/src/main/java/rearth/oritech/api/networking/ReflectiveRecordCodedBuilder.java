package rearth.oritech.api.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class ReflectiveRecordCodedBuilder {
    
    /**
     * Creates a PacketCodec for a given record type using reflection.
     * The record's components must have corresponding PacketCodecs registered in TYPE_TO_CODEC_MAP.
     *
     * @param recordClass The class of the record.
     * @param <T>         The type of the record.
     * @return A PacketCodec for the record.
     * @throws RuntimeException if an error occurs during reflection or codec lookup.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Record> PacketCodec<RegistryByteBuf, T> create(Class<T> recordClass) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException(recordClass.getName() + " is not a record type.");
        }
        
        var recordComponents = recordClass.getRecordComponents();
        var lookup = MethodHandles.publicLookup();
        
        var accessors = new ArrayList<MethodHandle>(recordComponents.length);
        var componentCodecs = new ArrayList<PacketCodec<RegistryByteBuf, ?>>(recordComponents.length);
        var componentTypes = new Class<?>[recordComponents.length];
        
        for (int i = 0; i < recordComponents.length; i++) {
            var component = recordComponents[i];
            componentTypes[i] = component.getType();
            try {
                accessors.add(lookup.unreflect(component.getAccessor()));
                var listCandidate = NetworkManager.getListType(component.getGenericType());
                if (listCandidate.isPresent()) {
                    componentCodecs.add(NetworkManager.getAutoCodec((Class<?>) listCandidate.get()).collect(PacketCodecs.toList()));
                } else {
                    componentCodecs.add(NetworkManager.getAutoCodec(component.getType()));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to unreflect accessor for component: " + component.getName() + " in " + recordClass.getName(), e);
            }
        }
        
        MethodHandle constructorHandle;
        try {
            Constructor<T> constructor = recordClass.getDeclaredConstructor(componentTypes);
            constructor.setAccessible(true); // Ensure accessibility if not public (though canonical constructors are)
            constructorHandle = lookup.unreflectConstructor(constructor);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to find or unreflect canonical constructor for record: " + recordClass.getName(), e);
        }
        
        return new PacketCodec<>() {
            @Override
            public T decode(RegistryByteBuf buffer) {
                var constructorArgs = new Object[recordComponents.length];
                for (int i = 0; i < recordComponents.length; i++) {
                    try {
                        constructorArgs[i] = componentCodecs.get(i).decode(buffer);
                    } catch (Exception e) {
                        throw new RuntimeException("Error decoding component " + recordComponents[i].getName() + " for record " + recordClass.getName(), e);
                    }
                }
                try {
                    return (T) constructorHandle.invokeWithArguments(constructorArgs);
                } catch (Throwable e) { // MethodHandle.invokeWithArguments throws Throwable
                    throw new RuntimeException("Error constructing record " + recordClass.getName(), e);
                }
            }
            
            @Override
            public void encode(RegistryByteBuf buffer, T recordInstance) {
                for (int i = 0; i < recordComponents.length; i++) {
                    try {
                        Object value = accessors.get(i).invoke(recordInstance);
                        PacketCodec<RegistryByteBuf, Object> valueCodec = (PacketCodec<RegistryByteBuf, Object>) componentCodecs.get(i);
                        valueCodec.encode(buffer, value);
                    } catch (Throwable e) {
                        throw new RuntimeException("Error encoding component " + recordComponents[i].getName() + " of record " + recordClass.getName(), e);
                    }
                }
            }
        };
    }
}
