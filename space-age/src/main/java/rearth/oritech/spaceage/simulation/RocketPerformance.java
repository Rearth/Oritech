package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// keeps the calculated mass, thrust and fuel capability together so a planned flight uses one stable performance snapshot
public record RocketPerformance(
        double dryMassKilograms,
        double fuelMassKilograms,
        double wetMassKilograms,
        int engineCount,
        double thrustNewtons,
        double availableBurnSeconds,
        double availableDeltaVMetersPerSecond,
        double liftoffAccelerationMetersPerSecondSquared) {

    public static final Codec<RocketPerformance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("dry_mass").forGetter(RocketPerformance::dryMassKilograms),
            Codec.DOUBLE.fieldOf("fuel_mass").forGetter(RocketPerformance::fuelMassKilograms),
            Codec.DOUBLE.fieldOf("wet_mass").forGetter(RocketPerformance::wetMassKilograms),
            Codec.INT.fieldOf("engine_count").forGetter(RocketPerformance::engineCount),
            Codec.DOUBLE.fieldOf("thrust").forGetter(RocketPerformance::thrustNewtons),
            Codec.DOUBLE.fieldOf("burn_seconds").forGetter(RocketPerformance::availableBurnSeconds),
            Codec.DOUBLE.fieldOf("available_delta_v").forGetter(RocketPerformance::availableDeltaVMetersPerSecond),
            Codec.DOUBLE.fieldOf("liftoff_acceleration").forGetter(RocketPerformance::liftoffAccelerationMetersPerSecondSquared)
    ).apply(instance, RocketPerformance::new));
}
