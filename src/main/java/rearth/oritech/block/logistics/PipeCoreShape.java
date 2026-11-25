package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCore.DirectionState;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipeBlockData;
import rearth.oritech.block.logistics.PipeCore.PipeKind;
import rearth.oritech.block.logistics.PipeCore.PipePart;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class PipeCoreShape {

    // convert model dimensions to block-local coordinates for shapes and gizmos
    private static final double WIDTH = 3 / 16.0;
    private static final double THICKNESS = 2 / 16.0;
    private static final double BRIDGE_STEP_LENGTH = 1 / 16.0;
    private static final double HIT_TOLERANCE = 1e-5;
    private static final PipeKind[] KINDS = PipeKind.values();
    static final VoxelShape CENTER = partShape(PipeKind.FLUID, PartPosition.CENTER);
    static final BlockShapes EMPTY = new BlockShapes(CENTER, Map.of(), List.of());

    static VoxelShape partShape(PipeKind kind, PartPosition position) {
        var surface = PartPosition.ToDirection(position);
        return Shapes.create(partBox(partCenter(kind, surface), surface));
    }

    static BlockShapes build(PipeBlockData parts) {
        var combined = Shapes.empty();
        var outlines = new EnumMap<PipeKind, VoxelShape>(PipeKind.class);
        var partShapes = new ArrayList<PartHit>();

        // keep surface parts first at targeting ties, then use kind order
        for (var position : PartPosition.values()) {
            for (var kind : KINDS) {
                var part = parts.getByKind(kind).get(position);
                if (part == null) continue;

                var shape = buildPart(parts, kind, position, part);
                partShapes.add(new PartHit(kind, position, shape));
                outlines.merge(kind, shape, Shapes::or);
            }
        }

        // keep empty cores targetable, including older pipes saved without part data
        if (outlines.isEmpty()) return EMPTY;
        for (var shape : outlines.values()) {
            combined = Shapes.or(combined, shape);
        }
        return new BlockShapes(combined, Map.copyOf(outlines), List.copyOf(partShapes));
    }

    static Map<PipeKind, List<AABB>> buildModelLayout(PipeBlockData parts, BlockShapes shapes) {
        var models = new EnumMap<PipeKind, VoxelShape>(PipeKind.class);
        models.putAll(shapes.outlines());

        for (var hit : shapes.parts()) {
            var surface = PartPosition.ToDirection(hit.position());
            if (surface == null) continue;

            var kind = hit.kind();
            var part = parts.getByKind(kind).get(hit.position());
            for (var direction : Direction.values()) {
                if (part.get(direction) != DirectionState.CONNECTED_EDGE) continue;

                // The two ribbons swap axes at the corner. Give the shared elbow to one of them.
                if (direction.getAxis().ordinal() >= surface.getAxis().ordinal()) continue;

                var start = pointOnFace(partCenter(kind, surface), direction);
                var end = start.relative(direction, THICKNESS);
                var elbow = Shapes.create(pipeBox(start, end, surface, direction.getAxis()));
                models.merge(kind, elbow, Shapes::or);
            }
        }

        // Snapshot the cuboids so matching layouts share a cache key by value.
        var layout = new EnumMap<PipeKind, List<AABB>>(PipeKind.class);
        models.forEach((kind, shape) -> layout.put(kind, List.copyOf(shape.toAabbs())));
        return Map.copyOf(layout);
    }

    record BlockShapes(VoxelShape combined, Map<PipeKind, VoxelShape> outlines, List<PartHit> parts) {

        @Nullable PartHit findHitPart(BlockPos pos, Vec3 start, Vec3 end) {
            PartHit closest = null;
            var closestDistance = Double.POSITIVE_INFINITY;
            for (var part : parts) {
                var hit = part.shape().clip(start, end, pos);
                if (hit == null) continue;

                var distance = hit.getLocation().distanceToSqr(start);
                if (distance < closestDistance) {
                    closest = part;
                    closestDistance = distance;
                }
            }
            return closest;
        }

        boolean isRibbonTop(PipeKind kind, Direction surface, Vec3 localHit) {
            var position = PartPosition.FromDirection(surface);
            var axis = surface.getAxis();
            for (var part : parts) {
                if (part.kind() != kind || part.position() != position) continue;

                // use the exposed face of each box, including the raised bridge deck
                for (var box : part.shape().toAabbs()) {
                    var top = surface.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.min(axis) : box.max(axis);
                    if (Math.abs(localHit.get(axis) - top) <= HIT_TOLERANCE && box.inflate(HIT_TOLERANCE).contains(localHit)) return true;
                }
                return false;
            }
            return false;
        }
    }

    record PartHit(PipeKind kind, PartPosition position, VoxelShape shape) {}

    private static VoxelShape buildPart(PipeBlockData parts, PipeKind kind, PartPosition position, PipePart part) {
        var surface = PartPosition.ToDirection(position);
        var result = partShape(kind, position);
        for (var direction : Direction.values()) {
            var path = connectionPath(parts, kind, position, direction, part.get(direction));
            for (int i = 1; i < path.size(); i++) {
                result = Shapes.or(result, Shapes.create(connectionBox(path.get(i - 1), path.get(i), surface, direction)));
            }
        }
        return result;
    }

    private static AABB connectionBox(Vec3 start, Vec3 end, @Nullable Direction surface, Direction direction) {
        var axis = direction.getAxis();
        if (surface != null && start.get(axis) == end.get(axis)) {
            // extend the vertical step into the flat side so it clears the pipe below
            var normal = surface.getAxis();
            var rising = (end.get(normal) - start.get(normal)) * surface.getAxisDirection().getStep() < 0;
            if (rising) {
                start = start.relative(direction.getOpposite(), BRIDGE_STEP_LENGTH);
            } else {
                end = end.relative(direction, BRIDGE_STEP_LENGTH);
            }
        }

        return pipeBox(start, end, surface, axis);
    }

    static List<Vec3> connectionPath(PipeBlockData parts, PipeKind kind, PartPosition position, Direction direction, DirectionState state) {
        if (state == DirectionState.NONE) return List.of();

        var surface = PartPosition.ToDirection(position);
        var center = partCenter(kind, surface);
        var end = connectionEnd(center, surface, direction, state);
        if (end == null) return List.of();

        var axis = direction.getAxis();
        // only surface arms heading toward lower lanes can rise
        if (surface == null || axis == surface.getAxis() || direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            return List.of(center, end);
        }

        var crossDirection = direction.getClockWise(surface.getAxis()).getAxis().getPositive();
        var bridgeMin = center.get(axis);
        var bridgeMax = end.get(axis);

        for (var lowerKind : KINDS) {
            if (lowerKind.laneCoordinate >= kind.laneCoordinate) continue;
            var lowerPart = parts.getByKind(lowerKind).get(position);
            if (lowerPart == null) continue;

            // surface arms reach the block edge or an inside corner, both beyond every lane
            var lowerState = lowerPart.get(crossDirection);
            if (lowerState == DirectionState.NONE || lowerState == DirectionState.CONNECTED_INSIDE) continue;

            var lane = lowerKind.laneCoordinate / 16.0;
            // lanes are one ribbon width apart, so consecutive crossings share a deck
            bridgeMin = Math.min(bridgeMin, lane - WIDTH / 2);
            bridgeMax = Math.max(bridgeMax, lane + WIDTH / 2);
        }

        if (bridgeMin >= bridgeMax) return List.of(center, end);

        var startPoint = center.with(axis, bridgeMax);
        var endPoint = center.with(axis, bridgeMin);
        var awayFromSupport = surface.getOpposite();
        return List.of(center, startPoint, startPoint.relative(awayFromSupport, THICKNESS),
            endPoint.relative(awayFromSupport, THICKNESS), endPoint, end);
    }

    static Vec3 partCenter(PipeKind kind, @Nullable Direction surface) {
        var lane = kind.laneCoordinate / 16.0;
        var center = new Vec3(lane, lane, lane);
        // the backing axis stays flush with the support. The other axes follow the lane.
        return surface == null ? center : center.with(surface.getAxis(), surfaceCoordinate(surface));
    }

    static AABB partBox(Vec3 center, @Nullable Direction surface) {
        return pipeBox(center, center, surface, null);
    }

    private static AABB pipeBox(Vec3 start, Vec3 end, @Nullable Direction surface, @Nullable Direction.Axis travelAxis) {
        var padding = new Vec3(WIDTH / 2, WIDTH / 2, WIDTH / 2);
        if (surface != null) padding = padding.with(surface.getAxis(), THICKNESS / 2);
        if (travelAxis != null) padding = padding.with(travelAxis, 0);
        return new AABB(start, end).inflate(padding.x, padding.y, padding.z);
    }

    static @Nullable Vec3 connectionEnd(Vec3 center, @Nullable Direction surface, Direction direction, DirectionState state) {
        return switch (state) {
            case NONE -> null;
            case CONNECTED, CONNECTED_EDGE -> pointOnFace(center, direction);
            // the two surface arms meet where their center lines cross
            case CONNECTED_CORNER -> center.with(direction.getAxis(), surfaceCoordinate(direction));
            // the center owns the arm between itself and a surface part
            case CONNECTED_INSIDE -> surface == null ? center.with(direction.getAxis(), surfaceCoordinate(direction)) : null;
        };
    }

    static Vec3 pointOnFace(Vec3 point, Direction face) {
        return point.with(face.getAxis(), faceCoordinate(face));
    }

    private static double surfaceCoordinate(Direction surface) {
        return faceCoordinate(surface) - surface.getAxisDirection().getStep() * THICKNESS / 2;
    }

    private static double faceCoordinate(Direction face) {
        return face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
    }
}
