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

import java.util.EnumMap;
import java.util.Map;

final class PipeCoreShape {

    // convert model dimensions to block-local coordinates for shapes and gizmos
    private static final double WIDTH = 4 / 16.0;
    private static final double THICKNESS = 2 / 16.0;
    private static final double HIT_TOLERANCE = 1e-5;
    static final VoxelShape CENTER = partShape(PipeKind.FLUID, PartPosition.CENTER);
    static final BlockShapes EMPTY = new BlockShapes(CENTER, Map.of());

    static VoxelShape partShape(PipeKind kind, PartPosition position) {
        var surface = PartPosition.ToDirection(position);
        return Shapes.create(partBox(partCenter(kind, surface), surface));
    }

    static boolean isRibbonTop(PipeKind kind, Direction surface, PipePart part, Vec3 localHit) {
        // the exposed face is one ribbon thickness away from the supporting block
        var coordinate = localHit.get(surface.getAxis());
        var top = faceCoordinate(surface) - surface.getAxisDirection().getStep() * THICKNESS;
        if (Math.abs(coordinate - top) > HIT_TOLERANCE) return false;

        var shape = buildPart(kind, PartPosition.FromDirection(surface), part);
        for (var box : shape.toAabbs()) {
            if (box.inflate(HIT_TOLERANCE).contains(localHit)) return true;
        }
        return false;
    }

    static BlockShapes build(PipeBlockData parts) {
        var combined = Shapes.empty();
        var outlines = new EnumMap<PipeKind, VoxelShape>(PipeKind.class);

        for (var kind : PipeKind.values()) {
            var shape = Shapes.empty();
            for (var entry : parts.getByKind(kind).entrySet()) {
                shape = Shapes.or(shape, buildPart(kind, entry.getKey(), entry.getValue()));
            }

            if (!shape.isEmpty()) {
                // keep the full outline of each kind for crosshair targeting
                outlines.put(kind, shape);
                combined = Shapes.or(combined, shape);
            }
        }

        // keep empty cores targetable, including older pipes saved without part data
        return outlines.isEmpty() ? EMPTY : new BlockShapes(combined, Map.copyOf(outlines));
    }

    record BlockShapes(VoxelShape combined, Map<PipeKind, VoxelShape> outlines) {}

    static @Nullable PartHit findHitPart(PipeBlockData parts, BlockPos pos, Vec3 start, Vec3 end) {
        PartHit closest = null;
        var closestDistance = Double.POSITIVE_INFINITY;

        // keep surface parts first at ties, then use kind order where different pipes intersect
        var kinds = PipeKind.values();
        for (var position : PartPosition.values()) {
            for (var kind : kinds) {
                var part = parts.getByKind(kind).get(position);
                if (part == null) continue;

                var shape = buildPart(kind, position, part);
                var hit = shape.clip(start, end, pos);
                if (hit != null) {
                    var distance = hit.getLocation().distanceToSqr(start);
                    if (distance < closestDistance) {
                        closest = new PartHit(kind, position, shape);
                        closestDistance = distance;
                    }
                }
            }
        }

        return closest;
    }

    record PartHit(PipeKind kind, PartPosition position, VoxelShape shape) {}

    private static VoxelShape buildPart(PipeKind kind, PartPosition position, PipePart part) {
        var surface = PartPosition.ToDirection(position);
        var center = partCenter(kind, surface);
        var box = partBox(center, surface);
        var result = Shapes.create(box);

        for (var direction : Direction.values()) {
            var end = connectionEnd(center, surface, direction, part.get(direction));
            if (end != null) {
                result = Shapes.or(result, Shapes.create(box.minmax(new AABB(end, end))));
            }
        }

        return result;
    }

    static Vec3 partCenter(PipeKind kind, @Nullable Direction surface) {
        var lane = kind.laneCoordinate / 16.0;
        var center = new Vec3(lane, lane, lane);
        // the backing axis stays flush with the support. The other axes follow the lane.
        return surface == null ? center : center.with(surface.getAxis(), surfaceCoordinate(surface));
    }

    static AABB partBox(Vec3 center, @Nullable Direction surface) {
        var axis = surface == null ? null : surface.getAxis();
        return AABB.ofSize(center, axis == Direction.Axis.X ? THICKNESS : WIDTH,
            axis == Direction.Axis.Y ? THICKNESS : WIDTH, axis == Direction.Axis.Z ? THICKNESS : WIDTH);
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
