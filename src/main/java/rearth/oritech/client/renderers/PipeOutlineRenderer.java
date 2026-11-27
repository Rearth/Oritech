package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCoreBlock;
import rearth.oritech.block.logistics.PipeCoreBlockEntity;
import rearth.oritech.block.logistics.PipeCoreBlockEntity.HitOutline;
import rearth.oritech.block.logistics.PipeCoreBlockEntity.WrenchTarget;
import rearth.oritech.block.logistics.PipeCoreBlockItem;
import rearth.oritech.block.logistics.PipeNetworks.Node;
import rearth.oritech.block.logistics.PipeToolPreview;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PipeOutlineRenderer {

    private static final int SELECTED_COLOR = 0xFFFFB84D;
    private static final int PLACE_COLOR = 0xFF63CFFF;
    private static final int DISABLE_COLOR = 0xFFFF6565;
    private static final int ENABLE_COLOR = 0xFF71EB8B;
    private static final int START_EXTRACTION_COLOR = 0xFFD69AFF;
    private static final int STOP_EXTRACTION_COLOR = 0xFFE1E7EF;
    private static final float SELECTED_WIDTH_SCALE = 1.35f;
    private static final int PREVIEW_REFRESH_TICKS = 5;

    private static @Nullable ToolTarget requestedTarget;
    private static @Nullable Preview preview;
    private static int previewId;
    private static int ticksUntilRefresh;

    private record ToolTarget(Level level, InteractionHand hand, Node node, @Nullable WrenchTarget wrench) {}

    private record Preview(BlockPos origin, VoxelShape shape, boolean reopen) {}

    public static void tick() {
        var minecraft = Minecraft.getInstance();
        var hit = minecraft.hitResult instanceof BlockHitResult blockHit ? blockHit : null;
        var target = hit == null ? null : findToolTarget(minecraft, hit);
        if (!Objects.equals(target, requestedTarget)) {
            requestedTarget = target;
            preview = null;
            previewId++;
            ticksUntilRefresh = 0;
        }

        // Refresh while aiming at the same port because machines can change their capabilities in place.
        if (target != null && ticksUntilRefresh-- <= 0) {
            ClientPacketDistributor.sendToServer(new PipeToolPreview.Request(previewId, target.hand(), hit));
            ticksUntilRefresh = PREVIEW_REFRESH_TICKS - 1;
        }
    }

    public static void receivePreview(PipeToolPreview.Outline outline, IPayloadContext context) {
        if (outline.id() != previewId || requestedTarget == null || requestedTarget.level() != context.player().level()) return;
        if (outline.boxes().isEmpty() || !requestedTarget.node().pos().equals(outline.origin())) {
            preview = null;
            return;
        }

        var shape = Shapes.empty();
        for (var box : outline.boxes()) shape = Shapes.or(shape, Shapes.create(box));
        preview = new Preview(outline.origin(), shape, outline.reopen());
    }

    private static @Nullable ToolTarget findToolTarget(Minecraft minecraft, BlockHitResult hit) {
        var player = minecraft.player;
        if (player == null || player.isSpectator() || hit.getType() != HitResult.Type.BLOCK || hit.isWorldBorderHit()) return null;

        var level = player.level();
        var pos = hit.getBlockPos();
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof PipeCoreBlockItem item) {
                var node = item.getPlacementTarget(new UseOnContext(player, hand, hit));
                return node == null ? null : new ToolTarget(level, hand, node, null);
            }
            if (stack.is(ItemContent.WRENCH)) {
                var wrench = level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe ? pipe.findWrenchTarget(hit) : null;
                return wrench == null ? null : new ToolTarget(level, hand, wrench.port().node(), wrench);
            }
        }
        return null;
    }

    public static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        var minecraft = Minecraft.getInstance();
        var hitResult = event.getHitResult();
        var target = findToolTarget(minecraft, hitResult);
        var currentPreview = target != null && target.equals(requestedTarget) ? preview : null;
        if (currentPreview != null && target.wrench() == null) {
            addToolOutline(event, currentPreview.origin(), currentPreview.shape(), PLACE_COLOR);
        }

        if (!event.getBlockState().is(BlockContent.PIPE_CORE.get())) return;

        var pos = event.getBlockPos();
        if (!(event.getLevel().getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) || !pipe.hasPipeParts()) return;

        if (currentPreview != null && target.wrench() != null) {
            replaceOutline(event, pos, new HitOutline(pipe.getPipeShape(), currentPreview.shape()), currentPreview.reopen() ? ENABLE_COLOR : DISABLE_COLOR);
            return;
        }

        var player = minecraft.player;
        if (player != null && PipeCoreBlock.canToggleExtraction(player, pos)) {
            var extraction = pipe.findExtractionTarget(hitResult);
            if (extraction != null) {
                var color = extraction.extracting() ? STOP_EXTRACTION_COLOR : START_EXTRACTION_COLOR;
                replaceOutline(event, pos, new HitOutline(pipe.getPipeShape(), extraction.shape()), color);
                return;
            }
        }

        var cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) return;

        var start = cameraEntity.getEyePosition(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        var hit = event.getHitResult().getLocation();
        // the hit lies on the surface, so extend the ray slightly to cross into the part
        var end = hit.add(hit.subtract(start).normalize().scale(0.001));
        replaceOutline(event, pos, pipe.findHitOutline(start, end), SELECTED_COLOR);
    }

    private static void addToolOutline(ExtractBlockOutlineRenderStateEvent event, BlockPos pos, VoxelShape shape, int color) {
        event.addCustomRenderer((renderState, buffers, poseStack, translucentPass, levelState) -> {
            if (renderState.isTranslucent() != translucentPass) return false;

            var camera = levelState.cameraRenderState.pos;
            var x = pos.getX() - camera.x;
            var y = pos.getY() - camera.y;
            var z = pos.getZ() - camera.z;
            if (renderState.highContrast()) {
                ShapeRenderer.renderShape(poseStack, buffers.getBuffer(RenderTypes.secondaryBlockOutline()), shape,
                    x, y, z, ARGB.black(255), 7f * SELECTED_WIDTH_SCALE);
            }
            var width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
            ShapeRenderer.renderShape(poseStack, buffers.getBuffer(RenderTypes.lines()), shape, x, y, z, color, width * SELECTED_WIDTH_SCALE);
            buffers.endLastBatch();
            return false;
        });
    }

    private static void replaceOutline(ExtractBlockOutlineRenderStateEvent event, BlockPos pos, @Nullable HitOutline outline, int selectedColor) {
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
            ShapeRenderer.renderShape(poseStack, consumer, outline.partShape(), x, y, z, selectedColor, width * SELECTED_WIDTH_SCALE);
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
