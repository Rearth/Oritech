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
import rearth.oritech.block.logistics.PipeCore.DirectionState;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipeKind;
import rearth.oritech.block.logistics.PipeCore.PipePart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Oritech.MOD_ID, value = Dist.CLIENT)
public final class PipeConnectionDebugRenderer {

    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final float GIZMO_OFFSET = 0.01f;
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

        var kinds = PipeKind.values();
        for (var chunk : LOADED_CHUNKS) {
            for (var blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity instanceof PipeCoreBlockEntity pipe && !pipe.isRemoved()) {
                    var origin = Vec3.atLowerCornerOf(pipe.getBlockPos());
                    for (var kind : kinds) {
                        var color = switch (kind) {
                            case ITEM -> 0xFF55FF55;
                            case FLUID -> 0xFF5599FF;
                            case ENERGY -> 0xFFFFAA00;
                        };

                        pipe.forEachPart(kind, (position, part) -> drawPart(gizmos, origin, kind, PartPosition.ToDirection(position), part, color));
                    }
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
            Gizmos.addGizmo(gizmo);
        }
    }

    private static void drawPart(List<Gizmo> gizmos, Vec3 origin, PipeKind kind, @Nullable Direction surface, PipePart part, int color) {
        var center = PipeCoreShape.partCenter(kind, surface).add(GIZMO_OFFSET);
        var marker = PipeCoreShape.partBox(center, surface).move(origin);
        gizmos.add(new CuboidGizmo(marker, GizmoStyle.stroke(color), false));

        var start = origin.add(center);
        for (var direction : Direction.values()) {
            var state = part.get(direction);
            var end = PipeCoreShape.connectionEnd(center, surface, direction, state);
            if (end == null) continue;

            if (state == DirectionState.CONNECTED_EDGE && surface != null) {
                // extend along the travel axis until the line meets the other ribbon's center line
                var axis = direction.getAxis();
                var targetCenter = PipeCoreShape.partCenter(kind, direction.getOpposite()).add(GIZMO_OFFSET);
                end = end.with(axis, targetCenter.get(axis) + direction.getAxisDirection().getStep());
            }

            line(gizmos, start, origin.add(end), color);
        }
    }

    private static void line(List<Gizmo> gizmos, Vec3 start, Vec3 end, int color) {
        gizmos.add(new LineGizmo(start, end, color, LineGizmo.DEFAULT_WIDTH));
    }
}
