package rearth.oritech.api.networking.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayDeque;
import java.util.Queue;

public class NetworkManagerImpl {
    
    public static final Queue<Runnable> PENDING_S2C_INITS = new ArrayDeque<>();
    
    public static void sendBlockHandle(BlockEntity blockEntity, CustomPacketPayload payload) {
        for (var player : PlayerLookup.tracking(blockEntity)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
    
    public static void sendPlayerHandle(CustomPacketPayload payload, ServerPlayer player) {
        ServerPlayNetworking.send(player, payload);
    }
    
    public static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
    
    public static <T extends CustomPacketPayload> void registerToClient(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Level, RegistryAccess> consumer) {
        PayloadTypeRegistry.playS2C().register(id, packetCodec);
        
        PENDING_S2C_INITS.add(() -> {
              ClientPlayNetworking.registerGlobalReceiver(id, (message, context) -> {
                  consumer.accept(message, context.player().clientLevel, context.client().level.registryAccess());
              });
          }
        );
    }
    
    public static <T extends CustomPacketPayload> void registerToServer(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Player, RegistryAccess> consumer) {
        PayloadTypeRegistry.playC2S().register(id, packetCodec);
        ServerPlayNetworking.registerGlobalReceiver(id, (message, context) -> {
            consumer.accept(message, context.player(), context.player().serverLevel().registryAccess());
        });
    }
    
}
