package rearth.oritech.spaceage.client.rocket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock.Type;
import rearth.oritech.spaceage.client.rocket.RocketThrusters.Nozzle;
import rearth.oritech.spaceage.client.rocket.ExhaustParticle.Kind;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketSimulationController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tick-driven particles stay behind the moving craft; only the nozzle flame follows it. */
public final class RocketEffects {

    private static final Map<UUID, CachedNozzles> NOZZLES = new HashMap<>();
    private static ClientLevel world;
    private static float shake;
    private static float previousShake;
    private static float impactShake;
    private static int particleBudget;

    private RocketEffects() {
    }

    public static List<Nozzle> nozzles(ActiveRocketData rocket) {

        var cached = NOZZLES.get(rocket.getRocketId());
        if (cached == null || cached.rocket() != rocket) {
            cached = new CachedNozzles(rocket, RocketThrusters.nozzles(rocket));
            NOZZLES.put(rocket.getRocketId(), cached);
        }
        return cached.nozzles();
    }

    public static void receive(ActiveRocketData rocket, List<UUID> engineSegments) {

        var enabled = rocket.getStaticSegments().values().stream().filter(segment -> engineSegments.contains(segment.segmentId()))
                .map(SegmentRef::of).toList();
        NOZZLES.put(rocket.getRocketId(), new CachedNozzles(rocket,
                RocketThrusters.nozzles(rocket).stream().filter(nozzle -> enabled.contains(nozzle.segment())).toList()));
    }

    public static void clear() {

        NOZZLES.clear();
        shake = 0;
        previousShake = 0;
        impactShake = 0;
    }

    public static void checkWorld() {

        var level = Minecraft.getInstance().level;
        if (world == level) return;
        RocketClientController.clearRockets();
        world = level;
    }

    public static void removed(ActiveRocketData rocket) {

        NOZZLES.remove(rocket.getRocketId());
        var level = Minecraft.getInstance().level;
        var flight = rocket.getFlight();
        if (level == null || flight == null || !flight.dimension().equals(level.dimension()) || flight.impactTick() < 0) return;
        if (Math.abs(level.getGameTime() - flight.impactTick()) <= 5)
            impact(Vec3.atBottomCenterOf(flight.impactPosition()), flight.impactSpeedMetersPerSecond() > 12 ? 2 : .3f);
    }

