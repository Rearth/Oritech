package rearth.oritech.fabric;

import com.google.auto.service.AutoService;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.util.TriConsumer;
import rearth.oritech.Oritech;
import rearth.oritech.OritechPlatform;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.util.fabric.FakeMachinePlayerImpl;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

@AutoService(OritechPlatform.class)
public class OritechPlatformFabric implements OritechPlatform {

    // Network
    public static final Queue<Runnable> PENDING_S2C_INITS = new ArrayDeque<>();
    
    @Override
    public void sendBlockHandle(BlockEntity blockEntity, CustomPacketPayload message) {
        for (var player : PlayerLookup.tracking(blockEntity)) {
            ServerPlayNetworking.send(player, message);
        }
    }

    @Override
    public void sendPlayerHandle(CustomPacketPayload message, ServerPlayer player) {
        ServerPlayNetworking.send(player, message);
    }

    @Override
    public void sendToServer(CustomPacketPayload message) {
        ClientPlayNetworking.send(message);
    }

    @Override
    public <T extends CustomPacketPayload> void registerToClient(
        CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec,
        TriConsumer<T, Level, RegistryAccess> consumer
    ) {
        PayloadTypeRegistry.playS2C().register(id, packetCodec);

        PENDING_S2C_INITS.add(() -> {
                ClientPlayNetworking.registerGlobalReceiver(id, (message, context) -> {
                    consumer.accept(message, context.player().clientLevel, context.client().level.registryAccess());
                });
            }
        );
    }

    @Override
    public <T extends CustomPacketPayload> void registerToServer(
        CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec,
        TriConsumer<T, Player, RegistryAccess> consumer
    ) {
        PayloadTypeRegistry.playC2S().register(id, packetCodec);
        ServerPlayNetworking.registerGlobalReceiver(id, (message, context) -> {
            consumer.accept(message, context.player(), context.player().serverLevel().registryAccess());
        });
    }
    
    // Attachment

    private static final Map<ResourceLocation, AttachmentType<?>> REGISTERED_TYPES = new HashMap<>();

    @Override
    public <T> void register(Attachment<T> attachment) {
        var created = AttachmentRegistry.<T>builder()
            .copyOnDeath()
            .initializer(attachment.initializer())
            .persistent(attachment.persistenceCodec())
            .syncWith(attachment.networkCodec(), AttachmentSyncPredicate.targetOnly())
            .buildAndRegister(attachment.identifier());

        REGISTERED_TYPES.put(attachment.identifier(), created);
    }

    @Override
    public <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) {
        var type = REGISTERED_TYPES.get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Querying attachment that has not been registered: {}", attachment.identifier());
            return false;
        }
        return entity.hasAttached(type);
    }

    @Override
    public <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) {
        AttachmentType<T> type = (AttachmentType<T>) REGISTERED_TYPES.get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Getting attachment that has not been registered: {}", attachment.identifier());
            return null;
        }
        return entity.getAttachedOrCreate(type);
    }

    @Override
    public <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) {
        AttachmentType<T> type = (AttachmentType<T>) REGISTERED_TYPES.get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Setting attachment that has not been registered: {}", attachment.identifier());
            return;
        }
        entity.setAttached(type, value);
    }

    @Override
    public <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) {
        var type = REGISTERED_TYPES.get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Removing attachment that has not been registered: {}", attachment.identifier());
            return;
        }
        entity.removeAttached(type);
    }

    // FakeMachinePlayer
    @Override
    public ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        return FakeMachinePlayerImpl.create(world, profile, inventory);
    }
    
    @Override
    public void resetCapabilities(ServerLevel world, BlockPos pos) {
        // nothing to do on fabric
    }
}
