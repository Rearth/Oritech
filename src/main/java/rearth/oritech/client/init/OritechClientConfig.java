package rearth.oritech.client.init;

import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.ModConfigSpec;

public class OritechClientConfig {
    public enum VisualEffects { FULL, REDUCED, NONE }

    // Client config
    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<VisualEffects> visualEffects = CLIENT
            .comment("Oritech visual effects: FULL keeps all effects; REDUCED keeps about a quarter of particles,"
                    + " removes beam outer layers and centrifuge splashes; NONE disables particles and beams.",
                    "Client-only: also filters Oritech particles sent by the server.")
            .defineEnum("visualEffects", VisualEffects.FULL);

    public static boolean renderBeams() {
        return visualEffects.get() != VisualEffects.NONE;
    }

    public static boolean renderFullEffects() {
        return visualEffects.get() == VisualEffects.FULL;
    }

    public static int particleCount(int count, RandomSource random) {
        return switch (visualEffects.get()) {
            case FULL -> count;
            case REDUCED -> count / 4 + (random.nextInt(4) < count % 4 ? 1 : 0);
            case NONE -> 0;
        };
    }

    public static boolean spawnParticle(RandomSource random) {
        return switch (visualEffects.get()) {
            case FULL -> true;
            case REDUCED -> random.nextInt(4) == 0;
            case NONE -> false;
        };
    }

    public static final ModConfigSpec.BooleanValue showMachinePreview = OritechClientConfig.CLIENT
            .comment("Render multiblock placement preview")
            .define("showMachinePreview", true);

    public static final ModConfigSpec.BooleanValue enableHelpButton = OritechClientConfig.CLIENT
            .comment("Enable help button in machine UIs")
            .define("enableHelpButton", true);

    public static final ModConfigSpec.BooleanValue showOracleIndexWarning = OritechClientConfig.CLIENT
            .comment("Show the missing Oracle Index warning once when entering a world")
            .define("showOracleIndexWarning", true);

    public static final ModConfigSpec.DoubleValue maxZiplineSpeed = OritechClientConfig.CLIENT
            .comment("Maximum zipline speed in blocks/second")
            .defineInRange("maxZiplineSpeed", 8.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue ziplineAcceleration = OritechClientConfig.CLIENT
            .comment("Zipline acceleration, higher = faster")
            .defineInRange("ziplineAcceleration", 0.1, 0.0, 10.0);

    public static final ModConfigSpec.BooleanValue ziplineAutoJump = OritechClientConfig.CLIENT
            .comment("Enable auto jump at zipline end")
            .define("ziplineAutoJump", true);

    public static final ModConfigSpec.BooleanValue ziplineCameraSwitch = OritechClientConfig.CLIENT
            .comment("Enable 3rd person camera while ziplining")
            .define("ziplineCameraSwitch", true);


    public static final ModConfigSpec CLIENT_SPEC = OritechClientConfig.CLIENT.build();
}
