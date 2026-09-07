package rearth.oritech.spaceage.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Builds the map's object, route, and stage tooltips from immutable display data. */
final class StarMapTooltip {

    private StarMapTooltip() {
    }

    static List<Component> build(Separation separation, SpaceSimulation.SpaceObjectData object,
                                 RocketStarMapWidget.NavigationSelection selection, ArrivalPosition arrival,
                                 PathPoint pathPoint, SpaceSimulation.FlightPlan plan,
                                 Map<SpaceSimulation.SegmentRef, String> defaultSegmentNames,
                                 Function<java.util.UUID, SpaceSimulation.SpaceObjectData> objectById) {
        if (separation != null) {
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable("screen.oritech_space_age.separation_stage", separation.stage()).withStyle(ChatFormatting.BOLD));
            lines.add(Component.translatable("screen.oritech_space_age.separation_time", format(separation.timeSeconds())));
            separation.segments().forEach(segment -> lines.add(Component.literal("• " + segmentName(plan, defaultSegmentNames, segment))));
            return lines;
        }
        if (object != null) return objectTooltip(object, selection, arrival);
        if (pathPoint == null) return List.of();

        var sample = pathPoint.sample();
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("screen.oritech_space_age.path_state", sample.stage(), pathPhaseName(sample.phase())).withStyle(ChatFormatting.BOLD));
        lines.add(Component.translatable("screen.oritech_space_age.path_speed", formatSpeed(pathPoint.speedMetersPerSecond())));
        lines.add(Component.translatable("screen.oritech_space_age.path_connected"));
        addSegmentNames(lines, sample.connectedSegments(), plan, defaultSegmentNames);
        if (!sample.attachedAsteroidId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
            var asteroid = objectById.apply(sample.attachedAsteroidId());
            if (asteroid != null) lines.add(Component.translatable("screen.oritech_space_age.path.attached_asteroid",
                    RocketStarMapWidget.objectName(asteroid)));
        }
        lines.add(Component.translatable("screen.oritech_space_age.path_firing"));
        if (sample.firingSegments().isEmpty()) lines.add(Component.translatable("screen.oritech_space_age.path_none"));
        else addSegmentNames(lines, sample.firingSegments(), plan, defaultSegmentNames);
        return lines;
    }

    private static List<Component> objectTooltip(SpaceSimulation.SpaceObjectData object,
                                                  RocketStarMapWidget.NavigationSelection selection,
                                                  ArrivalPosition arrival) {
        var lines = new ArrayList<Component>();
        lines.add(RocketStarMapWidget.objectName(object).copy().withStyle(ChatFormatting.BOLD));
        if (selection != null && selection.orbit() != SpaceSimulation.OrbitBand.SURFACE) {
            lines.add(Component.translatable("screen.oritech_space_age.orbit_selection", RocketStarMapWidget.orbitName(selection.orbit())));
        }
        if (selection != null && object.type() != SpaceObjects.ObjectType.ASTEROID) {
            var gravity = gravityAtOrbit(object, selection.orbit());
            var percentage = object.surfaceGravity() <= 0 ? 0 : gravity / object.surfaceGravity() * 100;
            lines.add(Component.translatable("screen.oritech_space_age.orbit_gravity", String.format(Locale.ROOT, "%.2f", gravity),
                    String.format(Locale.ROOT, "%.0f", percentage)));
        }
        var displayedX = arrival == null ? object.x() : arrival.x();
        var displayedY = arrival == null ? object.y() : arrival.y();
        lines.add(Component.translatable("screen.oritech_space_age.object.position", format(displayedX), format(displayedY)));
        if (arrival != null && Math.hypot(arrival.x() - object.x(), arrival.y() - object.y()) >= 1) {
            lines.add(Component.translatable("screen.oritech_space_age.object.arrival_position",
                    String.format(Locale.ROOT, "%.2f", arrival.timeSeconds() / 1_200), format(object.x()), format(object.y())));
        }
        lines.add(Component.translatable("screen.oritech_space_age.object.radius", format(object.radius())));
        if (object.type() == SpaceObjects.ObjectType.ASTEROID) {
            lines.add(Component.translatable("screen.oritech_space_age.object.mass", format(object.mass() * 1_000)));
            lines.add(Component.translatable("screen.oritech_space_age.object.velocity",
                    formatSpeed(Math.hypot(object.velocityX(), object.velocityY()))));
            lines.add(Component.translatable("screen.oritech_space_age.object.materials"));
            object.materials().forEach(material -> lines.add(Component.literal("• " + material.block() + " × " + material.amount())));
        }
        lines.add(Component.translatable("screen.oritech_space_age.object.detection", object.detectionState().name().toLowerCase(Locale.ROOT)));
        return lines;
    }

    private static void addSegmentNames(List<Component> lines, java.util.Set<SpaceSimulation.SegmentRef> segments,
                                        SpaceSimulation.FlightPlan plan, Map<SpaceSimulation.SegmentRef, String> defaults) {
        segments.stream().sorted(java.util.Comparator.comparingLong(ref -> ref.anchor().asLong()))
                .map(segment -> segmentName(plan, defaults, segment)).map(name -> Component.literal("• " + name)).forEach(lines::add);
    }

    private static String segmentName(SpaceSimulation.FlightPlan plan, Map<SpaceSimulation.SegmentRef, String> defaults,
                                      SpaceSimulation.SegmentRef segment) {
        var name = plan.configurationFor(segment).name();
        return name.isBlank() ? defaults.getOrDefault(segment, "Unknown segment") : name;
    }

    private static double gravityAtOrbit(SpaceSimulation.SpaceObjectData object, SpaceSimulation.OrbitBand orbit) {
        var distance = object.radius() + orbit.altitude();
        if (distance <= 0) return 0;
        var relativeDistance = object.radius() / distance;
        return object.surfaceGravity() * relativeDistance * relativeDistance;
    }

    private static Component pathPhaseName(RocketFlightPathCalculator.PathPhase phase) {
        return Component.translatable("screen.oritech_space_age.path." + phase.name().toLowerCase(Locale.ROOT));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.0f", value);
    }

    private static String formatSpeed(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    /** Stage, shared-clock release time and boosters grouped at this marker. */
    record Separation(int stage, double timeSeconds, List<SpaceSimulation.SegmentRef> segments) {
    }

    /** Displayed object position in world units, with its arrival time. */
    record ArrivalPosition(double x, double y, double timeSeconds) {
    }

    /** Original sample state and interpolated speed at the hovered point. */
    record PathPoint(RocketFlightPathCalculator.PathSample sample, double speedMetersPerSecond) {
    }
}
