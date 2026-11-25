package rearth.oritech.client.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCoreBlockEntity;
import rearth.oritech.block.logistics.PipeCoreBlockEntity.HitOutline;
import rearth.oritech.init.BlockContent;

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
        event.addCustomRenderer((renderState, buffers, poseStack, translucentPass, levelState) -> {
            // suppress the vanilla outline in both passes and draw in the matching pass
            if (renderState.isTranslucent() != translucentPass || outline == null) return true;

            var camera = levelState.cameraRenderState.pos;
            var x = origin.getX() - camera.x;
            var y = origin.getY() - camera.y;
            var z = origin.getZ() - camera.z;

            if (renderState.highContrast()) {
                var consumer = buffers.getBuffer(RenderTypes.secondaryBlockOutline());
                ShapeRenderer.renderShape(poseStack, consumer, outline.kindShape(), x, y, z, ARGB.black(255), 7f);
                ShapeRenderer.renderShape(poseStack, consumer, outline.partShape(), x, y, z, ARGB.black(255), 7f * SELECTED_WIDTH_SCALE);
            }

            var consumer = buffers.getBuffer(RenderTypes.lines());
            var color = renderState.highContrast() ? -11010079 : ARGB.black(102);
            var width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
            // draw the exact mining target over the outline of its kind
            ShapeRenderer.renderShape(poseStack, consumer, outline.kindShape(), x, y, z, color, width);
            ShapeRenderer.renderShape(poseStack, consumer, outline.partShape(), x, y, z, SELECTED_COLOR, width * SELECTED_WIDTH_SCALE);
            buffers.endLastBatch();
            return true;
        });
    }
}
