package rearth.oritech.block.logistics;

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
    private static final double THICKNESS = 1;
    private static final Vec3 MIDPOINT = new Vec3(8, 8, 8);
    private static final AABB CENTER_BOX = partBox(null);
    static final VoxelShape CENTER = shape(CENTER_BOX);

    private PipeCoreShape() {
    }

    static VoxelShape build(Map<PartPosition, PipePart> parts) {
        var result = Shapes.empty();

        for (var entry : parts.entrySet()) {
            var surface = PipeCore.PartPosition.ToDirection(entry.getKey());
            var box = partBox(surface);
            result = Shapes.or(result, shape(box));

            for (var direction : Direction.values()) {
                var arm = switch (entry.getValue().get(direction)) {
                    case NONE -> Shapes.empty();
                    case CONNECTED_INSIDE -> armToFace(CENTER_BOX, surface == null ? direction : surface);
                    // each part contributes its own arm. Outside wraps stop at the boundary of this block.
                    case CONNECTED, CONNECTED_CORNER, CONNECTED_EDGE -> armToFace(box, direction);
                };
                result = Shapes.or(result, arm);
            }
        }

        // keep empty cores targetable, including older pipes saved without part data
        return parts.isEmpty() ? CENTER : result;
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
