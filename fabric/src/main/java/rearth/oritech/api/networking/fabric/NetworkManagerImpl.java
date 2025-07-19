package rearth.oritech.api.networking.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayDeque;
import java.util.Queue;

public class NetworkManagerImpl {
    
    public static final Queue<Runnable> PENDING_S2C_INITS = new ArrayDeque<>();
    
    public static void sendBlockHandle(BlockEntity blockEntity, CustomPayload payload) {
        for (var player : PlayerLookup.tracking(blockEntity)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
    
    public static void sendPlayerHandle(CustomPayload payload, ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, payload);
    }
    
    public static void sendToServer(CustomPayload payload) {
        ClientPlayNetworking.send(payload);
    }
    
    public static <T extends CustomPayload> void registerToClient(CustomPayload.Id<T> id, PacketCodec<RegistryByteBuf, T> packetCodec, TriConsumer<T, World, DynamicRegistryManager> consumer) {
        PayloadTypeRegistry.playS2C().register(id, packetCodec);
        
        PENDING_S2C_INITS.add(() -> {
              ClientPlayNetworking.registerGlobalReceiver(id, (message, context) -> {
                  consumer.accept(message, context.player().clientWorld, context.client().world.getRegistryManager());
              });
          }
        );
    }
    
    public static <T extends CustomPayload> void registerToServer(CustomPayload.Id<T> id, PacketCodec<RegistryByteBuf, T> packetCodec, TriConsumer<T, PlayerEntity, DynamicRegistryManager> consumer) {
        PayloadTypeRegistry.playC2S().register(id, packetCodec);
        ServerPlayNetworking.registerGlobalReceiver(id, (message, context) -> {
            consumer.accept(message, context.player(), context.player().getServerWorld().getRegistryManager());
        });
    }
    
}
