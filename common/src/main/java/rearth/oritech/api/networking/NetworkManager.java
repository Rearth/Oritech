package rearth.oritech.api.networking;

import dev.architectury.fluid.FluidStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.network.NetworkContent;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class NetworkManager {
    
    private static final Map<Type, PacketCodec<? extends ByteBuf, ?>> AUTO_CODECS = new HashMap<>();
    
    @ExpectPlatform
    public static void sendBlockHandle(BlockEntity blockEntity, CustomPayload message) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static void sendPlayerHandle(CustomPayload message, ServerPlayerEntity player) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static void sendToServer(CustomPayload message) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static <T extends CustomPayload> void registerToClient(CustomPayload.Id<T> id, PacketCodec<RegistryByteBuf, T> packetCodec, TriConsumer<T, World, DynamicRegistryManager> consumer) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static <T extends CustomPayload> void registerToServer(CustomPayload.Id<T> id, PacketCodec<RegistryByteBuf, T> packetCodec, TriConsumer<T, World, DynamicRegistryManager> consumer) {
        throw new AssertionError();
    }
    
    public static void registerDefaultCodecs() {
        
        // missing bool, double, byte, short, string, uuid, identifier, vec3i, blockpos
        registerCodec(PacketCodecs.INTEGER, Integer.class, int.class);
        registerCodec(PacketCodecs.VAR_LONG, Long.class, long.class);
        registerCodec(PacketCodecs.FLOAT, Float.class, float.class);
        registerCodec(PacketCodecs.BOOL, Boolean.class, boolean.class);
        registerCodec(PacketCodecs.DOUBLE, Double.class, double.class);
        registerCodec(PacketCodecs.BYTE, Byte.class, byte.class);
        registerCodec(PacketCodecs.SHORT, Short.class, short.class);
        registerCodec(PacketCodecs.STRING, String.class);
        registerCodec(Identifier.PACKET_CODEC, Identifier.class);
        registerCodec(BlockPos.PACKET_CODEC, BlockPos.class);
        registerCodec(ItemStack.PACKET_CODEC, ItemStack.class);
        registerCodec(VEC2I_PACKED_CODEC, Vector2i.class);
        registerCodec(SIMPLE_BLOCK_STATE_PACKET_CODEC, BlockState.class);
        registerCodec(NetworkContent.FLUID_STACK_STREAM_CODEC, FluidStack.class);
        registerCodec(ItemFilterBlockEntity.FilterData.PACKET_CODEC, ItemFilterBlockEntity.FilterData.class);
        
    }
    
    public static <T> void registerCodec(PacketCodec<? extends ByteBuf, T> codec, Type... classes) {
        for (var clazz : classes)
            AUTO_CODECS.put(clazz, codec);
    }
    
    public static void init() {
        registerDefaultCodecs();
        registerToClient(MessagePayload.GENERIC_PACKET_ID, MessagePayload.PACKET_CODEC, NetworkManager::receiveMessage);
        
        registerToServer(ItemFilterBlockEntity.ItemFilterPayload.FILTER_PACKET_ID, ItemFilterBlockEntity.ItemFilterPayload.PACKET_CODEC, ItemFilterBlockEntity::handleClientUpdate);
    }
    
    public static void receiveMessage(MessagePayload message, World world, DynamicRegistryManager registryAccess) {
        var receivedBuf = new RegistryByteBuf(Unpooled.wrappedBuffer(message.message), registryAccess);
        var receiverEntity = world.getBlockEntity(message.pos);
        var receiverType = registryAccess.get(RegistryKeys.BLOCK_ENTITY_TYPE).get(message.targetEntityType);
        if (receiverEntity != null && receiverType != null && receiverType.equals(receiverEntity.getType())) {
            decodeFields(receiverEntity, message.syncType, receivedBuf);
            if (receiverEntity instanceof NetworkedEventHandler networkedBlock) {
                networkedBlock.onNetworkUpdated();
            }
        } else {
            Oritech.LOGGER.debug("Unable to start decoding for block entity type {} at {}. Target Mismatch!", receiverType, message.pos);
        }
    }
    
    // returns the number of encoded fields
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int encodeFields(Object target, SyncType type, ByteBuf byteBuf) {
        
        var encodedCount = 0;
        for (var field : getSyncFields(target, type)) {
            field.setAccessible(true);
            try {
                if (UpdatableField.class.isAssignableFrom(field.getType())) {
                    var fieldInstance = ((UpdatableField) field.get(target));
                    var deltaOnly = fieldInstance.useDeltaOnly(type);
                    var dataToSend = deltaOnly ? fieldInstance.getDeltaData() : fieldInstance;
                    var codec = deltaOnly ? fieldInstance.getDeltaCodec() : fieldInstance.getFullCodec();
                    codec.encode(byteBuf, dataToSend);
                } else {
                    var codec = getAutoCodec(field);
                    var value = field.get(target);
                    codec.encode(byteBuf, value);
                }
                
                encodedCount++;
                
            } catch (Exception ex) {
                Oritech.LOGGER.warn("failed to encode field: {}", field.getName(), ex);
            }
        }
        
        return encodedCount;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void decodeFields(Object target, SyncType type, ByteBuf byteBuf) {
        for (var field : getSyncFields(target, type)) {
            field.setAccessible(true);
            try {
                // fields that implement UpdatableField either get a delta or full update. Otherwise, we just set the full value
                if (UpdatableField.class.isAssignableFrom(field.getType())) {
                    var fieldInstance = ((UpdatableField) field.get(target));
                    var deltaOnly = fieldInstance.useDeltaOnly(type);
                    var codec = deltaOnly ? fieldInstance.getDeltaCodec() : fieldInstance.getFullCodec();
                    var value = codec.decode(byteBuf);
                    if (deltaOnly) {
                        fieldInstance.handleDeltaUpdate(value);
                    } else {
                        fieldInstance.handleFullUpdate(value);
                    }
                } else {
                    var codec = getAutoCodec(field);
                    var value = codec.decode(byteBuf);
                    field.set(target, value);
                }
                
            } catch (Exception ex) {
                Oritech.LOGGER.warn("failed to decode field: {}", field.getName(), ex);
            }
        }
    }
    
    private static @NotNull List<Field> getSyncFields(Object target, SyncType type) {
        var fields = new ArrayList<>(Arrays.asList(target.getClass().getDeclaredFields()));
        var superClass = target.getClass().getSuperclass();
        while (superClass != null) {
            fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
            superClass = superClass.getSuperclass();
        }
        
        var filteredFields = new ArrayList<Field>();
        fields.stream().filter(field -> hasSyncType(field.getAnnotation(SyncField.class), type)).forEachOrdered(filteredFields::add);
        
        if (target instanceof AdditionalNetworkingProvider additionalNetworkingProvider)
            filteredFields.addAll(additionalNetworkingProvider.additionalSyncedFields(type));
        
        return filteredFields;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static PacketCodec getAutoCodec(Class<?> type) {
        
        // try to create codec for records
        if (!AUTO_CODECS.containsKey(type) && type.isRecord()) {
            System.out.println("creating reflective codec for: " + type);
            var computedCodec = ReflectiveRecordCodedBuilder.create((Class<? extends Record>) type);
            AUTO_CODECS.put(type, computedCodec);
            return computedCodec;
        }
        
        if (!AUTO_CODECS.containsKey(type)) {
            Oritech.LOGGER.error("No codec defined for: {}", type);
        }
        
        return AUTO_CODECS.get(type);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static PacketCodec getAutoCodec(Field field) {
        var listType = getListType(field.getGenericType());
        if (listType.isPresent()) {
            var listTypeCodec = getAutoCodec((Class<?>) listType.get());
            return listTypeCodec.collect(PacketCodecs.toList());
        }
        var mapType = getMapType(field.getGenericType());
        if (mapType.isPresent()) {
            var keyCodec = getAutoCodec((Class<?>) mapType.get().getLeft());
            var valueCodec = getAutoCodec((Class<?>) mapType.get().getRight());
            
            if (keyCodec == null)
                Oritech.LOGGER.error("Unable to get codec for map key type: {}", field.getType());
            if (valueCodec == null)
                Oritech.LOGGER.error("Unable to get codec for map value type: {}", field.getType());
            
            return PacketCodecs.map(HashMap::new, keyCodec, valueCodec);
        }
        
        return getAutoCodec(field.getType());
    }
    
    // Method for checking if a given type is a List and for retrieving its type parameter
    public static Optional<Type> getListType(Type type) {
        if (type instanceof ParameterizedType pType) {
            var rawType = (Class<?>) pType.getRawType();
            if (rawType instanceof Class && List.class.isAssignableFrom(rawType)) {
                return Optional.of(pType.getActualTypeArguments()[0]);
            }
        }
        return Optional.empty();
    }
    
    // Method for checking if a given type is a Map and for retrieving its type parameters
    public static Optional<Pair<Type, Type>> getMapType(Type type) {
        if (type instanceof ParameterizedType pType) {
            var rawType = (Class<?>) pType.getRawType();
            if (rawType instanceof Class && Map.class.isAssignableFrom(rawType)) {
                var typeArgs = pType.getActualTypeArguments();
                return Optional.of(new Pair<>(typeArgs[0], typeArgs[1]));
            }
        }
        return Optional.empty();
    }
    
    private static boolean hasSyncType(SyncField annotation, SyncType type) {
        if (annotation == null) return false;
        
        for (var value : annotation.value()) {
            if (value.equals(type)) return true;
        }
        return false;
    }
    
    public record MessagePayload(BlockPos pos, Identifier targetEntityType, SyncType syncType,
                                 byte[] message) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return GENERIC_PACKET_ID;
        }
        
        public static final CustomPayload.Id<MessagePayload> GENERIC_PACKET_ID = new CustomPayload.Id<>(Oritech.id("generic"));
        
        public static final PacketCodec<RegistryByteBuf, MessagePayload> PACKET_CODEC = new PacketCodec<>() {
            @Override
            public MessagePayload decode(RegistryByteBuf buf) {
                return new MessagePayload(BlockPos.PACKET_CODEC.decode(buf), Identifier.PACKET_CODEC.decode(buf), SyncType.PACKET_CODEC.decode(buf), PacketCodecs.BYTE_ARRAY.decode(buf));
            }
            
            @Override
            public void encode(RegistryByteBuf buf, MessagePayload value) {
                BlockPos.PACKET_CODEC.encode(buf, value.pos);
                Identifier.PACKET_CODEC.encode(buf, value.targetEntityType);
                SyncType.PACKET_CODEC.encode(buf, value.syncType);
                PacketCodecs.BYTE_ARRAY.encode(buf, value.message);
            }
        };
    }
    
    // transmits only the block type, with the default block state. Custom properties are not sent.
    public static PacketCodec<RegistryByteBuf, BlockState> SIMPLE_BLOCK_STATE_PACKET_CODEC = new PacketCodec<>() {
        @Override
        public BlockState decode(RegistryByteBuf buf) {
            return Registries.BLOCK.get(Identifier.PACKET_CODEC.decode(buf)).getDefaultState();
        }
        
        @Override
        public void encode(RegistryByteBuf buf, BlockState value) {
            Identifier.PACKET_CODEC.encode(buf, Registries.BLOCK.getId(value.getBlock()));
        }
    };
    
    public static PacketCodec<RegistryByteBuf, Vector2i> VEC2I_PACKED_CODEC = PacketCodec.tuple(
      PacketCodecs.INTEGER, Vector2i::x,
      PacketCodecs.INTEGER, Vector2i::y,
      Vector2i::new
    );
    
    @SuppressWarnings("unchecked")
    public static <K, V> PacketCodec<RegistryByteBuf, HashMap<K, V>> createMapCodec(Class<K> keyType, Class<V> valueType) {
        return PacketCodecs.map(HashMap::new, getAutoCodec(keyType), getAutoCodec(valueType));
    }
    
}
