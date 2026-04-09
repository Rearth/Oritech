package rearth.oritech.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;

import java.util.concurrent.CompletableFuture;

public class ParticleContent {
    
    public enum EffectType {
        HIGHLIGHT_BLOCK, WEED_KILLER, DEBUG_BLOCK, WANDERING_SOUL,
        LASER_BOOM, CATALYST_CONNECTION, BLACK_HOLE_EMISSION, ACCELERATING
    }
    
    // public stuff
    
    public static void HighlightBlock(Level world, Vec3 pos) {
        sendParticle(world, new Payload(EffectType.HIGHLIGHT_BLOCK, pos, Vec3.ZERO, Vec3.ZERO, 0));
    }
    
    public static void DebugBlock(Level world, Vec3 pos) {
        sendParticle(world, new Payload(EffectType.DEBUG_BLOCK, pos, Vec3.ZERO, Vec3.ZERO, 0));
    }
    
    public static void Accelerating(Level world, Vec3 pos) {
        sendParticle(world, new Payload(EffectType.ACCELERATING, pos, Vec3.ZERO, Vec3.ZERO, 0));
    }
    
    public static void WeedKiller(Level world, Vec3 start, Vec3 end) {
        sendParticle(world, new Payload(EffectType.WEED_KILLER, start, start, end, 0));
    }
    
    public static void WanderingSoul(Level world, Vec3 pos, Vec3 offset, int duration) {
        sendParticle(world, new Payload(EffectType.WANDERING_SOUL, pos, offset, Vec3.ZERO, duration));
    }
    
    public static void LaserBoom(Level world, Vec3 start, Vec3 end) {
        sendParticle(world, new Payload(EffectType.LASER_BOOM, start, end, Vec3.ZERO, 0));
    }
    
    public static void CatalystConnection(Level world, Vec3 source, Vec3 dest) {
        sendParticle(world, new Payload(EffectType.CATALYST_CONNECTION, source, source, dest, 0));
    }
    
    public static void BlackHoleEmission(Level world, Vec3 origin, Vec3 target) {
        sendParticle(world, new Payload(EffectType.BLACK_HOLE_EMISSION, origin, target, Vec3.ZERO, 0));
    }
    
    private static void sendParticle(Level world, Payload payload) {
        if (world instanceof ServerLevel sl) {
            NetworkManager.sendNearby(sl, payload.pos, 64, payload);
        } else if (world.isClientSide) {
            handleOnClient(payload, world, null);
        }
    }
    
    // client handler
    
    public static void handleOnClient(Payload payload, Level world, RegistryAccess access) {
        var type = EffectType.values()[payload.effectId];
        switch (type) {
            case HIGHLIGHT_BLOCK -> spawnCubeOutline(ParticleTypes.ELECTRIC_SPARK, payload.pos, 1, 120, 6);
            case DEBUG_BLOCK -> spawnCubeOutline(ParticleTypes.ELECTRIC_SPARK, payload.pos, 1, 120, 2);
            case ACCELERATING -> spawnCubeOutline(ParticleTypes.SCULK_CHARGE_POP, payload.pos, 1, 5, 3);
            case WEED_KILLER -> {
                var dist = (int) payload.data2.distanceTo(payload.data1);
                spawnLine(ParticleTypes.DRAGON_BREATH, world, payload.data1, payload.data2, dist * 4 + world.random.nextInt(3), 0.2f);
            }
            case WANDERING_SOUL -> {
                var velocity = payload.data1.scale((1f / payload.extraInt) * 1.5f);
                spawnWithVelocityAndMaxAge(ParticleTypes.SCULK_SOUL, payload.pos, velocity, payload.extraInt);
            }
            case LASER_BOOM -> {
                var count = Math.min((int) (payload.pos.distanceTo(payload.data1) * 0.6f + 1), 12);
                spawnLineStaggered(ParticleTypes.SONIC_BOOM, world, payload.pos, payload.data1, count, 20);
            }
            case CATALYST_CONNECTION -> spawnEnchantParticles(world, payload.data2, payload.data1.add(0, 0.3f, 0), 0.3f);
            case BLACK_HOLE_EMISSION -> {
                var dist = (int) payload.data1.distanceTo(payload.pos);
                spawnLine(ParticleTypes.SCULK_CHARGE_POP, world, payload.pos, payload.data1, dist + world.random.nextInt(3), 0.2f);
            }
        }
    }
    
