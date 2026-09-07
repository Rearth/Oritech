package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
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
import java.util.function.Consumer;

/** A pannable and zoomable view of the shared solar-system plane. */
final class RocketStarMapWidget extends UIComponent {

    // Celestial object state, orbit geometry, and object hover detection.
    private final StarMapObjects mapObjects = new StarMapObjects();
    // One rocket icon at the end of each surviving branch.
    private final List<BranchMarker> rocketMarkers = new ArrayList<>();
    // Group boosters released together into one map marker.
    private final List<SeparationMarker> separationMarkers = new ArrayList<>();
    // Find a branch preview for selection and statistics.
    private final Map<UUID, RocketFlightPathCalculator.CraftPath> pathsByBranch = new HashMap<>();
    // Full visible path detail for tooltips, rebuilt only when the view or flight changes.
    private final Map<UUID, List<RenderedPathSegment>> renderedPathsByBranch = new HashMap<>();
    // Simplified draw geometry is reused while the player is looking at a stationary map.
    private List<StarMapLineRenderer.Line> cachedLines = List.of();
    private View cachedView;
    // A stationary pointer does not need another search through all trajectory samples every frame.
    private PathHover cachedPathHover;
    // Find the card behind a sample or arrival event.
    private final Map<UUID, SpaceSimulation.FlightPlanAction> actionsById = new HashMap<>();
    // Stable fallback labels shared with the assembler's segment ordering.
    private final Map<SpaceSimulation.SegmentRef, String> defaultSegmentNames = new HashMap<>();
    // Tell the planner when the player selects a surface or orbit.
    private final Consumer<NavigationSelection> selectionListener;
    // Ask the planner to open the target's right-click menu.
    private final Consumer<NavigationContextRequest> contextMenuListener;
    // World-space curves rebuilt only when the flight preview changes.
    private RocketMapPaths mapPaths;
    // Current solar system and draft plan used for labels and selection.
    private SpaceSimulation.FlightPlannerSnapshot snapshot;
    // Calculated flight samples and events shown by the map.
    private RocketFlightPathCalculator.FlightPath flightPath;
    // Branch drawn brightly and used for the statistics panel.
    private UUID selectedBranch;
    // Surface or orbit currently selected in the planner.
    private NavigationSelection selectedTarget;
    // Object icon under the pointer this frame.
    private StarMapObjects.Entry hoveredObject;
    // Surface or orbit under the pointer, ready for selection.
    private NavigationSelection hoveredSelection;
    // Booster event under the pointer, shown before other tooltips.
    private SeparationMarker hoveredSeparation;
    // Path state under the pointer, including interpolated speed.
    private StarMapTooltip.PathPoint hoveredPathPoint;
    // View conversion and zoom limits for this map instance.
    private final StarMapCamera camera = new StarMapCamera();
    // A left-button press is active inside the map.
    private boolean dragging;
    // Prevent a pan from also selecting the target under the pointer.
    private boolean movedWhileDragging;
    // The planner can hide map tooltips while a popup is open.
    private boolean tooltipsEnabled = true;

