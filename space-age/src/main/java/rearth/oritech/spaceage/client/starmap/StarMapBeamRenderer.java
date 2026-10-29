package rearth.oritech.spaceage.client.starmap;

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
import java.util.UUID;

/**
 * Cached beam projections; all moving highlights share one GUI render state.
 */
final class StarMapBeamRenderer {

    private static final long ANIMATION_START = System.nanoTime();
    private static final double PULSE_LENGTH = 16;
    private static final double SPEED = 70;

    private StarMapBeamRenderer() {
    }

    static @Nullable Link project(double fromX, double fromY, double toX, double toY, UUID id, boolean selected,
                                  int x, int y, int width, int height) {

        var length = Math.hypot(toX - fromX, toY - fromY);
        if (length < 32) return null;
        var dx = (toX - fromX) / length;
        var dy = (toY - fromY) / length;
        var range = new double[]{0, length};
        if (!clip(fromX, dx, x - 4, x + width + 4, range)
                || !clip(fromY, dy, y - 4, y + height + 4, range)) return null;
        var phase = Math.floorMod(id.hashCode(), 65536) / 65536d;
        // Fixed for this viewport: panning and zooming must not reset the animation phase.
        var spacing = Math.max(220, (Math.hypot(width + 8, height + 8) + PULSE_LENGTH) / 3 + 1);
        return new Link(fromX, fromY, dx, dy, length, range[0], range[1], phase, spacing, selected);
    }

    private static boolean clip(double origin, double direction, double min, double max, double[] range) {

        if (Math.abs(direction) < 1e-12) return origin >= min && origin <= max;
        var a = (min - origin) / direction;
        var b = (max - origin) / direction;
        range[0] = Math.max(range[0], Math.min(a, b));
        range[1] = Math.min(range[1], Math.max(a, b));
        return range[0] < range[1];
    }

    static double firstHead(Link link, double seconds) {

        var spacing = link.spacing;
        var distance = seconds * SPEED + link.phase * spacing - link.start;
        return distance - Math.floor(distance / spacing) * spacing;
    }

    static void submit(GuiGraphicsExtractor graphics, List<Link> links, int x, int y, int width, int height) {

        if (links.isEmpty()) return;
        var pose = new Matrix3x2f(graphics.pose());
        var scissor = graphics.peekScissorStack();
        var bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
        var clipped = scissor == null ? bounds : scissor.intersection(bounds);
        if (clipped == null) return;
        var scale = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.guiScale;
        var edge = 1f / Math.max(1, scale);
        var seconds = (System.nanoTime() - ANIMATION_START) / 1_000_000_000d;
        graphics.submitGuiElementRenderState(new PulseState(links, pose, seconds, edge, scissor, clipped));
    }

    record Link(double x, double y, double dx, double dy, double length, double start, double end,
                double phase, double spacing, boolean selected) {
    }

    private record PulseState(List<Link> links, Matrix3x2f pose, double seconds, float edge,
                              @Nullable ScreenRectangle scissorArea,
                              @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {

            for (var link : links) {
                var spacing = link.spacing;
                for (var head = firstHead(link, seconds); head < link.end - link.start + PULSE_LENGTH; head += spacing) {
                    var distance = link.start + head;
                    var fade = Math.clamp(distance / PULSE_LENGTH, 0, 1)
                            * Math.clamp((link.length + PULSE_LENGTH - distance) / PULSE_LENGTH, 0, 1);
                    var alpha = (link.selected ? 1 : .8) * fade;
                    segment(consumer, link, distance - PULSE_LENGTH, distance - 3, 0, alpha);
                    segment(consumer, link, distance - 3, distance, alpha, 0);
                }
            }
        }

        private void segment(VertexConsumer consumer, Link link, double from, double to, double fromAlpha, double toAlpha) {

            var start = Math.max(link.start, from);
            var end = Math.min(link.end, to);
            if (end <= start) return;
            var a = fromAlpha + (toAlpha - fromAlpha) * (start - from) / (to - from);
            var b = fromAlpha + (toAlpha - fromAlpha) * (end - from) / (to - from);
            var inner = (link.selected ? 1.8 : 1.2) * .5;
            band(consumer, link, start, end, -inner, inner, a, b, a, b);
            band(consumer, link, start, end, -inner - edge, -inner, 0, 0, a, b);
            band(consumer, link, start, end, inner, inner + edge, a, b, 0, 0);
        }

        private void band(VertexConsumer consumer, Link link, double start, double end, double left, double right,
                          double startLeft, double endLeft, double startRight, double endRight) {

            vertex(consumer, link, start, left, startLeft);
            vertex(consumer, link, start, right, startRight);
            vertex(consumer, link, end, right, endRight);
            vertex(consumer, link, end, left, endLeft);
        }

        private void vertex(VertexConsumer consumer, Link link, double distance, double side, double alpha) {

            consumer.addVertexWith2DPose(pose, (float) (link.x + link.dx * distance - link.dy * side),
                            (float) (link.y + link.dy * distance + link.dx * side))
                    .setColor((int) (alpha * 255) << 24 | 0xC7F8FF);
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
