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

/**
 * Submits all map lines as one clipped GUI render state.
 */
final class StarMapLineRenderer {

    private StarMapLineRenderer() {
    }

    static void submit(GuiGraphicsExtractor graphics, List<Line> lines,
                       int viewportX, int viewportY, int viewportWidth, int viewportHeight) {

        submit(graphics, lines, viewportX, viewportY, viewportWidth, viewportHeight, false);
    }

    static void submitConnected(GuiGraphicsExtractor graphics, List<Line> lines,
                                int viewportX, int viewportY, int viewportWidth, int viewportHeight) {

        submit(graphics, lines, viewportX, viewportY, viewportWidth, viewportHeight, true);
    }

    private static void submit(GuiGraphicsExtractor graphics, List<Line> lines,
                               int viewportX, int viewportY, int viewportWidth, int viewportHeight, boolean connected) {

        if (lines.isEmpty()) return;
        var pose = new Matrix3x2f(graphics.pose());
        var scissor = graphics.peekScissorStack();
        var bounds = new ScreenRectangle(viewportX, viewportY, viewportWidth, viewportHeight).transformMaxBounds(pose);
        var clippedBounds = scissor == null ? bounds : scissor.intersection(bounds);
        // Use physical pixels so the soft edge does not grow with the player's GUI scale.
        var guiScale = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.guiScale;
        var edgeWidth = 1f / Math.max(1, guiScale);
        graphics.submitGuiElementRenderState(new RenderState(List.copyOf(lines), pose, edgeWidth, connected, scissor, clippedBounds));
    }

    /**
     * Screen-space endpoints, packed ARGB color, GUI thickness, and whether to soften the edges.
     */
    record Line(double fromX, double fromY, double toX, double toY, int color, float width, boolean antialiased) {

        // Reference rings and guides keep their existing appearance; trajectories opt into smoothing.
        Line(double fromX, double fromY, double toX, double toY, int color, float width) {

            this(fromX, fromY, toX, toY, color, width, false);
        }
    }

    private static boolean touches(Line first, Line second) {

        return first.toX == second.fromX && first.toY == second.fromY;
    }

    // Both segments use exactly the same edge vertices, including while pulse widths change.
    static Join join(Line first, Line second) {

        var firstLength = Math.hypot(first.toX - first.fromX, first.toY - first.fromY);
        var secondLength = Math.hypot(second.toX - second.fromX, second.toY - second.fromY);
        var ax = -(first.toY - first.fromY) / firstLength;
        var ay = (first.toX - first.fromX) / firstLength;
        var bx = -(second.toY - second.fromY) / secondLength;
        var by = (second.toX - second.fromX) / secondLength;
        var divisor = Math.max(.25, 1 + ax * bx + ay * by);
        return new Join((ax + bx) / divisor, (ay + by) / divisor, (first.width + second.width) * .5f);
    }

    record Join(double x, double y, float width) {
    }

    /**
     * Captured geometry, transform, one-pixel edge width, clipping area and viewport bounds.
     */
    private record RenderState(List<Line> lines, Matrix3x2f pose, float edgeWidth, boolean connected,
                               @Nullable ScreenRectangle scissorArea,
                               @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {

            for (var index = 0; index < lines.size(); index++) {
                var line = lines.get(index);
                var dx = line.toX - line.fromX;
                var dy = line.toY - line.fromY;
                var length = Math.hypot(dx, dy);
                if (length < 0.001 || line.width <= 0) continue;
                var normalX = -dy / length;
                var normalY = dx / length;
                var start = new Join(normalX, normalY, line.width);
                var end = start;
                if (connected && index > 0 && touches(lines.get(index - 1), line)) start = join(lines.get(index - 1), line);
                if (connected && index + 1 < lines.size() && touches(line, lines.get(index + 1))) end = join(line, lines.get(index + 1));
                if (!line.antialiased) {
                    band(consumer, line, start, end, -start.width * 0.5f, start.width * 0.5f, -end.width * 0.5f, end.width * 0.5f,
                            line.color, line.color);
                    continue;
                }

                // Center the fade on the old edge, keeping the apparent thickness close to the original.
                var innerStart = Math.max(0, (start.width - edgeWidth) * 0.5f);
                var innerEnd = Math.max(0, (end.width - edgeWidth) * 0.5f);
                var outerStart = innerStart + edgeWidth;
                var outerEnd = innerEnd + edgeWidth;
                var alpha = Math.round((line.color >>> 24) * Math.min(1, line.width / edgeWidth));
                var centerColor = (line.color & 0x00FFFFFF) | (alpha << 24);
                var clearColor = line.color & 0x00FFFFFF;
                if (innerStart > 0 || innerEnd > 0)
                    band(consumer, line, start, end, -innerStart, innerStart, -innerEnd, innerEnd, centerColor, centerColor);
                band(consumer, line, start, end, -outerStart, -innerStart, -outerEnd, -innerEnd, clearColor, centerColor);
                band(consumer, line, start, end, innerStart, outerStart, innerEnd, outerEnd, centerColor, clearColor);
                // Only soften the sides: fading every short segment's ends would leave dots along a curve.
            }
        }

        private void band(VertexConsumer consumer, Line line, Join start, Join end,
                          float firstStart, float secondStart, float firstEnd, float secondEnd, int firstColor, int secondColor) {

            consumer.addVertexWith2DPose(pose, (float) (line.fromX + start.x * firstStart), (float) (line.fromY + start.y * firstStart)).setColor(firstColor);
            consumer.addVertexWith2DPose(pose, (float) (line.fromX + start.x * secondStart), (float) (line.fromY + start.y * secondStart)).setColor(secondColor);
            consumer.addVertexWith2DPose(pose, (float) (line.toX + end.x * secondEnd), (float) (line.toY + end.y * secondEnd)).setColor(secondColor);
            consumer.addVertexWith2DPose(pose, (float) (line.toX + end.x * firstEnd), (float) (line.toY + end.y * firstEnd)).setColor(firstColor);
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
