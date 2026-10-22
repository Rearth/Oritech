package rearth.oritech.spaceage.simulation.survey;

import net.minecraft.server.MinecraftServer;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.power.SolarPower;
import rearth.oritech.spaceage.simulation.power.SpacePowerService;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Commands and data share the same Earth coverage, extended by constellation milestones.
 */
public final class SpaceCommunications {

    private SpaceCommunications() {
    }

    public static int poweredAntennas(MissionState craft, boolean consume) {

        var account = SpacePowerService.account(craft.rocket);
        if (account != null) return account.poweredAntennas();
        var installed = craft.rocket.hardware().values().stream().mapToInt(RocketHardware::antennas).sum();
        var stored = craft.rocket.getDynamicSegments().values().stream().mapToDouble(DynamicRocketSegment::rf).sum();
        var available = (int) Math.min(installed, stored / SpaceBalance.ANTENNA_RF);
        if (consume) SolarPower.spend(craft.rocket, available * SpaceBalance.ANTENNA_RF);
        return available;
    }

    public static int coverage(UUID owner, SpaceSimulation.OrbitBand orbit, List<Node> nodes) {

        var slots = SpaceBalance.slots(orbit);
        return (int) nodes.stream().filter(n -> n.owner.equals(owner) && !n.ground && n.relay && n.antennas > 0
                && n.orbit == orbit && n.slot >= 0 && n.slot < slots).map(Node::slot).distinct().count();
    }

    public static NetworkStatus network(UUID owner, List<Node> nodes) {

        var ground = nodes.stream().filter(n -> n.owner.equals(owner) && n.ground).mapToInt(Node::antennas).sum();
        var low = coverage(owner, SpaceSimulation.OrbitBand.LOW, nodes);
        var high = coverage(owner, SpaceSimulation.OrbitBand.HIGH, nodes);
        var lowComplete = low == SpaceBalance.slots(SpaceSimulation.OrbitBand.LOW);
        var highComplete = lowComplete && high == SpaceBalance.slots(SpaceSimulation.OrbitBand.HIGH);
        return new NetworkStatus(ground, low, high,
                ground == 0 ? 0 : highComplete ? -1 : lowComplete ? SpaceBalance.LOW_COMMAND_ALTITUDE : SpaceBalance.GROUND_COMMAND_ALTITUDE);
    }

    private static double altitude(Node node) {

        return SpaceObjects.earthAltitude(node.x, node.y);
    }

    public static Connection connection(Node craft, List<Node> nodes) {

        return connection(craft, network(craft.owner, nodes));
    }

    private static Connection connection(Node craft, NetworkStatus network) {

        var connected = craft.antennas > 0 && network.groundAntennas > 0
                && (network.coverageAltitude < 0 || altitude(craft) <= network.coverageAltitude + 0.01);
        var reason = network.groundAntennas == 0 ? "communication.oritech_space_age.no_ground"
                : craft.antennas == 0 ? "communication.oritech_space_age.no_craft_antenna"
                  : !connected ? "communication.oritech_space_age.outside_coverage"
                    : "communication.oritech_space_age.connected";
        return new Connection(connected, network.coverageAltitude, altitude(craft), reason);
    }

    /**
     * Nearby powered craft may exchange observations without delivering them to Earth.
     */
    public static boolean linked(Node a, Node b) {

        return a.antennas > 0 && b.antennas > 0 && a.owner.equals(b.owner)
                && Math.hypot(a.x - b.x, a.y - b.y) <= SpaceBalance.ANTENNA_RANGE;
    }

    static boolean isRelay(MissionState craft) {

        var action = craft.action();
        return action != null && action.type() == SpaceSimulation.ActionType.MAINTAIN_POSITION
                && craft.position.target().equals(SpaceObjects.EARTH_ID)
                && SpaceBalance.hasSlots(craft.position.orbit()) && craft.position.slot() >= 0;
    }

    public static List<Node> update(MinecraftServer server, MissionSavedData data) {

        var nodes = new ArrayList<Node>();
        for (var craft : data.craft.values()) {
            craft.connected = false;
            if (craft.ended) continue;
            var antennas = poweredAntennas(craft, true);
            nodes.add(new Node(craft.rocket.getRocketId(), craft.owner, craft.position.x(), craft.position.y(), antennas,
                    craft.position.orbit(), craft.position.slot(), isRelay(craft), false));
        }
        data.stations.entrySet().removeIf(entry -> {
            var level = server.getLevel(entry.getKey().dimension());
            return level == null || !level.hasChunkAt(entry.getKey().pos()) || entry.getValue().isRemoved();
        });
        data.stations.forEach((pos, station) -> nodes.add(new Node(UUID.nameUUIDFromBytes(pos.toString().getBytes(StandardCharsets.UTF_8)),
                station.owner, SpaceObjects.EARTH_SURFACE_X, 0, station.poweredAntennas, SpaceSimulation.OrbitBand.SURFACE, -1, true, true)));
        var networks = new HashMap<UUID, NetworkStatus>();
        for (var node : nodes) {
            if (node.ground) continue;
            var craft = data.craft.get(node.id);
            craft.communication = connection(node, networks.computeIfAbsent(node.owner, owner -> network(owner, nodes)));
            craft.connected = craft.communication.connected;
            craft.route = craft.communication.reason;
        }
        data.networkNodes = List.copyOf(nodes);
        return nodes;
    }

    public record Node(UUID id, UUID owner, double x, double y, int antennas, SpaceSimulation.OrbitBand orbit,
                       int slot, boolean relay, boolean ground) {

    }

    public record NetworkStatus(int groundAntennas, int lowSlots, int highSlots, double coverageAltitude) {

        public boolean lowComplete() {

            return lowSlots == SpaceBalance.slots(SpaceSimulation.OrbitBand.LOW);
        }

        public boolean highComplete() {

            return lowComplete() && highSlots == SpaceBalance.slots(SpaceSimulation.OrbitBand.HIGH);
        }
    }

    public record Connection(boolean connected, double coverageAltitude, double altitude,
                             String reason) {

    }
}
