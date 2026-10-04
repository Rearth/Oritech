package rearth.oritech.spaceage.simulation;

import java.util.*;
import net.minecraft.server.MinecraftServer;

/** Earth broadcasts follow constellation milestones; craft uploads follow directional antenna range. */
public final class SpaceCommunications {
    public record Node(UUID id, UUID owner, double x, double y, int antennas, SpaceSimulation.OrbitBand orbit,
                       int slot, boolean relay, boolean ground) { }
    public record NetworkStatus(int groundAntennas, int lowSlots, int highSlots, double commandAltitude,
                                double receiverAltitude, double receiverGain) {
        public boolean lowComplete() { return lowSlots == SpaceBalance.slots(SpaceSimulation.OrbitBand.LOW); }
        public boolean highComplete() { return lowComplete() && highSlots == SpaceBalance.slots(SpaceSimulation.OrbitBand.HIGH); }
    }
    public record Connection(boolean receiveCommands, boolean upload, double uploadRange, double receiverDistance, String reason) { }

    public static int poweredAntennas(MissionState craft, boolean consume) {
        int count = 0;
        for (var entry : craft.rocket.getStaticSegments().entrySet()) {
            var hardware = RocketHardware.of(entry.getValue());
            var resources = craft.rocket.getDynamicSegments().get(entry.getKey());
            var available = (int) Math.min(hardware.antennas(), resources.availableRF / SpaceBalance.ANTENNA_RF);
            if (consume) resources.availableRF -= available * SpaceBalance.ANTENNA_RF;
            count += available;
        }
        return count;
    }

    public static double uploadRange(int antennas) {
        return antennas <= 0 ? 0 : Math.scalb(SpaceBalance.ANTENNA_RANGE, Math.min(30, antennas - 1));
    }

    public static int coverage(UUID owner, SpaceSimulation.OrbitBand orbit, List<Node> nodes) {
        int slots = SpaceBalance.slots(orbit);
        return (int) nodes.stream().filter(n -> n.owner.equals(owner) && !n.ground && n.relay && n.antennas > 0
                && n.orbit == orbit && n.slot >= 0 && n.slot < slots).map(Node::slot).distinct().count();
    }

    public static NetworkStatus network(UUID owner, List<Node> nodes) {
        int ground = nodes.stream().filter(n -> n.owner.equals(owner) && n.ground).mapToInt(Node::antennas).sum();
        int low = coverage(owner, SpaceSimulation.OrbitBand.LOW, nodes);
        int high = coverage(owner, SpaceSimulation.OrbitBand.HIGH, nodes);
        boolean lowComplete = low == SpaceBalance.slots(SpaceSimulation.OrbitBand.LOW);
        boolean highComplete = lowComplete && high == SpaceBalance.slots(SpaceSimulation.OrbitBand.HIGH);
        return new NetworkStatus(ground, low, high,
                ground == 0 ? 0 : highComplete ? -1 : lowComplete ? SpaceBalance.LOW_COMMAND_ALTITUDE : SpaceBalance.GROUND_COMMAND_ALTITUDE,
                highComplete ? 60_000 : lowComplete ? 10_000 : 0,
                highComplete ? 4 : lowComplete ? 2 : 1);
    }

    private static double altitude(Node node) { return Math.max(0, Math.hypot(node.x + 3_000_000, node.y) - 60_000); }

    private static boolean canSend(Node from, Node to, NetworkStatus network) {
        if (from.antennas == 0 || to.antennas == 0 || !from.owner.equals(to.owner)) return false;
        if (to.ground) return Math.max(0, altitude(from) - network.receiverAltitude) <= uploadRange(from.antennas) * network.receiverGain;
        return Math.hypot(from.x - to.x, from.y - to.y) <= uploadRange(from.antennas);
    }

    /** Upload paths run toward Earth. Station-keeping craft in Earth relay slots can forward uploads. */
    public static Map<UUID, List<UUID>> routes(List<Node> nodes) {
        var result = new HashMap<UUID, List<UUID>>();
        var queue = new ArrayDeque<Node>();
        for (var node : nodes) if (node.ground && node.antennas > 0) { result.put(node.id, List.of(node.id)); queue.add(node); }
        while (!queue.isEmpty()) {
            var receiver = queue.removeFirst();
            var network = network(receiver.owner, nodes);
            for (var sender : nodes) {
                if (result.containsKey(sender.id) || !canSend(sender, receiver, network)) continue;
                var route = new ArrayList<>(result.get(receiver.id)); route.add(sender.id); result.put(sender.id, List.copyOf(route));
                if (sender.relay) queue.add(sender);
            }
        }
        return result;
    }

    public static Connection connection(Node craft, List<Node> nodes, Map<UUID, List<UUID>> uploads) {
        var network = network(craft.owner, nodes);
        boolean receive = craft.antennas > 0 && network.groundAntennas > 0
                && (network.commandAltitude < 0 || altitude(craft) <= network.commandAltitude + 0.01);
        boolean upload = craft.antennas > 0 && uploads.containsKey(craft.id);
        double distance = Math.max(0, altitude(craft) - network.receiverAltitude);
        double range = uploadRange(craft.antennas) * network.receiverGain;
        var reason = network.groundAntennas == 0 ? "communication.oritech_space_age.no_ground"
                : craft.antennas == 0 ? "communication.oritech_space_age.no_craft_antenna"
                : !receive && !upload ? "communication.oritech_space_age.outside_both"
                : !receive ? "communication.oritech_space_age.upload_only"
                : !upload ? "communication.oritech_space_age.commands_only"
                : "communication.oritech_space_age.connected";
        return new Connection(receive, upload, range, distance, reason);
    }

    /** Nearby craft may exchange observations in both directions without delivering them to Earth. */
    public static boolean linked(Node a, Node b, List<Node> nodes) {
        var network = network(a.owner, nodes);
        return canSend(a, b, network) && canSend(b, a, network);
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
            craft.connected = false; craft.canTransmit = false;
            if (craft.ended) continue;
            var antennas = poweredAntennas(craft, true);
            nodes.add(new Node(craft.rocket.getRocketId(), craft.owner, craft.position.x(), craft.position.y(), antennas,
                    craft.position.orbit(), craft.position.slot(), isRelay(craft), false));
        }
        data.stations.entrySet().removeIf(entry -> {
            var level = server.getLevel(entry.getKey().dimension());
            return level == null || !level.hasChunkAt(entry.getKey().pos()) || entry.getValue().isRemoved();
        });
        data.stations.forEach((pos, station) -> nodes.add(new Node(UUID.nameUUIDFromBytes(pos.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                station.owner, -3_060_000, 0, station.poweredAntennas, SpaceSimulation.OrbitBand.SURFACE, -1, true, true)));
        var uploads = routes(nodes);
        for (var node : nodes) {
            if (node.ground) continue;
            var craft = data.craft.get(node.id);
            craft.communication = connection(node, nodes, uploads);
            craft.connected = craft.communication.receiveCommands;
            craft.canTransmit = craft.communication.upload;
            craft.route = craft.communication.reason;
        }
        data.networkNodes = List.copyOf(nodes);
        return nodes;
    }
    private SpaceCommunications() { }
}
