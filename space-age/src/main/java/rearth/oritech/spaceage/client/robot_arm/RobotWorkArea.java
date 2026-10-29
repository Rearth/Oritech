package rearth.oritech.spaceage.client.robot_arm;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3i;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.robot_arm.ActionCardItem;
import rearth.oritech.spaceage.block.robot_arm.RobotArmBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeComponents;

import java.util.HashSet;
import java.util.Set;

/**
 * Shows the blocks the bound arm can reach while recording: every face between a reachable and an unreachable block,
 * outlined where the border turns a corner.
 */
public final class RobotWorkArea {

    private static final ContextKey<Vec3> ORIGIN = new ContextKey<>(OritechSpaceAge.id("robot_work_area"));
    private static final int FACE_COLOR = 0x2855DDFF;
    private static final int EDGE_COLOR = 0xCC55DDFF;
    // Faces are pulled slightly into the area so they stay visible over the unreachable block behind them.
    private static final float INSET = 0.005f;
    // Offsets from the block above the arm, the same reference RobotArmBlockEntity#inRange measures from.
    private static final float[] FACES;
    private static final float[] EDGES;

    static {
        var range = RobotArmBlockEntity.RANGE;
        var faces = new FloatArrayList();
        var edges = new HashSet<Edge>();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (!reachable(x, y, z)) continue;
                    for (var side : Direction.values()) {
                        var normal = side.getUnitVec3i();
                        if (reachable(x + normal.getX(), y + normal.getY(), z + normal.getZ())) continue;
                        addFace(faces, edges, new Vector3i(x, y, z), side);
                    }
                }
            }
        }
        FACES = faces.toFloatArray();
        EDGES = new float[edges.size() * 6];
        var i = 0;
        for (var edge : edges) {
            EDGES[i++] = edge.from.x;
            EDGES[i++] = edge.from.y;
            EDGES[i++] = edge.from.z;
            EDGES[i++] = edge.to.x;
            EDGES[i++] = edge.to.y;
            EDGES[i++] = edge.to.z;
        }
    }

    private RobotWorkArea() {
    }

    private static boolean reachable(int x, int y, int z) {

        var range = RobotArmBlockEntity.RANGE;
        return x * x + y * y + z * z <= range * range;
    }

    private static void addFace(FloatArrayList faces, Set<Edge> edges, Vector3i block, Direction side) {

        var normal = side.getUnitVec3i();
        // The face lies on the block's side plane, spanned by the two remaining axes.
        var a = side.getAxis() == Direction.Axis.X ? new Vector3i(0, 1, 0) : new Vector3i(1, 0, 0);
        var b = side.getAxis() == Direction.Axis.Z ? new Vector3i(0, 1, 0) : new Vector3i(0, 0, 1);
        var base = new Vector3i(block).add(Math.max(normal.getX(), 0), Math.max(normal.getY(), 0), Math.max(normal.getZ(), 0));
        Vector3i[] corners = {base, new Vector3i(base).add(a), new Vector3i(base).add(a).add(b), new Vector3i(base).add(b)};
        // Edge i runs from corners[i] to corners[i + 1]; this is the step across it to the next block in the plane.
        Vector3i[] across = {new Vector3i(b).negate(), a, b, new Vector3i(a).negate()};
        for (var corner : corners) {
            faces.add(corner.x - normal.getX() * INSET);
            faces.add(corner.y - normal.getY() * INSET);
            faces.add(corner.z - normal.getZ() * INSET);
        }
        // Outline each flat patch: skip edges shared with a coplanar neighbouring face.
        for (int i = 0; i < 4; i++) {
            var neighbour = new Vector3i(block).add(across[i]);
            var beyond = new Vector3i(neighbour).add(normal.getX(), normal.getY(), normal.getZ());
            if (reachable(neighbour.x, neighbour.y, neighbour.z) && !reachable(beyond.x, beyond.y, beyond.z)) continue;
            edges.add(Edge.of(corners[i], corners[(i + 1) % 4]));
        }
    }

    public static void extract(ExtractLevelRenderStateEvent event) {

        var player = Minecraft.getInstance().player;
        if (player == null || !(player.getOffhandItem().getItem() instanceof ActionCardItem)) return;
        var program = player.getOffhandItem().get(SpaceAgeComponents.ROBOT_PROGRAM.get());
        if (program == null || program.binding().isEmpty()) return;
        var binding = program.binding().get();
        if (binding.dimension() != event.getLevel().dimension()) return;
        event.getRenderState().setRenderData(ORIGIN, Vec3.atLowerCornerOf(binding.pos().above()).subtract(event.getCamera().position()));
    }

    public static void submit(SubmitCustomGeometryEvent event) {

        var origin = event.getLevelRenderState().getRenderData(ORIGIN);
        if (origin == null) return;
        var x = (float) origin.x;
        var y = (float) origin.y;
        var z = (float) origin.z;
        var collector = event.getSubmitNodeCollector();
        collector.submitCustomGeometry(event.getPoseStack(), RenderTypes.debugQuads(), (pose, consumer) -> {
            for (int i = 0; i < FACES.length; i += 3)
                consumer.addVertex(pose, x + FACES[i], y + FACES[i + 1], z + FACES[i + 2]).setColor(FACE_COLOR);
        });
        collector.submitCustomGeometry(event.getPoseStack(), RenderTypes.lines(), (pose, consumer) -> {
            for (int i = 0; i < EDGES.length; i += 6) {
                var normalX = EDGES[i + 3] - EDGES[i];
                var normalY = EDGES[i + 4] - EDGES[i + 1];
                var normalZ = EDGES[i + 5] - EDGES[i + 2];
                consumer.addVertex(pose, x + EDGES[i], y + EDGES[i + 1], z + EDGES[i + 2]).setColor(EDGE_COLOR)
                        .setNormal(pose, normalX, normalY, normalZ).setLineWidth(2);
                consumer.addVertex(pose, x + EDGES[i + 3], y + EDGES[i + 4], z + EDGES[i + 5]).setColor(EDGE_COLOR)
                        .setNormal(pose, normalX, normalY, normalZ).setLineWidth(2);
            }
        });
    }

    /**
     * Undirected, so the two faces meeting at a corner add the edge once.
     */
    private record Edge(Vector3i from, Vector3i to) {

        static Edge of(Vector3i a, Vector3i b) {

            var ordered = a.x < b.x || a.x == b.x && (a.y < b.y || a.y == b.y && a.z < b.z);
            return ordered ? new Edge(a, b) : new Edge(b, a);
        }
    }
}
