package rearth.oritech.spaceage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.client.RocketClientController;
import rearth.oritech.spaceage.simulation.ActiveRocketData;

import java.util.UUID;

public final class RocketNetworking {

    private RocketNetworking() {
    }

    @SuppressWarnings("unchecked")
    public static void register(PayloadRegistrar registrar) {
        NetworkManager.registerCodec(ByteBufCodecs.fromCodecWithRegistries(ActiveRocketData.CODEC), ActiveRocketData.class);

        registrar.playToClient(SyncRocketPayload.TYPE, NetworkManager.getAutoCodec(SyncRocketPayload.class),
                RocketNetworking::receiveRocket);
        registrar.playToClient(UnloadRocketPayload.TYPE, NetworkManager.getAutoCodec(UnloadRocketPayload.class),
                RocketNetworking::unloadRocket);
        registrar.playToClient(CollisionPayload.TYPE, NetworkManager.getAutoCodec(CollisionPayload.class),
                RocketNetworking::receiveCollision);
        registrar.playToClient(ClearRocketsPayload.TYPE, NetworkManager.getAutoCodec(ClearRocketsPayload.class),
                RocketNetworking::clearRockets);
    }

    public static void sendRocket(ServerPlayer player, ActiveRocketData rocket) {
        PacketDistributor.sendToPlayer(player, new SyncRocketPayload(rocket));
    }

    public static void unloadRocket(ServerPlayer player, UUID rocketId) {
        PacketDistributor.sendToPlayer(player, new UnloadRocketPayload(rocketId));
    }

    public static void sendCollision(ServerPlayer player, UUID rocketId, BlockPos position,
                                     double speedMetersPerSecond, float explosionStrength, boolean nuclearExplosion) {
        PacketDistributor.sendToPlayer(player,
                new CollisionPayload(rocketId, position, speedMetersPerSecond, explosionStrength, nuclearExplosion));
    }

    public static void clearRockets(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, ClearRocketsPayload.INSTANCE);
    }

    private static void receiveRocket(SyncRocketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RocketClientController.receiveRocket(payload.rocket()));
    }

    private static void unloadRocket(UnloadRocketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            RocketClientController.removeRocket(payload.rocketId());
            OritechSpaceAge.LOGGER.debug("Unloaded orbiting rocket {} from the client", payload.rocketId());
        });
    }

    private static void receiveCollision(CollisionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            RocketClientController.removeRocket(payload.rocketId());
            OritechSpaceAge.LOGGER.debug("Received rocket collision {} at {}: speed={}m/s, strength={}, nuclear={}",
                    payload.rocketId(), payload.position(), payload.speedMetersPerSecond(),
                    payload.explosionStrength(), payload.nuclearExplosion());
        });
    }

    private static void clearRockets(ClearRocketsPayload payload, IPayloadContext context) {
        context.enqueueWork(RocketClientController::clearRockets);
    }

    public record SyncRocketPayload(ActiveRocketData rocket) implements CustomPacketPayload {

        private static final Type<SyncRocketPayload> TYPE = new Type<>(OritechSpaceAge.id("sync_rocket"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UnloadRocketPayload(UUID rocketId) implements CustomPacketPayload {

        private static final Type<UnloadRocketPayload> TYPE = new Type<>(OritechSpaceAge.id("unload_rocket"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CollisionPayload(UUID rocketId, BlockPos position, double speedMetersPerSecond,
                                   float explosionStrength, boolean nuclearExplosion) implements CustomPacketPayload {

        private static final Type<CollisionPayload> TYPE = new Type<>(OritechSpaceAge.id("rocket_collision"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClearRocketsPayload() implements CustomPacketPayload {

        private static final ClearRocketsPayload INSTANCE = new ClearRocketsPayload();
        private static final Type<ClearRocketsPayload> TYPE = new Type<>(OritechSpaceAge.id("clear_rockets"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
