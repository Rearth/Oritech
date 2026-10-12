package rearth.oritech.spaceage.simulation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Runs service cards on copied rocket state, including physical recipe cells and resource pools.
 */
final class RocketServiceForecast {

    private RocketServiceForecast() {
    }

    static boolean process(FlightPlanAction action, Craft craft, Context context) {

        var settings = action.settings().processing();
        var target = settings.host().equals(FlightPlanAction.NO_TARGET) ? craft
                : settings.host().equals(craft.dockedHost) ? context.stationCraft.get(settings.host()) : null;
        if (target == null || settings.modules().isEmpty()) return processIssue(action, context, "module_missing");
        if (!VacuumProcessing.zeroG(craft.x, craft.y, context.objects.values()))
            return processIssue(action, context, "requires_zero_g");
        var visitor = snapshot(craft);
        var machines = target == craft ? visitor : snapshot(target);
        var pass = RocketProcessingService.passStorage(visitor).pass;
        var partner = context.stationCraft.get(craft.dockedHost);
        var pending = pass != null && pass.action().equals(action.id()) ? new ArrayList<>(pass.pending())
                : new ArrayList<>(PhysicalCrafting.candidates(visitor, machines, partner == null ? null : snapshot(partner), settings, context.recipes));
        var existing = machines.getDynamicSegments().values().stream().flatMap(r -> r.crafters.values().stream())
                .anyMatch(m -> m.job != null && m.job.operator().equals(craft.craftId));
        if (pending.isEmpty() && !existing) return processIssue(action, context, "no_valid_input_pair");
        var assignments = new ArrayList<RocketFlightPathCalculator.WorkAssignment>();
        var skipped = new ArrayList<String>();
        long totalRF = 0;
        var elapsed = 0;
        var products = new LinkedHashSet<Identifier>();
        var timedOut = false;

        // Jump to the next machine completion instead of simulating every recipe tick.
        while (!pending.isEmpty() || settings.modules().stream().anyMatch(ref -> {
            var m = RocketProcessingService.controller(machines, ref);
            return m != null && m.job != null && m.job.operator().equals(craft.craftId);
        })) {
            var startIssue = "";
            for (var ref : settings.modules()) {
                var machine = RocketProcessingService.controller(machines, ref);
                if (machine == null) return processIssue(action, context, "module_missing");
                if (machine.job != null) continue;
                var local = RocketLayout.pair(machines, ref);
                var pair = pending.stream().filter(p -> PhysicalCrafting.overlap(p, local)).findFirst().orElseGet(() ->
                        pending.stream().filter(p -> PhysicalCrafting.canUse(machines, ref, p)).findFirst().orElse(null));
                if (pair == null) continue;
                var source = pair.rocket().equals(visitor.getRocketId()) ? visitor : machines;
                if (target == craft && partner != null && pair.rocket().equals(partner.craftId))
                    source = snapshot(partner);
                var issue = PhysicalCrafting.start(machines, ref, source, pair, context.recipes, craft.craftId);
                if (source != visitor && source != machines) apply(partner, source);
                if (issue.isEmpty()) assignments.add(new RocketFlightPathCalculator.WorkAssignment(ref, pair));
                else {
                    startIssue = issue;
                    skipped.add(issue + " (" + pair.controller().position().toShortString() + ")");
                }
                if (!issue.equals("module_busy")) pending.remove(pair);
            }
            var jobs = settings.modules().stream().map(ref -> RocketProcessingService.controller(machines, ref))
                    .filter(m -> m.job != null && m.job.operator().equals(craft.craftId)).toList();
            if (jobs.isEmpty()) return processIssue(action, context, startIssue.isEmpty() ? "module_busy" : startIssue);
            var ticks = jobs.stream().mapToInt(m -> m.job.duration() - m.job.elapsed()).min().orElse(0);
            if (action.service().timeoutTicks() > 0) ticks = Math.min(ticks, action.service().timeoutTicks() - elapsed);
            var rate = jobs.stream().mapToLong(m -> m.job.rfPerTick()).sum();
            var available = machines.getDynamicSegments().values().stream().mapToLong(r -> r.availableRF).sum();
            if (rate > 0) ticks = (int) Math.min(ticks, Math.max(1, available / rate));
            if (ticks <= 0) {
                if (action.service().timeoutTicks() > 0) {
                    elapsed = action.service().timeoutTicks();
                    timedOut = true;
                    break;
                }
                apply(craft, visitor);
                if (target != craft) apply(target, machines);
                craft.blockedState = RocketFlightPathCalculator.TerminalState.NOT_ENOUGH_SERVICE_RF;
                return processIssue(action, context, "processing_no_power");
            }
            var progressed = false;
            for (var ref : settings.modules()) {
                var machine = RocketProcessingService.controller(machines, ref);
                var job = machine.job;
                if (job == null || !job.operator().equals(craft.craftId)) continue;
                var source = job.source().rocket().equals(visitor.getRocketId()) ? visitor : machines;
                if (target == craft && partner != null && job.source().rocket().equals(partner.craftId))
                    source = snapshot(partner);
                var worked = PhysicalCrafting.work(machines, ref, source, ticks);
                totalRF += (long) worked * job.rfPerTick();
                progressed |= worked > 0;
                if (machine.job == null)
                    products.add(BuiltInRegistries.ITEM.getKey(job.result().getBlock().asItem()));
                if (source != visitor && source != machines) apply(partner, source);
            }
            if (!progressed) {
                if (action.service().timeoutTicks() > 0) {
                    elapsed = action.service().timeoutTicks();
                    timedOut = true;
                    break;
                }
                apply(craft, visitor);
                if (target != craft) apply(target, machines);
                craft.blockedState = RocketFlightPathCalculator.TerminalState.NOT_ENOUGH_SERVICE_RF;
                return processIssue(action, context, "processing_no_power");
            }
            elapsed += ticks;
            if (action.service().timeoutTicks() > 0 && elapsed >= action.service().timeoutTicks()) {
                timedOut = true;
                break;
            }
        }
        apply(craft, visitor);
        if (target != craft) apply(target, machines);
        if (!VacuumProcessing.zeroGForDuration(craft.x, craft.y, craft.velocityX, craft.velocityY, elapsed, context.objects.values()))
            return processIssue(action, context, "requires_zero_g");
        if (!upkeep(craft, context, elapsed)) return processIssue(action, context, "station_keeping_exhausted");
        context.processingEstimates.add(new RocketFlightPathCalculator.ProcessingEstimate(action.id(), List.copyOf(products), elapsed, totalRF,
                (long) target.segments.values().stream().mapToDouble(seg -> seg.rf).sum(), timedOut ? "processing_timed_out" : "", target != craft, assignments, skipped));
        craft.x += craft.velocityX * elapsed / 20;
        craft.y += craft.velocityY * elapsed / 20;
        craft.time += elapsed / 20.0;
        craft.addSample(RocketFlightPathCalculator.PathPhase.COAST, craft.currentTarget, action.id());
        return true;
    }

