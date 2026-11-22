package rearth.oritech.spaceage.block.hologram;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import rearth.oritech.spaceage.simulation.SpaceObjects;

import java.util.List;
import java.util.UUID;

/** Persistent display configuration only; never modifies a mission. */
public record HoloSettings(String mode, String framing, String category, UUID craft, double centerX, double centerY, double coverage, double minimum, double maximum, double margin, double height, double rotation, double markerSize, int layers, boolean locked, double response) {

    public static final int BODIES = 1;
    public static final int ORBITS = 2;
    private static final int PATHS = 4;
    public static final int SURVEYS = 8;
    public static final int LABELS = 16;
    public static final int OTHER_CRAFT = 32;
    private static final int SELECTED_PATHS = 64;
    private static final int LAYER_MASK = BODIES | ORBITS | PATHS | SURVEYS | LABELS | OTHER_CRAFT | SELECTED_PATHS;
    public static final HoloSettings DEFAULT = new HoloSettings("area", "fixed", "uncategorized", new UUID(0, 0), SpaceObjects.EARTH_X, 0, 1_000_000, 20_000, 20_000_000, 1.25, .7, 0, 1, 63, false, 1.2);
    public static final Codec<HoloSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("mode").forGetter(HoloSettings::mode),
            Codec.STRING.fieldOf("framing").forGetter(HoloSettings::framing),
            Codec.STRING.fieldOf("category").forGetter(HoloSettings::category),
            UUIDUtil.STRING_CODEC.fieldOf("craft").forGetter(HoloSettings::craft),
            Codec.DOUBLE.fieldOf("centerX").forGetter(HoloSettings::centerX),
            Codec.DOUBLE.fieldOf("centerY").forGetter(HoloSettings::centerY),
            Codec.DOUBLE.fieldOf("coverage").forGetter(HoloSettings::coverage),
            Codec.DOUBLE.fieldOf("minimum").forGetter(HoloSettings::minimum),
            Codec.DOUBLE.fieldOf("maximum").forGetter(HoloSettings::maximum),
            Codec.DOUBLE.fieldOf("margin").forGetter(HoloSettings::margin),
            Codec.DOUBLE.fieldOf("height").forGetter(HoloSettings::height),
            Codec.DOUBLE.fieldOf("rotation").forGetter(HoloSettings::rotation),
            Codec.DOUBLE.fieldOf("markerSize").forGetter(HoloSettings::markerSize),
            Codec.INT.fieldOf("layers").forGetter(HoloSettings::layers),
            Codec.BOOL.fieldOf("locked").forGetter(HoloSettings::locked),
            Codec.DOUBLE.optionalFieldOf("response", 1.2).forGetter(HoloSettings::response)
    ).apply(i, HoloSettings::new));

    public boolean shows(int layer) {

        return (layers & layer) != 0;
    }

    public List<String> framingOptions() {

        return switch (mode) {
            case "category" -> List.of("fixed", "fit", "journey");
            case "craft" -> List.of("fixed", "follow", "journey");
            default -> List.of("fixed");
        };
    }

    public List<Trajectories> trajectoryOptions() {

        return mode.equals("craft") ? List.of(Trajectories.ON, Trajectories.OFF, Trajectories.SELECTED_CRAFT)
                : List.of(Trajectories.ON, Trajectories.OFF);
    }

    public Trajectories trajectories() {

        if (!shows(PATHS)) return Trajectories.OFF;
        return shows(SELECTED_PATHS) && mode.equals("craft") ? Trajectories.SELECTED_CRAFT : Trajectories.ON;
    }

    public int layersWithTrajectories(Trajectories option) {

        return (layers & ~(PATHS | SELECTED_PATHS)) | option.layers;
    }

    public HoloSettings normalized() {

        var view = switch (mode) {
            case "category", "craft" -> mode;
            default -> "area";
        };
        var frame = mode.equals("craft") && framing.equals("local") ? "follow" : framing;
        return new HoloSettings(view, framingOptions().contains(frame) ? frame : "fixed",
                category.substring(0, Math.min(64, category.length())), craft,
                finite(centerX, SpaceObjects.EARTH_X), finite(centerY, 0),
                Math.clamp(finite(coverage, 1_000_000), 100, 100_000_000),
                Math.clamp(finite(minimum, 20_000), 100, 100_000_000),
                Math.clamp(finite(maximum, 20_000_000), Math.clamp(finite(minimum, 20_000), 100, 100_000_000), 100_000_000),
                Math.clamp(finite(margin, 1.25), 1.05, 3), Math.clamp(finite(height, .7), .16, .7),
                finite(rotation, 0) % 360, Math.clamp(finite(markerSize, 1), .25, 3),
                layersWithTrajectories(trajectories()) & LAYER_MASK, locked, Math.clamp(finite(response, 1.2), .1, 10));
    }

    private static double finite(double value, double fallback) {

        return Double.isFinite(value) ? value : fallback;
    }

    public enum Trajectories {
        ON(PATHS),
        OFF(0),
        SELECTED_CRAFT(PATHS | SELECTED_PATHS);

        private final int layers;

        Trajectories(int layers) {
            this.layers = layers;
        }
    }
}
