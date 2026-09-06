package rearth.oritech.spaceage.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
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
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
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

    private static final double PLANE_TILT = 0.58;
    private static final double MAX_ZOOM = 0.024;
    private static final int CIRCLE_SEGMENTS = 72;
    private static final double TRANSFER_CURVE_RATIO = 0.12;

    private final List<MapObject> objects = new ArrayList<>();
    private final List<BranchMarker> rocketMarkers = new ArrayList<>();
    private final List<SeparationMarker> separationMarkers = new ArrayList<>();
    private final Map<UUID, RocketFlightPathCalculator.CraftPath> pathsByBranch = new HashMap<>();
    private final Map<UUID, List<RenderedPathSegment>> renderedPathsByBranch = new HashMap<>();
    private final Map<UUID, SpaceSimulation.FlightPlanAction> actionsById = new HashMap<>();
    private final Map<UUID, MotionPosition> displayedObjectPositions = new HashMap<>();
    private final Map<SpaceSimulation.SegmentRef, String> defaultSegmentNames = new HashMap<>();
    private final Consumer<NavigationSelection> selectionListener;
    private final Consumer<NavigationContextRequest> contextMenuListener;
    private SpaceSimulation.FlightPlannerSnapshot snapshot;
    private RocketFlightPathCalculator.FlightPath flightPath;
    private UUID selectedBranch;
    private NavigationSelection selectedTarget;
    private MapObject hoveredObject;
    private NavigationSelection hoveredSelection;
    private SeparationMarker hoveredSeparation;
    private HoveredPathPoint hoveredPathPoint;
    private double cameraX;
    private double cameraY;
    private double zoom;
    private double minimumZoom;
    private boolean dragging;
    private boolean movedWhileDragging;
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
        setSnapshot(snapshot);
        updateFlightPath(snapshot, flightPath, selectedBranch);
        fitSystem();
    }

    void updateFlightPath(SpaceSimulation.FlightPlannerSnapshot snapshot,
                          RocketFlightPathCalculator.FlightPath flightPath,
                          UUID selectedBranch) {
        this.snapshot = snapshot;
        this.flightPath = flightPath;
        this.selectedBranch = selectedBranch;
        pathsByBranch.clear();
        actionsById.clear();
        snapshot.plan().branches().forEach(branch -> branch.actions()
                .forEach(action -> actionsById.put(action.id(), action)));
        displayedObjectPositions.clear();
        rocketMarkers.clear();
        separationMarkers.clear();
        for (var path : flightPath.paths()) {
            pathsByBranch.put(path.branchId(), path);
            if (path.samples().isEmpty()
                    || path.terminalState() == RocketFlightPathCalculator.TerminalState.DISCARDED
                    || path.terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED) continue;
            var last = path.samples().getLast();
            var marker = new ItemWidget(0, 0, 12, new ItemStack(Items.FIREWORK_ROCKET));
            marker.withShowOverlay(false).withTooltipFromStack(false);
            rocketMarkers.add(new BranchMarker(path.branchId(), last.timeSeconds(), last.x(), last.y(), marker));
        }
        var selectedPath = pathsByBranch.get(selectedBranch);
        if (selectedPath != null) {
            for (var moment : selectedPath.actionMoments()) {
                var action = actionsById.get(moment.actionId());
                if (!moment.completed() || action == null
                        || action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) continue;
                var target = snapshot.objects().stream().filter(object -> object.id().equals(action.targetId()))
                        .findFirst().orElse(null);
                if (target == null || target.type() != SpaceObjects.ObjectType.ASTEROID) continue;
                displayedObjectPositions.put(target.id(), new MotionPosition(
                        target.xAt(moment.timeSeconds()), target.yAt(moment.timeSeconds()), moment.timeSeconds()));
            }
        }
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
        cameraX = previous.cameraX;
        cameraY = previous.cameraY;
        zoom = Math.clamp(previous.zoom, minimumZoom * 0.5, MAX_ZOOM);
    }

    private void setSnapshot(SpaceSimulation.FlightPlannerSnapshot snapshot) {
        this.snapshot = snapshot;
        objects.clear();
        for (var object : snapshot.objects()) {
            int size = object.type() == SpaceObjects.ObjectType.ASTEROID ? 12 : 18;
            objects.add(new MapObject(object, new BlockWidget(0, 0, size, placeholderBlock(object.type()))));
        }
    }

    private void fitSystem() {
        if (objects.isEmpty()) {
            zoom = minimumZoom = 0.0001;
            return;
        }
        double minX = objects.stream().mapToDouble(this::objectX).min().orElse(-1);
        double maxX = objects.stream().mapToDouble(this::objectX).max().orElse(1);
        double minY = objects.stream().mapToDouble(this::objectY).min().orElse(-1);
        double maxY = objects.stream().mapToDouble(this::objectY).max().orElse(1);
        cameraX = (minX + maxX) * 0.5;
        cameraY = (minY + maxY) * 0.5;
        double fitX = Math.max(1, maxX - minX) * 1.12;
        double fitY = Math.max(1, maxY - minY) * PLANE_TILT * 1.12;
        minimumZoom = Math.min((width - 30) / fitX, (height - 40) / fitY) * 0.7;
        zoom = minimumZoom * 1.35;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int viewportX = x + 5;
        int viewportY = y + 22;
        int viewportWidth = width - 10;
        int viewportHeight = height - 27;
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        var textureManager = Minecraft.getInstance().getTextureManager();
        AbstractTexture skyTexture = textureManager.getTexture(AbstractEndPortalRenderer.END_SKY_LOCATION);
        AbstractTexture portalTexture = textureManager.getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION);
        var textureSetup = TextureSetup.doubleTexture(skyTexture.getTextureView(), skyTexture.getSampler(),
                portalTexture.getTextureView(), portalTexture.getSampler());
        graphics.fill(RenderPipelines.END_PORTAL, textureSetup,
                viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        // The vanilla portal shader is intentionally bright. This veil keeps the map labels and paths readable.
        graphics.fill(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight, 0x99030810);

        renderSurfaceDiscs(graphics, viewportX, viewportY, viewportWidth, viewportHeight);
        var lines = new ArrayList<LineSegment>();
        addSolarOrbits(lines);
        addSurfaceRadii(lines);
        addAsteroidMotion(lines);
        addLocalOrbits(lines);
        addFlightPaths(lines);
        submitLines(graphics, lines, viewportX, viewportY, viewportWidth, viewportHeight);

        hoveredObject = null;
        hoveredSelection = null;
        hoveredSeparation = null;
        hoveredPathPoint = null;
        findHoveredPath(mouseX, mouseY);
        for (var object : objects) renderObject(graphics, object, mouseX, mouseY, delta);
        findHoveredSelection(mouseX, mouseY);
        for (var marker : rocketMarkers) renderRocket(graphics, marker, mouseX, mouseY, delta);
        renderSeparations(graphics, mouseX, mouseY);
        graphics.disableScissor();

        renderHeader(graphics);
        renderStats(graphics);
        renderLegend(graphics);
    }

    private void addSolarOrbits(List<LineSegment> lines) {
        var sun = objectById(SpaceSimulation.SUN_ID);
        if (sun == null) return;
        for (var object : objects) {
            if (object.data.type() != SpaceObjects.ObjectType.EARTH
                    && object.data.type() != SpaceObjects.ObjectType.MARS) continue;
            double radius = Math.hypot(object.data.x() - sun.data.x(), object.data.y() - sun.data.y());
            addCircle(lines, sun.data.x(), sun.data.y(), radius, 0x553C5368, 0.55f);
        }
    }

    private void addLocalOrbits(List<LineSegment> lines) {
        for (var object : objects) {
            double objectX = objectX(object);
            double objectY = objectY(object);
            for (var band : RocketFlightPlanRules.availableOrbits(object.data.type())) {
                if (band == SpaceSimulation.OrbitBand.SURFACE) continue;
                double radius = object.data.radius() + band.altitude();
                if (radius * zoom < 18) continue;
                boolean selected = selectedTarget != null && selectedTarget.objectId.equals(object.data.id())
                        && selectedTarget.orbit == band;
                addCircle(lines, objectX, objectY, radius,
                        selected ? 0xDDF6C65B : 0x77577A91, selected ? 1.2f : 0.65f);
            }
        }
    }

    private void addSurfaceRadii(List<LineSegment> lines) {
        for (var object : objects) {
            if (object.data.radius() * zoom < 2) continue;
            boolean selected = selectedTarget != null && selectedTarget.objectId.equals(object.data.id())
                    && selectedTarget.orbit == SpaceSimulation.OrbitBand.SURFACE;
            addCircle(lines, objectX(object), objectY(object), object.data.radius(),
                    selected ? 0xDDF6C65B : 0x997C93A6, selected ? 1.25f : 0.8f);
        }
    }

    private void renderSurfaceDiscs(GuiGraphicsExtractor graphics,
                                    int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        for (var object : objects) {
            if (object.data.radius() * zoom < 2) continue;
            var center = project(objectX(object), objectY(object));
            int color = switch (object.data.type()) {
                case SUN -> 0x66D7A438;
                case EARTH -> 0x665A91B8;
                case MARS -> 0x66975D4C;
                case ASTEROID -> 0x666F675E;
            };
            double radiusX = object.data.radius() * zoom;
            double radiusY = radiusX * PLANE_TILT;
            int startY = Math.max(viewportY, (int) Math.floor(center.y - radiusY));
            int endY = Math.min(viewportY + viewportHeight, (int) Math.ceil(center.y + radiusY));
            for (int row = startY; row < endY; row++) {
                double normalizedY = (row + 0.5 - center.y) / radiusY;
                double halfWidth = radiusX * Math.sqrt(Math.max(0, 1 - normalizedY * normalizedY));
                int startX = Math.max(viewportX, (int) Math.floor(center.x - halfWidth));
                int endX = Math.min(viewportX + viewportWidth, (int) Math.ceil(center.x + halfWidth));
                if (startX < endX) graphics.fill(startX, row, endX, row + 1, color);
            }
        }
    }

    private void addAsteroidMotion(List<LineSegment> lines) {
        for (var object : objects) {
            var position = displayedObjectPositions.get(object.data.id());
            if (position == null) continue;
            if (Math.hypot(position.x - object.data.x(), position.y - object.data.y()) < 1) continue;
            var from = project(object.data.x(), object.data.y());
            var to = project(position.x, position.y);
            lines.add(new LineSegment(from.x, from.y, to.x, to.y, 0x6685A7BC, 0.7f));
        }
    }

    private void addFlightPaths(List<LineSegment> lines) {
        if (flightPath == null) return;
        renderedPathsByBranch.clear();
        for (var path : flightPath.paths()) {
            var renderedSegments = renderedPathSegments(path);
            for (var segment : renderedSegments) {
                var second = segment.secondSample;
                int color = switch (second.phase()) {
                    case ACCELERATE -> 0xFFFF8A20;
                    case REDIRECT -> 0xFF8FDB68;
                    case COAST -> 0xFF66B9D5;
                    case BRAKE -> 0xFFB68CFF;
                };
                if (!path.branchId().equals(selectedBranch)) color = color & 0x00FFFFFF | 0x66000000;
                lines.add(new LineSegment(segment.from.x, segment.from.y, segment.to.x, segment.to.y, color,
                        path.branchId().equals(selectedBranch) ? 1.3f : 0.8f));
            }
        }
        for (var path : flightPath.asteroidPaths()) {
            for (var segment : renderedAsteroidPathSegments(path)) {
                lines.add(new LineSegment(segment.from.x, segment.from.y,
                        segment.to.x, segment.to.y, 0xFFD89A62, 1.1f));
            }
        }
    }

    private List<RenderedPathSegment> renderedPathSegments(RocketFlightPathCalculator.CraftPath path) {
        var cached = renderedPathsByBranch.get(path.branchId());
        if (cached != null) return cached;
        var samples = path.samples();
        var segments = new ArrayList<RenderedPathSegment>();
        Point runOrigin = renderedBranchOrigin(path.branchId());
        UUID abortedTarget = null;
        PathCurve abortedCurve = null;
        int runStart = 1;
        while (runStart < samples.size()) {
            UUID actionId = samples.get(runStart).actionId();
            UUID targetId = samples.get(runStart).targetId();
            int runEnd = runStart;
            while (runEnd + 1 < samples.size() && samples.get(runEnd + 1).actionId().equals(actionId)) runEnd++;
            var action = actionsById.get(actionId);
            boolean navigation = action != null && action.type() == SpaceSimulation.ActionType.NAVIGATE_TO;
            Point curveDestination = navigation ? abortDestination(path.branchId(), actionId) : null;
            boolean continuesAbortedNavigation = navigation && targetId.equals(abortedTarget);
            PathCurve inheritedCurve = abortedCurve != null
                    && (!navigation || continuesAbortedNavigation) ? abortedCurve : null;
            if (navigation && inheritedCurve == null && curveDestination == null) {
                abortedTarget = null;
                abortedCurve = null;
            }
            var renderedRun = addRenderedPathRun(segments, samples, runStart - 1, runEnd, runOrigin,
                    curveDestination, inheritedCurve, navigation ? curveDirection(path.branchId()) : 0);
            runOrigin = renderedRun.end;
            if (curveDestination != null) {
                abortedTarget = targetId;
                abortedCurve = renderedRun.curve;
            } else if (navigation && inheritedCurve != null) {
                abortedTarget = null;
                abortedCurve = null;
            }
            runStart = runEnd + 1;
        }
        renderedPathsByBranch.put(path.branchId(), segments);
        return segments;
    }

    private RenderedRun addRenderedPathRun(List<RenderedPathSegment> segments,
                                           List<RocketFlightPathCalculator.PathSample> samples,
                                           int firstIndex, int lastIndex, Point runOrigin,
                                           Point curveDestination, PathCurve inheritedCurve,
                                           int curveDirection) {
        int pointCount = lastIndex - firstIndex + 1;
        var points = new Point[pointCount];
        for (int index = 0; index < pointCount; index++) {
            var sample = samples.get(firstIndex + index);
            points[index] = new Point(sample.x(), sample.y());
        }
        if (inheritedCurve == null) alignPathOrigin(points, runOrigin);
        PathCurve fixedCurve = inheritedCurve;
        if (fixedCurve == null && curveDestination != null) {
            fixedCurve = PathCurve.create(points[0], curveDestination, curveDirection);
        }
        if (fixedCurve != null) {
            for (int index = 0; index < points.length; index++) points[index] = fixedCurve.apply(points[index]);
        }
        Point renderedEnd = points[points.length - 1];
        for (var line : curvedPolyline(points, fixedCurve == null ? curveDirection : 0)) {
            int secondSampleIndex = firstIndex + line.segmentIndex;
            if (secondSampleIndex > lastIndex) continue;
            segments.add(new RenderedPathSegment(project(line.from.x, line.from.y),
                    project(line.to.x, line.to.y), line.from, line.to,
                    samples.get(secondSampleIndex - 1), samples.get(secondSampleIndex),
                    line.sampleProgressFrom, line.sampleProgressTo));
            renderedEnd = line.to;
        }
        return new RenderedRun(renderedEnd, fixedCurve);
    }

    private Point abortDestination(UUID branchId, UUID actionId) {
        if (flightPath == null) return null;
        return flightPath.navigationAborts().stream()
                .filter(abort -> abort.branchId().equals(branchId) && abort.actionId().equals(actionId))
                .findFirst().map(abort -> new Point(abort.destinationX(), abort.destinationY())).orElse(null);
    }

    private List<RenderedAsteroidSegment> renderedAsteroidPathSegments(
            RocketFlightPathCalculator.AsteroidPath path) {
        var samples = path.samples();
        if (samples.size() < 2) return List.of();
        var points = new Point[samples.size()];
        for (int index = 0; index < samples.size(); index++) {
            points[index] = new Point(samples.get(index).x(), samples.get(index).y());
        }
        var incoming = incomingAsteroidSegment(path.asteroidId(), samples.getFirst().timeSeconds());
        alignPathOrigin(points, incoming == null ? null : incoming.worldTo);
        int direction = asteroidCurveDirection(path.asteroidId(), points, incoming);
        return curvedPolyline(points, direction).stream()
                .map(line -> new RenderedAsteroidSegment(project(line.from.x, line.from.y),
                        project(line.to.x, line.to.y)))
                .toList();
    }

    private static void alignPathOrigin(Point[] points, Point origin) {
        if (origin == null) return;
        double offsetX = origin.x - points[0].x;
        double offsetY = origin.y - points[0].y;
        var distances = new double[points.length];
        for (int index = 1; index < points.length; index++) {
            distances[index] = distances[index - 1]
                    + Math.hypot(points[index].x - points[index - 1].x,
                    points[index].y - points[index - 1].y);
        }
        double totalDistance = distances[points.length - 1];
        if (totalDistance == 0) {
            java.util.Arrays.fill(points, origin);
            return;
        }
        for (int index = 0; index < points.length; index++) {
            double remaining = 1 - distances[index] / totalDistance;
            points[index] = new Point(points[index].x + offsetX * remaining,
                    points[index].y + offsetY * remaining);
        }
    }

    private List<CurvedLine> curvedPolyline(Point[] points, int curveDirection) {
        var lines = new ArrayList<CurvedLine>();
        var distances = new double[points.length];
        for (int index = 1; index < points.length; index++) {
            distances[index] = distances[index - 1]
                    + Math.hypot(points[index].x - points[index - 1].x,
                    points[index].y - points[index - 1].y);
        }
        Point start = points[0];
        Point end = points[points.length - 1];
        double chordX = end.x - start.x;
        double chordY = end.y - start.y;
        double chordLength = Math.hypot(chordX, chordY);
        double curve = isNearlyStraight(points, distances[points.length - 1], chordX, chordY, chordLength)
                && curveDirection != 0
                ? chordLength * TRANSFER_CURVE_RATIO * curveDirection : 0;
        double normalX = chordLength == 0 ? 0 : -chordY / chordLength;
        double normalY = chordLength == 0 ? 0 : chordX / chordLength;
        double totalDistance = distances[points.length - 1];

        for (int index = 1; index < points.length; index++) {
            Point rawFrom = points[index - 1];
            Point rawTo = points[index];
            double segmentLength = Math.hypot(rawTo.x - rawFrom.x, rawTo.y - rawFrom.y);
            double screenLength = Math.hypot((rawTo.x - rawFrom.x) * zoom,
                    (rawTo.y - rawFrom.y) * zoom * PLANE_TILT);
            int minimumSubdivisions = points.length <= 4 ? 8 : 1;
            int subdivisions = curve == 0 ? 1 : Math.min(32,
                    Math.max(minimumSubdivisions, (int) Math.ceil(screenLength / 16)));
            Point from = curvedPoint(rawFrom, normalX, normalY, curve,
                    totalDistance == 0 ? 0 : distances[index - 1] / totalDistance);
            for (int subdivision = 1; subdivision <= subdivisions; subdivision++) {
                double sampleProgress = subdivision / (double) subdivisions;
                double baseX = rawFrom.x + (rawTo.x - rawFrom.x) * sampleProgress;
                double baseY = rawFrom.y + (rawTo.y - rawFrom.y) * sampleProgress;
                double distance = distances[index - 1] + segmentLength * sampleProgress;
                Point to = curvedPoint(new Point(baseX, baseY), normalX, normalY, curve,
                        totalDistance == 0 ? 0 : distance / totalDistance);
                lines.add(new CurvedLine(from, to, index,
                        (subdivision - 1d) / subdivisions, sampleProgress));
                from = to;
            }
        }
        return lines;
    }

    private static boolean isNearlyStraight(Point[] points, double pathLength,
                                            double chordX, double chordY, double chordLength) {
        if (chordLength < 0.001 || pathLength > chordLength * 1.05) return false;
        double maximumDeviation = 0;
        for (int index = 1; index < points.length - 1; index++) {
            double offsetX = points[index].x - points[0].x;
            double offsetY = points[index].y - points[0].y;
            maximumDeviation = Math.max(maximumDeviation,
                    Math.abs(offsetX * chordY - offsetY * chordX) / chordLength);
        }
        return maximumDeviation <= chordLength * 0.025;
    }

    private static Point curvedPoint(Point point, double normalX, double normalY,
                                     double curve, double progress) {
        double offset = Math.sin(Math.PI * progress) * curve;
        return new Point(point.x + normalX * offset, point.y + normalY * offset);
    }

    private static int curveDirection(UUID branchId) {
        return (branchId.hashCode() & 1) == 0 ? 1 : -1;
    }

    private static int asteroidCurveDirection(UUID asteroidId, Point[] points, RenderedPathSegment incomingSegment) {
        if (incomingSegment == null) return curveDirection(asteroidId);
        Point incoming = new Point(incomingSegment.worldTo.x - incomingSegment.worldFrom.x,
                incomingSegment.worldTo.y - incomingSegment.worldFrom.y);
        Point start = points[0];
        Point end = points[points.length - 1];
        double chordX = end.x - start.x;
        double chordY = end.y - start.y;
        double cross = chordX * incoming.y - chordY * incoming.x;
        return Math.abs(cross) < 0.001 ? curveDirection(asteroidId) : cross > 0 ? 1 : -1;
    }

    private RenderedPathSegment incomingAsteroidSegment(UUID asteroidId, double releaseTime) {
        RenderedPathSegment closest = null;
        double closestTime = Double.NEGATIVE_INFINITY;
        for (var segments : renderedPathsByBranch.values()) {
            for (var segment : segments) {
                boolean carriesAsteroid = segment.firstSample.attachedAsteroidId().equals(asteroidId)
                        || segment.secondSample.attachedAsteroidId().equals(asteroidId);
                double segmentTime = segment.secondSample.timeSeconds();
                if (carriesAsteroid && segmentTime <= releaseTime + 0.01 && segmentTime >= closestTime
                        && Math.hypot(segment.worldTo.x - segment.worldFrom.x,
                        segment.worldTo.y - segment.worldFrom.y) > 0.001) {
                    closest = segment;
                    closestTime = segmentTime;
                }
            }
        }
        return closest;
    }

    private void addCircle(List<LineSegment> lines, double centerX, double centerY, double radius,
                           int color, float width) {
        Point previous = project(centerX + radius, centerY);
        for (int index = 1; index <= CIRCLE_SEGMENTS; index++) {
            double angle = Math.PI * 2 * index / CIRCLE_SEGMENTS;
            Point next = project(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius);
            lines.add(new LineSegment(previous.x, previous.y, next.x, next.y, color, width));
            previous = next;
        }
    }

    private void submitLines(GuiGraphicsExtractor graphics, List<LineSegment> lines,
                             int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        if (lines.isEmpty()) return;
        var pose = new Matrix3x2f(graphics.pose());
        var scissor = graphics.peekScissorStack();
        var bounds = new ScreenRectangle(viewportX, viewportY, viewportWidth, viewportHeight);
        var clippedBounds = scissor == null ? bounds : scissor.intersection(bounds);
        graphics.submitGuiElementRenderState(new LineRenderState(List.copyOf(lines), pose, scissor, clippedBounds));
    }

    private void renderObject(GuiGraphicsExtractor graphics, MapObject object,
                              int mouseX, int mouseY, float delta) {
        var position = project(objectX(object), objectY(object));
        int minimum = object.data.type() == SpaceObjects.ObjectType.ASTEROID ? 10 : 16;
        int size = (int) Math.clamp(object.data.radius() * zoom * 2, minimum, 54);
        object.widget.setPosition((int) Math.round(position.x - size / 2f),
                (int) Math.round(position.y - size / 2f));
        object.widget.setSize(size, size);
        object.widget.render(graphics, mouseX, mouseY, delta);
        if (isInsideViewport(mouseX, mouseY) && object.widget.isMouseOver(mouseX, mouseY)) hoveredObject = object;
        if (object.data.type() != SpaceObjects.ObjectType.ASTEROID) {
            graphics.text(Minecraft.getInstance().font, objectName(object.data),
                    (int) Math.round(position.x + size / 2d + 3), (int) Math.round(position.y - 4),
                    0xFF9FB2C4, false);
        }

        if (selectedTarget != null && selectedTarget.objectId.equals(object.data.id())
                && selectedTarget.orbit == SpaceSimulation.OrbitBand.SURFACE) {
            graphics.fill((int) Math.round(position.x - size / 2f - 2),
                    (int) Math.round(position.y - size / 2f - 2),
                    (int) Math.round(position.x + size / 2f + 2),
                    (int) Math.round(position.y - size / 2f), 0xFFF6C65B);
        }
    }

    private void renderRocket(GuiGraphicsExtractor graphics, BranchMarker marker,
                              int mouseX, int mouseY, float delta) {
        var position = renderedPosition(marker.branchId, marker.timeSeconds, marker.worldX, marker.worldY);
        var ownPath = renderedPathsByBranch.get(marker.branchId);
        if (ownPath == null || ownPath.isEmpty()) {
            Point branchOrigin = renderedBranchOrigin(marker.branchId);
            if (branchOrigin != null) position = project(branchOrigin.x, branchOrigin.y);
        }
        marker.widget.setPosition((int) Math.round(position.x - 6), (int) Math.round(position.y - 6));
        marker.widget.render(graphics, mouseX, mouseY, delta);
    }

    private Point renderedBranchOrigin(UUID branchId) {
        if (flightPath == null) return null;
        for (var event : flightPath.boosterEvents()) {
            if (!event.childBranchId().equals(branchId)) continue;
            var parent = pathsByBranch.get(event.branchId());
            if (parent != null) renderedPathSegments(parent);
            return renderedWorldPosition(event.branchId(), event.timeSeconds(), event.x(), event.y());
        }
        var branch = snapshot.plan().branches().stream().filter(item -> item.id().equals(branchId))
                .findFirst().orElse(null);
        if (branch == null || branch.isRoot()) return null;
        for (var parent : flightPath.paths()) {
            var moment = parent.actionMoments().stream()
                    .filter(item -> item.actionId().equals(branch.parentSeparationAction()))
                    .findFirst().orElse(null);
            if (moment == null) continue;
            renderedPathSegments(parent);
            return renderedWorldPosition(parent.branchId(), moment.timeSeconds(), moment.x(), moment.y());
        }
        return null;
    }

    private void renderSeparations(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (var marker : separationMarkers) {
            var position = renderedPosition(marker.branchId, marker.timeSeconds, marker.worldX, marker.worldY);
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
        var segments = renderedPathsByBranch.get(branchId);
        if (segments == null) return project(worldX, worldY);
        for (var segment : segments) {
            double firstTime = segment.firstSample.timeSeconds();
            double secondTime = segment.secondSample.timeSeconds();
            if (timeSeconds < firstTime - 0.01 || timeSeconds > secondTime + 0.01) continue;
            double timeProgress = secondTime - firstTime <= 0.0001 ? 1
                    : Math.clamp((timeSeconds - firstTime) / (secondTime - firstTime), 0, 1);
            if (timeProgress < segment.sampleProgressFrom - 0.0001
                    || timeProgress > segment.sampleProgressTo + 0.0001) continue;
            double segmentProgress = segment.sampleProgressTo - segment.sampleProgressFrom <= 0.0001 ? 1
                    : Math.clamp((timeProgress - segment.sampleProgressFrom)
                    / (segment.sampleProgressTo - segment.sampleProgressFrom), 0, 1);
            return new Point(segment.from.x + (segment.to.x - segment.from.x) * segmentProgress,
                    segment.from.y + (segment.to.y - segment.from.y) * segmentProgress);
        }
        return project(worldX, worldY);
    }

    private Point renderedWorldPosition(UUID branchId, double timeSeconds, double worldX, double worldY) {
        var segments = renderedPathsByBranch.get(branchId);
        if (segments == null) return new Point(worldX, worldY);
        for (var segment : segments) {
            double firstTime = segment.firstSample.timeSeconds();
            double secondTime = segment.secondSample.timeSeconds();
            if (timeSeconds < firstTime - 0.01 || timeSeconds > secondTime + 0.01) continue;
            double timeProgress = secondTime - firstTime <= 0.0001 ? 1
                    : Math.clamp((timeSeconds - firstTime) / (secondTime - firstTime), 0, 1);
            if (timeProgress < segment.sampleProgressFrom - 0.0001
                    || timeProgress > segment.sampleProgressTo + 0.0001) continue;
            double segmentProgress = segment.sampleProgressTo - segment.sampleProgressFrom <= 0.0001 ? 1
                    : Math.clamp((timeProgress - segment.sampleProgressFrom)
                    / (segment.sampleProgressTo - segment.sampleProgressFrom), 0, 1);
            return new Point(segment.worldFrom.x + (segment.worldTo.x - segment.worldFrom.x) * segmentProgress,
                    segment.worldFrom.y + (segment.worldTo.y - segment.worldFrom.y) * segmentProgress);
        }
        return new Point(worldX, worldY);
    }

    private void findHoveredSelection(double mouseX, double mouseY) {
        if (!isInsideViewport(mouseX, mouseY)) return;
        Point world = unproject(mouseX, mouseY);
        double closestDistance = 5;
        NavigationSelection closest = null;
        for (var object : objects) {
            double objectX = objectX(object);
            double objectY = objectY(object);
            double offsetX = world.x - objectX;
            double offsetY = world.y - objectY;
            double angle = Math.atan2(offsetY, offsetX);
            for (var band : RocketFlightPlanRules.availableOrbits(object.data.type())) {
                if (band == SpaceSimulation.OrbitBand.SURFACE) continue;
                double radius = object.data.radius() + band.altitude();
                if (radius * zoom < 18) continue;
                Point ringPoint = project(objectX + Math.cos(angle) * radius,
                        objectY + Math.sin(angle) * radius);
                double screenDistance = Math.hypot(mouseX - ringPoint.x, mouseY - ringPoint.y);
                if (screenDistance < closestDistance) {
                    closestDistance = screenDistance;
                    closest = new NavigationSelection(object.data.id(), band);
                }
            }
        }
        if (closest != null) {
            hoveredSelection = closest;
            return;
        }
        if (hoveredObject != null) {
            hoveredSelection = new NavigationSelection(hoveredObject.data.id(), SpaceSimulation.OrbitBand.SURFACE);
        }
    }

    private void findHoveredPath(double mouseX, double mouseY) {
        if (flightPath == null || !isInsideViewport(mouseX, mouseY)) return;
        double closestDistance = 6;
        for (var path : flightPath.paths()) {
            var renderedSegments = renderedPathsByBranch.get(path.branchId());
            if (renderedSegments == null) renderedSegments = renderedPathSegments(path);
            for (var segment : renderedSegments) {
                var from = segment.from;
                var to = segment.to;
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
                    hoveredPathPoint = new HoveredPathPoint(segment.secondSample,
                            interpolatePathSpeed(segment.firstSample, segment.secondSample, sampleProgress));
                }
            }
        }
    }

    private static double interpolatePathSpeed(RocketFlightPathCalculator.PathSample first,
                                               RocketFlightPathCalculator.PathSample second,
                                               double progress) {
        // Approximate speed along the short displayed chord between two trajectory samples.
        double velocityX = first.velocityX() + (second.velocityX() - first.velocityX()) * progress;
        double velocityY = first.velocityY() + (second.velocityY() - first.velocityY()) * progress;
        return Math.hypot(velocityX, velocityY);
    }

    private void renderHeader(GuiGraphicsExtractor graphics) {
        var font = Minecraft.getInstance().font;
        graphics.fill(x + 6, y + 6, x + width - 6, y + 21, 0xDD080D18);
        graphics.text(font, Component.translatable("screen.oritech_space_age.star_system"),
                x + 10, y + 9, 0xFFCAD8E5, true);
        if (selectedTarget != null) {
            var object = objectById(selectedTarget.objectId);
            if (object != null) {
                graphics.text(font, Component.translatable("screen.oritech_space_age.selected_target",
                                objectName(object.data), orbitName(selectedTarget.orbit)),
                        x + 82, y + 9, 0xFFF6C65B, false);
            }
        }
        graphics.text(font, Component.translatable("screen.oritech_space_age.map_controls"),
                x + width - 160, y + 9, 0xFFA7BACB, false);
    }

    private void renderStats(GuiGraphicsExtractor graphics) {
        var font = Minecraft.getInstance().font;
        OritechSurface.PANEL_DARK.render(graphics, x + 9, y + 27, 176, 58);
        graphics.text(font, Component.translatable("screen.oritech_space_age.flight_stats")
                        .withStyle(ChatFormatting.BOLD), x + 16, y + 33, 0xFFF2F6FA, false);
        graphics.text(font, Component.translatable("screen.oritech_space_age.last_command_days",
                        String.format(Locale.ROOT, "%.2f", flightPath.lastCommandSeconds() / 1_200d)),
                x + 16, y + 45, 0xFFCAD8E5, false);
        var selectedPath = pathsByBranch.get(selectedBranch);
        if (selectedPath != null) {
            graphics.text(font, Component.translatable("screen.oritech_space_age.remaining_delta_v",
                            String.format(Locale.ROOT, "%.0f", selectedPath.remainingDeltaV())),
                    x + 16, y + 57, 0xFFCAD8E5, false);
            graphics.text(font, Component.translatable("screen.oritech_space_age.plan_status",
                            Component.translatable("screen.oritech_space_age.terminal."
                                    + selectedPath.terminalState().name().toLowerCase(Locale.ROOT))),
                    x + 16, y + 69,
                    selectedPath.terminalState().isFailure()
                            ? 0xFFFF7777 : 0xFFCAD8E5, false);
        }
    }

    private void renderLegend(GuiGraphicsExtractor graphics) {
        var font = Minecraft.getInstance().font;
        int panelX = x + width - 181;
        int panelY = y + 27;
        OritechSurface.PANEL_DARK.render(graphics, panelX, panelY, 172, 76);
        graphics.text(font, Component.translatable("screen.oritech_space_age.path_legend")
                        .withStyle(ChatFormatting.BOLD), panelX + 7, panelY + 6, 0xFFF2F6FA, false);
        renderLegendEntry(graphics, panelX + 8, panelY + 20, 0xFFFF8A20,
                Component.translatable("screen.oritech_space_age.path.accelerate"));
        renderLegendEntry(graphics, panelX + 8, panelY + 31, 0xFF8FDB68,
                Component.translatable("screen.oritech_space_age.path.redirect"));
        renderLegendEntry(graphics, panelX + 8, panelY + 42, 0xFF66B9D5,
                Component.translatable("screen.oritech_space_age.path.coast"));
        renderLegendEntry(graphics, panelX + 8, panelY + 53, 0xFFB68CFF,
                Component.translatable("screen.oritech_space_age.path.brake"));
        renderLegendEntry(graphics, panelX + 8, panelY + 64, 0xFFFFD45C,
                Component.translatable("screen.oritech_space_age.path.separation"));
    }

    private static void renderLegendEntry(GuiGraphicsExtractor graphics, int entryX, int entryY,
                                          int color, Component label) {
        graphics.fill(entryX, entryY + 3, entryX + 13, entryY + 5, color);
        graphics.text(Minecraft.getInstance().font, label, entryX + 18, entryY, 0xFFCAD8E5, false);
    }

    @Override
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        if (!isInsideViewport(mouseX, mouseY)) return false;
        Point before = unproject(mouseX, mouseY);
        zoom = Math.clamp(zoom * Math.pow(1.2, scrollDelta), minimumZoom * 0.5, MAX_ZOOM);
        Point after = unproject(mouseX, mouseY);
        cameraX += before.x - after.x;
        cameraY += before.y - after.y;
        return true;
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!isInsideViewport(mouseX, mouseY)) return false;
        if (button == 1 && hoveredObject != null) {
            contextMenuListener.accept(new NavigationContextRequest(
                    new NavigationSelection(hoveredObject.data.id(), SpaceSimulation.OrbitBand.SURFACE),
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
        cameraX -= deltaX / zoom;
        cameraY -= deltaY / (zoom * PLANE_TILT);
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
        if (hoveredSeparation != null) {
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable("screen.oritech_space_age.separation_stage",
                    hoveredSeparation.stage).withStyle(ChatFormatting.BOLD));
            lines.add(Component.translatable("screen.oritech_space_age.separation_time",
                    format(hoveredSeparation.timeSeconds)));
            for (var segment : hoveredSeparation.segments) {
                lines.add(Component.literal("• " + segmentName(segment)));
            }
            return lines;
        }
        MapObject object = hoveredSelection == null ? hoveredObject : objectById(hoveredSelection.objectId);
        if (object != null) {
            var lines = new ArrayList<Component>();
            lines.add(objectName(object.data).copy().withStyle(ChatFormatting.BOLD));
            if (hoveredSelection != null && hoveredSelection.orbit != SpaceSimulation.OrbitBand.SURFACE) {
                lines.add(Component.translatable("screen.oritech_space_age.orbit_selection",
                        orbitName(hoveredSelection.orbit)));
            }
            if (hoveredSelection != null && object.data.type() != SpaceObjects.ObjectType.ASTEROID) {
                double gravity = gravityAtOrbit(object.data, hoveredSelection.orbit);
                double percentage = object.data.surfaceGravity() <= 0
                        ? 0 : gravity / object.data.surfaceGravity() * 100;
                lines.add(Component.translatable("screen.oritech_space_age.orbit_gravity",
                        String.format(Locale.ROOT, "%.2f", gravity),
                        String.format(Locale.ROOT, "%.0f", percentage)));
            }
            lines.add(Component.translatable("screen.oritech_space_age.object.position",
                    format(objectX(object)), format(objectY(object))));
            var motion = displayedObjectPositions.get(object.data.id());
            if (motion != null && Math.hypot(motion.x - object.data.x(), motion.y - object.data.y()) >= 1) {
                lines.add(Component.translatable("screen.oritech_space_age.object.arrival_position",
                    String.format(Locale.ROOT, "%.2f", motion.timeSeconds / 1_200),
                    format(object.data.x()), format(object.data.y())));
            }
            lines.add(Component.translatable("screen.oritech_space_age.object.radius", format(object.data.radius())));
            if (object.data.type() == SpaceObjects.ObjectType.ASTEROID) {
                lines.add(Component.translatable("screen.oritech_space_age.object.mass",
                        format(object.data.mass() * 1_000)));
                lines.add(Component.translatable("screen.oritech_space_age.object.velocity",
                        formatSpeed(Math.hypot(object.data.velocityX(), object.data.velocityY()))));
                lines.add(Component.translatable("screen.oritech_space_age.object.materials"));
                object.data.materials().forEach(material -> lines.add(Component.literal(
                        "• " + material.block() + " × " + material.amount())));
            }
            lines.add(Component.translatable("screen.oritech_space_age.object.detection",
                    object.data.detectionState().name().toLowerCase(Locale.ROOT)));
            return lines;
        }
        if (hoveredPathPoint == null) return List.of();

        var hoveredPathSample = hoveredPathPoint.sample;
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("screen.oritech_space_age.path_state", hoveredPathSample.stage(),
                pathPhaseName(hoveredPathSample.phase())).withStyle(ChatFormatting.BOLD));
        lines.add(Component.translatable("screen.oritech_space_age.path_speed",
                formatSpeed(hoveredPathPoint.speedMetersPerSecond)));
        lines.add(Component.translatable("screen.oritech_space_age.path_connected"));
        addSegmentNames(lines, hoveredPathSample.connectedSegments());
        if (!hoveredPathSample.attachedAsteroidId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
            var asteroid = objectById(hoveredPathSample.attachedAsteroidId());
            if (asteroid != null) lines.add(Component.translatable(
                    "screen.oritech_space_age.path.attached_asteroid", objectName(asteroid.data)));
        }
        lines.add(Component.translatable("screen.oritech_space_age.path_firing"));
        if (hoveredPathSample.firingSegments().isEmpty()) {
            lines.add(Component.translatable("screen.oritech_space_age.path_none"));
        } else {
            addSegmentNames(lines, hoveredPathSample.firingSegments());
        }
        return lines;
    }

    private void addSegmentNames(List<Component> lines, java.util.Set<SpaceSimulation.SegmentRef> segments) {
        segments.stream().sorted(java.util.Comparator.comparingLong(ref -> ref.anchor().asLong()))
                .map(this::segmentName).map(name -> Component.literal("• " + name)).forEach(lines::add);
    }

    private String segmentName(SpaceSimulation.SegmentRef segment) {
        String name = snapshot.plan().configurationFor(segment).name();
        return name.isBlank() ? defaultSegmentNames.getOrDefault(segment, "Unknown segment") : name;
    }

    private static Component pathPhaseName(RocketFlightPathCalculator.PathPhase phase) {
        return Component.translatable("screen.oritech_space_age.path." + phase.name().toLowerCase(Locale.ROOT));
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        boolean insideMap = mouseX >= x + 5 && mouseX < x + width - 5
                && mouseY >= y + 22 && mouseY < y + height - 5;
        boolean overStats = mouseX >= x + 9 && mouseX < x + 185 && mouseY >= y + 27 && mouseY < y + 85;
        boolean overLegend = mouseX >= x + width - 181 && mouseX < x + width - 9
                && mouseY >= y + 27 && mouseY < y + 103;
        return insideMap && !overStats && !overLegend;
    }

    private Point project(double worldX, double worldY) {
        return new Point(x + width * 0.5 + (worldX - cameraX) * zoom,
                y + 22 + (height - 27) * 0.5 + (worldY - cameraY) * zoom * PLANE_TILT);
    }

    private Point unproject(double screenX, double screenY) {
        return new Point(cameraX + (screenX - (x + width * 0.5)) / zoom,
                cameraY + (screenY - (y + 22 + (height - 27) * 0.5)) / (zoom * PLANE_TILT));
    }

    private MapObject objectById(UUID id) {
        return objects.stream().filter(object -> object.data.id().equals(id)).findFirst().orElse(null);
    }

    void setTooltipsEnabled(boolean enabled) {
        tooltipsEnabled = enabled;
    }

    private double objectX(MapObject object) {
        var position = displayedObjectPositions.get(object.data.id());
        return position == null ? object.data.x() : position.x;
    }

    private double objectY(MapObject object) {
        var position = displayedObjectPositions.get(object.data.id());
        return position == null ? object.data.y() : position.y;
    }

    private static double gravityAtOrbit(SpaceSimulation.SpaceObjectData object,
                                         SpaceSimulation.OrbitBand orbit) {
        double distance = object.radius() + orbit.altitude();
        if (distance <= 0) return 0;
        // Game distances are scaled, but inverse-square falloff still gives the player a useful comparison between
        // the selectable orbit bands around the same body.
        double relativeDistance = object.radius() / distance;
        return object.surfaceGravity() * relativeDistance * relativeDistance;
    }

    private static BlockState placeholderBlock(SpaceObjects.ObjectType type) {
        return switch (type) {
            case EARTH -> Blocks.GRASS_BLOCK.defaultBlockState();
            case SUN -> Blocks.GOLD_BLOCK.defaultBlockState();
            case MARS -> Blocks.RED_SAND.defaultBlockState();
            case ASTEROID -> Blocks.IRON_ORE.defaultBlockState();
        };
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

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.0f", value);
    }

    private static String formatSpeed(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    record NavigationSelection(UUID objectId, SpaceSimulation.OrbitBand orbit) {
    }

    record NavigationContextRequest(NavigationSelection selection, int mouseX, int mouseY) {
    }

    private record MapObject(SpaceSimulation.SpaceObjectData data, BlockWidget widget) {
    }

    private record MotionPosition(double x, double y, double timeSeconds) {
    }

    private record BranchMarker(UUID branchId, double timeSeconds,
                                double worldX, double worldY, ItemWidget widget) {
    }

    private record SeparationMarker(UUID branchId, int stage, double timeSeconds,
                                    double worldX, double worldY,
                                    List<SpaceSimulation.SegmentRef> segments, List<UUID> childBranches) {
        private boolean matches(RocketFlightPathCalculator.BoosterEvent event) {
            return branchId.equals(event.branchId()) && stage == event.stage()
                    && Math.abs(timeSeconds - event.timeSeconds()) < 0.01
                    && Math.hypot(worldX - event.x(), worldY - event.y()) < 1;
        }
    }

    private record HoveredPathPoint(RocketFlightPathCalculator.PathSample sample,
                                    double speedMetersPerSecond) {
    }

    private record RenderedRun(Point end, PathCurve curve) {
    }

    private record PathCurve(double startX, double startY,
                             double directionX, double directionY, double lengthSquared,
                             double normalX, double normalY, double curve) {

        private static PathCurve create(Point start, Point end, int curveDirection) {
            double directionX = end.x - start.x;
            double directionY = end.y - start.y;
            double lengthSquared = directionX * directionX + directionY * directionY;
            double length = Math.sqrt(lengthSquared);
            double normalX = length == 0 ? 0 : -directionY / length;
            double normalY = length == 0 ? 0 : directionX / length;
            return new PathCurve(start.x, start.y, directionX, directionY, lengthSquared,
                    normalX, normalY, length * TRANSFER_CURVE_RATIO * curveDirection);
        }

        private Point apply(Point point) {
            if (lengthSquared <= 0.000001) return point;
            double progress = Math.clamp(
                    ((point.x - startX) * directionX + (point.y - startY) * directionY) / lengthSquared,
                    0, 1);
            double offset = Math.sin(Math.PI * progress) * curve;
            return new Point(point.x + normalX * offset, point.y + normalY * offset);
        }
    }

    private record RenderedPathSegment(Point from, Point to, Point worldFrom, Point worldTo,
                                       RocketFlightPathCalculator.PathSample firstSample,
                                       RocketFlightPathCalculator.PathSample secondSample,
                                       double sampleProgressFrom, double sampleProgressTo) {
    }

    private record RenderedAsteroidSegment(Point from, Point to) {
    }

    private record CurvedLine(Point from, Point to, int segmentIndex,
                              double sampleProgressFrom, double sampleProgressTo) {
    }

    private record Point(double x, double y) {
    }

    private record LineSegment(double fromX, double fromY, double toX, double toY, int color, float width) {
    }

    private record LineRenderState(List<LineSegment> lines, Matrix3x2f pose,
                                   @Nullable ScreenRectangle scissorArea,
                                   @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer vertices) {
            for (var line : lines) {
                double dx = line.toX - line.fromX;
                double dy = line.toY - line.fromY;
                double length = Math.hypot(dx, dy);
                if (length < 0.001) continue;
                float normalX = (float) (-dy / length * line.width * 0.5);
                float normalY = (float) (dx / length * line.width * 0.5);
                vertices.addVertexWith2DPose(pose, (float) line.fromX - normalX, (float) line.fromY - normalY)
                        .setColor(line.color);
                vertices.addVertexWith2DPose(pose, (float) line.fromX + normalX, (float) line.fromY + normalY)
                        .setColor(line.color);
                vertices.addVertexWith2DPose(pose, (float) line.toX + normalX, (float) line.toY + normalY)
                        .setColor(line.color);
                vertices.addVertexWith2DPose(pose, (float) line.toX - normalX, (float) line.toY - normalY)
                        .setColor(line.color);
            }
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
