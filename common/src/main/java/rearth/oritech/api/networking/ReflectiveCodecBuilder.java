package rearth.oritech.api.networking;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import static rearth.oritech.api.networking.NetworkManager.getAutoCodec;


public class ReflectiveCodecBuilder {
    
    public static <E extends Enum<E>> StreamCodec<RegistryFriendlyByteBuf, E> createForEnum(Class<E> enumClass) {
        return new StreamCodec<>() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, E value) {
                buf.writeShort(value.ordinal());
            }
            
            @Override
            public E decode(RegistryFriendlyByteBuf buf) {
                var ordinal = buf.readShort();
                return enumClass.getEnumConstants()[ordinal];
            }
        };
    }
    
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
    public static <T extends Record> StreamCodec<RegistryFriendlyByteBuf, T> create(Class<T> recordClass) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException(recordClass.getName() + " is not a record type.");
        }
        
        var recordComponents = recordClass.getRecordComponents();
        var lookup = MethodHandles.publicLookup();
        
        var accessors = new ArrayList<MethodHandle>(recordComponents.length);
        var componentCodecs = new ArrayList<StreamCodec<RegistryFriendlyByteBuf, ?>>(recordComponents.length);
        var componentTypes = new Class<?>[recordComponents.length];
        
        for (int i = 0; i < recordComponents.length; i++) {
            var component = recordComponents[i];
            componentTypes[i] = component.getType();
            try {
                accessors.add(lookup.unreflect(component.getAccessor()));
                var listCandidate = NetworkManager.getListType(component.getGenericType());
                var mapCandidate = NetworkManager.getMapType(component.getGenericType());
                if (listCandidate.isPresent()) {
                    var codec = getAutoCodec((Class<?>) listCandidate.get()).apply(ByteBufCodecs.list());
                    if (codec == null)
                        throw new RuntimeException("Failed to get codec for record component: " + component.getName() + " in " + recordClass.getName());
                    componentCodecs.add(codec);
                } else if (mapCandidate.isPresent()) {
                    var keyCodec = getAutoCodec((Class<?>) mapCandidate.get().getA());
                    var valueCodec = getAutoCodec((Class<?>) mapCandidate.get().getB());
                    var codec = ByteBufCodecs.map(HashMap::new, keyCodec, valueCodec);
                    componentCodecs.add(codec);
                } else {
                    var codec = getAutoCodec(component.getType());
                    if (codec == null)
                        throw new RuntimeException("Failed to get codec for record component: " + component.getName() + " in " + recordClass.getName());
                    componentCodecs.add(codec);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to unreflect accessor for component: " + component.getName() + " in " + recordClass.getName(), e);
            } catch (NullPointerException e) {
                throw new RuntimeException("Failed to get codec for component: " + component.getName() + " in " + recordClass.getName(), e);
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
        
        return new StreamCodec<>() {
            @Override
            public T decode(RegistryFriendlyByteBuf buffer) {
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
            public void encode(RegistryFriendlyByteBuf buffer, T recordInstance) {
                for (int i = 0; i < recordComponents.length; i++) {
                    try {
                        Object value = accessors.get(i).invoke(recordInstance);
                        StreamCodec<RegistryFriendlyByteBuf, Object> valueCodec = (StreamCodec<RegistryFriendlyByteBuf, Object>) componentCodecs.get(i);
                        valueCodec.encode(buffer, value);
                    } catch (Throwable e) {
                        throw new RuntimeException("Error encoding component " + recordComponents[i].getName() + " of record " + recordClass.getName(), e);
                    }
                }
            }
        };
    }
}
