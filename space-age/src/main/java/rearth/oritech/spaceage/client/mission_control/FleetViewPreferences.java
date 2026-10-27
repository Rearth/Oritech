package rearth.oritech.spaceage.client.mission_control;

import net.neoforged.fml.loading.FMLPaths;
import rearth.oritech.spaceage.OritechSpaceAge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Local display preferences survive screen changes and game restarts.
 */
public final class FleetViewPreferences {

    public static final FleetCategories CATEGORIES = new FleetCategories(FMLPaths.CONFIGDIR.get().resolve("oritech-space-age-fleet-categories.json"));
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("oritech-space-age-collapsed-craft.txt");
    private static final Set<UUID> COLLAPSED = load();

    private static final Path SUN_FILE = FMLPaths.CONFIGDIR.get().resolve("oritech-space-age-show-sun.txt");
    private static boolean showSun = loadSun();

    public static boolean showSun() {

        return showSun;
    }

    public static void toggleSun() {

        showSun = !showSun;
        try {
            Files.writeString(SUN_FILE, Boolean.toString(showSun));
        } catch (IOException exception) {
            OritechSpaceAge.LOGGER.warn("Could not save Sun fleet preference", exception);
        }
    }

    private static boolean loadSun() {

        try {
            return Files.exists(SUN_FILE) && Boolean.parseBoolean(Files.readString(SUN_FILE));
        } catch (IOException exception) {
            return false;
        }
    }

    private FleetViewPreferences() {
    }

    static boolean collapsed(UUID craft) {

        return COLLAPSED.contains(craft);
    }

    static void toggle(UUID craft) {

        if (!COLLAPSED.remove(craft)) COLLAPSED.add(craft);
        try {
            Files.write(FILE, COLLAPSED.stream().map(UUID::toString).sorted().toList());
        } catch (IOException exception) {
            OritechSpaceAge.LOGGER.warn("Could not save fleet display preferences", exception);
        }
    }

    private static Set<UUID> load() {

        var result = new HashSet<UUID>();
        if (!Files.exists(FILE)) return result;
        try {
            for (var line : Files.readAllLines(FILE)) result.add(UUID.fromString(line.trim()));
        } catch (IOException | IllegalArgumentException exception) {
            OritechSpaceAge.LOGGER.warn("Could not read fleet display preferences", exception);
        }
        return result;
    }
}
