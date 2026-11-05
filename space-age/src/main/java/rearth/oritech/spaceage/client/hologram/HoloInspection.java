package rearth.oritech.spaceage.client.hologram;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Craft;
import rearth.oritech.spaceage.simulation.AsteroidShape;
import rearth.oritech.spaceage.simulation.FleetCategories;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Concise content for the single world-space inspection callout. */
final class HoloInspection {

    private HoloInspection() {
    }

    static List<Component> body(SpaceObjectData object, Component name, @Nullable String tug) {

        var lines = new ArrayList<Component>();
        lines.add(name);
        if (object.type() == ObjectType.ASTEROID) {
            if (AsteroidShape.volume(object.materials()) > 0) {
                var size = AsteroidShape.size(object.materials());
                lines.add(Component.translatable("screen.oritech_space_age.object.physical_size", size.width(), size.height(), size.depth()));
            }
            lines.add(Component.translatable("screen.oritech_space_age.object.mass", number(object.mass() * 1_000)));
            var speed = Math.hypot(object.velocityX(), object.velocityY());
            if (!object.tugId().equals(FlightPlanAction.NO_TARGET))
                lines.add(Component.translatable("screen.oritech_space_age.object.tugged", tug == null ? Component.translatable("screen.oritech_space_age.object.craft") : tug));
            else lines.add(Component.translatable(speed > 0 ? "screen.oritech_space_age.object.drifting" : "screen.oritech_space_age.object.stationary"));
            if (speed > 0) lines.add(Component.translatable("screen.oritech_space_age.object.velocity", String.format(Locale.ROOT, "%.1f", speed)));
        } else if (object.type() == ObjectType.RESOURCE_BODY)
            lines.add(Component.translatable("screen.oritech_space_age.object.infinite_deposit"));
        else lines.add(Component.translatable("screen.oritech_space_age.holo.surface_gravity", number(object.surfaceGravity())));
        var total = object.materials().stream().mapToDouble(m -> Math.max(0, m.amount())).sum();
        object.materials().stream().sorted(Comparator.comparingInt(m -> -m.amount())).limit(6).forEach(material -> {
            var blockName = BuiltInRegistries.BLOCK.getValue(material.block()).getName();
            lines.add(object.type() == ObjectType.RESOURCE_BODY
                    ? Component.translatable("screen.oritech_space_age.object.resource_weight", blockName, number(total > 0 ? material.amount() * 100 / total : 0))
                    : material.amount() > 0 ? Component.translatable("screen.oritech_space_age.object.material_known", blockName, number(material.amount()))
                    : Component.translatable("screen.oritech_space_age.object.material_unknown", blockName));
        });
        return List.copyOf(lines);
    }

    static List<Component> craft(Craft craft, HoloSnapshot snapshot, Component targetName) {

        return List.of(Component.literal(craft.name()), Component.translatable(craft.status()),
                Component.translatable("screen.oritech_space_age.holo.inspection_category", new FleetCategories(snapshot.categories()).label(craft.category())),
                Component.translatable("screen.oritech_space_age.holo.inspection_target", targetName),
                Component.translatable("screen.oritech_space_age.object.velocity", number(Math.hypot(craft.vx(), craft.vy()))),
                Component.translatable(craft.connected() ? "screen.oritech_space_age.holo.linked" : "screen.oritech_space_age.holo.unlinked"));
    }

    private static String number(double value) {

        return String.format(Locale.ROOT, Math.abs(value) < 10 && value != Math.rint(value) ? "%,.1f" : "%,.0f", value);
    }
}
