package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;

import java.util.*;

/** Slots describe stationary destinations, rather than propagated physical orbits. */
final class SunOrbitRules {

    private SunOrbitRules() {
    }

    static void validate(MissionSavedData data) {

        var occupied = new HashMap<UUID, Set<Integer>>();
        for (var state : data.craft.values().stream().sorted(Comparator.comparing(s -> s.rocket.getRocketId())).toList()) {
            if (state.ended || !state.position.target().equals(SpaceSimulation.SUN_ID)
                    || state.action() != null && state.action().type() == ActionType.NAVIGATE_TO) continue;
            if (SolarPower.output(state) <= 0) {
                release(state, "status.oritech_space_age.solar_lost");
                data.dockingLinks.removeIf(link -> link.host().equals(state.rocket.getRocketId()) || link.visitor().equals(state.rocket.getRocketId()));
                continue;
            }
            if (RocketDocking.visitorLink(data, state.rocket.getRocketId()) != null) continue;
            var used = occupied.computeIfAbsent(state.owner, ignored -> new HashSet<>());
            var slot = state.position.slot();
            if (slot < 0 || used.contains(slot)) {
                slot = 0;
                while (used.contains(slot)) slot++;
                if (slot >= SpaceBalance.slots(SpaceSimulation.SUN_ID, OrbitBand.SOLAR)) {
                    release(state, "status.oritech_space_age.orbit_full");
                    continue;
                }
                var p = state.position;
                var angle = Math.PI + slot * Math.PI * 2 / SpaceBalance.slots(SpaceSimulation.SUN_ID, OrbitBand.SOLAR);
                state.position = new MissionState.Position(Math.cos(angle) * SolarPower.MIN_DISTANCE, Math.sin(angle) * SolarPower.MIN_DISTANCE,
                        0, 0, SpaceSimulation.SUN_ID, OrbitBand.SOLAR, slot, p.stage(), p.asteroid(), p.anchor(), p.headingX(), p.headingY());
            }
            used.add(slot);
        }
    }

    private static void release(MissionState state, String reason) {

        var p = state.position;
        state.position = new MissionState.Position(p.x(), p.y(), 0, 0, FlightPlanAction.NO_TARGET,
                OrbitBand.TIGHT, -1, p.stage(), p.asteroid(), p.anchor(), p.headingX(), p.headingY());
        if (state.action() != null) state.complete();
        state.status = reason;
    }
}
