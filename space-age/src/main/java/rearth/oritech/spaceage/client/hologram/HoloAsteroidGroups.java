package rearth.oritech.spaceage.client.hologram;

import rearth.oritech.spaceage.block.hologram.HoloSettings;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.block.hologram.HoloView;
import rearth.oritech.spaceage.block.hologram.HoloView.Frame;
import rearth.oritech.spaceage.client.starmap.StarMapIconGroups;
import rearth.oritech.spaceage.client.starmap.StarMapIconGroups.Icon;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Use projected table distance, so zooming in naturally separates crowded contacts. */
final class HoloAsteroidGroups {

    private final StarMapIconGroups groups = new StarMapIconGroups();

    Set<UUID> hidden(HoloSnapshot snapshot, Frame frame, UUID pinned) {

        var important = new HashSet<UUID>();
        if (pinned != null) important.add(pinned);
        for (var craft : snapshot.craft()) {
            if (HoloView.selected(snapshot.settings(), craft) || snapshot.settings().shows(HoloSettings.OTHER_CRAFT))
                important.add(craft.target());
        }
        for (var object : snapshot.objects())
            if (snapshot.craft().stream().anyMatch(craft -> craft.id().equals(object.tugId()))) important.add(object.id());
        // Group only overlapping small icons; keep nearby but distinct contacts visible.
        var scale = 10 / (.16 * snapshot.settings().markerSize());
        var icons = snapshot.objects().stream().filter(object -> object.type() == ObjectType.ASTEROID)
                .filter(object -> HoloView.inside(HoloView.project(object.x(), object.y(), frame, snapshot), snapshot.surface()))
                .map(object -> {
                    var point = HoloView.project(object.x(), object.y(), frame, snapshot);
                    return new Icon(object.id(), object.type(), point.x() * scale, point.y() * scale, important.contains(object.id()));
                }).toList();
        var hidden = new HashSet<>(groups.hidden(icons));
        hidden.removeAll(important);
        return hidden;
    }
}
