package rearth.oritech.spaceage.client.starmap;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.client.starmap.StarMapIconGroups.Icon;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarMapIconGroupsTest {

    private static Icon icon(int id, ObjectType type, double x, double y, boolean selected) {

        return new Icon(new UUID(0, id), type, x, y, selected);
    }

    @Test
    void pairsRemainVisibleAndCategoriesAreCountedSeparately() {

        var first = icon(1, ObjectType.ASTEROID, 0, 0, false);
        var second = icon(2, ObjectType.ASTEROID, 3, 2, false);
        var rocket = icon(3, ObjectType.CRAFT, 0, 0, false);
        assertTrue(new StarMapIconGroups().hidden(List.of(first, second, rocket)).isEmpty());
    }

    @Test
    void crowdedAsteroidsAndRocketsEachKeepOneSelectedRepresentative() {

        var asteroids = List.of(icon(1, ObjectType.ASTEROID, 0, 0, false),
                icon(2, ObjectType.ASTEROID, 4, 2, true), icon(3, ObjectType.ASTEROID, 8, 4, false));
        assertEquals(Set.of(asteroids.getFirst().id(), asteroids.getLast().id()), new StarMapIconGroups().hidden(asteroids));
        var rockets = List.of(icon(4, ObjectType.CRAFT, 0, 0, false),
                icon(5, ObjectType.CRAFT, 4, 2, true), icon(6, ObjectType.CRAFT, 8, 4, false));
        var combined = new ArrayList<>(asteroids);
        combined.addAll(rockets);
        assertEquals(Set.of(asteroids.getFirst().id(), asteroids.getLast().id(), rockets.getFirst().id(), rockets.getLast().id()),
                new StarMapIconGroups().hidden(combined));
    }

    @Test
    void iconsCombineOnlyWhenOverlappingAndDoNotFlickerAtTheBoundary() {

        var groups = new StarMapIconGroups();
        var first = icon(1, ObjectType.ASTEROID, 0, 0, false);
        var second = icon(2, ObjectType.ASTEROID, 11, 0, false);
        var third = icon(3, ObjectType.ASTEROID, 0, 11, false);
        assertTrue(groups.hidden(List.of(first, second, third)).isEmpty());
        var hidden = Set.of(second.id(), third.id());
        assertEquals(hidden, groups.hidden(List.of(first, icon(2, ObjectType.ASTEROID, 9, 0, false),
                icon(3, ObjectType.ASTEROID, 0, 9, false))));
        assertEquals(hidden, groups.hidden(List.of(first, second, third)));
        assertTrue(groups.hidden(List.of(first, icon(2, ObjectType.ASTEROID, 13, 0, false),
                icon(3, ObjectType.ASTEROID, 0, 13, false))).isEmpty());
        assertTrue(groups.hidden(List.of(first, second, third)).isEmpty());
    }

    @Test
    void zoomingInSeparatesContactsAndInputOrderDoesNotChangeTheRepresentative() {

        var first = icon(1, ObjectType.CRAFT, 0, 0, false);
        var second = icon(2, ObjectType.CRAFT, 2, 0, false);
        var third = icon(3, ObjectType.CRAFT, 4, 0, false);
        assertEquals(Set.of(second.id(), third.id()), new StarMapIconGroups().hidden(List.of(third, second, first)));
        assertTrue(new StarMapIconGroups().hidden(List.of(first, icon(2, ObjectType.CRAFT, 20, 0, false),
                icon(3, ObjectType.CRAFT, 40, 0, false))).isEmpty());
    }
}
