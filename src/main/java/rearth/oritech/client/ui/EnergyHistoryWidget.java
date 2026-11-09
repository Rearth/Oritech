package rearth.oritech.client.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.transfer.energy.EnergyHistory;
import rearth.oritech.api.transfer.energy.EnergyHistory.Sample;
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Two plots share a time axis; RF and RF/t keep independent vertical scales.
 */
final class EnergyHistoryWidget extends UIComponent {

    private static final int STORED_COLOR = 0xFFFFB347;
    private static final int INSERT_COLOR = 0xFF62D9E8;
    private static final int EXTRACT_COLOR = 0xFFD6A1F5;
    private static final int TEXT_COLOR = 0xFFE0E4E8;
    // Resolve on demand because pole networking can replace the client's history after a topology change.
    private final Supplier<EnergyHistory> history;
    private List<Sample> frozenSamples;
    private int windowSeconds = 300;
    private boolean logarithmicScale;

    EnergyHistoryWidget(int x, int y, int width, int height, Supplier<EnergyHistory> history) {
        super(x, y, width, height);
        this.history = history;
    }

    void setWindowSeconds(int seconds) {
        windowSeconds = seconds;
    }

    void setLogarithmicScale(boolean enabled) {
        logarithmicScale = enabled;
    }

    boolean isPaused() {
        return frozenSamples != null;
    }

    void togglePaused() {
        // Freeze only this view. Server recording and incoming updates continue while inspecting old values.
        frozenSamples = isPaused() ? null : history.get().getSamples();
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        var font = Minecraft.getInstance().font;
        var samples = isPaused() ? frozenSamples : history.get().getSamples();
        graphics.fill(x, y, x + width, y + height, 0xFF20262C);
        graphics.outline(x, y, width, height, 0xFF111519);
        // Hovered samples change as the timeline advances; don't retain a tooltip over empty history/gaps.
        setTooltip(null);
        if (samples.isEmpty()) {
            graphics.text(font, Component.translatable("title.oritech.energy.history.empty"), x + 8, y + height / 2, TEXT_COLOR, false);
            return;
        }

        var latest = samples.getLast();
        // Keep a fixed-duration axis even during startup, leaving unrecorded time blank on the left.
        long startTick = latest.tick() - windowSeconds * 20L;
        var visible = samples.stream().filter(sample -> sample.tick() > startTick).toList();
        // RF and RF/t need separate scales. Input/output share one scale so their heights are comparable.
        // A minimum of one also keeps an empty storage with no transfers drawable.
        double storedMax = 1;
        double rateMax = 1;
        for (var sample : visible) {
            storedMax = Math.max(storedMax, sample.stored());
            rateMax = Math.max(rateMax, Math.max(sample.inserted(), sample.extracted()));
        }
        storedMax = niceMaximum(storedMax);
        rateMax = niceMaximum(rateMax);
        int plotX = x + 5;
        int plotW = width - 10;
        int plotH = (height - 57) / 2;
        int storedY = y + 17;
        int transferY = storedY + plotH + 20;

        renderGrid(graphics, plotX, storedY, plotW, plotH);
        renderGrid(graphics, plotX, transferY, plotW, plotH);

        graphics.text(font, Component.translatable("title.oritech.energy.history.stored", energy(latest.stored())), x + 5, y + 4, STORED_COLOR, false);
        graphics.text(font, Component.translatable("title.oritech.energy.history.inserted", energy(latest.inserted())), x + 5, transferY - 12, INSERT_COLOR, false);

        var output = Component.translatable("title.oritech.energy.history.extracted", energy(latest.extracted()));

        graphics.text(font, output, x + width - 5 - font.width(output), transferY - 12, EXTRACT_COLOR, false);
        graphics.text(font, energy(storedMax) + " RF", plotX + 3, storedY + 3, TEXT_COLOR, false);
        graphics.text(font, energy(rateMax) + " RF/t", plotX + 3, transferY + 3, TEXT_COLOR, false);

        // Compute each scale's denominator once; stored values and tooltip units remain untransformed.
        double storedScaleMax = axisValue(storedMax);
        double rateScaleMax = axisValue(rateMax);
        var segments = new ArrayList<Segment>();
        Sample previous = null;

        for (var sample : visible) {
            float sx = sampleX(sample, startTick, plotX, plotW);
            // Adjacent averages touch halfway between their centers, including at resolution changes.
            // A larger distance is an unloaded gap, which must remain blank instead of implying activity.
            if (previous != null && sample.tick() - previous.tick() == (previous.spanTicks() + sample.spanTicks()) / 2) {
                float px = sampleX(previous, startTick, plotX, plotW);
                segments.add(new Segment(px, sampleY(previous.stored(), storedScaleMax, storedY, plotH), sx,
                        sampleY(sample.stored(), storedScaleMax, storedY, plotH), STORED_COLOR));
                segments.add(new Segment(px, sampleY(previous.inserted(), rateScaleMax, transferY, plotH), sx,
                        sampleY(sample.inserted(), rateScaleMax, transferY, plotH), INSERT_COLOR));
                segments.add(new Segment(px, sampleY(previous.extracted(), rateScaleMax, transferY, plotH), sx,
                        sampleY(sample.extracted(), rateScaleMax, transferY, plotH), EXTRACT_COLOR));
            } else {
                // Show isolated samples too, including the first recorded second.
                graphics.fill((int) sx, (int) sampleY(sample.stored(), storedScaleMax, storedY, plotH), (int) sx + 1,
                        (int) sampleY(sample.stored(), storedScaleMax, storedY, plotH) + 1, STORED_COLOR);
            }
            previous = sample;
        }

        // Batch the three curves into one render state instead of submitting hundreds of GUI fills.
        // Bounds use the captured pose, matching the coordinate space of the current scissor.
        var pose = new Matrix3x2f(graphics.pose());
        var bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
        var scissor = graphics.peekScissorStack();

        // Match the star-map renderer: keep the soft edge one physical pixel wide at every GUI scale.
        var guiScale = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.guiScale;
        float edgeWidth = 1f / Math.max(1, guiScale);
        graphics.submitGuiElementRenderState(new LinesState(segments, pose, edgeWidth, scissor,
                scissor == null ? bounds : bounds.intersection(scissor)));

        int axisY = y + height - 12;
        String windowLabel = windowSeconds >= 300 ? windowSeconds / 60 + "m" : windowSeconds + "s";
        graphics.text(font, "-" + windowLabel, plotX, axisY, TEXT_COLOR, false);
        var now = Component.translatable(isPaused() ? "title.oritech.energy.history.paused" : "title.oritech.energy.history.now");
        graphics.text(font, now, plotX + plotW - font.width(now), axisY, TEXT_COLOR, false);

        if (mouseX >= plotX && mouseX < plotX + plotW && mouseY >= storedY && mouseY <= transferY + plotH) {
            Sample hovered = null;
            double nearest = Double.MAX_VALUE;
            double targetTick = startTick + (mouseX - plotX) / (double) (plotW - 1) * windowSeconds * 20;
            for (var sample : visible) {
                double distance = Math.abs(sample.tick() - targetTick);
                if (distance <= sample.spanTicks() / 2.0 && distance < nearest) {
                    nearest = distance;
                    hovered = sample;
                }
            }
            // Select by the represented interval, so hover also works where the resolution changes.
            if (hovered != null) {
                int markerX = Math.round(sampleX(hovered, startTick, plotX, plotW));
                graphics.fill(markerX, storedY, markerX + 1, storedY + plotH, 0x80FFFFFF);
                graphics.fill(markerX, transferY, markerX + 1, transferY + plotH, 0x80FFFFFF);
                var tooltip = new ArrayList<Component>(List.of(
                        Component.translatable("title.oritech.energy.history.ago", (latest.tick() - hovered.tick()) / 20),
                        Component.translatable("title.oritech.energy.history.stored", energy(hovered.stored())).withColor(STORED_COLOR),
                        Component.translatable("title.oritech.energy.history.inserted", energy(hovered.inserted())).withColor(INSERT_COLOR),
                        Component.translatable("title.oritech.energy.history.extracted", energy(hovered.extracted())).withColor(EXTRACT_COLOR)));
                if (hovered.spanTicks() > EnergyHistory.SAMPLE_TICKS)
                    tooltip.add(Component.translatable("title.oritech.energy.history.average", hovered.spanTicks() / 20));
                setTooltip(tooltip);
            }
        }
    }

