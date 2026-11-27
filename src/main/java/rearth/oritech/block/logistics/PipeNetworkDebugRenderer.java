package rearth.oritech.block.logistics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.logistics.PipeCore.DirectionState;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeNetworks.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Oritech.MOD_ID, value = Dist.CLIENT)
public final class PipeNetworkDebugRenderer {

    private static final boolean ENABLED = true;
    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final Comparator<Node> NODE_ORDER = Comparator.comparing(Node::pos).thenComparing(Node::kind).thenComparing(Node::position);

    private static @Nullable ClientLevel snapshotLevel;
    private static @Nullable CompletableFuture<List<LineGizmo>> pendingSnapshot;
    private static List<LineGizmo> snapshot = List.of();
    private static int ticksUntilRefresh;

    private PipeNetworkDebugRenderer() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        var minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        var level = server != null ? minecraft.level : null;
        if (snapshotLevel != level) {
            snapshotLevel = level;
            snapshot = List.of();
            pendingSnapshot = null;
            ticksUntilRefresh = 0;
        }
        if (level == null) return;

        if (pendingSnapshot != null && pendingSnapshot.isDone()) {
            snapshot = pendingSnapshot.getNow(List.of());
            pendingSnapshot = null;
        }

        if (ticksUntilRefresh > 0) ticksUntilRefresh--;
        if (ticksUntilRefresh > 0 || pendingSnapshot != null) return;

        // The graph and block entities belong to the server thread. Share only the finished lines.
        ticksUntilRefresh = REFRESH_INTERVAL_TICKS;
        var dimension = level.dimension();
        pendingSnapshot = server.submit(() -> buildSnapshot(server.getLevel(dimension))).exceptionally(error -> {
            Oritech.LOGGER.warn("Could not build the pipe network debug overlay", error);
            return List.of();
        });
    }

    @SubscribeEvent
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        if (!ENABLED || Minecraft.getInstance().level != snapshotLevel) return;

        for (var line : snapshot) {
            Gizmos.addGizmo(line).setAlwaysOnTop();
        }
    }

    private static List<LineGizmo> buildSnapshot(@Nullable ServerLevel level) {
        if (level == null) return List.of();
        var networks = PipeNetworks.getExisting(level);
        if (networks == null) return List.of();

        var loaded = networks.getLoadedNodes(level);
        var nodes = new ArrayList<>(loaded);
        nodes.sort(NODE_ORDER);
        var visited = new HashSet<Node>();
        var usedColors = new HashSet<Integer>();
        var lines = new ArrayList<LineGizmo>();
        for (var node : nodes) {
            if (visited.contains(node)) continue;

            var network = networks.getNetwork(node);
            visited.addAll(network);
            // Use the whole component for its color, including its unloaded sections.
            var color = networkColor(Collections.min(network, NODE_ORDER), usedColors);
            for (var member : network) {
                if (loaded.contains(member)) drawPart(level, member, networks.getConnections(member), color, lines);
            }
        }
        return List.copyOf(lines);
    }

    private static int networkColor(Node node, Set<Integer> usedColors) {
        var seed = (node.pos().asLong() * 31 + node.kind().ordinal()) * 31 + node.position().ordinal();
        var random = RandomSource.create(Mth.murmurHash3Mixer(Long.hashCode(seed)));
        int color;
        do {
            color = ARGB.opaque(Mth.hsvToRgb(random.nextFloat(), 0.55f + random.nextFloat() * 0.35f, 0.85f + random.nextFloat() * 0.15f));
        } while (!usedColors.add(color));
        return color;
    }

    private static void drawPart(ServerLevel level, Node node, Set<Node> neighbors, int color, List<LineGizmo> lines) {
        var pipe = PipeCoreBlock.getLoadedPipe(level, node.pos());
        if (pipe == null) return;
        var part = pipe.getPart(node.kind(), node.position());
        if (part == null) return;

        var origin = Vec3.atLowerCornerOf(node.pos());
        int firstLine = lines.size();
        for (var direction : Direction.values()) {
            var state = part.get(direction);
            var target = PipeNetworks.connectionTarget(node, direction, state);
            if (state != DirectionState.CONNECTED_MACHINE && (target == null || !neighbors.contains(target))) continue;

            var path = pipe.debugConnectionPath(node.kind(), node.position(), direction, state);
            for (int i = 1; i < path.size(); i++) {
                lines.add(new LineGizmo(origin.add(path.get(i - 1)), origin.add(path.get(i)), color, LineGizmo.DEFAULT_WIDTH));
            }
        }

        if (neighbors.isEmpty() && lines.size() == firstLine) {
            // A single disconnected part is still its own network.
            var surface = PartPosition.ToDirection(node.position());
            var center = origin.add(PipeCoreShape.partCenter(node.kind(), surface));
            var direction = surface != null && surface.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
            lines.add(new LineGizmo(center.relative(direction, -1 / 16.0), center.relative(direction, 1 / 16.0), color, LineGizmo.DEFAULT_WIDTH));
        }
    }
}
