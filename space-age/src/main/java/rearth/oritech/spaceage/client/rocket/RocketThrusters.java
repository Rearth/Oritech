package rearth.oritech.spaceage.client.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.client.ui.render.PreviewEffect;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock.Type;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.FlightMotion;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketFlight;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Shared nozzle placement and full-power/coast decisions for world and mission-control effects. */
public final class RocketThrusters {

    private static final Identifier CHEMICAL = OritechSpaceAge.id("textures/effect/thruster_chemical_strip.png");
    private static final Identifier ION = OritechSpaceAge.id("textures/effect/thruster_ion_strip.png");

    private RocketThrusters() {
    }

    public static List<Nozzle> nozzles(ActiveRocketData rocket) {

        var result = new ArrayList<Nozzle>();
        var hardware = rocket.hardware();
        for (var segment : rocket.getStaticSegments().values()) {
            var resources = rocket.getDynamicSegments().get(segment.segmentId());
            var engines = hardware.get(segment.segmentId());
            for (var block : segment.blocks()) {
                if (!(block.state().getBlock() instanceof RocketEngineBlock engine)) continue;
                var seconds = engine.getType() == Type.ION ? engines.ionSeconds(resources) : engines.chemicalSeconds(resources);
                var origin = Vec3.atLowerCornerOf(block.relativePos()).add(engine.exhaustOffset(block.state()));
                result.add(new Nozzle(SegmentRef.of(segment), block.relativePos(), origin,
                        engine.exhaustDirection(block.state()).normalize(), engine.exhaustScale(block.state()), engine.getType(), seconds));
            }
        }
        return List.copyOf(result);
    }

    public static boolean visible(RocketFlight flight, double time) {

        return flight != null && flight.canReachOrbit() && time >= flight.launchTick() && !flight.isInSpace(time)
                && (flight.impactTick() < 0 || time < flight.impactTick())
                && (flight.takeoffCollisionTick() < 0 || time < flight.takeoffCollisionTick());
    }

    public static boolean crashing(RocketFlight flight, double time) {

        return flight.reentryTick() >= 0 && time >= flight.reentryTick() && flight.impactSpeedMetersPerSecond() > 12;
    }

    public static boolean firing(Nozzle nozzle, RocketFlight flight, double time) {

        if (!visible(flight, time) || crashing(flight, time) || nozzle.burnSeconds() <= 0) return false;
        // Physical descent is a compressed presentation of the final braking leg.
        if (flight.reentryTick() >= 0 && time >= flight.reentryTick()) return true;
        return (time - flight.launchTick()) / 20 < nozzle.burnSeconds();
    }

    public static Set<SegmentRef> firingSegments(MissionState.Telemetry telemetry, List<CraftPath> paths, double elapsedTicks) {

        if (!telemetry.status().equals(MissionState.STATUS_IN_FLIGHT) && !telemetry.status().equals(MissionState.STATUS_TRANSFER))
            return Set.of();
        var path = paths.stream().filter(candidate -> candidate.branchId().equals(telemetry.plan().root().id())).findFirst().orElse(null);
        if (path == null || path.samples().isEmpty()) return Set.of();
        var time = (telemetry.actionTicks() + elapsedTicks) / 20;
        if (time >= path.durationSeconds()) return Set.of();
        var sample = path.samples().get(FlightMotion.endIndex(path.samples(), time));
        return sample.phase() == PathPhase.COAST || sample.phase() == PathPhase.PARACHUTE ? Set.of() : sample.firingSegments();
    }

    public static boolean correctionBurn(MissionState.Telemetry telemetry, double elapsedTicks) {

        if (!telemetry.status().equals(MissionState.STATUS_MAINTAINING_POSITION) || telemetry.power().solar() > 0) return false;
        var performance = RocketPerformanceCalculator.calculate(telemetry.rocket());
        var duty = SpaceBalance.stationKeepingBurnSecondsPerSecond(performance.thrustNewtons(), performance.wetMassKilograms());
        // The simulation accounts for sparse full-power corrections as an average rate.
        return (telemetry.actionTicks() + elapsedTicks) % 100 < duty * 100;
    }

    public static PreviewEffect effect(Nozzle nozzle, double time) {

        var ion = nozzle.type() == Type.ION;
        var phase = time * 2.7 + Math.floorMod(nozzle.block().asLong(), 41);
        var flicker = (float) (1 + .07 * Math.sin(phase));
        return new PreviewEffect(ion ? ION : CHEMICAL, nozzle.origin(), nozzle.direction(),
                nozzle.scale() * (ion ? .65f : 1.1f) * flicker,
                nozzle.scale() * (ion ? 2.8f : 3.6f) * flicker, Math.floorMod((int) phase, 4), 4);
    }

    public record Nozzle(SegmentRef segment, BlockPos block, Vec3 origin, Vec3 direction, float scale, Type type, double burnSeconds) {
    }
}