    // Round the top of each axis up to a readable value (e.g. 13k -> 20k), while retaining a zero baseline.
    private static double niceMaximum(double value) {
        double step = Math.pow(10, Math.floor(Math.log10(value)));
        return Math.ceil(value / step) * step;
    }

    private static String energy(double value) {
        return TooltipHelper.getEnergyText(Math.round(value));
    }

    private float sampleX(Sample sample, long startTick, int plotX, int plotW) {
        return plotX + (sample.tick() - startTick) / (float) (windowSeconds * 20) * (plotW - 1);
    }

    private double axisValue(double value) {
        // log(1 + value) preserves a zero baseline, unlike a plain logarithm which is undefined at zero.
        return logarithmicScale ? Math.log1p(value) : value;
    }

    private float sampleY(double value, double scaledMaximum, int plotY, int plotH) {
        return plotY + plotH - 1 - (float) (axisValue(value) / scaledMaximum) * (plotH - 1);
    }

    private static void renderGrid(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.outline(x, y, width, height, 0xFF69747E);
        for (int i = 1; i < 4; i++) {
            int gy = y + height * i / 4;
            graphics.fill(x + 1, gy, x + width - 1, gy + 1, 0xFF343D46);
        }
    }

    private record Segment(float x1, float y1, float x2, float y2, int color) {
    }

    private record LinesState(List<Segment> segments, Matrix3x2f pose, float edgeWidth,
                              @Nullable ScreenRectangle scissorArea,
                              @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer vertices) {
            for (var segment : segments) {
                float dx = segment.x2() - segment.x1();
                float dy = segment.y2() - segment.y1();
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                if (length == 0) continue;
                float nx = -dy / length;
                float ny = dx / length;
                // Center the fade on the old 1.2px line's edge to preserve its apparent thickness.
                float inner = Math.max(0, 0.6f - edgeWidth / 2);
                float outer = inner + edgeWidth;
                int clearColor = segment.color() & 0x00FFFFFF;
                band(vertices, segment, nx, ny, -inner, inner, segment.color(), segment.color());
                band(vertices, segment, nx, ny, -outer, -inner, clearColor, segment.color());
                band(vertices, segment, nx, ny, inner, outer, segment.color(), clearColor);
                // Fade only across the sides; fading each short segment's ends would make curves look dotted.
            }
        }

        private void band(VertexConsumer vertices, Segment segment, float nx, float ny,
                          float first, float second, int firstColor, int secondColor) {
            vertices.addVertexWith2DPose(pose, segment.x1() + nx * first, segment.y1() + ny * first).setColor(firstColor);
            vertices.addVertexWith2DPose(pose, segment.x1() + nx * second, segment.y1() + ny * second).setColor(secondColor);
            vertices.addVertexWith2DPose(pose, segment.x2() + nx * second, segment.y2() + ny * second).setColor(secondColor);
            vertices.addVertexWith2DPose(pose, segment.x2() + nx * first, segment.y2() + ny * first).setColor(firstColor);
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
