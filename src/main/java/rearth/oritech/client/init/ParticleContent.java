package rearth.oritech.client.init;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;

public class ParticleContent {

    public enum EffectType {
        HIGHLIGHT_BLOCK, WEED_KILLER, DEBUG_BLOCK, WANDERING_SOUL,
        LASER_BOOM, CATALYST_CONNECTION, BLACK_HOLE_EMISSION, ACCELERATING
    }

    // public stuff

    public static void HighlightBlock(Level level, Vec3 pos) {
        sendParticle(level, new Payload(EffectType.HIGHLIGHT_BLOCK, pos, Vec3.ZERO, Vec3.ZERO, 0));
    }

    public static void DebugBlock(Level level, Vec3 pos) {
        sendParticle(level, new Payload(EffectType.DEBUG_BLOCK, pos, Vec3.ZERO, Vec3.ZERO, 0));
    }

    public static void Accelerating(Level level, Vec3 pos) {
        sendParticle(level, new Payload(EffectType.ACCELERATING, pos, Vec3.ZERO, Vec3.ZERO, 0));
    }

    public static void WeedKiller(Level level, Vec3 start, Vec3 end) {
        sendParticle(level, new Payload(EffectType.WEED_KILLER, start, start, end, 0));
    }

    public static void WanderingSoul(Level level, Vec3 pos, Vec3 offset, int duration) {
        sendParticle(level, new Payload(EffectType.WANDERING_SOUL, pos, offset, Vec3.ZERO, duration));
    }

    public static void LaserBoom(Level level, Vec3 start, Vec3 end) {
        sendParticle(level, new Payload(EffectType.LASER_BOOM, start, end, Vec3.ZERO, 0));
    }

    public static void CatalystConnection(Level level, Vec3 source, Vec3 dest) {
        sendParticle(level, new Payload(EffectType.CATALYST_CONNECTION, source, source, dest, 0));
    }

    public static void BlackHoleEmission(Level level, Vec3 origin, Vec3 target) {
        sendParticle(level, new Payload(EffectType.BLACK_HOLE_EMISSION, origin, target, Vec3.ZERO, 0));
    }

    private static void sendParticle(Level level, Payload payload) {
        if (level instanceof ServerLevel sl) {
            double radius = 64;
            double radiusSq = radius * radius;
            for (var player : sl.players()) {
                if (player.distanceToSqr(payload.pos.x, payload.pos.y, payload.pos.z) <= radiusSq) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        } else if (level.isClientSide()) {
            handleOnClient(payload, level, null);
        }
    }

    /** Keep vanilla particle encoding and spawn semantics, but identify these as Oritech effects. */
    public static void sendParticles(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                                     int count, double xDist, double yDist, double zDist, double speed) {
        var payload = new ParticleBatchPayload(new ClientboundLevelParticlesPacket(particle, false, false,
                x, y, z, (float) xDist, (float) yDist, (float) zDist, (float) speed, count));
        var position = new Vec3(x, y, z);
        for (var player : level.players()) {
            // Match ServerLevel.sendParticles' normal delivery radius.
            if (player.blockPosition().closerToCenterThan(position, 32)) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public record ParticleBatchPayload(ClientboundLevelParticlesPacket particles) implements CustomPacketPayload {
        public static final Type<ParticleBatchPayload> PACKET_ID = new Type<>(Oritech.id("particle_batch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ParticleBatchPayload> PACKET_CODEC =
                ClientboundLevelParticlesPacket.STREAM_CODEC.map(ParticleBatchPayload::new, ParticleBatchPayload::particles);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }

    public static void addParticle(Level level, ParticleOptions particle, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        if (level.isClientSide()) ClientParticleEffects.addParticle(level, particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    public static void handleParticleBatch(ParticleBatchPayload payload, IPayloadContext context) {
        ClientParticleEffects.handleParticleBatch(payload, context);
    }

    public static void handleOnClient(Payload payload, Level level, RegistryAccess access) {
        if (level.isClientSide()) ClientParticleEffects.handleOnClient(payload, level, access);
    }

    // client handler

    public static void handleOnClient(Payload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleOnClient(payload, context.player().level(), context.player().registryAccess()));
    }

    // Network payload

    public record Payload(int effectId, Vec3 pos, Vec3 data1, Vec3 data2, int extraInt) implements CustomPacketPayload {
        public static final Type<Payload> PACKET_ID = new Type<>(Oritech.id("complex_particle"));

        Payload(EffectType type, Vec3 pos, Vec3 data1, Vec3 data2, int extraInt) {
            this(type.ordinal(), pos, data1, data2, extraInt);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> PACKET_CODEC = new StreamCodec<>() {
            @Override
            public Payload decode(RegistryFriendlyByteBuf buf) {
                return new Payload(
                        buf.readInt(),
                        NetworkManager.VEC3D_PACKET_CODEC.decode(buf),
                        NetworkManager.VEC3D_PACKET_CODEC.decode(buf),
                        NetworkManager.VEC3D_PACKET_CODEC.decode(buf),
                        buf.readInt()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, Payload value) {
                buf.writeInt(value.effectId);
                NetworkManager.VEC3D_PACKET_CODEC.encode(buf, value.pos);
                NetworkManager.VEC3D_PACKET_CODEC.encode(buf, value.data1);
                NetworkManager.VEC3D_PACKET_CODEC.encode(buf, value.data2);
                buf.writeInt(value.extraInt);
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }

    public static void registerParticles() {
        Oritech.LOGGER.debug("Oritech particles are registered via clientbound payload handlers");
    }

}
