package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipePart;

import java.util.Map;

final class PipeCoreShape {

    // these dimensions use Block.box units (sixteen units per block)
    private static final double HALF_WIDTH = 2;
    private static final double THICKNESS = 2;
    private static final Vec3 MIDPOINT = new Vec3(8, 8, 8);
    private static final AABB CENTER_BOX = partBox(null);
    static final VoxelShape CENTER = shape(CENTER_BOX);

    static VoxelShape partShape(PartPosition position) {
        return shape(partBox(PartPosition.ToDirection(position)));
    }

    static boolean isRibbonTop(Direction face, Vec3 localHit) {
        // the exposed face is one ribbon thickness away from the supporting block
        var coordinate = face.getAxis().choose(localHit.x, localHit.y, localHit.z);
        var thickness = THICKNESS / 16;
        var top = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? thickness : 1 - thickness;
        return Math.abs(coordinate - top) < 1e-5;
    }

    static VoxelShape build(Map<PartPosition, PipePart> parts) {
        var result = Shapes.empty();

        for (var entry : parts.entrySet()) {
            result = Shapes.or(result, buildPart(entry.getKey(), entry.getValue()));
        }

        // keep empty cores targetable, including older pipes saved without part data
        return parts.isEmpty() ? CENTER : result;
    }

    static @Nullable PartPosition findHitPart(Map<PartPosition, PipePart> parts, BlockPos pos, Vec3 start, Vec3 end) {
        PartPosition closest = null;
        var closestDistance = Double.POSITIVE_INFINITY;

        // use a stable order when two parts meet at the same point, with surface parts before the center
        for (var position : PartPosition.values()) {
            var part = parts.get(position);
            if (part == null) continue;

            var hit = buildPart(position, part).clip(start, end, pos);
            if (hit != null) {
                var distance = hit.getLocation().distanceToSqr(start);
                if (distance < closestDistance) {
                    closest = position;
                    closestDistance = distance;
                }
            }
        }

        return closest;
    }

    private static VoxelShape buildPart(PartPosition position, PipePart part) {
        var surface = PartPosition.ToDirection(position);
        var box = partBox(surface);
        var result = shape(box);

        for (var direction : Direction.values()) {
            var arm = switch (part.get(direction)) {
                case NONE -> Shapes.empty();
                // the center owns the arm between itself and a surface part
                case CONNECTED_INSIDE -> surface == null ? armToFace(box, direction) : Shapes.empty();
                // each part contributes its own arm. Outside wraps stop at this block's boundary.
                case CONNECTED, CONNECTED_CORNER, CONNECTED_EDGE -> armToFace(box, direction);
            };
            result = Shapes.or(result, arm);
        }

        return result;
    }

    private static AABB partBox(@Nullable Direction surface) {
        var axis = surface == null ? null : surface.getAxis();
        var center = surface == null ? MIDPOINT : MIDPOINT.relative(surface, 8 - THICKNESS / 2);
        var width = HALF_WIDTH * 2;
        return AABB.ofSize(center, axis == Direction.Axis.X ? THICKNESS : width,
                axis == Direction.Axis.Y ? THICKNESS : width, axis == Direction.Axis.Z ? THICKNESS : width);
    }

    private static VoxelShape armToFace(AABB box, Direction direction) {
        return shape(box.expandTowards(Vec3.ZERO.relative(direction, 8 - HALF_WIDTH)));
    }

    private static VoxelShape shape(AABB box) {
        return Block.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}
