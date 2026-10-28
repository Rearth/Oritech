package rearth.oritech.spaceage.simulation.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import rearth.oritech.spaceage.simulation.SpaceBalance;

/** Shared physical ascent for rendering, obstruction timing and collision energy. */
public record RocketAscent(double initialSpeed, double acceleration) {

    public static final Codec<RocketAscent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("initial_speed").forGetter(RocketAscent::initialSpeed),
            Codec.DOUBLE.fieldOf("acceleration").forGetter(RocketAscent::acceleration)
    ).apply(i, RocketAscent::new));

    public static RocketAscent launch(RocketPerformance performance, double speed) {

        var acceleration = performance.liftoffAccelerationMetersPerSecondSquared() - SpaceBalance.STANDARD_GRAVITY;
        if (speed <= 0) return new RocketAscent(0, Math.max(.1, acceleration));
        // Weak craft slow down under surface gravity. The space simulation already decided this craft reaches space,
        // so the slowdown is capped to keep at least ~70% of the launch speed at orbit height.
        var slowest = -speed * speed / (4 * SpaceBalance.LAUNCH_ORBIT_HEIGHT_BLOCKS);
        return new RocketAscent(speed, Math.max(slowest, acceleration));
    }

    public double secondsTo(double height) {

        if (height <= 0) return 0;
        return 2 * height / (Math.sqrt(initialSpeed * initialSpeed + 2 * acceleration * height) + initialSpeed);
    }

    public double height(double seconds) {

        return initialSpeed * seconds + .5 * acceleration * seconds * seconds;
    }

    public double speed(double seconds) {

        return initialSpeed + acceleration * seconds;
    }
}