    private static ActiveRocketData snapshot(Craft craft) {

        // Keep launch topology as the baseline; the copy owns all mutable layouts and jobs.
        var structures = new HashMap<UUID, StaticRocketSegment>();
        var resources = new HashMap<UUID, DynamicRocketSegment>();
        craft.segments.values().forEach(segment -> {
            var r = segment.physical.copy();
            r.availableRF = (long) segment.rf;
            r.availableFuelBurnTimeTicks = (long) segment.fuelTicks;
            r.currentFuelWeight = segment.fuelMass / RocketPerformanceCalculator.KILOGRAMS_PER_WEIGHT_UNIT;
            r.crafters.clear();
            segment.crafters.forEach((pos, c) -> r.crafters.put(pos, c.copy()));
            structures.put(segment.structure.segmentId(), segment.structure);
            resources.put(segment.structure.segmentId(), r);
        });
        return new ActiveRocketData(craft.craftId, structures, resources, null);
    }

    private static void apply(Craft craft, ActiveRocketData rocket) {

        // Products change mass and explosives as well as resources for the following navigation card.
        craft.segments.forEach((ref, segment) -> {
            var id = RocketLayout.segmentId(rocket, ref);
            var r = rocket.getDynamicSegments().get(id);
            segment.physical = r.copy();
            segment.crafters.clear();
            r.crafters.forEach((pos, c) -> segment.crafters.put(pos, c.copy()));
            segment.rf = r.availableRF;
            segment.fuelTicks = r.availableFuelBurnTimeTicks;
            segment.fuelMass = r.currentFuelWeight * RocketPerformanceCalculator.KILOGRAMS_PER_WEIGHT_UNIT;
            segment.chemicalSeconds = segment.hardware.chemicalSeconds(r);
            segment.ionSeconds = segment.hardware.ionSeconds(r);
            var current = rocket.getStaticSegments().get(id);
            segment.dryMass = current.staticWeight() * RocketPerformanceCalculator.KILOGRAMS_PER_WEIGHT_UNIT;
            segment.payloadEnergy = current.payloadEnergyJoules();
            segment.wetMass = segment.dryMass + segment.fuelMass;
        });
    }

