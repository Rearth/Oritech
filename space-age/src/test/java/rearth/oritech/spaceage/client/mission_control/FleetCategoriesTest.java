package rearth.oritech.spaceage.client.mission_control;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetCategoriesTest {

    @TempDir
    Path directory;

    @Test
    void automaticSectionsAndManualOverridesSurviveRestart() {

        var file = directory.resolve("categories.json");
        var preferences = new FleetCategories(file);
        var solar = UUID.randomUUID();
        var relay = UUID.randomUUID();
        assertEquals(List.of(FleetCategories.UNCATEGORIZED, FleetCategories.SUN),
                preferences.categories().stream().map(FleetCategories.Category::id).toList());
        assertTrue(preferences.collapsed(FleetCategories.SUN));
        assertEquals(FleetCategories.SUN, preferences.category(solar, true));
        assertEquals(FleetCategories.UNCATEGORIZED, preferences.category(relay, false));
        var category = preferences.create("  Relays  ");
        preferences.assign(relay, category);
        preferences.assign(solar, FleetCategories.UNCATEGORIZED);
        preferences.toggle(category);
        preferences.toggle(FleetCategories.SUN);
        var loaded = new FleetCategories(file);
        assertEquals(category, loaded.category(relay, false), "Manual assignments survive restart");
        assertEquals(FleetCategories.SUN, loaded.category(relay, true), "Sun orbit always groups automatically");
        assertEquals(FleetCategories.SUN, loaded.category(solar, true), "Manual Uncategorized cannot hide a Sun craft");
        assertTrue(loaded.collapsed(category));
        assertFalse(loaded.collapsed(FleetCategories.SUN));
        assertEquals("Relays", loaded.categories().getLast().name());
        assertEquals(FleetCategories.SUN, loaded.category(UUID.randomUUID(), true));
    }

    @Test
    void deletingCategoriesMovesMembersToUncategorizedIncludingAbsentCraft() {

        var file = directory.resolve("categories.json");
        var preferences = new FleetCategories(file);
        var active = UUID.randomUUID();
        var absent = UUID.randomUUID();
        var automatic = UUID.randomUUID();
        var category = preferences.create("Mining");
        preferences.assign(active, category);
        preferences.assign(absent, category);
        preferences.delete(category, List.of(active));
        preferences.delete(FleetCategories.SUN, List.of(automatic));
        var loaded = new FleetCategories(file);
        for (var craft : List.of(active, absent, automatic, UUID.randomUUID()))
            assertEquals(FleetCategories.UNCATEGORIZED, loaded.category(craft, false));
        assertEquals(List.of(FleetCategories.UNCATEGORIZED, FleetCategories.SUN), loaded.categories().stream().map(FleetCategories.Category::id).toList());
        loaded.delete(FleetCategories.UNCATEGORIZED, List.of(active));
        assertEquals(2, new FleetCategories(file).categories().size(), "Both automatic sections always exist");
        assertEquals(FleetCategories.SUN, loaded.category(automatic, true));
    }

    @Test
    void missingSunSectionIsRestoredFromExistingPreferences() throws Exception {

        var file = directory.resolve("categories.json");
        Files.writeString(file, "{\"categories\":[],\"assignments\":{}}");
        var preferences = new FleetCategories(file);
        assertEquals(List.of(FleetCategories.UNCATEGORIZED, FleetCategories.SUN),
                preferences.categories().stream().map(FleetCategories.Category::id).toList());
        preferences.delete(FleetCategories.SUN, List.of());
        assertEquals(FleetCategories.SUN, preferences.category(UUID.randomUUID(), true));
    }

    @Test
    void deletingOneSectionPreservesOtherSectionsAndTheirCollapsedStates() {

        var file = directory.resolve("categories.json");
        var preferences = new FleetCategories(file);
        var first = preferences.create("Stations");
        var second = preferences.create("Stations");
        assertNotEquals(first, second, "Names need not be unique; stable IDs own assignments");
        var craft = UUID.randomUUID();
        preferences.assign(craft, second);
        preferences.toggle(second);
        preferences.toggle(FleetCategories.UNCATEGORIZED);
        preferences.delete(first, List.of());
        var loaded = new FleetCategories(file);
        assertEquals(second, loaded.category(craft, false));
        assertTrue(loaded.collapsed(second));
        assertTrue(loaded.collapsed(FleetCategories.UNCATEGORIZED));
        assertNull(loaded.create("   "));
        loaded.assign(craft, "missing");
        assertEquals(second, loaded.category(craft, false));
    }
}
