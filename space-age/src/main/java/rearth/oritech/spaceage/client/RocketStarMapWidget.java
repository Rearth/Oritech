package rearth.oritech.spaceage.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class RocketStarMapWidget extends UIComponent {
    private static final double LAUNCH_ARC_DISTANCE = 100_000;
    private static final double DISTANCE_SCALE = 20_000;
    private static final double MAX_DISPLAY_DISTANCE = 100_000_000;
    private static final int DISTANCE_PIXELS = 510;
    private static final float MIN_PATH_POINT_DISTANCE_SQUARED = 0.25f;
    private final int contentHeight;
    private static final OrbitMarker[] ORBIT_MARKERS = {
            new OrbitMarker(1_000, "low_orbit"), new OrbitMarker(20_000, "medium_orbit"),
            new OrbitMarker(40_000, "high_orbit"), new OrbitMarker(100_000, "outer_space"),
            new OrbitMarker(1_000_000, "deep_space"), new OrbitMarker(10_000_000, "inner_system"),
            new OrbitMarker(20_000_000, "asteroid_belt")
    };

    private final List<MapObject> objects = new ArrayList<>();
    private final List<TrajectoryLeg> trajectory = new ArrayList<>();
    private final List<BranchMarker> rocketMarkers = new ArrayList<>();
    private final Map<UUID, RocketFlightPathCalculator.CraftPath> pathsByBranch = new HashMap<>();
    private UUID selectedBranch;
    private double lastCommandDays;
    private float scrollY;
    private float renderedScrollY;
    private boolean dragging;
    private MapObject hovered;

    RocketStarMapWidget(int x, int y, int width, int height,
                          SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket,
                          UUID selectedBranch) {
        super(x, y, width, height);
        this.surface = OritechSurface.PANEL_INSET;
        contentHeight = Math.max(550, height - 10);
        for (var object : snapshot.objects()) {
            int size = object.type() == SpaceObjects.ObjectType.ASTEROID ? 14 : 22;
            int objectX = Math.round(mapX(object.x()) - size / 2f);
            int objectY = contentBottom() - distancePixels(object.y()) - size / 2;
            var widget = new BlockWidget(objectX, objectY, size, placeholderBlock(object.type()));
            objects.add(new MapObject(object, widget));
        }
        updateFlightPath(snapshot, rocket, selectedBranch);
        scrollY = maxScroll();
        renderedScrollY = scrollY;
    }

    void updateFlightPath(SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket,
                          UUID selectedBranch) {
        this.selectedBranch = selectedBranch;
        trajectory.clear();
        rocketMarkers.clear();
        pathsByBranch.clear();
        var result = RocketFlightPathCalculator.calculate(rocket, snapshot.objects(), snapshot.plan(),
                snapshot.launchX(), snapshot.launchZ());
        lastCommandDays = result.lastCommandSeconds() / 1_200d;
        for (var path : result.paths()) {
            pathsByBranch.put(path.branchId(), path);
            addPath(path);
        }
    }

    private void addPath(RocketFlightPathCalculator.CraftPath path) {
        if (path.samples().isEmpty()) return;

        // Physics samples are intentionally more detailed than the map. Keeping only points that move at least half
        // a screen pixel avoids submitting thousands of indistinguishable quads without changing the visible curve.
        var previous = path.samples().getFirst();
        float previousX = mapPathX(previous);
        float previousY = (float) (contentBottom() - distancePixelsExact(previous.y()));
        for (int index = 1; index < path.samples().size(); index++) {
            var sample = path.samples().get(index);
            float sampleX = mapPathX(sample);
            float sampleY = (float) (contentBottom() - distancePixelsExact(sample.y()));
            float deltaX = sampleX - previousX;
            float deltaY = sampleY - previousY;
            boolean phaseChanged = sample.phase() != previous.phase() || sample.projected() != previous.projected();
            boolean lastSample = index == path.samples().size() - 1;
            if (deltaX * deltaX + deltaY * deltaY < MIN_PATH_POINT_DISTANCE_SQUARED
                    && !phaseChanged && !lastSample) continue;

            trajectory.add(new TrajectoryLeg(path.branchId(), previousX, previousY, sampleX, sampleY,
                    previous.phase(), previous.projected()));
            previous = sample;
            previousX = sampleX;
            previousY = sampleY;
        }
        if (path.terminalState() != RocketFlightPathCalculator.TerminalState.DISCARDED) {
            var marker = new ItemWidget(Math.round(previousX - 6), Math.round(previousY - 6), 12,
                    new ItemStack(Items.FIREWORK_ROCKET));
            marker.withShowOverlay(false).withTooltipFromStack(false);
            rocketMarkers.add(new BranchMarker(path.branchId(), marker));
        }
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (height - 10));
    }

    private int contentBottom() {
        return contentHeight - 24;
    }

    private static int distancePixels(double distance) {
        return (int) Math.round(distancePixelsExact(distance));
    }

    private static double distancePixelsExact(double distance) {
        double clampedDistance = Math.clamp(distance, 0, MAX_DISPLAY_DISTANCE);
        double maximum = Math.log1p(MAX_DISPLAY_DISTANCE / DISTANCE_SCALE);
        // One continuous logarithmic transform keeps nearby orbits readable without creating slope changes where
        // distance bands meet. Objects and paths must always use this same transform.
        return Math.log1p(clampedDistance / DISTANCE_SCALE) / maximum * DISTANCE_PIXELS;
    }

    private float mapX(double spaceX) {
        double normalizedX = Math.clamp(spaceX / SpaceSimulation.HORIZONTAL_POSITION_LIMIT, -1, 1);
        return width / 2f + (float) normalizedX * (width - 68) / 2f;
    }

    private float mapPathX(RocketFlightPathCalculator.PathSample sample) {
        if (sample.targetId().equals(SpaceSimulation.FlightPlanAction.SURFACE_TARGET)) return mapX(sample.x());
        double spaceX = sample.x();
        double distance = sample.y();
        double launchProgress = Math.clamp(distance / LAUNCH_ARC_DISTANCE, 0, 1);
        double smoothProgress = launchProgress * launchProgress * (3 - 2 * launchProgress);
        // This is only presentation easing: launches begin vertically, then blend into their real horizontal path.
        return mapX(spaceX * smoothProgress);
    }

    private static BlockState placeholderBlock(SpaceObjects.ObjectType type) {
        return switch (type) {
            case EARTH -> Blocks.GRASS_BLOCK.defaultBlockState();
            case SUN -> Blocks.GOLD_BLOCK.defaultBlockState();
            case MARS -> Blocks.RED_SAND.defaultBlockState();
            case ASTEROID -> Blocks.IRON_ORE.defaultBlockState();
        };
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0xFF080D18);
        renderedScrollY += (scrollY - renderedScrollY) * 0.22f;

        int viewportX = x + 5;
        int viewportY = y + 5;
        int viewportWidth = width - 10;
        int viewportHeight = height - 10;
        float localMouseX = mouseX - viewportX;
        float localMouseY = mouseY - viewportY + renderedScrollY;

        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(viewportX, viewportY - renderedScrollY);

        var font = Minecraft.getInstance().font;
        for (var marker : ORBIT_MARKERS) {
            int markerY = contentBottom() - distancePixels(marker.distance);
            drawDashedLine(graphics, 5, markerY, viewportWidth - 8, 0xFF354657);
            graphics.text(font, Component.translatable("screen.oritech_space_age.orbit." + marker.translation),
                    8, markerY - 10, 0xFF71879A, false);
        }
        if (!trajectory.isEmpty()) {
            // A custom render state draws sub-pixel lines directly in GUI space. Normal widget fills would round each
            // segment independently and make shallow curves look stair-stepped.
            var pathPose = new Matrix3x2f(graphics.pose());
            var scissor = graphics.peekScissorStack();
            var pathBounds = new ScreenRectangle(0, 0, viewportWidth, contentHeight).transformMaxBounds(pathPose);
            var clippedBounds = scissor == null ? pathBounds : scissor.intersection(pathBounds);
            graphics.submitGuiElementRenderState(new FlightPathRenderState(
                    List.copyOf(trajectory), selectedBranch, pathPose, scissor, clippedBounds));
        }
        hovered = null;
        boolean overStatsPanel = mouseX >= x + 9 && mouseX < x + 171
                && mouseY >= y + 27 && mouseY < y + 72;
        for (var object : objects) {
            object.widget.render(graphics, (int) localMouseX, (int) localMouseY, delta);
            if (!overStatsPanel && isMouseOver(mouseX, mouseY)
                    && object.widget.isMouseOver(localMouseX, localMouseY)) hovered = object;
        }
        for (var marker : rocketMarkers) {
            if (!marker.branchId.equals(selectedBranch)) {
                marker.widget.render(graphics, (int) localMouseX, (int) localMouseY, delta);
            }
        }
        for (var marker : rocketMarkers) {
            if (marker.branchId.equals(selectedBranch)) {
                marker.widget.render(graphics, (int) localMouseX, (int) localMouseY, delta);
            }
        }
        pose.popMatrix();
        graphics.disableScissor();

        graphics.fill(x + 6, y + 6, x + width - 7, y + 21, 0xD8080D18);
        graphics.text(font, Component.translatable("screen.oritech_space_age.star_system"),
                x + 10, y + 9, 0xFFCAD8E5, true);
        graphics.fill(x + width - 180, y + 12, x + width - 174, y + 14, 0xFFFF8A20);
        graphics.text(font, Component.translatable("screen.oritech_space_age.path.burn"),
                x + width - 170, y + 9, 0xFFCAD8E5, false);
        graphics.fill(x + width - 126, y + 12, x + width - 120, y + 14, 0xFF66B9D5);
        graphics.text(font, Component.translatable("screen.oritech_space_age.path.coast"),
                x + width - 116, y + 9, 0xFFCAD8E5, false);
        graphics.fill(x + width - 68, y + 12, x + width - 66, y + 14, 0xFFB68CFF);
        graphics.fill(x + width - 64, y + 12, x + width - 62, y + 14, 0xFFB68CFF);
        graphics.text(font, Component.translatable("screen.oritech_space_age.path.projected"),
                x + width - 58, y + 9, 0xFFCAD8E5, false);
        OritechSurface.PANEL_DARK.render(graphics, x + 9, y + 27, 162, 44);
        graphics.text(font, Component.translatable("screen.oritech_space_age.flight_stats")
                        .withStyle(ChatFormatting.BOLD),
                x + 16, y + 33, 0xFFF2F6FA, false);
        graphics.text(font, Component.translatable("screen.oritech_space_age.last_command_days",
                        String.format(Locale.ROOT, "%.2f", lastCommandDays)),
                x + 16, y + 45, 0xFFCAD8E5, false);
        var selectedPath = pathsByBranch.get(selectedBranch);
        if (selectedPath != null) {
            graphics.text(font, Component.translatable("screen.oritech_space_age.selected_craft_stats",
                            selectedPath.segments().size(),
                            String.format(Locale.ROOT, "%.2f", selectedPath.durationSeconds() / 1_200d)),
                    x + 16, y + 57, 0xFFCAD8E5, false);
        }
        renderScrollbar(graphics, viewportX + viewportWidth - 2, viewportY, viewportHeight);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int barX, int barY, int trackHeight) {
        int thumbHeight = Math.max(12, trackHeight * trackHeight / contentHeight);
        int thumbY = barY + Math.round((trackHeight - thumbHeight) * renderedScrollY / Math.max(1, maxScroll()));
        graphics.fill(barX, barY, barX + 2, barY + trackHeight, 0x443B4652);
        graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, 0xCC8192A3);
    }

    private static void drawDashedLine(GuiGraphicsExtractor graphics, int fromX, int y, int toX, int color) {
        for (int lineX = fromX; lineX < toX; lineX += 8) {
            graphics.fill(lineX, y, Math.min(lineX + 4, toX), y + 1, color);
        }
    }

    @Override
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        scrollY = Math.clamp(scrollY - (float) scrollDelta * 28, 0, maxScroll());
        return true;
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        dragging = true;
        return true;
    }

    @Override
    public boolean handleDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (!dragging || button != 0) return false;
        scrollY = Math.clamp(scrollY - (float) deltaY, 0, maxScroll());
        return true;
    }

    @Override
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (button != 0 || !dragging) return false;
        dragging = false;
        return true;
    }

    @Override
    public boolean hasTooltip() {
        return hovered != null;
    }

    @Override
    public List<Component> getTooltip() {
        if (hovered == null) return List.of();
        return List.of(objectName(hovered.data.type()).copy().withStyle(ChatFormatting.BOLD),
                Component.translatable("screen.oritech_space_age.object.position",
                        format(hovered.data.x()), format(hovered.data.y())),
                Component.translatable("screen.oritech_space_age.object.detection",
                        hovered.data.detectionState().name().toLowerCase(Locale.ROOT)));
    }

    private record MapObject(SpaceSimulation.SpaceObjectData data, BlockWidget widget) {
    }

    private record BranchMarker(UUID branchId, ItemWidget widget) {
    }

    private record TrajectoryLeg(UUID branchId, float fromX, float fromY, float toX, float toY,
                                 RocketFlightPathCalculator.PathPhase phase, boolean projected) {
    }

    private record FlightPathRenderState(List<TrajectoryLeg> legs, UUID selectedBranch, Matrix3x2f pose,
                                         @Nullable ScreenRectangle scissorArea,
                                         @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

        private static final float HALF_WIDTH = 0.65f;
        private static final float DASH_LENGTH = 4f;
        private static final float DASH_GAP = 2.5f;

        @Override
        public void buildVertices(VertexConsumer vertices) {
            float dashOffset = 0;
            boolean continuingProjection = false;
            for (var leg : legs) {
                float dx = leg.toX - leg.fromX;
                float dy = leg.toY - leg.fromY;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                if (length < 0.001f) continue;

                int color = switch (leg.phase) {
                    case PLANNED_BURN -> 0xFFFF8A20;
                    case PLANNED_COAST -> 0xFF66B9D5;
                    case PROJECTED_BURN -> 0xFFB68CFF;
                };
                if (!leg.branchId.equals(selectedBranch)) color = color & 0x00FFFFFF | 0x66000000;
                if (leg.projected) {
                    // Keep the dash position across physics samples so the pattern does not restart at every leg.
                    if (!continuingProjection) dashOffset = 0;
                    dashOffset = addDashedLine(vertices, leg, length, color, dashOffset);
                    continuingProjection = true;
                } else {
                    addLine(vertices, leg.fromX, leg.fromY, leg.toX, leg.toY, color);
                    continuingProjection = false;
                }
            }
        }

        private float addDashedLine(VertexConsumer vertices, TrajectoryLeg leg, float length,
                                    int color, float dashOffset) {
            float cycleLength = DASH_LENGTH + DASH_GAP;
            float distance = 0;
            while (distance < length) {
                float cyclePosition = (dashOffset + distance) % cycleLength;
                boolean drawing = cyclePosition < DASH_LENGTH;
                float sectionLength = (drawing ? DASH_LENGTH : cycleLength) - cyclePosition;
                float sectionEnd = Math.min(length, distance + sectionLength);
                if (drawing) {
                    float startProgress = distance / length;
                    float endProgress = sectionEnd / length;
                    addLine(vertices,
                            lerp(leg.fromX, leg.toX, startProgress),
                            lerp(leg.fromY, leg.toY, startProgress),
                            lerp(leg.fromX, leg.toX, endProgress),
                            lerp(leg.fromY, leg.toY, endProgress), color);
                }
                distance = sectionEnd;
            }
            return (dashOffset + length) % cycleLength;
        }

        private static float lerp(float start, float end, float progress) {
            return start + (end - start) * progress;
        }

        private void addLine(VertexConsumer vertices, float fromX, float fromY,
                             float toX, float toY, int color) {
            float dx = toX - fromX;
            float dy = toY - fromY;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.001f) return;

            float normalX = -dy / length * HALF_WIDTH;
            float normalY = dx / length * HALF_WIDTH;
            vertices.addVertexWith2DPose(pose, fromX - normalX, fromY - normalY).setColor(color);
            vertices.addVertexWith2DPose(pose, fromX + normalX, fromY + normalY).setColor(color);
            vertices.addVertexWith2DPose(pose, toX + normalX, toY + normalY).setColor(color);
            vertices.addVertexWith2DPose(pose, toX - normalX, toY - normalY).setColor(color);
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

    private record OrbitMarker(double distance, String translation) {
    }

    private static Component objectName(SpaceObjects.ObjectType type) {
        return Component.translatable("screen.oritech_space_age.object." + type.name().toLowerCase(Locale.ROOT));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }

}
