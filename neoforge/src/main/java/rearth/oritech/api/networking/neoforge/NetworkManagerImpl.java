package rearth.oritech.api.networking.neoforge;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;

public class NetworkManagerImpl {
    
    public static final Queue<Consumer<PayloadRegistrar>> PENDING_S2C_INITS = new ArrayDeque<>();
    public static final Queue<Consumer<PayloadRegistrar>> PENDING_C2S_INITS = new ArrayDeque<>();
    
    public static void sendBlockHandle(BlockEntity blockEntity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) blockEntity.getLevel(), new ChunkPos(blockEntity.getBlockPos()), payload);
    }
    
    public static void sendPlayerHandle(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }
    
    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
    
    public static <T extends CustomPacketPayload> void registerToClient(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Level, RegistryAccess> consumer) {
        PENDING_S2C_INITS.add(payloadRegistrar -> {
            payloadRegistrar.playToClient(id,packetCodec, (payload, context) -> consumer.accept(payload, context.player().level(), context.player().registryAccess()));
        });
    }
    
    public static <T extends CustomPacketPayload> void registerToServer(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Player, RegistryAccess> consumer) {
        PENDING_C2S_INITS.add(payloadRegistrar -> {
            payloadRegistrar.playToServer(id,packetCodec, (payload, context) -> consumer.accept(payload, context.player(), context.player().registryAccess()));
        });
    }
    
}
