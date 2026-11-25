package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCoreBlockEntity;
import rearth.oritech.block.logistics.PipeCoreBlockEntity.HitOutline;
import rearth.oritech.init.BlockContent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PipeOutlineRenderer {

    private static final int SELECTED_COLOR = 0xFFFFB84D;
    private static final float SELECTED_WIDTH_SCALE = 1.35f;

    public static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        if (!event.getBlockState().is(BlockContent.PIPE_CORE.get())) return;

        var pos = event.getBlockPos();
        if (!(event.getLevel().getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) || !pipe.hasPipeParts()) return;

        var minecraft = Minecraft.getInstance();
        var cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) return;

        var start = cameraEntity.getEyePosition(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        var hit = event.getHitResult().getLocation();
        // the hit lies on the surface, so extend the ray slightly to cross into the part
        var end = hit.add(hit.subtract(start).normalize().scale(0.001));
        replaceOutline(event, pos, pipe.findHitOutline(start, end));
    }

    private static void replaceOutline(ExtractBlockOutlineRenderStateEvent event, BlockPos pos, @Nullable HitOutline outline) {
        var origin = pos.immutable();
        var unselected = outline == null ? List.<OutlineEdge>of() : unselectedEdges(outline.kindShape(), outline.partShape());
        event.addCustomRenderer((renderState, buffers, poseStack, translucentPass, levelState) -> {
            // suppress the vanilla outline in both passes and draw in the matching pass
            if (renderState.isTranslucent() != translucentPass || outline == null) return true;

            var camera = levelState.cameraRenderState.pos;
            var x = origin.getX() - camera.x;
            var y = origin.getY() - camera.y;
            var z = origin.getZ() - camera.z;

            if (renderState.highContrast()) {
                var consumer = buffers.getBuffer(RenderTypes.secondaryBlockOutline());
                renderEdges(poseStack.last(), consumer, unselected, x, y, z, ARGB.black(255), 7f);
                ShapeRenderer.renderShape(poseStack, consumer, outline.partShape(), x, y, z, ARGB.black(255), 7f * SELECTED_WIDTH_SCALE);
            }

            var consumer = buffers.getBuffer(RenderTypes.lines());
            var color = renderState.highContrast() ? -11010079 : ARGB.black(102);
            var width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
            renderEdges(poseStack.last(), consumer, unselected, x, y, z, color, width);
            ShapeRenderer.renderShape(poseStack, consumer, outline.partShape(), x, y, z, SELECTED_COLOR, width * SELECTED_WIDTH_SCALE);
            buffers.endLastBatch();
            return true;
        });
    }

    private static List<OutlineEdge> unselectedEdges(VoxelShape kindShape, VoxelShape partShape) {
        if (kindShape == partShape) return List.of();

        var selected = new ArrayList<OutlineEdge>();
        partShape.forAllEdges((x1, y1, z1, x2, y2, z2) -> selected.add(OutlineEdge.of(x1, y1, z1, x2, y2, z2)));
        selected.sort(Comparator.comparingDouble(OutlineEdge::start));

        var edges = new ArrayList<OutlineEdge>();
        kindShape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            var edge = OutlineEdge.of(x1, y1, z1, x2, y2, z2);
            var start = edge.start();

            // A kind outline can merge several parts into one edge. Trim only the intervals that the highlight draws.
            for (var highlight : selected) {
                if (highlight.start() >= edge.end()) break;
                if (highlight.end() <= start || !edge.line().equals(highlight.line())) continue;

                if (highlight.start() > start) edges.add(new OutlineEdge(edge.line(), start, highlight.start()));
                start = highlight.end();
                if (start >= edge.end()) break;
            }
            if (start < edge.end()) edges.add(new OutlineEdge(edge.line(), start, edge.end()));
        });
        return List.copyOf(edges);
    }

    private static void renderEdges(PoseStack.Pose pose, VertexConsumer consumer, List<OutlineEdge> edges, double x, double y, double z, int color, float width) {
        for (var edge : edges) {
            var axis = edge.line().axis();
            var normal = axis.getPositive().step();
            var start = edge.line().anchor().with(axis, edge.start());
            var end = edge.line().anchor().with(axis, edge.end());
            consumer.addVertex(pose, (float) (start.x + x), (float) (start.y + y), (float) (start.z + z)).setColor(color).setNormal(pose, normal).setLineWidth(width);
            consumer.addVertex(pose, (float) (end.x + x), (float) (end.y + y), (float) (end.z + z)).setColor(color).setNormal(pose, normal).setLineWidth(width);
        }
    }

    private record EdgeLine(Direction.Axis axis, Vec3 anchor) {}

    private record OutlineEdge(EdgeLine line, double start, double end) {
        private static OutlineEdge of(double x1, double y1, double z1, double x2, double y2, double z2) {
            var axis = x1 != x2 ? Direction.Axis.X : y1 != y2 ? Direction.Axis.Y : Direction.Axis.Z;
            var line = new EdgeLine(axis, new Vec3(x1, y1, z1).with(axis, 0));
            return new OutlineEdge(line, axis.choose(x1, y1, z1), axis.choose(x2, y2, z2));
        }
    }
}
