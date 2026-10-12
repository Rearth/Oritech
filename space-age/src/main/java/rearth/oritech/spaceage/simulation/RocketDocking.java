package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Docking;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Station-and-visitors links, exposed ports and frozen rendezvous checks.
 */
public final class RocketDocking {

    private RocketDocking() {
    }

    public static List<HardwareRef> ports(ActiveRocketData rocket) {

        var result = new ArrayList<HardwareRef>();
        rocket.getStaticSegments().values().forEach(s -> s.blocks().stream()
                .filter(b -> b.state().is(SpaceAgeBlocks.ROCKET_COUPLING.get()))
                .forEach(b -> result.add(new HardwareRef(SegmentRef.of(s), b.relativePos()))));
        result.sort(Comparator.comparingLong(r -> r.position().asLong()));
        return List.copyOf(result);
    }

    public static boolean stationary(MissionState state) {

        return !state.ended && Math.hypot(state.position.vx(), state.position.vy()) <= .001
                && (state.action() == null || state.action().type() != ActionType.NAVIGATE_TO);
    }

    public static boolean linked(MissionSavedData data, UUID id) {

        return data.dockingLinks.stream().anyMatch(l -> l.host().equals(id) || l.visitor().equals(id));
    }

    public static DockingLink visitorLink(MissionSavedData data, UUID id) {

        return data.dockingLinks.stream().filter(l -> l.visitor().equals(id)).findFirst().orElse(null);
    }

    public static boolean occupied(MissionSavedData data, UUID id, HardwareRef port) {

        return data.dockingLinks.stream().anyMatch(l -> l.host().equals(id) && l.hostPort().equals(port)
                || l.visitor().equals(id) && l.visitorPort().equals(port));
    }

    public static List<DockingTarget> targets(MissionSavedData data, UUID owner) {

        return data.craft.values().stream().filter(m -> m.owner.equals(owner) && stationary(m)
                        && visitorLink(data, m.rocket.getRocketId()) == null)
                .map(m -> new DockingTarget(m.rocket.getRocketId(), m.plan.name().isBlank() ? "Station " + m.rocket.getRocketId().toString().substring(0, 8) : m.plan.name(),
                        MissionState.copyRocket(m.rocket), m.position, m.positionRevision,
                        ports(m.rocket).stream().filter(p -> !occupied(data, m.rocket.getRocketId(), p)).toList(),
                        data.dockingLinks.stream().filter(l -> l.host().equals(m.rocket.getRocketId())).map(DockingLink::visitor).toList(),
                        data.dockingLinks.stream().filter(l -> l.host().equals(m.rocket.getRocketId())).toList(),
                        mass(data, m) - RocketPerformanceCalculator.calculate(m.rocket).wetMassKilograms()))
                .toList();
    }

    /**
     * The named first segment retains the craft ID; every occupied port must remain with it.
     */
    static boolean preservesPorts(MissionSavedData data, MissionState state, FlightPlanAction action) {

        if (action.segments().size() != 2) return true; // Releasing an asteroid leaves the rocket ports intact.
        var graph = new HashMap<SegmentRef, Set<SegmentRef>>();
        var refs = new HashMap<UUID, SegmentRef>();
        state.rocket.getStaticSegments().forEach((id, segment) -> refs.put(id, SegmentRef.of(segment)));
        state.rocket.getDynamicSegments().forEach((id, resources) -> graph.put(refs.get(id), resources.getConnectedSegments().stream().map(refs::get).collect(Collectors.toSet())));
        var retained = retainedAfterCut(graph, action.segments().get(0), action.segments().get(1));
        var id = state.rocket.getRocketId();
        return data.dockingLinks.stream().allMatch(link ->
                (!link.host().equals(id) || retained.contains(link.hostPort().segment()))
                        && (!link.visitor().equals(id) || retained.contains(link.visitorPort().segment())));
    }

    static Set<SegmentRef> retainedAfterCut(Map<SegmentRef, Set<SegmentRef>> graph, SegmentRef retained, SegmentRef detached) {

        var result = new HashSet<SegmentRef>();
        var open = new ArrayDeque<SegmentRef>();
        open.add(retained);
        while (!open.isEmpty()) {
            var ref = open.removeFirst();
            if (!result.add(ref)) continue;
            for (var next : graph.getOrDefault(ref, Set.of())) {
                if (ref.equals(retained) && next.equals(detached) || ref.equals(detached) && next.equals(retained))
                    continue;
                if (!result.contains(next)) open.add(next);
            }
        }
        return result;
    }

