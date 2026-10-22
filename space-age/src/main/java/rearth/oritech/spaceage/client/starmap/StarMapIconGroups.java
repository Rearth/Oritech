package rearth.oritech.spaceage.client.starmap;

import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reduce crowded map icons in screen space, independently for rockets and asteroids.
 */
final class StarMapIconGroups {

    private static final double GROUP_RADIUS = 10;
    private static final double RELEASE_RADIUS = 12;
    private Set<UUID> previousHidden = Set.of();

    Set<UUID> hidden(List<Icon> icons) {

        var ordered = icons.stream().sorted(Comparator.comparing(Icon::selected).reversed()
                .thenComparing(Icon::id)).toList();
        var grouped = new HashSet<UUID>();
        var hidden = new HashSet<UUID>();
        for (var icon : ordered) {
            if (grouped.contains(icon.id())) continue;
            var nearby = ordered.stream().filter(other -> !grouped.contains(other.id()) && other.type() == icon.type()
                    && Math.hypot(other.x() - icon.x(), other.y() - icon.y()) <= (previousHidden.contains(other.id()) ? RELEASE_RADIUS : GROUP_RADIUS)).toList();
            if (nearby.size() <= 2) continue;
            for (var other : nearby) {
                grouped.add(other.id());
                if (!other.id().equals(icon.id())) hidden.add(other.id());
            }
        }
        // Keep grouped contacts hidden a little longer when zooming in to avoid boundary flicker.
        previousHidden = hidden;
        return hidden;
    }

    record Icon(UUID id, ObjectType type, double x, double y, boolean selected) {
    }
}
