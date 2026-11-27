package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCore.DirectionState;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipeBlockData;
import rearth.oritech.block.logistics.PipeCore.PipeKind;
import rearth.oritech.block.logistics.PipeCore.PipePart;
import rearth.oritech.block.logistics.PipeCoreBlockEntity.ModelPart;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class PipeCoreShape {

    // convert model dimensions to block-local coordinates for shapes and gizmos
    private static final double WIDTH = 3 / 16.0;
    private static final double THICKNESS = 2 / 16.0;
    private static final double MACHINE_COLLAR_MARGIN = 1 / 32.0;
    private static final double EXTRACTION_SLEEVE_LENGTH = 3 / 16.0;
    private static final double BRIDGE_STEP_LENGTH = 1 / 16.0;
    private static final double HIT_TOLERANCE = 1e-5;
    private static final PipeKind[] KINDS = PipeKind.values();
    static final VoxelShape CENTER = Shapes.create(partBox(new Vec3(0.5, 0.5, 0.5), null));
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

    static List<ModelPart> buildModelLayout(PipeBlockData parts, BlockShapes shapes) {
        // Snapshot the cuboids so matching layouts share a cache key by value.
        var layout = new ArrayList<ModelPart>();
        for (var kind : KINDS) {
            if (!shapes.outlines().containsKey(kind)) continue;
            var model = modelShape(parts, shapes, kind);
            var extractingCollars = Shapes.empty();
            for (var entry : parts.getByKind(kind).entrySet()) {
                var surface = PartPosition.ToDirection(entry.getKey());
                if (surface != null && entry.getValue().isExtracting(surface)) {
                    extractingCollars = Shapes.or(extractingCollars, machineCollar(kind, surface));
                }
            }
            if (!extractingCollars.isEmpty()) {
                // Give backing collars their own tint without drawing the same faces twice.
                layout.add(new ModelPart(kind, true, List.copyOf(extractingCollars.toAabbs())));
                model = Shapes.join(model, extractingCollars, BooleanOp.ONLY_FIRST);
            }
            if (!model.isEmpty()) layout.add(new ModelPart(kind, false, List.copyOf(model.toAabbs())));
        }
        return List.copyOf(layout);
    }

    static VoxelShape modelShape(PipeBlockData parts, BlockShapes shapes, PipeKind kind) {
        var model = shapes.outlines().getOrDefault(kind, Shapes.empty());
        for (var hit : shapes.parts()) {
            if (hit.kind() != kind) continue;
            var surface = PartPosition.ToDirection(hit.position());
            if (surface == null) continue;

            var part = parts.getByKind(kind).get(hit.position());
            for (var direction : Direction.values()) {
                if (part.get(direction) != DirectionState.CONNECTED_EDGE) continue;

                // The two ribbons swap axes at the corner. Give the shared elbow to one of them.
                if (direction.getAxis().ordinal() >= surface.getAxis().ordinal()) continue;

                var start = pointOnFace(partCenter(kind, surface), direction);
                var end = start.relative(direction, THICKNESS);
                model = Shapes.or(model, Shapes.create(pipeBox(start, end, surface, direction.getAxis())));
            }
        }

        return model;
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

        @Nullable PartHit findHitPart(BlockHitResult hit, Vec3 localHit) {
            for (var part : parts) {
                if (hit.isInside() ? contains(part.shape(), localHit) : isOnFace(part.shape(), hit.getDirection(), localHit)) return part;
            }
            return null;
        }

        boolean isRibbonTop(Direction surface, Vec3 localHit) {
            var position = PartPosition.FromDirection(surface);
            for (var part : parts) {
                if (part.position() == position && isOnFace(part.shape(), surface.getOpposite(), localHit)) return true;
            }
            return false;
        }
    }

    record PartHit(PipeKind kind, PartPosition position, VoxelShape shape) {}

    private static boolean contains(VoxelShape shape, Vec3 point) {
        return shape.toAabbs().stream().anyMatch(box -> box.inflate(HIT_TOLERANCE).contains(point));
    }

    private static boolean isOnFace(VoxelShape shape, Direction face, Vec3 point) {
        var axis = face.getAxis();
        for (var box : shape.toAabbs()) {
            var side = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.max(axis) : box.min(axis);
            if (Math.abs(point.get(axis) - side) <= HIT_TOLERANCE && box.inflate(HIT_TOLERANCE).contains(point)) return true;
        }
        return false;
    }

    private static VoxelShape buildPart(PipeBlockData parts, PipeKind kind, PartPosition position, PipePart part) {
        var result = partShape(kind, position);
        for (var direction : Direction.values()) {
            result = Shapes.or(result, connectionShape(parts, kind, position, direction, part.get(direction)));
        }
        return result;
    }

    static VoxelShape connectionShape(PipeBlockData parts, PipeKind kind, PartPosition position, Direction direction, DirectionState state) {
        var surface = PartPosition.ToDirection(position);
        if (direction == surface && state == DirectionState.CONNECTED_MACHINE) return machineCollar(kind, surface);

        var result = Shapes.empty();
        var path = connectionPath(parts, kind, position, direction, state);
        for (int i = 1; i < path.size(); i++) {
            result = Shapes.or(result, Shapes.create(connectionBox(path.get(i - 1), path.get(i), surface, direction)));
        }
        if (state == DirectionState.CONNECTED_MACHINE && parts.getByKind(kind).get(position).isExtracting(direction)) {
            result = Shapes.or(result, extractionSleeve(path, surface, direction));
        }
        return result;
    }

    static VoxelShape extractionMarker(PipeBlockData parts, PipeKind kind, PartPosition position, Direction direction) {
        var surface = PartPosition.ToDirection(position);
        if (direction == surface) return machineCollar(kind, surface);

        var path = connectionPath(parts, kind, position, direction, DirectionState.CONNECTED_MACHINE);
        return extractionSleeve(path, surface, direction);
    }

    private static VoxelShape extractionSleeve(List<Vec3> path, @Nullable Direction surface, Direction direction) {
        var axis = direction.getAxis();
        var end = path.getLast();
        var legStart = path.get(path.size() - 2);
        // Fit the sleeve on the last leg so it stays clear of any bridge that leads into it.
        var length = Math.min(EXTRACTION_SLEEVE_LENGTH, Math.abs(end.get(axis) - legStart.get(axis)));
        var start = end.relative(direction.getOpposite(), length);
        var margin = MACHINE_COLLAR_MARGIN;
        var padding = new Vec3(margin, margin, margin).with(axis, 0);
        var box = pipeBox(start, end, surface, axis).inflate(padding.x, padding.y, padding.z);
        if (surface != null) box = box.move(Vec3.ZERO.relative(surface.getOpposite(), margin));
        return Shapes.create(box);
    }

    private static VoxelShape machineCollar(PipeKind kind, Direction surface) {
        // The ribbon already touches the machine. A raised collar marks the hidden port.
        var collar = partBox(partCenter(kind, surface), surface).inflate(MACHINE_COLLAR_MARGIN)
            .move(Vec3.ZERO.relative(surface.getOpposite(), MACHINE_COLLAR_MARGIN));
        return Shapes.create(collar);
    }

    static VoxelShape portMarker(PipeKind kind, PartPosition position, Direction direction) {
        var surface = PartPosition.ToDirection(position);
        if (direction == surface) return machineCollar(kind, surface);

        var center = partCenter(kind, surface);
        var radius = surface != null && direction.getAxis() == surface.getAxis() ? THICKNESS / 2 : WIDTH / 2;
        var start = center.relative(direction, radius);
        return Shapes.create(pipeBox(start, start.relative(direction, BRIDGE_STEP_LENGTH), surface, direction.getAxis()));
    }

    static @Nullable Direction findWrenchDirection(PipeBlockData parts, PartHit hit, Direction face, Vec3 localHit) {
        var kind = hit.kind();
        var position = hit.position();
        var part = parts.getByKind(kind).get(position);
        var surface = PartPosition.ToDirection(position);
        var onBody = contains(partShape(kind, position), localHit);
        var onCollar = surface != null && part.get(surface) == DirectionState.CONNECTED_MACHINE
            && isOnFace(machineCollar(kind, surface), face, localHit);

        if (!onBody && !onCollar) {
            for (var direction : Direction.values()) {
                if (isOnFace(connectionShape(parts, kind, position, direction, part.get(direction)), face, localHit)) return direction;
            }
        }

        if (surface != null && face == surface.getOpposite()) {
            // From the ribbon's exposed side, aim at an edge to reopen a lateral port.
            // The middle selects the hidden backing port, while the surrounding area can reopen the center link.
            var offset = localHit.subtract(partCenter(kind, surface)).with(surface.getAxis(), 0);
            var edge = Direction.getApproximateNearest(offset);
            var nearEdge = offset.lengthSqr() >= WIDTH * WIDTH / 16;
            if (nearEdge && edge.getAxis() != surface.getAxis() && part.isDisabled(edge)) return edge;
            if ((!nearEdge || !part.isDisabled(face)) && isWrenchable(part, surface)) return surface;
        }

        if (onCollar && !part.isDisabled(face)) return surface;
        return isWrenchable(part, face) ? face : null;
    }

    static @Nullable Direction findExtractionDirection(PipeBlockData parts, PartHit hit, BlockHitResult blockHit, Vec3 localHit) {
        var kind = hit.kind();
        if (!kind.supportsExtraction()) return null;

        var position = hit.position();
        var part = parts.getByKind(kind).get(position);
        for (var direction : Direction.values()) {
            if (part.get(direction) != DirectionState.CONNECTED_MACHINE) continue;
            var shape = connectionShape(parts, kind, position, direction, DirectionState.CONNECTED_MACHINE);
            if (blockHit.isInside() ? contains(shape, localHit) : isOnFace(shape, blockHit.getDirection(), localHit)) return direction;
        }
        return null;
    }

    private static boolean isWrenchable(PipePart part, Direction direction) {
        return part.isDisabled(direction) || part.get(direction) != DirectionState.NONE;
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
            case CONNECTED, CONNECTED_EDGE, CONNECTED_MACHINE -> pointOnFace(center, direction);
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