    public static void impact(Vec3 position, float strength) {

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || minecraft.player == null || !OritechClientConfig.renderBeams()) return;
        var distance = minecraft.gameRenderer.getMainCamera().position().distanceTo(position);
        impactShake = Math.max(impactShake, strength * attenuation(distance));
        if (distance > 160) return;
        // Network impact notifications can arrive outside the particle-emission tick.
        particleBudget = Math.max(particleBudget, 48);
        for (var i = 0; i < 40; i++) {
            var angle = i * Math.PI * 2 / 40;
            var outward = new Vec3(Math.cos(angle), .12 + level.getRandom().nextDouble() * .25, Math.sin(angle));
            particle(level, position.add(outward.scale(.5)), outward.scale(strength > 1 ? .7 : .35),
                    strength > 1 ? .8f : .45f, Kind.SMOKE);
            if (strength > 1 && i % 2 == 0) particle(level, position.add(0, .5, 0), outward.scale(1.2), .18f, Kind.EMBER);
        }
    }

    public static void tick(ClientTickEvent.Post event) {

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        checkWorld();
        if (level == null || minecraft.isPaused()) return;
        previousShake = shake;
        impactShake *= .83f;
        var targetShake = impactShake;
        particleBudget = 128;
        var time = level.getGameTime();
        var camera = minecraft.gameRenderer.getMainCamera().position();
        if (OritechClientConfig.renderBeams()) {
            for (var rocket : RocketClientController.getActiveRockets()) {
                var flight = rocket.getFlight();
                if (!RocketThrusters.visible(flight, time) || !flight.dimension().equals(level.dimension())) continue;
                var position = RocketSimulationController.getRocketPosition(flight, time);
                if (camera.distanceToSqr(position) > 256 * 256) continue;
                var crashing = RocketThrusters.crashing(flight, time);
                var engines = nozzles(rocket);
                var active = 0f;
                for (var nozzle : engines) {
                    if (!RocketThrusters.firing(nozzle, flight, time)) continue;
                    var ion = nozzle.type() == Type.ION;
                    active += nozzle.scale() * (ion ? .08f : 1);
                    var origin = position.add(nozzle.origin()).add(0, .5, 0);
                    if (camera.distanceToSqr(origin) > 160 * 160) continue;
                    var previous = RocketSimulationController.getRocketPosition(flight, time - 1).add(nozzle.origin()).add(0, .5, 0);
                    emit(level, previous, origin, nozzle);
                }
                targetShake = Math.max(targetShake, Math.min(1.2f, active * .22f) * attenuation(camera.distanceTo(position)));
                if (crashing && camera.distanceToSqr(position) < 160 * 160) {
                    // A rising, dirty wake distinguishes ballistic reentry from controlled exhaust below the engines.
                    for (var i = 0; i < 6; i++) {
                        var offset = new Vec3(level.getRandom().nextGaussian(), level.getRandom().nextDouble() * 2, level.getRandom().nextGaussian());
                        particle(level, position.add(offset), offset.scale(.12).add(0, .35, 0), .65f, Kind.SMOKE);
                        particle(level, position.add(offset), offset.scale(.2).add(0, .65, 0), .2f, Kind.EMBER);
                    }
                }
                if (active >= 1 && time % 18 == Math.floorMod(rocket.getRocketId().hashCode(), 18))
                    level.playLocalSound(position.x, position.y, position.z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                            Math.min(3, 1 + active * .2f), .55f, false);
            }
        }
        shake += (targetShake - shake) * .4f;
    }

    private static void emit(ClientLevel level, Vec3 previous, Vec3 origin, Nozzle nozzle) {

        var ion = nozzle.type() == Type.ION;
        var scale = nozzle.scale();
        var count = ion ? 2 : 8;
        for (var i = 0; i < count; i++) {
            var spread = new Vec3(level.getRandom().nextGaussian(), level.getRandom().nextGaussian(), level.getRandom().nextGaussian()).scale(.08 * scale);
            var start = previous.lerp(origin, (i + .5) / count).add(nozzle.direction().scale(scale * (ion ? 1.8 : 2.8)));
            var velocity = nozzle.direction().scale(.25 + level.getRandom().nextDouble() * .2).add(spread);
            if (ion) {
                particle(level, start, velocity, .1f * scale, Kind.ION);
            } else {
                particle(level, start.add(spread.scale(2)), velocity.scale(.6), .55f * scale, Kind.SMOKE);
                if (i % 2 == 0) particle(level, start, velocity.scale(1.8), .1f * scale, Kind.EMBER);
            }
        }
        // Loaded terrain only; exhaust hitting a pad or hillside pushes dust out along its surface.
        var end = origin.add(nozzle.direction().scale(7 * scale));
        if (!level.hasChunkAt(BlockPos.containing(end))) return;
        var hit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        if (hit.getType() == HitResult.Type.MISS) return;
        for (var i = 0; i < (ion ? 1 : 10); i++) {
            var angle = level.getRandom().nextDouble() * Math.PI * 2;
            var radial = new Vec3(Math.cos(angle), .1, Math.sin(angle));
            particle(level, hit.getLocation().add(radial.scale(.3)).add(0, .15, 0), radial.scale((ion ? .15 : .5) * scale),
                    (ion ? .4f : .65f) * scale, Kind.SMOKE);
        }
    }

    private static void particle(ClientLevel level, Vec3 position, Vec3 velocity, float size, Kind kind) {

        var minecraft = Minecraft.getInstance();
        if (particleBudget <= 0 || !OritechClientConfig.spawnParticle(level.getRandom())) return;
        var setting = minecraft.options.particles().get();
        if (setting == ParticleStatus.MINIMAL || setting == ParticleStatus.DECREASED && level.getRandom().nextBoolean()) return;
        particleBudget--;
        minecraft.particleEngine.add(new ExhaustParticle(level, position, velocity, size, kind));
    }

    public static float attenuation(double distance) {

        var falloff = Math.max(0, 1 - distance / 64);
        return (float) (falloff * falloff);
    }

    public static void camera(ViewportEvent.ComputeCameraAngles event) {

        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !OritechClientConfig.rocketCameraShake.get() || !OritechClientConfig.renderBeams()) return;
        var partial = event.getPartialTick();
        var time = minecraft.level.getGameTime() + partial;
        var amplitude = (previousShake + (shake - previousShake) * partial) * (OritechClientConfig.renderFullEffects() ? 1 : .4);
        event.setPitch(event.getPitch() + (float) (Math.sin(time * 2.3) * amplitude));
        event.setYaw(event.getYaw() + (float) (Math.sin(time * 1.7 + 1) * amplitude * .45));
        event.setRoll(event.getRoll() + (float) (Math.sin(time * 2.9 + 2) * amplitude * .65));
    }

    private record CachedNozzles(ActiveRocketData rocket, List<Nozzle> nozzles) {
    }
}
