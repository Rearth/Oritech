package rearth.oritech.block.logistics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.CuboidGizmo;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipePart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Oritech.MOD_ID, value = Dist.CLIENT)
public final class PipeConnectionDebugRenderer {

    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final double SURFACE_OFFSET = 0.45;
    private static final GizmoStyle PART_STYLE = GizmoStyle.stroke(0xFFFFFFFF);
    private static final Set<LevelChunk> LOADED_CHUNKS = new HashSet<>();

    private static @Nullable ClientLevel snapshotLevel;
    private static List<Gizmo> snapshot = List.of();
    private static int ticksSinceRefresh;

    private PipeConnectionDebugRenderer() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel && Minecraft.getInstance().isSingleplayer()) {
            LOADED_CHUNKS.add(event.getChunk());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) {
            LOADED_CHUNKS.remove(event.getChunk());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level) {
            LOADED_CHUNKS.removeIf(chunk -> chunk.getLevel() == level);
            if (snapshotLevel == level) {
                snapshotLevel = null;
                snapshot = List.of();
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || !minecraft.isSingleplayer()) return;
        if (snapshotLevel == level && ++ticksSinceRefresh < REFRESH_INTERVAL_TICKS) return;

        ticksSinceRefresh = 0;
        snapshotLevel = level;
        var gizmos = new ArrayList<Gizmo>();

        // a view-distance change can evict chunks without the usual unload packet
        LOADED_CHUNKS.removeIf(chunk -> {
            var pos = chunk.getPos();
            return chunk.getLevel() != level || level.getChunkSource().getChunk(pos.x(), pos.z(), false) != chunk;
        });

        for (var chunk : LOADED_CHUNKS) {
            for (var blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity instanceof PipeCoreBlockEntity pipe && !pipe.isRemoved()) {
                    var center = Vec3.atCenterOf(pipe.getBlockPos());
                    pipe.forEachPart((position, part) -> drawPart(gizmos, center, PartPosition.ToDirection(position), part));
                }
            }
        }

        snapshot = gizmos;
    }

    @SubscribeEvent
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level != snapshotLevel || !minecraft.isSingleplayer()) return;

        // Minecraft's frame collector is active here. Reuse the snapshot until the next client refresh.
        for (var gizmo : snapshot) {
            Gizmos.addGizmo(gizmo).setAlwaysOnTop();
        }
    }

    private static void drawPart(List<Gizmo> gizmos, Vec3 center, @Nullable Direction surface, PipePart part) {
        var start = surface == null ? center : center.relative(surface, SURFACE_OFFSET);
        var axis = surface == null ? null : surface.getAxis();
        var marker = AABB.ofSize(start, axis == Direction.Axis.X ? 0.025 : 0.2,
            axis == Direction.Axis.Y ? 0.025 : 0.2, axis == Direction.Axis.Z ? 0.025 : 0.2);
        gizmos.add(new CuboidGizmo(marker, PART_STYLE, false));

        for (var direction : Direction.values()) {
            switch (part.get(direction)) {
                case NONE -> { }
                case CONNECTED -> line(gizmos, start, start.relative(direction, 1), 0xFF55FF55);
                case CONNECTED_INSIDE -> line(gizmos, start, start.relative(direction, SURFACE_OFFSET), 0xFF55FFFF);
                case CONNECTED_CORNER -> {
                    var corner = start.relative(direction, SURFACE_OFFSET);
                    line(gizmos, start, corner, 0xFFFFFF55);
                    line(gizmos, corner, center.relative(direction, SURFACE_OFFSET), 0xFFFFFF55);
                }
                case CONNECTED_EDGE -> {
                    if (surface == null) continue;
                    var corner = start.relative(direction, 1 - SURFACE_OFFSET);
                    line(gizmos, start, corner, 0xFFFF55FF);
                    line(gizmos, corner, corner.relative(surface, 1 - SURFACE_OFFSET), 0xFFFF55FF);
                }
            }
        }
    }

    private static void line(List<Gizmo> gizmos, Vec3 start, Vec3 end, int color) {
        gizmos.add(new LineGizmo(start, end, color, LineGizmo.DEFAULT_WIDTH));
    }
}
