package rearth.oritech.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import rearth.oritech.client.init.ParticleContent.EffectType;
import rearth.oritech.client.init.ParticleContent.Payload;
import rearth.oritech.client.init.ParticleContent.ParticleBatchPayload;

import java.util.concurrent.CompletableFuture;

/** Client-only particle creation, kept separate from payload codecs used on dedicated servers. */
public final class ClientParticleEffects {
    public static void addParticle(Level level, ParticleOptions particle, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        if (level.isClientSide() && OritechClientConfig.spawnParticle(level.getRandom())) {
            level.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    public static void handleParticleBatch(ParticleBatchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var client = Minecraft.getInstance();
            if (client.level == null || client.getConnection() == null) return;
            var packet = payload.particles();
            var mode = OritechClientConfig.visualEffects.get();
            if (mode == OritechClientConfig.VisualEffects.NONE) return;
            if (mode == OritechClientConfig.VisualEffects.REDUCED) {
                // Count zero means one particle with explicit velocity, not an empty batch.
                if (packet.getCount() == 0) {
                    if (!OritechClientConfig.spawnParticle(client.level.getRandom())) return;
                } else {
                    int count = OritechClientConfig.particleCount(packet.getCount(), client.level.getRandom());
                    if (count == 0) return;
                    packet = new ClientboundLevelParticlesPacket(packet.getParticle(), packet.isOverrideLimiter(),
                            packet.alwaysShow(), packet.getX(), packet.getY(), packet.getZ(), packet.getXDist(),
                            packet.getYDist(), packet.getZDist(), packet.getMaxSpeed(), count);
                }
            }
            client.getConnection().handleParticleEvent(packet);
        });
    }

    public static void handleOnClient(Payload payload, Level level, RegistryAccess access) {
        if (OritechClientConfig.visualEffects.get() == OritechClientConfig.VisualEffects.NONE) return;
        var type = EffectType.values()[payload.effectId()];
        switch (type) {
            case HIGHLIGHT_BLOCK -> spawnCubeOutline(ParticleTypes.ELECTRIC_SPARK, payload.pos(), 1, 120, 6);
            case DEBUG_BLOCK -> spawnCubeOutline(ParticleTypes.ELECTRIC_SPARK, payload.pos(), 1, 120, 2);
            case ACCELERATING -> spawnCubeOutline(ParticleTypes.SCULK_CHARGE_POP, payload.pos(), 1, 5, 3);
            case WEED_KILLER -> {
                var dist = (int) payload.data2().distanceTo(payload.data1());
                spawnLine(ParticleTypes.LANDING_HONEY, level, payload.data1(), payload.data2(), dist * 4 + level.getRandom().nextInt(3), 0.2f);
            }
            case WANDERING_SOUL -> {
                var velocity = payload.data1().scale((1f / payload.extraInt()) * 1.5f);
                spawnWithVelocityAndMaxAge(ParticleTypes.SCULK_SOUL, payload.pos(), velocity, payload.extraInt());
            }
            case LASER_BOOM -> {
                var count = Math.min((int) (payload.pos().distanceTo(payload.data1()) * 0.6f + 1), 12);
                spawnLineStaggered(ParticleTypes.SONIC_BOOM, level, payload.pos(), payload.data1(), count, 20);
            }
            case CATALYST_CONNECTION ->
                    spawnEnchantParticles(level, payload.data2(), payload.data1().add(0, 0.3f, 0), 0.3f);
            case BLACK_HOLE_EMISSION -> {
                var dist = (int) payload.data1().distanceTo(payload.pos());
                spawnLine(ParticleTypes.SCULK_CHARGE_POP, level, payload.pos(), payload.data1(), dist + level.getRandom().nextInt(3), 0.2f);
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
            if (!OritechClientConfig.spawnParticle(mc.level.getRandom())) {
                start = start.add(step);
                continue;
            }
            var p = mc.particleEngine.createParticle(particle, start.x, start.y, start.z, 0, 0, 0);
            if (p != null) p.setLifetime(maxAge);
            start = start.add(step);
        }
    }

    private static void spawnWithVelocityAndMaxAge(ParticleOptions particle, Vec3 pos, Vec3 velocity, int maxAge) {
        if (!OritechClientConfig.spawnParticle(Minecraft.getInstance().level.getRandom())) return;
        var p = Minecraft.getInstance().particleEngine.createParticle(particle, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        if (p != null) p.setLifetime(maxAge);
    }

    private static void spawnLine(ParticleOptions particle, Level level, Vec3 start, Vec3 end, int count, float spread) {
        Vec3 diff = end.subtract(start);
        for (int i = 0; i < count; i++) {
            double t = count > 1 ? (double) i / (count - 1) : 0;
            Vec3 pos = start.add(diff.scale(t));
            addParticle(level, particle,
                    pos.x + (level.getRandom().nextDouble() - 0.5) * 2 * spread,
                    pos.y + (level.getRandom().nextDouble() - 0.5) * 2 * spread,
                    pos.z + (level.getRandom().nextDouble() - 0.5) * 2 * spread,
                    0, 0, 0);
        }
    }

    private static void spawnEnchantParticles(Level level, Vec3 source, Vec3 dest, float spread) {
        Vec3 diff = dest.subtract(source);
        addParticle(level, ParticleTypes.ENCHANT,
                source.x + (level.getRandom().nextDouble() - 0.3) * 2 * spread,
                source.y + (level.getRandom().nextDouble() - 0.3) * 2 * spread,
                source.z + (level.getRandom().nextDouble() - 0.3) * 2 * spread,
                diff.x, diff.y, diff.z);
    }

    private static void spawnLineStaggered(ParticleOptions particle, Level level, Vec3 start, Vec3 end, float count, long pauseMillis) {
        var step = end.subtract(start).scale(1f / count);
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < count; i++) {
                var pos = start.add(step.scale(i));
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().level == level)
                        addParticle(level, particle, pos.x(), pos.y(), pos.z(), 0, 0, 0);
                });
                try {
                    Thread.sleep(pauseMillis);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

}
