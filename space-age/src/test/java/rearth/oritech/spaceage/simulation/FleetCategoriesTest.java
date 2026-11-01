package rearth.oritech.spaceage.simulation;

import org.junit.jupiter.api.Test;
import net.minecraft.nbt.NbtOps;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetCategoriesTest {

    @Test
    void launchCategoriesApplyUntilManuallyOverriddenAndRespectAutomaticSunGrouping() {

        var preferences = new FleetCategories();
        var launchCategory = preferences.create("Automated satellites");
        var craft = UUID.randomUUID();
        assertEquals(launchCategory, preferences.category(craft, false, launchCategory));
        var loaded = copy(preferences);
        assertEquals(launchCategory, loaded.category(craft, false, launchCategory));
        assertEquals(FleetCategories.SUN, loaded.category(craft, true, launchCategory));
        loaded.assign(craft, FleetCategories.UNCATEGORIZED);
        assertEquals(FleetCategories.UNCATEGORIZED, loaded.category(craft, false, launchCategory));
        var another = UUID.randomUUID();
        loaded.delete(launchCategory, List.of());
        assertEquals(FleetCategories.UNCATEGORIZED, loaded.category(another, false, launchCategory));
    }

    @Test
    void automaticSectionsAndManualOverridesSurviveRestart() {

        var preferences = new FleetCategories();
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
        var loaded = copy(preferences);
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

        var preferences = new FleetCategories();
        var active = UUID.randomUUID();
        var absent = UUID.randomUUID();
        var automatic = UUID.randomUUID();
        var category = preferences.create("Mining");
        preferences.assign(active, category);
        preferences.assign(absent, category);
        preferences.delete(category, List.of(active));
        preferences.delete(FleetCategories.SUN, List.of(automatic));
        var loaded = copy(preferences);
        for (var craft : List.of(active, absent, automatic, UUID.randomUUID()))
            assertEquals(FleetCategories.UNCATEGORIZED, loaded.category(craft, false));
        assertEquals(List.of(FleetCategories.UNCATEGORIZED, FleetCategories.SUN), loaded.categories().stream().map(FleetCategories.Category::id).toList());
        loaded.delete(FleetCategories.UNCATEGORIZED, List.of(active));
        assertEquals(2, copy(loaded).categories().size(), "Both automatic sections always exist");
        assertEquals(FleetCategories.SUN, loaded.category(automatic, true));
    }

    @Test
    void missingSunSectionIsRestoredFromSavedWorldData() {

        var preferences = new FleetCategories(new FleetCategories.Saved(
                List.of(new FleetCategories.Category(FleetCategories.UNCATEGORIZED, "", false)), Map.of()));
        assertEquals(List.of(FleetCategories.UNCATEGORIZED, FleetCategories.SUN),
                preferences.categories().stream().map(FleetCategories.Category::id).toList());
        preferences.delete(FleetCategories.SUN, List.of());
        assertEquals(FleetCategories.SUN, preferences.category(UUID.randomUUID(), true));
    }

    @Test
    void deletingOneSectionPreservesOtherSectionsAndTheirCollapsedStates() {

        var preferences = new FleetCategories();
        var first = preferences.create("Stations");
        var second = preferences.create("Stations");
        assertNotEquals(first, second, "Names need not be unique; stable IDs own assignments");
        var craft = UUID.randomUUID();
        preferences.assign(craft, second);
        preferences.toggle(second);
        preferences.toggle(FleetCategories.UNCATEGORIZED);
        preferences.delete(first, List.of());
        var loaded = copy(preferences);
        assertEquals(second, loaded.category(craft, false));
        assertTrue(loaded.collapsed(second));
        assertTrue(loaded.collapsed(FleetCategories.UNCATEGORIZED));
        assertNull(loaded.create("   "));
        loaded.assign(craft, "missing");
        assertEquals(second, loaded.category(craft, false));
    }

    @Test
    void worldsAndPlayersHaveSeparateSavedCategories() {

        var owner = UUID.randomUUID();
        var world = new SpaceSimulationSavedData();
        var simulation = world.getOrCreate(owner);
        world.setDirty(false);
        var category = simulation.fleetCategories.create("Relays");
        var craft = UUID.randomUUID();
        simulation.fleetCategories.assign(craft, category);
        simulation.fleetCategories.toggle(category);
        assertTrue(world.isDirty(), "Category edits mark world data dirty");
        assertEquals(2, new SpaceSimulationSavedData().getOrCreate(owner).fleetCategories.categories().size(),
                "The same player in another world starts with automatic sections only");
        assertEquals(2, world.getOrCreate(UUID.randomUUID()).fleetCategories.categories().size(),
                "Another player's categories are independent");
        var saved = SpaceSimulationSavedData.CODEC.encodeStart(NbtOps.INSTANCE, world).getOrThrow();
        var loaded = SpaceSimulationSavedData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        var restored = loaded.getOrCreate(owner).fleetCategories;
        assertEquals(simulation.fleetCategories.saved(), restored.saved());
        assertEquals(category, restored.category(craft, false));
        assertTrue(restored.collapsed(category));
        loaded.setDirty(false);
        restored.toggle(category);
        assertTrue(loaded.isDirty(), "Loaded categories retain their world save callback");
    }

    private static FleetCategories copy(FleetCategories categories) {

        var saved = FleetCategories.Saved.CODEC.encodeStart(NbtOps.INSTANCE, categories.saved()).getOrThrow();
        return new FleetCategories(FleetCategories.Saved.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow());
    }

}
