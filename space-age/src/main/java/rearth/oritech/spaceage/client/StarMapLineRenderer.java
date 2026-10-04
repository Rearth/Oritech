package rearth.oritech.spaceage.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Submits all map lines as one clipped GUI render state. */
final class StarMapLineRenderer {

    private StarMapLineRenderer() {
    }

    static void submit(GuiGraphicsExtractor graphics, List<Line> lines,
                       int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        if (lines.isEmpty()) return;
        var pose = new Matrix3x2f(graphics.pose());
        var scissor = graphics.peekScissorStack();
        var bounds = new ScreenRectangle(viewportX, viewportY, viewportWidth, viewportHeight).transformMaxBounds(pose);
        var clippedBounds = scissor == null ? bounds : scissor.intersection(bounds);
        // Use physical pixels so the soft edge does not grow with the player's GUI scale.
        int guiScale = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.guiScale;
        float edgeWidth = 1f / Math.max(1, guiScale);
        graphics.submitGuiElementRenderState(new RenderState(List.copyOf(lines), pose, edgeWidth, scissor, clippedBounds));
    }

    /** Screen-space endpoints, packed ARGB color, GUI thickness, and whether to soften the edges. */
    record Line(double fromX, double fromY, double toX, double toY, int color, float width, boolean antialiased) {
        // Reference rings and guides keep their existing appearance; trajectories opt into smoothing.
        Line(double fromX, double fromY, double toX, double toY, int color, float width) {
            this(fromX, fromY, toX, toY, color, width, false);
        }
    }

    /** Captured geometry, transform, one-pixel edge width, clipping area and viewport bounds. */
    private record RenderState(List<Line> lines, Matrix3x2f pose, float edgeWidth,
                               @Nullable ScreenRectangle scissorArea,
                               @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (var line : lines) {
                double dx = line.toX - line.fromX;
                double dy = line.toY - line.fromY;
                double length = Math.hypot(dx, dy);
                if (length < 0.001 || line.width <= 0) continue;
                double normalX = -dy / length;
                double normalY = dx / length;
                if (!line.antialiased) {
                    band(consumer, line, normalX, normalY, -line.width * 0.5f, line.width * 0.5f,
                            line.color, line.color);
                    continue;
                }

                // Center the fade on the old edge, keeping the apparent thickness close to the original.
                float inner = Math.max(0, (line.width - edgeWidth) * 0.5f);
                float outer = inner + edgeWidth;
                int alpha = Math.round((line.color >>> 24) * Math.min(1, line.width / edgeWidth));
                int centerColor = (line.color & 0x00FFFFFF) | (alpha << 24);
                int clearColor = line.color & 0x00FFFFFF;
                if (inner > 0) band(consumer, line, normalX, normalY, -inner, inner, centerColor, centerColor);
                band(consumer, line, normalX, normalY, -outer, -inner, clearColor, centerColor);
                band(consumer, line, normalX, normalY, inner, outer, centerColor, clearColor);
                // Only soften the sides: fading every short segment's ends would leave dots along a curve.
            }
        }

        private void band(VertexConsumer consumer, Line line, double normalX, double normalY,
                          float firstOffset, float secondOffset, int firstColor, int secondColor) {
            float firstX = (float) (normalX * firstOffset);
            float firstY = (float) (normalY * firstOffset);
            float secondX = (float) (normalX * secondOffset);
            float secondY = (float) (normalY * secondOffset);
            consumer.addVertexWith2DPose(pose, (float) line.fromX + firstX, (float) line.fromY + firstY).setColor(firstColor);
            consumer.addVertexWith2DPose(pose, (float) line.fromX + secondX, (float) line.fromY + secondY).setColor(secondColor);
            consumer.addVertexWith2DPose(pose, (float) line.toX + secondX, (float) line.toY + secondY).setColor(secondColor);
            consumer.addVertexWith2DPose(pose, (float) line.toX + firstX, (float) line.toY + firstY).setColor(firstColor);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

    }
}
