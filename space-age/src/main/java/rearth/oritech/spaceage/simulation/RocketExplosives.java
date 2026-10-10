package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.init.SpaceAgeDataMaps;

import java.util.Collection;
import java.util.Comparator;

/**
 * Payload values are captured at launch, so compiled legs survive reloads consistently.
 */
public final class RocketExplosives {

    private RocketExplosives() {
    }

    public static SpaceSimulation.SpaceObjectData detonationTarget(Collection<SpaceSimulation.SpaceObjectData> objects, double x, double y) {

        return objects.stream().filter(o -> o.type() == SpaceObjects.ObjectType.ASTEROID
                        && Math.hypot(o.x() - x, o.y() - y) <= o.radius() + 1_000)
                .min(Comparator.comparingDouble(o -> Math.hypot(o.x() - x, o.y() - y))).orElse(null);
    }

    public static double installed(StaticRocketSegment segment) {

        return segment.blocks().stream().mapToDouble(b -> {
            var value = b.state().getBlock().builtInRegistryHolder().getData(SpaceAgeDataMaps.ROCKET_EXPLOSIVES);
            return value == null ? 0 : value;
        }).sum();
    }

    public static double remaining(ActiveRocketData rocket) {

        return rocket.getStaticSegments().entrySet().stream().mapToDouble(e ->
                e.getValue().payloadEnergyJoules() + rocket.getDynamicSegments().get(e.getKey()).availableFuelBurnTimeTicks
                        * SpaceBalance.FUEL_ENERGY_PER_BURN_TICK).sum();
    }

    public static float worldStrength(double totalEnergy) {

        return (float) Math.clamp(4 * Math.cbrt(Math.max(0, totalEnergy) / SpaceBalance.TNT_ENERGY_JOULES), 0, 32);
    }
}
