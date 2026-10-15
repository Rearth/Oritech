package rearth.oritech.spaceage;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Construction limits are synced with the server so previews use the same bounds. */
public final class SpaceAgeConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue APERTURE_ARM_LIMIT;

    static {
        var builder = new ModConfigSpec.Builder();
        APERTURE_ARM_LIMIT = builder.comment("Maximum length of each Beam Aperture frame arm. Opposite arms must match.")
                .defineInRange("apertureArmLimit", 16, 1, 32);
        SPEC = builder.build();
    }

    public static int apertureArmLimit() {

        return SPEC.isLoaded() ? APERTURE_ARM_LIMIT.get() : APERTURE_ARM_LIMIT.getDefault();
    }

    private SpaceAgeConfig() {
    }
}
