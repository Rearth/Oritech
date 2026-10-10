package rearth.oritech.spaceage.client;

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
final class FleetViewPreferences {

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("oritech-space-age-collapsed-craft.txt");
    private static final Set<UUID> COLLAPSED = load();

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
