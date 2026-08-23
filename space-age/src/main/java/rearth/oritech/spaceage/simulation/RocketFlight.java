package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Vector2i;

import java.util.Optional;

// stores where a rocket is going and when each phase happens so the server can persist the simulation and clients can
// reproduce the same movement without receiving a packet every tick
public record RocketFlight(
        boolean canReachOrbit,
        String failureReason,
        ResourceKey<Level> dimension,
        RocketPerformance performance,
        BlockPos launchPosition,
        BlockPos orbitPosition,
        BlockPos impactPosition,
        Vector2i targetOrbit,
        double requiredDeltaVMetersPerSecond,
        double impactSpeedMetersPerSecond,
        long orbitArrivalTick,
        long reentryTick,
        long impactTick,
        BlockPos takeoffCollisionPosition,
        long takeoffCollisionTick) {

    private static final Codec<Vector2i> ORBIT_POSITION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(Vector2i::x),
            Codec.INT.fieldOf("y").forGetter(Vector2i::y)
    ).apply(instance, Vector2i::new));

    public static final Codec<RocketFlight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("can_reach_orbit").forGetter(RocketFlight::canReachOrbit),
            Codec.STRING.optionalFieldOf("failure_reason").forGetter(flight -> Optional.ofNullable(flight.failureReason)),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(RocketFlight::dimension),
            RocketPerformance.CODEC.fieldOf("performance").forGetter(RocketFlight::performance),
            BlockPos.CODEC.fieldOf("launch_position").forGetter(RocketFlight::launchPosition),
            BlockPos.CODEC.fieldOf("orbit_position").forGetter(RocketFlight::orbitPosition),
            BlockPos.CODEC.fieldOf("impact_position").forGetter(RocketFlight::impactPosition),
            ORBIT_POSITION_CODEC.fieldOf("target_orbit").forGetter(RocketFlight::targetOrbit),
            Codec.DOUBLE.fieldOf("required_delta_v").forGetter(RocketFlight::requiredDeltaVMetersPerSecond),
            Codec.DOUBLE.optionalFieldOf("impact_speed", 0D).forGetter(RocketFlight::impactSpeedMetersPerSecond),
            Codec.LONG.fieldOf("orbit_arrival_tick").forGetter(RocketFlight::orbitArrivalTick),
            Codec.LONG.fieldOf("reentry_tick").forGetter(RocketFlight::reentryTick),
            Codec.LONG.fieldOf("impact_tick").forGetter(RocketFlight::impactTick),
            BlockPos.CODEC.optionalFieldOf("takeoff_collision_position").forGetter(flight -> Optional.ofNullable(flight.takeoffCollisionPosition)),
            Codec.LONG.optionalFieldOf("takeoff_collision_tick", -1L).forGetter(RocketFlight::takeoffCollisionTick)
    ).apply(instance, (canReachOrbit, failureReason, dimension, performance, launchPosition, orbitPosition,
                       impactPosition, targetOrbit, requiredDeltaV, impactSpeed, orbitArrivalTick, reentryTick,
                       impactTick, takeoffCollisionPosition, takeoffCollisionTick) ->
            new RocketFlight(canReachOrbit, failureReason.orElse(null), dimension, performance, launchPosition,
                    orbitPosition, impactPosition, targetOrbit, requiredDeltaV, impactSpeed, orbitArrivalTick,
                    reentryTick, impactTick, takeoffCollisionPosition.orElse(null), takeoffCollisionTick)));

    public RocketFlight {
        targetOrbit = new Vector2i(targetOrbit);
    }

    RocketFlight scheduledAt(long launchTick) {
        return new RocketFlight(canReachOrbit, failureReason, dimension, performance, launchPosition, orbitPosition,
                impactPosition, targetOrbit, requiredDeltaVMetersPerSecond, impactSpeedMetersPerSecond,
                launchTick + orbitArrivalTick, reentryTick < 0 ? -1 : launchTick + reentryTick,
                impactTick < 0 ? -1 : launchTick + impactTick, takeoffCollisionPosition,
                takeoffCollisionTick < 0 ? -1 : launchTick + takeoffCollisionTick);
    }

    public boolean isInSpace(double gameTime) {
        return canReachOrbit && gameTime >= orbitArrivalTick && (reentryTick < 0 || gameTime < reentryTick);
    }

    RocketFlight withImpact(BlockPos position, double speedMetersPerSecond, long relativeImpactTick) {
        return new RocketFlight(canReachOrbit, failureReason, dimension, performance, launchPosition, orbitPosition,
                position, targetOrbit, requiredDeltaVMetersPerSecond, speedMetersPerSecond,
                orbitArrivalTick, reentryTick, relativeImpactTick, takeoffCollisionPosition, takeoffCollisionTick);
    }

    RocketFlight withTakeoffCollision(BlockPos position, long relativeCollisionTick) {
        return new RocketFlight(canReachOrbit, failureReason, dimension, performance, launchPosition, orbitPosition,
                impactPosition, targetOrbit, requiredDeltaVMetersPerSecond, impactSpeedMetersPerSecond,
                orbitArrivalTick, reentryTick, impactTick, position, relativeCollisionTick);
    }

    static RocketFlight failed(ResourceKey<Level> dimension, RocketPerformance performance,
                               BlockPos launchPosition, BlockPos orbitPosition, BlockPos impactPosition,
                               Vector2i targetOrbit, String reason) {
        return new RocketFlight(false, reason, dimension, performance, launchPosition, orbitPosition, impactPosition,
                targetOrbit, 0, 0, -1, -1, -1, null, -1);
    }
}