    // client utilities
    
    private static void spawnCubeOutline(ParticleOptions particle, Vec3 origin, float size, int duration, int segments) {
        spawnLineWithAge(particle, origin, origin.add(size, 0, 0), segments, duration);
        spawnLineWithAge(particle, origin.add(size, 0, 0), origin.add(size, 0, size), segments, duration);
        spawnLineWithAge(particle, origin, origin.add(0, 0, size), segments, duration);
        spawnLineWithAge(particle, origin.add(0, 0, size), origin.add(size, 0, size), segments, duration);
        
        origin = origin.add(0, size, 0);
        
        spawnLineWithAge(particle, origin, origin.add(size, 0, 0), segments, duration);
        spawnLineWithAge(particle, origin.add(size, 0, 0), origin.add(size, 0, size), segments, duration);
        spawnLineWithAge(particle, origin, origin.add(0, 0, size), segments, duration);
        spawnLineWithAge(particle, origin.add(0, 0, size), origin.add(size, 0, size), segments, duration);
        
        spawnLineWithAge(particle, origin, origin.add(0, -size, 0), segments, duration);
        spawnLineWithAge(particle, origin.add(size, 0, 0), origin.add(size, -size, 0), segments, duration);
        spawnLineWithAge(particle, origin.add(0, 0, size), origin.add(0, -size, size), segments, duration);
        spawnLineWithAge(particle, origin.add(size, 0, size), origin.add(size, -size, size), segments, duration);
    }
    
    private static void spawnLineWithAge(ParticleOptions particle, Vec3 start, Vec3 end, float count, int maxAge) {
        var mc = Minecraft.getInstance();
        Vec3 step = end.subtract(start).scale(1f / count);
        for (int i = 0; i < count; i++) {
            var p = mc.particleEngine.createParticle(particle, start.x, start.y, start.z, 0, 0, 0);
            if (p != null) p.setLifetime(maxAge);
            start = start.add(step);
        }
    }
    
    private static void spawnWithVelocityAndMaxAge(ParticleOptions particle, Vec3 pos, Vec3 velocity, int maxAge) {
        var p = Minecraft.getInstance().particleEngine.createParticle(particle, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        if (p != null) p.setLifetime(maxAge);
    }
    
    private static void spawnLine(ParticleOptions particle, Level world, Vec3 start, Vec3 end, int count, float spread) {
        Vec3 diff = end.subtract(start);
        for (int i = 0; i < count; i++) {
            double t = count > 1 ? (double) i / (count - 1) : 0;
            Vec3 pos = start.add(diff.scale(t));
            world.addParticle(particle,
                pos.x + (world.random.nextDouble() - 0.5) * 2 * spread,
                pos.y + (world.random.nextDouble() - 0.5) * 2 * spread,
                pos.z + (world.random.nextDouble() - 0.5) * 2 * spread,
                0, 0, 0);
        }
    }
    
    private static void spawnEnchantParticles(Level world, Vec3 source, Vec3 dest, float spread) {
        Vec3 diff = dest.subtract(source);
        world.addParticle(ParticleTypes.ENCHANT,
            source.x + (world.random.nextDouble() - 0.3) * 2 * spread,
            source.y + (world.random.nextDouble() - 0.3) * 2 * spread,
            source.z + (world.random.nextDouble() - 0.3) * 2 * spread,
            diff.x, diff.y, diff.z);
    }
    
    private static void spawnLineStaggered(ParticleOptions particle, Level world, Vec3 start, Vec3 end, float count, long pauseMillis) {
        var step = end.subtract(start).scale(1f / count);
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < count; i++) {
                var pos = start.add(step.scale(i));
                world.addParticle(particle, pos.x(), pos.y(), pos.z(), 0, 0, 0);
                try {
                    Thread.sleep(pauseMillis);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
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
        NetworkManager.registerToClient(Payload.PACKET_ID, Payload.PACKET_CODEC, ParticleContent::handleOnClient);
        Oritech.LOGGER.debug("Registering Oritech particles");
    }
    
}
