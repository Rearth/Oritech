package rearth.oritech.api.networking;

import com.mojang.serialization.Codec;
import dev.architectury.fluid.FluidStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.OritechPlatform;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.block.entity.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;
import rearth.oritech.block.entity.arcane.EnchantmentCatalystBlockEntity;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.armor.JetpackItem;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class NetworkManager {
    
    private static final Map<Type, StreamCodec<? extends ByteBuf, ?>> AUTO_CODECS = new HashMap<>();
    private static final Map<Integer, List<Field>> CACHED_FIELDS = new HashMap<Integer, List<Field>>();
    
    // these two are basically copies of the architectury built-in fluid stack codecs, but using the OPTIONAL_STREAM_CODEC to allow for empty fluid stacks
    public static Codec<FluidStack> FLUID_STACK_CODEC;
    public static StreamCodec<RegistryFriendlyByteBuf, FluidStack> FLUID_STACK_STREAM_CODEC;
    
    public static void sendBlockHandle(BlockEntity blockEntity, CustomPacketPayload message) {
        OritechPlatform.INSTANCE.sendBlockHandle(blockEntity, message);
    }
    
    public static void sendPlayerHandle(CustomPacketPayload message, ServerPlayer player) {
        OritechPlatform.INSTANCE.sendPlayerHandle(message, player);
    }
    
    public static void sendToServer(CustomPacketPayload message) {
        OritechPlatform.INSTANCE.sendToServer(message);
    }
    
    public static <T extends CustomPacketPayload> void registerToClient(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Level, RegistryAccess> consumer) {
        OritechPlatform.INSTANCE.registerToClient(id, packetCodec, consumer);
    }
    
    public static <T extends CustomPacketPayload> void registerToServer(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Player, RegistryAccess> consumer) {
        OritechPlatform.INSTANCE.registerToServer(id, packetCodec, consumer);
    }
    
    public static void registerDefaultCodecs() {
        
        registerCodec(ByteBufCodecs.INT, Integer.class, int.class);
        registerCodec(ByteBufCodecs.VAR_LONG, Long.class, long.class);
        registerCodec(ByteBufCodecs.FLOAT, Float.class, float.class);
        registerCodec(ByteBufCodecs.BOOL, Boolean.class, boolean.class);
        registerCodec(ByteBufCodecs.DOUBLE, Double.class, double.class);
        registerCodec(ByteBufCodecs.BYTE, Byte.class, byte.class);
        registerCodec(ByteBufCodecs.SHORT, Short.class, short.class);
        registerCodec(ByteBufCodecs.STRING_UTF8, String.class);
        registerCodec(ResourceLocation.STREAM_CODEC, ResourceLocation.class);
        registerCodec(BlockPos.STREAM_CODEC, BlockPos.class);
        registerCodec(ItemStack.OPTIONAL_STREAM_CODEC, ItemStack.class);
        registerCodec(VEC2I_PACKED_CODEC, Vector2i.class);
        registerCodec(VEC3D_PACKET_CODEC, Vec3.class);
        registerCodec(SIMPLE_BLOCK_STATE_PACKET_CODEC, BlockState.class);
        registerCodec(FLUID_STACK_STREAM_CODEC, FluidStack.class);
        registerCodec(ItemFilterBlockEntity.FilterData.PACKET_CODEC, ItemFilterBlockEntity.FilterData.class);
        registerCodec(OritechRecipeType.PACKET_CODEC, OritechRecipe.class);
        registerCodec(LaserArmBlockEntity.LASER_TARGET_PACKET_CODEC, LivingEntity.class);
        registerCodec(AugmentApplicationEntity.ResearchState.PACKET_CODEC, AugmentApplicationEntity.ResearchState.class);
        
    }
    
    public static <T> void registerCodec(StreamCodec<? extends ByteBuf, T> codec, Type... classes) {
        for (var clazz : classes)
            AUTO_CODECS.put(clazz, codec);
    }
    
    @SuppressWarnings("unchecked")
    public static void init() {
        registerDefaultCodecs();
        
        registerToServer(ItemFilterBlockEntity.ItemFilterPayload.FILTER_PACKET_ID, ItemFilterBlockEntity.ItemFilterPayload.PACKET_CODEC, ItemFilterBlockEntity::handleClientUpdate);
        registerToServer(EnchanterBlockEntity.SelectEnchantingPacket.PACKET_ID, getAutoCodec(EnchanterBlockEntity.SelectEnchantingPacket.class), EnchanterBlockEntity::receiveEnchantmentSelection);
        registerToServer(RedstoneAddonBlockEntity.RedstoneAddonServerUpdate.PACKET_ID, getAutoCodec(RedstoneAddonBlockEntity.RedstoneAddonServerUpdate.class), RedstoneAddonBlockEntity::receiveOnServer);
        registerToServer(PortableLaserItem.LaserPlayerUsePacket.PACKET_ID, getAutoCodec(PortableLaserItem.LaserPlayerUsePacket.class), PortableLaserItem::receiveUsePacket);
        registerToServer(MachineBlockEntity.InventoryInputModeSelectorPacket.PACKET_ID, getAutoCodec(MachineBlockEntity.InventoryInputModeSelectorPacket.class), MachineBlockEntity::receiveCycleModePacket);
        registerToServer(InventoryProxyAddonBlockEntity.InventoryProxySlotSelectorPacket.PACKET_ID, getAutoCodec(InventoryProxyAddonBlockEntity.InventoryProxySlotSelectorPacket.class), InventoryProxyAddonBlockEntity::receiveSlotSelection);
        registerToServer(JetpackItem.JetpackUsageUpdatePacket.PACKET_ID, getAutoCodec(JetpackItem.JetpackUsageUpdatePacket.class), JetpackItem::receiveUsagePacket);
        registerToServer(PlayerAugments.AugmentInstallTriggerPacket.PACKET_ID, getAutoCodec(PlayerAugments.AugmentInstallTriggerPacket.class), PlayerAugments::receiveInstallTrigger);
        registerToServer(PlayerAugments.LoadPlayerAugmentsToMachinePacket.PACKET_ID, getAutoCodec(PlayerAugments.LoadPlayerAugmentsToMachinePacket.class), PlayerAugments::receivePlayerLoadMachine);
        registerToServer(PlayerAugments.OpenAugmentScreenPacket.PACKET_ID, getAutoCodec(PlayerAugments.OpenAugmentScreenPacket.class), PlayerAugments::receiveOpenAugmentScreen);
        registerToServer(PlayerAugments.AugmentPlayerTogglePacket.PACKET_ID, getAutoCodec(PlayerAugments.AugmentPlayerTogglePacket.class), PlayerAugments::receiveToggleAugment);
        registerToServer(ShrinkerBlockEntity.ShrinkerPlayerUsePacket.PACKET_ID, getAutoCodec(ShrinkerBlockEntity.ShrinkerPlayerUsePacket.class), ShrinkerBlockEntity::onPlayerUse);
        
        
        registerToClient(MessagePayload.GENERIC_PACKET_ID, MessagePayload.PACKET_CODEC, NetworkManager::receiveMessage);
        registerToClient(ItemPipeInterfaceEntity.RenderStackData.PIPE_ITEMS_ID, getAutoCodec(ItemPipeInterfaceEntity.RenderStackData.class), ItemPipeInterfaceEntity::receiveVisualItemsPacket);
        registerToClient(EnchantmentCatalystBlockEntity.CatalystSyncPacket.PACKET_ID, getAutoCodec(EnchantmentCatalystBlockEntity.CatalystSyncPacket.class), EnchantmentCatalystBlockEntity::receiveUpdatePacket);
        registerToClient(SpawnerControllerBlockEntity.SpawnerSyncPacket.PACKET_ID, getAutoCodec(SpawnerControllerBlockEntity.SpawnerSyncPacket.class), SpawnerControllerBlockEntity::receiveUpdatePacket);
        registerToClient(RedstoneAddonBlockEntity.RedstoneAddonClientUpdate.PACKET_ID, getAutoCodec(RedstoneAddonBlockEntity.RedstoneAddonClientUpdate.class), RedstoneAddonBlockEntity::receiveOnClient);
        registerToClient(AcceleratorControllerBlockEntity.ParticleRenderTrail.PACKET_ID, getAutoCodec(AcceleratorControllerBlockEntity.ParticleRenderTrail.class), AcceleratorControllerBlockEntity::receiveTrail);
        registerToClient(AcceleratorControllerBlockEntity.LastEventPacket.PACKET_ID, getAutoCodec(AcceleratorControllerBlockEntity.LastEventPacket.class), AcceleratorControllerBlockEntity::receiveEvent);
    }
    
    public static void receiveMessage(MessagePayload message, Level world, RegistryAccess registryAccess) {
        var receivedBuf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(message.message), registryAccess);
        var receiverEntity = world.getBlockEntity(message.pos);
        var receiverType = registryAccess.registryOrThrow(Registries.BLOCK_ENTITY_TYPE).get(message.targetEntityType);
        if (receiverEntity != null && receiverType != null && receiverType.equals(receiverEntity.getType())) {
            decodeFields(receiverEntity, message.syncType, receivedBuf, world);
            if (receiverEntity instanceof NetworkedEventHandler networkedBlock) {
                networkedBlock.onNetworkUpdated();
            }
        } else {
            Oritech.LOGGER.debug("Unable to start decoding for block entity type {} at {}. Target Mismatch!", receiverType, message.pos);
        }
    }
    
    // returns the number of encoded fields
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int encodeFields(Object target, SyncType type, ByteBuf byteBuf, @Nullable Level world) {
        
        var fields = getCachedFields(target, type);
        
        var encodedCount = 0;
        for (var field : fields) {
            try {
                if (UpdatableField.class.isAssignableFrom(field.getType())) {
                    var fieldInstance = ((UpdatableField) field.get(target));
                    var deltaOnly = fieldInstance.useDeltaOnly(type);
                    var dataToSend = deltaOnly ? fieldInstance.getDeltaData() : fieldInstance;
                    var codec = deltaOnly ? fieldInstance.getDeltaCodec() : fieldInstance.getFullCodec();
                    if (codec instanceof WorldPacketCodec worldPacketCodec) {
                        worldPacketCodec.encode(byteBuf, dataToSend, world);
                    } else {
                        codec.encode(byteBuf, dataToSend);
                    }
                } else {
                    var codec = getAutoCodec(field);
                    var value = field.get(target);
                    if (codec instanceof WorldPacketCodec worldPacketCodec) {
                        worldPacketCodec.encode(byteBuf, value, world);
                    } else {
                        codec.encode(byteBuf, value);
                    }
                }
                
                encodedCount++;
                
            } catch (Exception ex) {
                Oritech.LOGGER.warn("failed to encode field: {}", field.getName(), ex);
            }
        }
        
        return encodedCount;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void decodeFields(Object target, SyncType type, ByteBuf byteBuf, Level world) {
        
        var fields = getCachedFields(target, type);
        
        for (var field : fields) {
            try {
                // fields that implement UpdatableField either get a delta or full update. Otherwise, we just set the full value
                if (UpdatableField.class.isAssignableFrom(field.getType())) {
                    var fieldInstance = ((UpdatableField) field.get(target));
                    var deltaOnly = fieldInstance.useDeltaOnly(type);
                    var codec = deltaOnly ? fieldInstance.getDeltaCodec() : fieldInstance.getFullCodec();
                    Object value;
                    if (codec instanceof WorldPacketCodec worldPacketCodec) {
                        value = worldPacketCodec.decode(byteBuf, world);
                    } else {
                        value = codec.decode(byteBuf);
                    }
                    if (deltaOnly) {
                        fieldInstance.handleDeltaUpdate(value);
                    } else {
                        fieldInstance.handleFullUpdate(value);
                    }
                } else {
                    var codec = getAutoCodec(field);
                    Object value;
                    if (codec instanceof WorldPacketCodec worldPacketCodec) {
                        value = worldPacketCodec.decode(byteBuf, world);
                    } else {
                        value = codec.decode(byteBuf);
                    }
                    field.set(target, value);
                }
                
            } catch (Exception ex) {
                Oritech.LOGGER.warn("failed to decode field: {}", field.getName(), ex);
            }
        }
    }
    
    private static @NotNull List<Field> getCachedFields(Object target, SyncType type) {
        var key = target.getClass().hashCode() + type.hashCode();
        return CACHED_FIELDS.computeIfAbsent(key, elem -> getSyncFields(target, type));
    }
    
    private static @NotNull List<Field> getSyncFields(Object target, SyncType type) {
        var fields = new ArrayList<>(Arrays.asList(target.getClass().getDeclaredFields()));
        var superClass = target.getClass().getSuperclass();
        while (superClass != null) {
            fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
            superClass = superClass.getSuperclass();
        }
        
        var filteredFields = new ArrayList<Field>();
        fields.stream().filter(field -> hasSyncType(field.getAnnotation(SyncField.class), type)).forEachOrdered(field -> {
            field.setAccessible(true);
            filteredFields.add(field);
        });
        
        if (target instanceof AdditionalNetworkingProvider additionalNetworkingProvider) {
            var addedFields = additionalNetworkingProvider.additionalSyncedFields(type);
            addedFields.forEach(field -> {
                field.setAccessible(true);
                filteredFields.add(field);
            });
        }
        
        return filteredFields;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static StreamCodec getAutoCodec(Class<?> type) {
        
        // try to create codec for records
        if (!AUTO_CODECS.containsKey(type)) {
            if (type.isRecord()) {
                Oritech.LOGGER.debug("creating reflective codec for: " + type);
                var computedCodec = ReflectiveCodecBuilder.create((Class<? extends Record>) type);
                AUTO_CODECS.put(type, computedCodec);
                return computedCodec;
            } else if (type.isEnum()) {
                Oritech.LOGGER.debug("creating reflective enum codec for: " + type);
                var computedCodec = ReflectiveCodecBuilder.createForEnum((Class<? extends Enum>) type);
                AUTO_CODECS.put(type, computedCodec);
                return computedCodec;
            }
        }
        
        if (!AUTO_CODECS.containsKey(type)) {
            Oritech.LOGGER.error("No codec defined for: {}", type);
        }
        
        return AUTO_CODECS.get(type);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static StreamCodec getAutoCodec(Field field) {
        var listType = getListType(field.getGenericType());
        if (listType.isPresent()) {
            var listTypeCodec = getAutoCodec((Class<?>) listType.get());
            return listTypeCodec.apply(ByteBufCodecs.list());
        }
        var setType = getSetType(field.getGenericType());
        if (setType.isPresent()) {
            var setTypeCodec = getAutoCodec((Class<?>) setType.get());
            return setTypeCodec.apply(toSet());
        }
        var mapType = getMapType(field.getGenericType());
        if (mapType.isPresent()) {
            var keyCodec = getAutoCodec((Class<?>) mapType.get().getA());
            var valueCodec = getAutoCodec((Class<?>) mapType.get().getB());
            
            if (keyCodec == null)
                Oritech.LOGGER.error("Unable to get codec for map key type: {}", field.getType());
            if (valueCodec == null)
                Oritech.LOGGER.error("Unable to get codec for map value type: {}", field.getType());
            
            return ByteBufCodecs.map(HashMap::new, keyCodec, valueCodec);
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
    
    // Method for checking if a given type is a Set and for retrieving its type parameter
    public static Optional<Type> getSetType(Type type) {
        if (type instanceof ParameterizedType pType) {
            var rawType = (Class<?>) pType.getRawType();
            if (rawType instanceof Class && Set.class.isAssignableFrom(rawType)) {
                return Optional.of(pType.getActualTypeArguments()[0]);
            }
        }
        return Optional.empty();
    }
    
    // Method for checking if a given type is a Map and for retrieving its type parameters
    public static Optional<Tuple<Type, Type>> getMapType(Type type) {
        if (type instanceof ParameterizedType pType) {
            var rawType = (Class<?>) pType.getRawType();
            if (rawType instanceof Class && Map.class.isAssignableFrom(rawType)) {
                var typeArgs = pType.getActualTypeArguments();
                return Optional.of(new Tuple<>(typeArgs[0], typeArgs[1]));
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
    
    public record MessagePayload(BlockPos pos, ResourceLocation targetEntityType, SyncType syncType,
                                 byte[] message) implements CustomPacketPayload {
        @Override
        public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return GENERIC_PACKET_ID;
        }
        
        public static final CustomPacketPayload.Type<MessagePayload> GENERIC_PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("generic"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, MessagePayload> PACKET_CODEC = new StreamCodec<>() {
            @Override
            public MessagePayload decode(RegistryFriendlyByteBuf buf) {
                return new MessagePayload(BlockPos.STREAM_CODEC.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf), SyncType.PACKET_CODEC.decode(buf), ByteBufCodecs.BYTE_ARRAY.decode(buf));
            }
            
            @Override
            public void encode(RegistryFriendlyByteBuf buf, MessagePayload value) {
                BlockPos.STREAM_CODEC.encode(buf, value.pos);
                ResourceLocation.STREAM_CODEC.encode(buf, value.targetEntityType);
                SyncType.PACKET_CODEC.encode(buf, value.syncType);
                ByteBufCodecs.BYTE_ARRAY.encode(buf, value.message);
            }
        };
    }
    
    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, Set<V>> toSet() {
        return (codec) -> ByteBufCodecs.collection(HashSet::new, codec);
    }
    
    // transmits only the block type, with the default block state. Custom properties are not sent.
    public static StreamCodec<RegistryFriendlyByteBuf, BlockState> SIMPLE_BLOCK_STATE_PACKET_CODEC = new StreamCodec<>() {
        @Override
        public BlockState decode(RegistryFriendlyByteBuf buf) {
            return BuiltInRegistries.BLOCK.get(ResourceLocation.STREAM_CODEC.decode(buf)).defaultBlockState();
        }
        
        @Override
        public void encode(RegistryFriendlyByteBuf buf, BlockState value) {
            ResourceLocation.STREAM_CODEC.encode(buf, BuiltInRegistries.BLOCK.getKey(value.getBlock()));
        }
    };
    
    public static StreamCodec<RegistryFriendlyByteBuf, Vector2i> VEC2I_PACKED_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, Vector2i::x,
      ByteBufCodecs.INT, Vector2i::y,
      Vector2i::new
    );
    
    @SuppressWarnings("unchecked")
    public static <K, V> StreamCodec<RegistryFriendlyByteBuf, HashMap<K, V>> createMapCodec(Class<K> keyType, Class<V> valueType) {
        return ByteBufCodecs.map(HashMap::new, getAutoCodec(keyType), getAutoCodec(valueType));
    }
    
    public static StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3D_PACKET_CODEC = new StreamCodec<>() {
        @Override
        public Vec3 decode(RegistryFriendlyByteBuf buf) {
            var x = buf.readDouble();
            var y = buf.readDouble();
            var z = buf.readDouble();
            return new Vec3(x, y, z);
        }
        
        @Override
        public void encode(RegistryFriendlyByteBuf buf, Vec3 value) {
            buf.writeDouble(value.x);
            buf.writeDouble(value.y);
            buf.writeDouble(value.z);
        }
    };
    
}
