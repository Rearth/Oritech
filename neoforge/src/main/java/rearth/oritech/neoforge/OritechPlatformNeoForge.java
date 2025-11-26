package rearth.oritech.neoforge;

import com.google.auto.service.AutoService;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.logging.log4j.util.TriConsumer;
import rearth.oritech.Oritech;
import rearth.oritech.OritechPlatform;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.util.neoforge.FakeMachinePlayerImpl;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;

@AutoService(OritechPlatform.class)
public class OritechPlatformNeoForge implements OritechPlatform {
    
    // Network
    public static final Queue<Consumer<PayloadRegistrar>> PENDING_S2C_INITS = new ArrayDeque<>();
    public static final Queue<Consumer<PayloadRegistrar>> PENDING_C2S_INITS = new ArrayDeque<>();
    
    @Override
    public void sendBlockHandle(BlockEntity blockEntity, CustomPacketPayload message) {
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) blockEntity.getLevel(), new ChunkPos(blockEntity.getBlockPos()), message);
    }
    
    @Override
    public void sendPlayerHandle(CustomPacketPayload message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }
    
    @Override
    public void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }
    
    @Override
    public <T extends CustomPacketPayload> void registerToClient(
      CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec,
      TriConsumer<T, Level, RegistryAccess> consumer
    ) {
        PENDING_S2C_INITS.add(payloadRegistrar -> {
            payloadRegistrar.playToClient(id, packetCodec, (payload, context) -> consumer.accept(payload, context.player().level(), context.player().registryAccess()));
        });
    }
    
    @Override
    public <T extends CustomPacketPayload> void registerToServer(
      CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec,
      TriConsumer<T, Player, RegistryAccess> consumer
    ) {
        PENDING_C2S_INITS.add(payloadRegistrar -> {
            payloadRegistrar.playToServer(id, packetCodec, (payload, context) -> consumer.accept(payload, context.player(), context.player().registryAccess()));
        });
    }
    
    // Attachment
    
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Oritech.MOD_ID);
    
    @Override
    public <T> void register(Attachment<T> attachment) {
        ATTACHMENT_TYPES.register(attachment.identifier().getPath(), () ->
                   AttachmentType
                     .builder(attachment.initializer())
                     .serialize(attachment.persistenceCodec())
                     .sync(attachment.networkCodec())
                     .copyOnDeath()
                     .build());
    }
    
    @Override
    public <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) {
        var type = ATTACHMENT_TYPES.getRegistry().get().get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Querying attachment that has not been registered: {}", attachment.identifier());
            return false;
        }
        return entity.hasData(type);
    }
    
    @Override
    public <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) {
        var type = (AttachmentType<T>) ATTACHMENT_TYPES.getRegistry().get().get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Getting attachment that has not been registered: {}", attachment.identifier());
            return null;
        }
        return entity.getData(type);
    }
    
    @Override
    public <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) {
        var type = (AttachmentType<T>) ATTACHMENT_TYPES.getRegistry().get().get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Setting attachment that has not been registered: {}", attachment.identifier());
            return;
        }
        entity.setData(type, value);
    }
    
    @Override
    public <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) {
        var type = ATTACHMENT_TYPES.getRegistry().get().get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Removing attachment that has not been registered: {}", attachment.identifier());
            return;
        }
        entity.removeData(type);
    }
    
    // FakeMachinePlayer
    
    @Override
    public ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        return FakeMachinePlayerImpl.create(world, profile, inventory);
    }
    
    @Override
    public void resetCapabilities(ServerLevel world, BlockPos pos) {
        world.invalidateCapabilities(pos);
    }
}
