package rearth.oritech.spaceage.client.mission_control;

import rearth.oritech.spaceage.simulation.FleetCategories;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.spaceage.network.FleetCategoryNetworking;
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

    public static final FleetCategories CATEGORIES = categories();
    private static UUID categorySystem;
    private static FleetCategories.Saved pendingCategories;
    private static int pendingCategoryMenu = -1;
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("oritech-space-age-collapsed-craft.txt");
    private static final Set<UUID> COLLAPSED = load();

    private static final Path SUN_FILE = FMLPaths.CONFIGDIR.get().resolve("oritech-space-age-show-sun.txt");
    private static boolean showSun = loadSun();

    private FleetViewPreferences() {
    }

    private static FleetCategories categories() {

        var categories = new FleetCategories();
        categories.onChange(saved -> {
            if (categorySystem != null && Minecraft.getInstance().player != null) {
                pendingCategories = saved;
                pendingCategoryMenu = Minecraft.getInstance().player.containerMenu.containerId;
                ClientPacketDistributor.sendToServer(new FleetCategoryNetworking.UpdateCategories(categorySystem, saved));
            }
        });
        return categories;
    }

    public static boolean receiveCategories(UUID system, FleetCategories.Saved saved) {

        var player = Minecraft.getInstance().player;
        // Ignore older polls/acks while our latest edit is still travelling to the server.
        if (system.equals(categorySystem) && pendingCategories != null && player != null
                && player.containerMenu.containerId == pendingCategoryMenu && !pendingCategories.equals(saved)) return false;
        pendingCategories = null;
        var changed = !system.equals(categorySystem) || !CATEGORIES.saved().equals(saved);
        categorySystem = system;
        CATEGORIES.replace(saved);
        return changed;
    }

    public static void clearWorld() {

        categorySystem = null;
        pendingCategories = null;
        pendingCategoryMenu = -1;
        CATEGORIES.replace(FleetCategories.Saved.EMPTY);
    }

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