    public static String check(MissionSavedData data, MissionState visitor, Docking docking) {

        var issue = rendezvousIssue(data, visitor, docking);
        if (!issue.isEmpty()) return issue;
        var host = data.craft.get(docking.host());
        if (visitorLink(data, docking.host()) != null || linked(data, visitor.rocket.getRocketId()))
            return "dock_already_linked";
        if (!ports(visitor.rocket).contains(docking.localPort()) || !ports(host.rocket).contains(docking.hostPort()))
            return "dock_port_missing";
        if (occupied(data, docking.host(), docking.hostPort()) || occupied(data, visitor.rocket.getRocketId(), docking.localPort()))
            return "dock_port_occupied";
        return "";
    }

    public static String rendezvousIssue(MissionSavedData data, MissionState visitor, Docking docking) {

        var host = data.craft.get(docking.host());
        if (host == null || host.ended || !host.owner.equals(visitor.owner) || host == visitor)
            return "dock_target_missing";
        if (!stationary(host) || host.positionRevision != docking.revision()
                || Math.hypot(host.position.x() - docking.x(), host.position.y() - docking.y()) > .001)
            return "dock_target_moved";
        return "";
    }

    public static boolean atRendezvous(double x, double y, double vx, double vy, Docking docking) {

        return Math.hypot(x - docking.x(), y - docking.y()) <= 1 && Math.hypot(vx, vy) <= .01;
    }

    public static boolean connect(MissionSavedData data, MissionState visitor, Docking docking) {

        var issue = check(data, visitor, docking);
        if (!issue.isEmpty()) {
            visitor.status = "status.oritech_space_age." + issue;
            return false;
        }
        if (!atRendezvous(visitor.position.x(), visitor.position.y(), visitor.position.vx(), visitor.position.vy(), docking)) {
            visitor.status = "status.oritech_space_age.dock_approach_required";
            return false;
        }
        data.dockingLinks.add(new DockingLink(docking.host(), visitor.rocket.getRocketId(), docking.hostPort(), docking.localPort()));
        visitor.position = fixed(visitor.position, data.craft.get(docking.host()).position);
        var result = RocketCargoTransfer.exchange(visitor.rocket, data.craft.get(docking.host()).rocket, visitor.action().settings().exchange());
        RocketProcessingService.passStorage(visitor.rocket).lastExchange = result;
        visitor.complete();
        visitor.serviceProgress = result.moved();
        visitor.status = result.remaining() > 0 ? "status.oritech_space_age.cargo_leftovers" : "status.oritech_space_age.docked";
        return true;
    }

    public static boolean undock(MissionSavedData data, MissionState visitor) {

        var link = visitorLink(data, visitor.rocket.getRocketId());
        if (link == null) {
            visitor.status = "status.oritech_space_age.not_docked";
            return false;
        }
        if (RocketProcessingService.remoteBusy(data, visitor.rocket.getRocketId())) {
            visitor.status = "status.oritech_space_age.cargo_working";
            return false;
        }
        var result = RocketCargoTransfer.exchange(visitor.rocket, data.craft.get(link.host()).rocket, visitor.action().settings().exchange());
        RocketProcessingService.passStorage(visitor.rocket).lastExchange = result;
        data.dockingLinks.remove(link);
        visitor.positionRevision++;
        visitor.complete();
        visitor.serviceProgress = result.moved();
        visitor.status = result.remaining() > 0 ? "status.oritech_space_age.cargo_leftovers" : "status.oritech_space_age.undocked";
        return true;
    }

    public static MissionState serviceHost(MissionSavedData data, MissionState visitor, UUID hostId) {

        var link = visitorLink(data, visitor.rocket.getRocketId());
        return link != null && link.host().equals(hostId) ? data.craft.get(hostId) : null;
    }

    public static double mass(MissionSavedData data, MissionState host) {

        return RocketPerformanceCalculator.calculate(host.rocket).wetMassKilograms() + data.dockingLinks.stream()
                .filter(l -> l.host().equals(host.rocket.getRocketId())).map(l -> data.craft.get(l.visitor())).filter(Objects::nonNull)
                .mapToDouble(m -> RocketPerformanceCalculator.calculate(m.rocket).wetMassKilograms()).sum();
    }

    static void synchronize(MissionSavedData data) {

        data.dockingLinks.removeIf(l -> !data.craft.containsKey(l.host()) || !data.craft.containsKey(l.visitor())
                || data.craft.get(l.host()).ended || data.craft.get(l.visitor()).ended);
        for (var link : data.dockingLinks) {
            var visitor = data.craft.get(link.visitor());
            visitor.position = fixed(visitor.position, data.craft.get(link.host()).position);
        }
    }

    private static MissionState.Position fixed(MissionState.Position visitor, MissionState.Position host) {

        return new MissionState.Position(host.x(), host.y(), 0, 0, host.target(), host.orbit(), -1, visitor.stage(),
                visitor.asteroid(), visitor.anchor(), visitor.headingX(), visitor.headingY());
    }

}