    RocketStarMapWidget(int x, int y, int width, int height,
                        SpaceSimulation.FlightPlannerSnapshot snapshot,
                        RocketFlightPathCalculator.FlightPath flightPath,
                        ActiveRocketData rocket,
                        UUID selectedBranch, NavigationSelection selectedTarget,
                        Consumer<NavigationSelection> selectionListener,
                        Consumer<NavigationContextRequest> contextMenuListener) {
        super(x, y, width, height);
        surface = OritechSurface.PANEL_INSET;
        this.selectionListener = selectionListener;
        this.contextMenuListener = contextMenuListener;
        this.selectedTarget = selectedTarget;
        var segments = rocket.getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of)
                .sorted(java.util.Comparator.comparingInt((SpaceSimulation.SegmentRef item) -> item.anchor().getY())
                        .thenComparingInt(item -> item.anchor().getX())
                        .thenComparingInt(item -> item.anchor().getZ()))
                .toList();
        for (int index = 0; index < segments.size(); index++) {
            defaultSegmentNames.put(segments.get(index), "S" + (index + 1));
        }
        updateFlightPath(snapshot, flightPath, selectedBranch);
        fitSystem();
    }

    void updateFlightPath(SpaceSimulation.FlightPlannerSnapshot snapshot,
                          RocketFlightPathCalculator.FlightPath flightPath,
                          UUID selectedBranch) {
        this.snapshot = snapshot;
        mapObjects.setSnapshot(snapshot);
        this.flightPath = flightPath;
        mapPaths = new RocketMapPaths(snapshot.plan(), flightPath);
        this.selectedBranch = selectedBranch;
        pathsByBranch.clear();
        renderedPathsByBranch.clear();
        cachedView = null;
        cachedPathHover = null;
        actionsById.clear();
        snapshot.plan().branches().forEach(branch -> branch.actions()
                .forEach(action -> actionsById.put(action.id(), action)));
        flightPath.paths().forEach(path -> pathsByBranch.put(path.branchId(), path));
        updateRocketMarkers();
        mapObjects.updateArrivalPositions(pathsByBranch.get(selectedBranch), actionsById, snapshot);
        updateSeparationMarkers();
    }

    private void updateRocketMarkers() {
        rocketMarkers.clear();
        for (var path : flightPath.paths()) {
            if (path.samples().isEmpty()
                    || path.terminalState() == RocketFlightPathCalculator.TerminalState.DISCARDED
                    || path.terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED) continue;
            var last = path.samples().getLast();
            var marker = new ItemWidget(0, 0, 12, new ItemStack(Items.FIREWORK_ROCKET));
            marker.withShowOverlay(false).withTooltipFromStack(false);
            rocketMarkers.add(new BranchMarker(path.branchId(), last.timeSeconds(), last.x(), last.y(), marker));
        }
    }

    private void updateSeparationMarkers() {
        // Several boosters can detach at once; list them together instead of drawing overlapping crosses.
        separationMarkers.clear();
        for (var event : flightPath.boosterEvents()) {
            var marker = separationMarkers.stream().filter(existing -> existing.matches(event)).findFirst().orElse(null);
            if (marker == null) {
                marker = new SeparationMarker(event.branchId(), event.stage(), event.timeSeconds(),
                        event.x(), event.y(), new ArrayList<>(), new ArrayList<>());
                separationMarkers.add(marker);
            }
            marker.segments.add(event.segment());
            marker.childBranches.add(event.childBranchId());
        }
    }

    void setSelectedTarget(NavigationSelection selection) {
        selectedTarget = selection;
    }

    void copyViewFrom(RocketStarMapWidget previous) {
        if (previous == null || !previous.snapshot.simulationId().equals(snapshot.simulationId())) return;
        camera.copyFrom(previous.camera);
    }

    private void fitSystem() {
        camera.fit(mapObjects.positions(),
                viewportX(), viewportY(), viewportWidth(), viewportHeight());
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int viewportX = x + 5;
        int viewportY = y + 22;
        int viewportWidth = width - 10;
        int viewportHeight = height - 27;
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        // Keep the animated portal backdrop; a dark veil preserves path and label contrast above it.
        var textures = Minecraft.getInstance().getTextureManager();
        AbstractTexture skyTexture = textures.getTexture(AbstractEndPortalRenderer.END_SKY_LOCATION);
        AbstractTexture portalTexture = textures.getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION);
        var textureSetup = TextureSetup.doubleTexture(skyTexture.getTextureView(), skyTexture.getSampler(),
                portalTexture.getTextureView(), portalTexture.getSampler());
        graphics.fill(RenderPipelines.END_PORTAL, textureSetup,
                viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        graphics.fill(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight, 0x99030810);

        var viewport = new StarMapObjects.Viewport(viewportX, viewportY, viewportWidth, viewportHeight);
        mapObjects.renderSurfaceDiscs(graphics, camera, viewport);
        prepareLines(viewport);
        StarMapLineRenderer.submit(graphics, cachedLines, viewportX, viewportY, viewportWidth, viewportHeight);

        hoveredObject = null;
        hoveredSelection = null;
        hoveredSeparation = null;
        hoveredPathPoint = null;
        findHoveredPath(mouseX, mouseY);
        hoveredObject = mapObjects.renderIcons(graphics, camera, viewport, selectedTarget, mouseX, mouseY, delta,
                isInsideViewport(mouseX, mouseY));
        if (isInsideViewport(mouseX, mouseY)) hoveredSelection = mapObjects.findHoveredOrbit(mouseX, mouseY, camera, viewport, hoveredObject);
        for (var marker : rocketMarkers) renderRocket(graphics, marker, mouseX, mouseY, delta);
        renderSeparations(graphics, mouseX, mouseY);
        graphics.disableScissor();

        StarMapOverlay.render(graphics, x, y, width, selectedTargetLabel(), flightPath, pathsByBranch.get(selectedBranch));
    }

    private void prepareLines(StarMapObjects.Viewport viewport) {
        var view = new View(camera.revision(), x, y, width, height, selectedTarget);
        if (view.equals(cachedView)) return;
        var lines = new ArrayList<StarMapLineRenderer.Line>();
        mapObjects.addReferenceLines(lines, camera, viewport, selectedTarget);
        addFlightPaths(lines);
        cachedLines = StarMapLineSimplifier.simplify(lines, viewport.x(), viewport.y(), viewport.width(), viewport.height());
        cachedView = view;
        cachedPathHover = null;
    }

    private void addFlightPaths(List<StarMapLineRenderer.Line> lines) {
        if (flightPath == null) return;
        renderedPathsByBranch.clear();
        for (var path : flightPath.paths()) {
            var renderedSegments = renderedPathSegments(path);
            renderedPathsByBranch.put(path.branchId(), renderedSegments);
            for (var segment : renderedSegments) {
                var second = segment.secondSample;
                int color = switch (second.phase()) {
                    case ACCELERATE -> 0xFFFF8A20;
                    case REDIRECT -> 0xFF8FDB68;
                    case COAST -> 0xFF66B9D5;
                    case BRAKE -> 0xFFB68CFF;
                };
                if (!path.branchId().equals(selectedBranch)) color = color & 0x00FFFFFF | 0x66000000;
                lines.add(new StarMapLineRenderer.Line(segment.from.x, segment.from.y, segment.to.x, segment.to.y, color,
                        path.branchId().equals(selectedBranch) ? 1.3f : 0.8f, true));
            }
        }
        for (var path : flightPath.asteroidPaths()) {
            for (var segment : mapPaths.renderedAsteroidPathSegments(path)) {
                var from = project(segment.from().x(), segment.from().y());
                var to = project(segment.to().x(), segment.to().y());
                lines.add(new StarMapLineRenderer.Line(from.x, from.y, to.x, to.y, 0xFFD89A62, 1.1f, true));
            }
        }
    }

    private List<RenderedPathSegment> renderedPathSegments(RocketFlightPathCalculator.CraftPath path) {
        // All map consumers use the same curve. Only projection changes when the camera moves.
        return mapPaths.renderedPathSegments(path).stream().map(segment -> new RenderedPathSegment(
                project(segment.from().x(), segment.from().y()), project(segment.to().x(), segment.to().y()),
                segment.firstSample(), segment.secondSample(),
                segment.sampleProgressFrom(), segment.sampleProgressTo()))
                .filter(segment -> StarMapLineSimplifier.intersects(segment.from.x, segment.from.y, segment.to.x, segment.to.y,
                        viewportX(), viewportY(), viewportWidth(), viewportHeight(), 6)).toList();
    }

    private void renderRocket(GuiGraphicsExtractor graphics, BranchMarker marker,
                              int mouseX, int mouseY, float delta) {
        var position = renderedPosition(marker.branchId, marker.timeSeconds, marker.worldX, marker.worldY);
        if (!StarMapLineSimplifier.intersects(position.x, position.y, position.x, position.y,
                viewportX(), viewportY(), viewportWidth(), viewportHeight(), 8)) return;
        marker.widget.setPosition((int) Math.round(position.x - 6), (int) Math.round(position.y - 6));
        marker.widget.render(graphics, mouseX, mouseY, delta);
    }

    private void renderSeparations(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (var marker : separationMarkers) {
            var position = renderedPosition(marker.branchId, marker.timeSeconds, marker.worldX, marker.worldY);
            if (!StarMapLineSimplifier.intersects(position.x, position.y, position.x, position.y,
                    viewportX(), viewportY(), viewportWidth(), viewportHeight(), 8)) continue;
            int markerX = (int) Math.round(position.x);
            int markerY = (int) Math.round(position.y);
            boolean selected = marker.branchId.equals(selectedBranch) || marker.childBranches.contains(selectedBranch);
            int color = selected ? 0xFFFFD45C : 0xAAFFD45C;
            graphics.fill(markerX - 1, markerY - 5, markerX + 2, markerY + 6, color);
            graphics.fill(markerX - 5, markerY - 1, markerX + 6, markerY + 2, color);
            if (isInsideViewport(mouseX, mouseY) && Math.hypot(mouseX - position.x, mouseY - position.y) <= 7) {
                hoveredSeparation = marker;
            }
        }
    }

    private Point renderedPosition(UUID branchId, double timeSeconds, double worldX, double worldY) {
        var position = mapPaths.renderedWorldPosition(branchId, timeSeconds, worldX, worldY);
        return project(position.x(), position.y());
    }

    private void findHoveredPath(double mouseX, double mouseY) {
        if (flightPath == null || !isInsideViewport(mouseX, mouseY)) return;
        if (cachedPathHover != null && cachedPathHover.x == mouseX && cachedPathHover.y == mouseY) {
            hoveredPathPoint = cachedPathHover.point;
            return;
        }
        double closestDistance = 6;
        for (var path : flightPath.paths()) {
            var renderedSegments = renderedPathsByBranch.get(path.branchId());
            if (renderedSegments == null) renderedSegments = renderedPathSegments(path);
            for (var segment : renderedSegments) {
                var from = segment.from;
                var to = segment.to;
                if (mouseX < Math.min(from.x, to.x) - closestDistance || mouseX > Math.max(from.x, to.x) + closestDistance
                        || mouseY < Math.min(from.y, to.y) - closestDistance || mouseY > Math.max(from.y, to.y) + closestDistance) continue;
                double lineX = to.x - from.x;
                double lineY = to.y - from.y;
                double lengthSquared = lineX * lineX + lineY * lineY;
                double progress = lengthSquared <= 0 ? 0 : Math.clamp(
                        ((mouseX - from.x) * lineX + (mouseY - from.y) * lineY) / lengthSquared, 0, 1);
                double nearestX = from.x + lineX * progress;
                double nearestY = from.y + lineY * progress;
                double distance = Math.hypot(mouseX - nearestX, mouseY - nearestY);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    // Samples describe the interval ending at that sample, matching the path's colour.
                    double sampleProgress = segment.sampleProgressFrom
                            + (segment.sampleProgressTo - segment.sampleProgressFrom) * progress;
                    hoveredPathPoint = new StarMapTooltip.PathPoint(segment.secondSample,
                            interpolatePathSpeed(segment.firstSample, segment.secondSample, sampleProgress));
                }
            }
        }
        cachedPathHover = new PathHover(mouseX, mouseY, hoveredPathPoint);
    }

    private static double interpolatePathSpeed(RocketFlightPathCalculator.PathSample first,
                                               RocketFlightPathCalculator.PathSample second,
                                               double progress) {
        // Approximate speed along the short displayed chord between two trajectory samples.
        double velocityX = first.velocityX() + (second.velocityX() - first.velocityX()) * progress;
        double velocityY = first.velocityY() + (second.velocityY() - first.velocityY()) * progress;
        return Math.hypot(velocityX, velocityY);
    }

    private Component selectedTargetLabel() {
        if (selectedTarget == null) return null;
        var object = mapObjects.byId(selectedTarget.objectId);
        return object == null ? null : Component.translatable("screen.oritech_space_age.selected_target",
                objectName(object.data()), orbitName(selectedTarget.orbit));
    }

    @Override
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        if (!isInsideViewport(mouseX, mouseY)) return false;
        camera.zoomAt(mouseX, mouseY, scrollDelta, viewportX(), viewportY(), viewportWidth(), viewportHeight());
        return true;
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!isInsideViewport(mouseX, mouseY)) return false;
        if (button == 1 && hoveredObject != null) {
            contextMenuListener.accept(new NavigationContextRequest(
                    new NavigationSelection(hoveredObject.data().id(), SpaceSimulation.OrbitBand.SURFACE),
                    (int) mouseX, (int) mouseY));
            return true;
        }
        if (button != 0) return false;
        dragging = true;
        movedWhileDragging = false;
        return true;
    }

    @Override
    public boolean handleDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (!dragging || button != 0) return false;
        camera.pan(deltaX, deltaY);
        movedWhileDragging |= Math.abs(deltaX) + Math.abs(deltaY) > 0.5;
        return true;
    }

    @Override
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) return false;
        dragging = false;
        if (!movedWhileDragging && hoveredSelection != null) {
            selectedTarget = hoveredSelection;
            selectionListener.accept(selectedTarget);
        }
        return true;
    }

    @Override
    public boolean hasTooltip() {
        return tooltipsEnabled && (hoveredSeparation != null || hoveredObject != null || hoveredSelection != null
                || hoveredPathPoint != null);
    }

    @Override
    public List<Component> getTooltip() {
        if (!tooltipsEnabled) return List.of();
        StarMapObjects.Entry object = hoveredSelection == null ? hoveredObject : mapObjects.byId(hoveredSelection.objectId);
        var separation = hoveredSeparation == null ? null
                : new StarMapTooltip.Separation(hoveredSeparation.stage, hoveredSeparation.timeSeconds, hoveredSeparation.segments);
        return StarMapTooltip.build(separation, object == null ? null : object.data(), hoveredSelection,
                object == null ? null : mapObjects.arrivalPosition(object.data().id()), hoveredPathPoint,
                snapshot.plan(), defaultSegmentNames, id -> {
                    var found = mapObjects.byId(id);
                    return found == null ? null : found.data();
                });
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        boolean insideMap = mouseX >= viewportX() && mouseX < viewportX() + viewportWidth()
                && mouseY >= viewportY() && mouseY < viewportY() + viewportHeight();
        boolean overStats = mouseX >= x + 9 && mouseX < x + 185 && mouseY >= y + 27 && mouseY < y + 85;
        boolean overLegend = mouseX >= x + width - 181 && mouseX < x + width - 9
                && mouseY >= y + 27 && mouseY < y + 103;
        return insideMap && !overStats && !overLegend;
    }

    private Point project(double worldX, double worldY) {
        var point = camera.project(worldX, worldY, viewportX(), viewportY(), viewportWidth(), viewportHeight());
        return new Point(point.x(), point.y());
    }

    private int viewportX() {
        return x + 5;
    }

    private int viewportY() {
        return y + 22;
    }

    private int viewportWidth() {
        return width - 10;
    }

    private int viewportHeight() {
        return height - 27;
    }

    void setTooltipsEnabled(boolean enabled) {
        tooltipsEnabled = enabled;
    }

    static Component objectName(SpaceObjects.ObjectType type) {
        return Component.translatable("screen.oritech_space_age.object." + type.name().toLowerCase(Locale.ROOT));
    }

    static Component objectName(SpaceSimulation.SpaceObjectData object) {
        return object.name().isBlank() ? objectName(object.type()) : Component.literal(object.name());
    }

    static Component orbitName(SpaceSimulation.OrbitBand orbit) {
        return Component.translatable("screen.oritech_space_age.orbit." + orbit.name().toLowerCase(Locale.ROOT));
    }

    /**
     * @param objectId Selected celestial object.
     * @param orbit Selected surface or orbit band.
     */
    record NavigationSelection(UUID objectId, SpaceSimulation.OrbitBand orbit) {
    }

    /**
     * @param selection Target for the context menu.
     * @param mouseX Pointer X in screen pixels.
     * @param mouseY Pointer Y in screen pixels.
     */
    record NavigationContextRequest(NavigationSelection selection, int mouseX, int mouseY) {
    }

    /** Camera revision, panel bounds and selected ring determine all projected line geometry. */
    private record View(long cameraRevision, int x, int y, int width, int height, NavigationSelection target) {
    }

    /** Pointer position and its last path hit; a null point means nothing was close enough. */
    private record PathHover(double x, double y, StarMapTooltip.PathPoint point) {
    }

    /**
     * @param branchId Branch owning this result.
     * @param timeSeconds Seconds since the root branch started.
     * @param worldX Unmodified simulation X used as a fallback.
     * @param worldY Unmodified simulation Y used as a fallback.
     * @param widget Icon drawn at this position.
     */
    private record BranchMarker(UUID branchId, double timeSeconds,
                                double worldX, double worldY, ItemWidget widget) {
    }

    /**
     * @param branchId Branch owning this result.
     * @param stage One-based engine stage.
     * @param timeSeconds Seconds since the root branch started.
     * @param worldX Unmodified simulation X used as a fallback.
     * @param worldY Unmodified simulation Y used as a fallback.
     * @param segments Boosters grouped at this marker.
     * @param childBranches Branches released together at this marker.
     */
    private record SeparationMarker(UUID branchId, int stage, double timeSeconds,
                                    double worldX, double worldY,
                                    List<SpaceSimulation.SegmentRef> segments, List<UUID> childBranches) {
        private boolean matches(RocketFlightPathCalculator.BoosterEvent event) {
            return branchId.equals(event.branchId()) && stage == event.stage()
                    && Math.abs(timeSeconds - event.timeSeconds()) < 0.01
                    && Math.hypot(worldX - event.x(), worldY - event.y()) < 1;
        }
    }

    // Projected geometry keeps the original samples and interpolation range for hover details.
    /**
     * @param from Start of this line piece.
     * @param to End of this line piece.
     * @param firstSample Simulation state before this piece.
     * @param secondSample State after this piece, including phase and firing engines.
     * @param sampleProgressFrom Fraction of the original sample interval where this piece starts.
     * @param sampleProgressTo Fraction of the original sample interval where this piece ends.
     */
    private record RenderedPathSegment(Point from, Point to,
                                       RocketFlightPathCalculator.PathSample firstSample,
                                       RocketFlightPathCalculator.PathSample secondSample,
                                       double sampleProgressFrom, double sampleProgressTo) {
    }

    /**
     * @param x X coordinate; projection converts between world and screen space.
     * @param y Y coordinate; projection converts between world and screen space.
     */
    private record Point(double x, double y) {
    }

}
