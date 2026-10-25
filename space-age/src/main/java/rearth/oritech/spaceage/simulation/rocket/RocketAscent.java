package rearth.oritech.spaceage.simulation.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Shared physical ascent for rendering, obstruction timing and collision energy. */
public record RocketAscent(double initialSpeed, double acceleration) {

    public static final Codec<RocketAscent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("initial_speed").forGetter(RocketAscent::initialSpeed),
            Codec.DOUBLE.fieldOf("acceleration").forGetter(RocketAscent::acceleration)
    ).apply(i, RocketAscent::new));

    public static RocketAscent launch(RocketPerformance performance, double speed) {

        // This short world visual does not simulate orbital gravity; weak craft coast on their launch velocity.
        var acceleration = performance.liftoffAccelerationMetersPerSecondSquared() - RocketPerformanceCalculator.STANDARD_GRAVITY;
        return new RocketAscent(speed, Math.max(speed > 0 ? 0 : .1, acceleration));
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
