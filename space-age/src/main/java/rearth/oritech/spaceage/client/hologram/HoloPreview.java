package rearth.oritech.spaceage.client.hologram;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.spaceage.client.starmap.StarMapLineRenderer;
import rearth.oritech.spaceage.client.starmap.StarMapLineRenderer.Line;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;
import rearth.oritech.spaceage.block.hologram.HoloSettings;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Point;
import rearth.oritech.spaceage.block.hologram.HoloView;
import rearth.oritech.spaceage.block.hologram.HoloView.Frame;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Top-down inspection preview. Panning/zooming picks a fixed projection area. */
final class HoloPreview extends UIComponent {

    private final Supplier<HoloSnapshot> snapshot;
    private final Supplier<HoloSettings> settings;
    private final Consumer<Frame> pick;
    private Frame frame;
    private boolean dragging;
    private Component hovered;
    private final Map<UUID, BlockWidget> models = new HashMap<>();

    HoloPreview(int x, int y, int width, int height, Supplier<HoloSnapshot> snapshot,
                Supplier<HoloSettings> settings, Consumer<Frame> pick) {

        super(x, y, width, height);
        this.snapshot = snapshot;
        this.settings = settings;
        this.pick = pick;
    }

    private Point project(double px, double py) {

        var scale = Math.min(width, height) / frame.coverage();
        return new Point(x + width / 2d + (px - frame.x()) * scale, y + height / 2d + (py - frame.y()) * scale);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        var data = snapshot.get();
        var s = settings.get();
        var configured = new HoloSnapshot(s, data.surface(), data.objects(), data.craft(), data.categories(), data.tick(), data.speed(), data.facing(), data.status());
        frame = HoloView.frame(configured, frame);
        graphics.fill(x, y, x + width, y + height, 0xFF0B1824);
        graphics.enableScissor(x, y, x + width, y + height);
        for (int gx = x; gx < x + width; gx += 20) graphics.fill(gx, y, gx + 1, y + height, 0x203A97AC);
        for (int gy = y; gy < y + height; gy += 20) graphics.fill(x, gy, x + width, gy + 1, 0x203A97AC);
        hovered = null;
        var lines = new ArrayList<Line>();
        for (var craft : data.craft()) {
            if (!s.shows(HoloSettings.PATHS) || !HoloView.selected(s, craft)) continue;
            for (int i = 1; i < craft.path().size(); i++)
                line(lines, craft.path().get(i - 1).x(), craft.path().get(i - 1).y(), craft.path().get(i).x(), craft.path().get(i).y(), 0xFF5CC9EF);
        }
        for (var object : data.objects()) {
            if (!s.shows(HoloSettings.ORBITS) || object.type() == ObjectType.SURVEY_REGION) continue;
            for (var orbit : RocketFlightPlanRules.availableOrbits(object.type())) {
                if (orbit == OrbitBand.SURFACE) continue;
                var radius = SurveyRules.orbitRadius(object, orbit);
                if (radius * Math.min(width, height) / frame.coverage() < 3) continue;
                for (int i = 0; i < 48; i++) {
                    var a = i * Math.PI / 24;
                    var b = (i + 1) * Math.PI / 24;
                    line(lines, object.x() + Math.cos(a) * radius, object.y() + Math.sin(a) * radius,
                            object.x() + Math.cos(b) * radius, object.y() + Math.sin(b) * radius, 0x885A8D9E);
                }
            }
        }
        StarMapLineRenderer.submitConnected(graphics, lines, x, y, width, height);
        for (var object : data.objects()) {
            if (object.type() == ObjectType.SURVEY_REGION && !s.shows(HoloSettings.SURVEYS)) continue;
            if (object.type() != ObjectType.SURVEY_REGION && !s.shows(HoloSettings.BODIES)) continue;
            var pos = project(object.x(), object.y());
            if (pos.x() < x || pos.x() > x + width || pos.y() < y || pos.y() > y + height) continue;
            if (object.type() == ObjectType.SURVEY_REGION) {
                graphics.fill((int) pos.x() - 2, (int) pos.y() - 2, (int) pos.x() + 2, (int) pos.y() + 2, 0x55758D9B);
                continue;
            }
            var size = (float) Math.clamp(HoloView.modelDiameter(object, frame, configured)
                    * Math.min(width, height) / Math.max(1, Math.min(data.surface().width(), data.surface().depth())), 8, 48);
            var widget = models.computeIfAbsent(object.id(), ignored -> {
                var result = new BlockWidget(0, 0, 64, HoloRenderer.block(object)).withScale(object.type() == ObjectType.ASTEROID ? 1 : .5f);
                if (object.type() != ObjectType.EARTH && object.type() != ObjectType.SUN) result.withCachedRendering();
                return result;
            });
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) pos.x() - size / 2, (float) pos.y() - size / 2);
            graphics.pose().scale(size / 64, size / 64);
            widget.render(graphics, 0, 0, delta);
            graphics.pose().popMatrix();
            if (Math.hypot(mouseX - pos.x(), mouseY - pos.y()) < 8)
                hovered = object.name().isBlank() ? Component.translatable("screen.oritech_space_age.object." + object.type().name().toLowerCase(Locale.ROOT)) : Component.literal(object.name());
        }
        for (var craft : data.craft()) {
            if (!HoloView.selected(s, craft) && !s.shows(HoloSettings.OTHER_CRAFT)) continue;
            var pos = project(craft.x(), craft.y());
            graphics.fill((int) pos.x() - 2, (int) pos.y() - 2, (int) pos.x() + 2, (int) pos.y() + 2, 0xFF89F4FF);
            if (Math.hypot(mouseX - pos.x(), mouseY - pos.y()) < 8) hovered = Component.literal(craft.name());
        }
        graphics.disableScissor();
        graphics.text(Minecraft.getInstance().font, HoloControllerScreen.text("extent", Math.round(frame.coverage())), x + 5, y + height - 12, 0xFF9FD9E9, false);
    }

    private void line(List<Line> lines, double ax, double ay, double bx, double by, int color) {

        var a = project(ax, ay);
        var b = project(bx, by);
        lines.add(new Line(a.x(), a.y(), b.x(), b.y(), color, .8f, true));
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {

        dragging = button == 0 && isMouseOver(mouseX, mouseY);
        return dragging;
    }

    @Override
    public boolean handleDrag(double mouseX, double mouseY, double dx, double dy, int button) {

        if (!dragging || frame == null) return false;
        var scale = Math.min(width, height) / frame.coverage();
        frame = new Frame(frame.x() - dx / scale, frame.y() - dy / scale, frame.coverage());
        pick.accept(frame);
        return true;
    }

    @Override
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {

        dragging = false;
        return true;
    }

    @Override
    public boolean handleMouseScroll(double mouseX, double mouseY, double amount) {

        if (!isMouseOver(mouseX, mouseY) || frame == null) return false;
        var scale = Math.min(width, height) / frame.coverage();
        var px = frame.x() + (mouseX - x - width / 2d) / scale;
        var py = frame.y() + (mouseY - y - height / 2d) / scale;
        var coverage = Math.clamp(frame.coverage() * Math.pow(1.2, -amount), 100, 100_000_000);
        var ratio = coverage / frame.coverage();
        frame = new Frame(px + (frame.x() - px) * ratio, py + (frame.y() - py) * ratio, coverage);
        pick.accept(frame);
        return true;
    }

    @Override
    public boolean hasTooltip() {

        return hovered != null;
    }

    @Override
    public List<Component> getTooltip() {

        return hovered == null ? List.of() : List.of(hovered);
    }
}
