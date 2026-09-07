package rearth.oritech.spaceage.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared planner text and validation, kept apart from screen layout. */
final class FlightPlannerLabels {
    private FlightPlannerLabels() { }

    static Component actionName(SpaceSimulation.ActionType type) {
        return Component.translatable("screen.oritech_space_age.action." + type.name().toLowerCase(Locale.ROOT));
    }

    static Component actionVelocity(SpaceSimulation.FlightPlanAction action) {
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) {
            return Component.translatable("screen.oritech_space_age.action.no_velocity");
        }
        return switch (action.velocityMode()) {
            case ZERO -> Component.translatable("screen.oritech_space_age.action.velocity_zero");
            case MAXIMUM -> Component.translatable("screen.oritech_space_age.action.velocity_maximum");
            case CUSTOM -> Component.translatable("screen.oritech_space_age.action.velocity_custom", action.targetVelocity());
        };
    }

    static boolean isEarthSurface(SpaceSimulation.FlightPlanAction action) {
        return action != null && action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                && action.targetId().equals(SpaceObjects.EARTH_ID)
                && action.orbit() == SpaceSimulation.OrbitBand.SURFACE;
    }

    static Component addonTypeName(SpaceSimulation.ActionAddonType type) {
        return Component.translatable("screen.oritech_space_age.action.condition." + type.name().toLowerCase(Locale.ROOT));
    }

    static Component addonUnit(SpaceSimulation.ActionAddonType type) {
        return Component.translatable("screen.oritech_space_age.action.condition_unit." + type.name().toLowerCase(Locale.ROOT));
    }

    static Component addonSummary(SpaceSimulation.ActionAddon addon) {
        return Component.translatable("screen.oritech_space_age.action.condition_summary."
                + addon.type().name().toLowerCase(Locale.ROOT), addon.value());
    }

    static String formatAddonValue(SpaceSimulation.ActionAddonType type, double value) {
        return switch (type) {
            case DISTANCE_FROM_TARGET -> String.format(Locale.ROOT, "%.0f m", value);
            case TIME_BEFORE_ARRIVAL -> String.format(Locale.ROOT, "%.0f s", value);
            case DESIRED_UNCERTAINTY -> String.format(Locale.ROOT, "±%.0f blocks", value);
        };
    }

    static Integer parseSpeedLimit(String text) {
        var value = text.strip().toLowerCase(Locale.ROOT);
        if (value.equals("max") || value.equals("maximum")) return 0;
        if (value.endsWith("m/s")) value = value.substring(0, value.length() - 3).strip();
        try {
            var speed = Integer.parseInt(value);
            return speed > 0 && speed <= 100_000 ? speed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static Integer parseArrivalVelocity(String text) {
        var value = text.strip().toLowerCase(Locale.ROOT);
        if (value.endsWith("m/s")) value = value.substring(0, value.length() - 3).strip();
        try {
            var speed = Integer.parseInt(value);
            return speed >= 0 && speed <= 100_000 ? speed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static Integer parseAddonValue(SpaceSimulation.ActionAddonType type, String value) {
        try {
            int result = Integer.parseInt(value.strip());
            return result > 0 && rearth.oritech.spaceage.simulation.RocketFlightPlanRules.clampAddonValue(type, result) == result
                    ? result : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static List<Component> arrivalTooltip(AsteroidImpactRules.ImpactPrediction prediction) {
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("screen.oritech_space_age.arrival_outcome."
                + prediction.outcome().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD));
        lines.add(Component.translatable("screen.oritech_space_age.arrival_relative_speed",
                String.format(Locale.ROOT, "%.1f", prediction.relativeSpeedMetersPerSecond())));
        lines.add(Component.translatable("screen.oritech_space_age.arrival_position", prediction.landingX(), prediction.landingZ()));
        if (prediction.craterRadiusBlocks() > 0) lines.add(Component.translatable("screen.oritech_space_age.arrival_crater", prediction.craterRadiusBlocks()));
        if (prediction.fragmentCount() > 0) {
            lines.add(Component.translatable("screen.oritech_space_age.arrival_fragments."
                    + prediction.fragmentationMode().name().toLowerCase(Locale.ROOT), prediction.fragmentCount()));
            for (int index = 0; index < prediction.fragments().size(); index++) {
                var fragment = prediction.fragments().get(index);
                lines.add(Component.translatable("screen.oritech_space_age.arrival_fragment", index + 1,
                        String.format(Locale.ROOT, "%.0f", fragment.mass() * 1_000), String.format(Locale.ROOT, "%.0f", fragment.radius())));
            }
            if (prediction.remainingTargetMass() > 0) {
                lines.add(Component.translatable("screen.oritech_space_age.arrival_remaining_asteroid",
                        String.format(Locale.ROOT, "%.0f", prediction.remainingTargetMass() * 1_000)));
            }
        }
        if (!prediction.recoverableMaterials().isEmpty()) {
            lines.add(Component.translatable("screen.oritech_space_age.arrival_recoverable"));
            prediction.recoverableMaterials().forEach(material -> lines.add(Component.literal("• " + material.block() + " × " + material.amount())));
        }
        return lines;
    }

    static List<Component> releasedAsteroidTooltip(RocketFlightPathCalculator.AsteroidPath path) {
        if (path.earthImpact() == null) {
            return List.of(Component.translatable("screen.oritech_space_age.released_asteroid_miss"),
                    Component.translatable("screen.oritech_space_age.action.predicted_uncertainty",
                            path.landingUncertaintyBlocks()));
        }
        var lines = new ArrayList<>(arrivalTooltip(path.earthImpact()));
        lines.add(Component.translatable("screen.oritech_space_age.action.predicted_uncertainty", path.landingUncertaintyBlocks()));
        return lines;
    }
}
