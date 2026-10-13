package rearth.oritech.client.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import rearth.oritech.util.CollisionShapes;

public class MachineOutlineRenderer {

    public static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        var level = event.getLevel();
        // Cores and door helper blocks all resolve to the same model anchor.
        var controllerPos = CollisionShapes.getControllerPos(event.getBlockState(), level, event.getBlockPos());
        var state = level.getBlockState(controllerPos);

        var outline = CollisionShapes.getOutlineShape(state);
        if (outline == null || outline.isEmpty()) return;

        // Capture only extracted geometry/position, not the level: rendering can happen after extraction.
        var origin = controllerPos.immutable();
        event.addCustomRenderer((renderState, buffers, poseStack, translucentPass, levelState) -> {
            // NeoForge invokes this callback in both passes. Suppress the local vanilla outline in both.
            if (renderState.isTranslucent() != translucentPass) return true;

            var camera = levelState.cameraRenderState.pos;
            var x = origin.getX() - camera.x;
            var y = origin.getY() - camera.y;
            var z = origin.getZ() - camera.z;

            // Match vanilla's colors, line width and accessibility outline instead of drawing through walls.
            if (renderState.highContrast()) {
                ShapeRenderer.renderShape(poseStack, buffers.getBuffer(RenderTypes.secondaryBlockOutline()),
                        outline, x, y, z, ARGB.black(255), 7f);
            }

            var color = renderState.highContrast() ? -11010079 : ARGB.black(102);
            var width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
            ShapeRenderer.renderShape(poseStack, buffers.getBuffer(RenderTypes.lines()), outline, x, y, z, color, width);
            buffers.endLastBatch();
            return true;
        });
    }
}
