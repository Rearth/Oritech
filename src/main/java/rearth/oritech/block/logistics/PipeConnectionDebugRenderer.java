package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCoreBlockEntity.PartPosition;
import rearth.oritech.block.logistics.PipeCoreBlockEntity.PipePart;

import java.util.Map;

final class PipeConnectionDebugRenderer {

    private static final int LIFETIME_MS = 5_000;
    private static final double SURFACE_OFFSET = 0.45;
    private static final GizmoStyle PART_STYLE = GizmoStyle.stroke(0xFFFFFFFF);

    private PipeConnectionDebugRenderer() {}

    static void draw(@Nullable Level level, BlockPos pos, Map<PartPosition, PipePart> parts) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getServer().isDedicatedServer()
            || !serverLevel.getServer().isSameThread()) return;

        var center = Vec3.atCenterOf(pos);
        try {
            for (var entry : parts.entrySet()) {
                drawPart(center, PartPosition.ToDirection(entry.getKey()), entry.getValue());
            }
        } catch (IllegalStateException ignored) {
            // the integrated server only has a gizmo collector during packet processing and ticks
        }
    }

    private static void drawPart(Vec3 center, @Nullable Direction surface, PipePart part) {
        var start = surface == null ? center : center.relative(surface, SURFACE_OFFSET);
        var axis = surface == null ? null : surface.getAxis();
        var marker = AABB.ofSize(start, axis == Direction.Axis.X ? 0.025 : 0.2,
            axis == Direction.Axis.Y ? 0.025 : 0.2, axis == Direction.Axis.Z ? 0.025 : 0.2);
        Gizmos.cuboid(marker, PART_STYLE).setAlwaysOnTop().persistForMillis(LIFETIME_MS);

        for (var direction : Direction.values()) {
            switch (part.get(direction)) {
                case NONE -> { }
                case CONNECTED -> line(start, start.relative(direction, 1), 0xFF55FF55);
                case CONNECTED_INSIDE -> line(start, start.relative(direction, SURFACE_OFFSET), 0xFF55FFFF);
                case CONNECTED_CORNER -> {
                    var corner = start.relative(direction, SURFACE_OFFSET);
                    line(start, corner, 0xFFFFFF55);
                    line(corner, center.relative(direction, SURFACE_OFFSET), 0xFFFFFF55);
                }
                case CONNECTED_EDGE -> {
                    if (surface == null) continue;
                    var corner = start.relative(direction, 1 - SURFACE_OFFSET);
                    line(start, corner, 0xFFFF55FF);
                    line(corner, corner.relative(surface, 1 - SURFACE_OFFSET), 0xFFFF55FF);
                }
            }
        }
    }

    private static void line(Vec3 start, Vec3 end, int color) {
        Gizmos.line(start, end, color).setAlwaysOnTop().persistForMillis(LIFETIME_MS);
    }
}