    private static boolean processIssue(FlightPlanAction action, Context context, String issue) {

        context.processingEstimates.add(new RocketFlightPathCalculator.ProcessingEstimate(action.id(), List.of(), 0, 0, 0, issue,
                !action.settings().processing().host().equals(FlightPlanAction.NO_TARGET)));
        return false;
    }

    static boolean dock(FlightPlanAction action, Craft craft, Context context) {

        var docking = action.settings().docking();
        var target = context.stations.get(docking.host());
        if (target == null || target.id().equals(craft.craftId) || !craft.dockedHost.equals(FlightPlanAction.NO_TARGET)
                || target.revision() != docking.revision() || !target.ports().contains(docking.hostPort())
                || context.claimedPorts.getOrDefault(docking.host(), Set.of()).contains(docking.hostPort()) || !craft.segments.containsKey(docking.localPort().segment())
                || Math.hypot(target.position().x() - docking.x(), target.position().y() - docking.y()) > .001)
            return false;
        // Docking only attaches ports; a preceding Navigate To owns the approach and its fuel cost.
        if (!craft.currentTarget.equals(target.id())
                || !RocketDocking.atRendezvous(craft.x, craft.y, craft.velocityX, craft.velocityY, docking))
            return false;
        var host = context.stationCraft.get(target.id());
        // A depleted station still holds its coordinate and can receive a rescue supply.
        StationServiceRules.forecast(host, Math.max(0, craft.time - host.time) * 20);
        host.time = craft.time;
        craft.x = docking.x();
        craft.y = docking.y();
        craft.dockedHost = target.id();
        craft.dockedHostPort = docking.hostPort();
        craft.occupiedPortSegments.add(docking.localPort().segment());
        context.claimedPorts.computeIfAbsent(docking.host(), ignored -> new HashSet<>()).add(docking.hostPort());
        exchange(action, craft, host);
        return true;
    }

    static boolean undock(FlightPlanAction action, Craft craft, Context context) {

        if (craft.dockedHost.equals(FlightPlanAction.NO_TARGET)) return false;
        var host = context.stationCraft.get(craft.dockedHost);
        if (host == null) return false;
        if (host.segments.values().stream().flatMap(seg -> seg.crafters.values().stream()).anyMatch(m -> m.job != null && m.job.source().rocket().equals(craft.craftId)))
            return false;
        exchange(action, craft, host);
        var claimed = context.claimedPorts.get(craft.dockedHost);
        if (claimed != null) claimed.remove(craft.dockedHostPort);
        craft.dockedHost = FlightPlanAction.NO_TARGET;
        craft.dockedHostPort = RocketServiceSettings.HardwareRef.NONE;
        craft.occupiedPortSegments.clear();
        return true;
    }

    private static void exchange(FlightPlanAction action, Craft craft, Craft host) {

        var visitor = snapshot(craft);
        var station = snapshot(host);
        RocketCargoTransfer.exchange(visitor, station, action.settings().exchange());
        apply(craft, visitor);
        apply(host, station);
    }

    private static boolean upkeep(Craft craft, Context context, double ticks) {

        if (ticks <= 0) return true;
        if (!craft.dockedHost.equals(FlightPlanAction.NO_TARGET)) {
            var host = context.stationCraft.get(craft.dockedHost);
            if (host == null || !StationServiceRules.forecast(host, ticks, host.mass() + craft.mass())) return false;
            craft.segments.values().forEach(s -> s.spendRF(StationServiceRules.antennaRF(s.hardware, ticks)));
            host.time += ticks / 20;
            return true;
        }
        if (Math.hypot(craft.velocityX, craft.velocityY) <= .001) return StationServiceRules.forecast(craft, ticks);
        craft.segments.values().forEach(s -> s.spendRF(StationServiceRules.antennaRF(s.hardware, ticks)));
        return true;
    }
}
