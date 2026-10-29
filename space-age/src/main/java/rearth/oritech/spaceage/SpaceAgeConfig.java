package rearth.oritech.spaceage;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Construction limits are synced with the server so previews use the same bounds.
 */
public final class SpaceAgeConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue APERTURE_ARM_LIMIT;
    public static final ModConfigSpec.IntValue CUBE_SAT_MASS;
    public static final ModConfigSpec.LongValue CUBE_SAT_RF_PER_BLOCK;
    public static final ModConfigSpec.DoubleValue LAUNCH_SPEED_FACTOR;
    public static final ModConfigSpec.IntValue LAUNCH_SPEED_CAP;
    public static final ModConfigSpec.IntValue ORBIT_SPEED;

    static {
        var builder = new ModConfigSpec.Builder();
        APERTURE_ARM_LIMIT = builder.comment("Maximum length of each Beam Aperture frame arm. Opposite arms must match.")
                .defineInRange("apertureArmLimit", 16, 1, 32);
        CUBE_SAT_MASS = builder.comment("Mass of a folded cube-sat in tonnes, independent of its contents.")
                .defineInRange("cubeSatMass", 2, 0, 1000);
        CUBE_SAT_RF_PER_BLOCK = builder.comment("Addon splicer RF used per block when compacting a rocket into a cube-sat.")
                .defineInRange("cubeSatRfPerBlock", 100_000L, 0L, Long.MAX_VALUE);
        LAUNCH_SPEED_FACTOR = builder.comment("Launch velocity in m/s per particle speed hitting a particle launch ramp, e.g. 0.1 turns particle speed 1000 into 100 m/s.")
                .defineInRange("launchSpeedFactor", 0.1, 0, 100);
        LAUNCH_SPEED_CAP = builder.comment("Maximum launch velocity of a particle launch ramp in m/s.")
                .defineInRange("launchSpeedCap", 1500, 0, 100_000);
        ORBIT_SPEED = builder.comment("Launch velocity in m/s at which a cube-sat without a matching plan stays in Earth orbit. 0 derives it from Earth's gravity.")
                .defineInRange("orbitSpeed", 0, 0, 100_000);
        SPEC = builder.build();
    }

    private SpaceAgeConfig() {
    }

    public static int apertureArmLimit() {

        return SPEC.isLoaded() ? APERTURE_ARM_LIMIT.get() : APERTURE_ARM_LIMIT.getDefault();
    }

    public static int cubeSatMass() {

        return SPEC.isLoaded() ? CUBE_SAT_MASS.get() : CUBE_SAT_MASS.getDefault();
    }

    public static long cubeSatRfPerBlock() {

        return SPEC.isLoaded() ? CUBE_SAT_RF_PER_BLOCK.get() : CUBE_SAT_RF_PER_BLOCK.getDefault();
    }

    public static double launchSpeedFactor() {

        return SPEC.isLoaded() ? LAUNCH_SPEED_FACTOR.get() : LAUNCH_SPEED_FACTOR.getDefault();
    }

    public static int launchSpeedCap() {

        return SPEC.isLoaded() ? LAUNCH_SPEED_CAP.get() : LAUNCH_SPEED_CAP.getDefault();
    }

    public static int orbitSpeed() {

        return SPEC.isLoaded() ? ORBIT_SPEED.get() : ORBIT_SPEED.getDefault();
    }
}
