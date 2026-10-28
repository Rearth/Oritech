package rearth.oritech.spaceage.client.starmap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.spaceage.block.basic.AsteroidBlock;
import rearth.oritech.spaceage.block.basic.AsteroidBlock.Ore;
import rearth.oritech.spaceage.client.mission_control.FleetViewPreferences;
import rearth.oritech.spaceage.client.starmap.StarMapIconGroups.Icon;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.ResourceBodies;
import rearth.oritech.spaceage.simulation.ResourceBodies.Definition;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Celestial object state and all object, surface, and orbit map rendering.
 */
final class StarMapObjects {

    static final double CRAFT_NAME_ZOOM = .004;
    // Reference rings adapt to their screen size so true orbit points remain on the visible ring.
    private static final int MIN_CIRCLE_SEGMENTS = 48;
    private static final int MAX_CIRCLE_SEGMENTS = 512;
    // Texture resolution is independent of the body's displayed world-space size.
    private static final int ICON_TEXTURE_SIZE = 128;
    private static final int PLANET_TEXTURE_PADDING = 16;
    // Each object keeps its 3D block widget so its familiar planet icon can be reused each frame.
    private final List<Entry> objects = new ArrayList<>();
    private final List<FogPatch> fogBridges = new ArrayList<>();
    // Asteroids move only for the selected craft's completed navigation events.
    private final Map<UUID, StarMapTooltip.ArrivalPosition> displayedPositions = new HashMap<>();
    private Set<UUID> fleetMarkers = Set.of();
    private final ItemStack craftIcon = new ItemStack(Items.FIREWORK_ROCKET);
    private final ItemStack waypointIcon = new ItemStack(Items.ENDER_EYE);

    void setFleetMarkers(Set<UUID> craftIds) {

        fleetMarkers = Set.copyOf(craftIds);
    }

    private static void addCircle(List<StarMapLineRenderer.Line> lines, StarMapCamera camera, Viewport viewport,
                                  double centerX, double centerY, double radius, int color, float width) {

        if (!circleIntersects(viewport, camera, centerX, centerY, radius)) return;
        var segments = circleSegments(radius * camera.zoom());
        var previous = project(camera, viewport, centerX + radius, centerY);
        for (var index = 1; index <= segments; index++) {
            var angle = Math.PI * 2 * index / segments;
            var next = project(camera, viewport, centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius);
            lines.add(new StarMapLineRenderer.Line(previous.x(), previous.y(), next.x(), next.y(), color, width));
            previous = next;
        }
    }

    static int circleSegments(double screenRadius) {

        return (int) Math.clamp(Math.ceil(Math.PI * 2 * screenRadius / 8),
                MIN_CIRCLE_SEGMENTS, MAX_CIRCLE_SEGMENTS);
    }

    private static StarMapCamera.Point project(StarMapCamera camera, Viewport viewport, double x, double y) {

        return camera.project(x, y, viewport.x(), viewport.y(), viewport.width(), viewport.height());
    }

    private static BlockState iconBlock(SpaceSimulation.SpaceObjectData object) {

        return switch (object.type()) {
            case EARTH -> SpaceAgeBlocks.EARTH.get().defaultBlockState();
            case SUN -> SpaceAgeBlocks.SUN.get().defaultBlockState();
            case MARS -> SpaceAgeBlocks.MARS.get().defaultBlockState();
            case SURVEY_REGION -> Blocks.GRAY_STAINED_GLASS.defaultBlockState();
            case ASTEROID -> asteroidBlock(object);
            case RESOURCE_BODY -> {
                var definition = resourceBodyDefinition(object);
                yield definition == null ? SpaceAgeBlocks.ASTEROID_1.get().defaultBlockState()
                        : BuiltInRegistries.BLOCK.getValue(definition.modelBlock()).defaultBlockState();
            }
            case CRAFT, WAYPOINT -> SpaceAgeBlocks.ROCKET_COUPLING.get().defaultBlockState();
        };
    }

    static Definition resourceBodyDefinition(SpaceSimulation.SpaceObjectData object) {

        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : ResourceBodies.find(connection.registryAccess(), object.id());
    }

    static Component resourceBodyText(SpaceSimulation.SpaceObjectData object, String field, String fallback) {

        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return Component.literal(fallback);
        return connection.registryAccess().lookupOrThrow(ResourceBodies.REGISTRY).listElements()
                .filter(holder -> ResourceBodies.id(holder.key().identifier()).equals(object.id()))
                .map(holder -> Component.translatableWithFallback(
                        holder.key().identifier().toLanguageKey("resource_body", field), fallback))
                .findFirst().orElseGet(() -> Component.literal(fallback));
    }

    private static BlockState asteroidBlock(SpaceSimulation.SpaceObjectData object) {

        // Seed the shape by identity so telemetry refreshes and screen switches keep it stable.
        var random = new Random(object.id().getMostSignificantBits() ^ object.id().getLeastSignificantBits());
        var block = switch (random.nextInt(3)) {
            case 0 -> SpaceAgeBlocks.ASTEROID_1.get();
            case 1 -> SpaceAgeBlocks.ASTEROID_2.get();
            default -> SpaceAgeBlocks.ASTEROID_3.get();
        };
        var ore = Ore.IRON;
        var largestAmount = 0;
        for (var material : object.materials()) {
            var path = material.block().getPath();
            for (var candidate : Ore.values()) {
                var orePath = candidate.getSerializedName() + "_ore";
                if ((path.equals(orePath) || path.endsWith("_" + orePath)) && material.amount() > largestAmount) {
                    ore = candidate;
                    largestAmount = material.amount();
                }
            }
        }
        return block.defaultBlockState().setValue(AsteroidBlock.ORE, ore);
    }

    private static boolean intersects(Viewport viewport, double x, double y, double radiusX, double radiusY) {

        return x + radiusX >= viewport.x() && x - radiusX < viewport.right()
                && y + radiusY >= viewport.y() && y - radiusY < viewport.bottom();
    }

    private static boolean isPlanet(SpaceObjects.ObjectType type) {

        return type == SpaceObjects.ObjectType.SUN || type == SpaceObjects.ObjectType.EARTH
                || type == SpaceObjects.ObjectType.MARS || type == SpaceObjects.ObjectType.RESOURCE_BODY;
    }

    RocketMapPaths.Point surfacePosition(RocketMapPaths.Point point) {

        for (var object : objects) {
            if (!isPlanet(object.data.type())) continue;
            var body = object.data;
            if (Math.abs(Math.hypot(point.x() - body.x(), point.y() - body.y()) - body.radius()) < .1)
                return PlanetSurfaceProjection.closestEdge(point, body.x(), body.y(), body.radius(), object.outline);
        }
        return point;
    }

    private static boolean circleIntersects(Viewport viewport, StarMapCamera camera, double x, double y, double radius) {

        var center = project(camera, viewport, x, y);
        var radiusX = radius * camera.zoom();
        return intersects(viewport, center.x(), center.y(), radiusX, radiusX * StarMapCamera.PLANE_TILT);
    }

    void setSnapshot(SpaceSimulation.FlightPlannerSnapshot snapshot) {

        objects.clear();
        for (var object : snapshot.mapObjects()) {
            if (!FleetViewPreferences.showSun() && object.type() == SpaceObjects.ObjectType.CRAFT
                    && snapshot.stations().stream().anyMatch(station -> station.id().equals(object.id())
                    && station.position().target().equals(SpaceSimulation.SUN_ID))
                    && snapshot.plan().branches().stream().flatMap(branch -> branch.actions().stream()).noneMatch(action -> action.targetId().equals(object.id()))) continue;
            var widget = new BlockWidget(0, 0, ICON_TEXTURE_SIZE, iconBlock(object));
            // Normalize the two-block celestial models to the same unit as asteroid models.
            if (isPlanet(object.type())) widget.withScale(.5f).withRenderPadding(PLANET_TEXTURE_PADDING);
            if (object.type() != SpaceObjects.ObjectType.EARTH && object.type() != SpaceObjects.ObjectType.SUN)
                widget.withCachedRendering();
            objects.add(new Entry(object, widget, isPlanet(object.type()) ? PlanetSurfaceProjection.outline(widget) : List.of()));
        }
        connectFog();
    }

    private void connectFog() {

        fogBridges.clear();
        var regions = objects.stream().map(Entry::data).filter(o -> o.type() == SpaceObjects.ObjectType.SURVEY_REGION).toList();
        for (int a = 0; a < regions.size(); a++)
            for (int b = a + 1; b < regions.size(); b++) {
                var first = regions.get(a);
                var second = regions.get(b);
                if (Math.hypot(first.x() - second.x(), first.y() - second.y()) > (first.radius() + second.radius()) * 1.15)
                    continue;
                for (int part = 1; part <= 4; part++) {
                    var t = part / 5.0;
                    fogBridges.add(new FogPatch(first.x() + (second.x() - first.x()) * t,
                            first.y() + (second.y() - first.y()) * t, Math.min(first.radius(), second.radius()) * .8));
                }
            }
    }

    void updateArrivalPositions(RocketFlightPathCalculator.CraftPath selectedPath,
                                Map<UUID, SpaceSimulation.FlightPlanAction> actionsById,
                                SpaceSimulation.FlightPlannerSnapshot snapshot) {

        displayedPositions.clear();
        if (selectedPath == null) return;
        for (var moment : selectedPath.actionMoments()) {
            var action = actionsById.get(moment.actionId());
            if (!moment.completed() || action == null || action.type() != SpaceSimulation.ActionType.NAVIGATE_TO)
                continue;
            var targetId = selectedPath.samples().stream().filter(sample -> sample.actionId().equals(action.id()))
                    .map(sample -> sample.targetId()).findFirst().orElse(action.targetId());
            var target = snapshot.mapObjects().stream().filter(object -> object.id().equals(targetId)).findFirst().orElse(null);
            if (target != null && target.type() == SpaceObjects.ObjectType.ASTEROID) {
                displayedPositions.put(target.id(), new StarMapTooltip.ArrivalPosition(
                        target.xAt(moment.timeSeconds()), target.yAt(moment.timeSeconds()), moment.timeSeconds()));
            }
        }
    }

    List<StarMapCamera.Point> positions() {

        return objects.stream().map(object -> new StarMapCamera.Point(x(object), y(object))).toList();
    }

    void renderSurfaceDiscs(GuiGraphicsExtractor graphics, StarMapCamera camera, Viewport viewport) {

        var discs = new ArrayList<StarMapObjectRenderer.Disc>();
        for (var patch : fogBridges) {
            var center = project(camera, viewport, patch.x(), patch.y());
            var radius = patch.radius() * camera.zoom();
            if (radius >= 2 && intersects(viewport, center.x(), center.y(), radius, radius * StarMapCamera.PLANE_TILT)) {
                discs.add(new StarMapObjectRenderer.Disc(center.x(), center.y(), radius, radius * StarMapCamera.PLANE_TILT,
                        0x507F858D, 0x007F858D, circleSegments(radius)));
            }
        }
        for (var object : objects) {
            if (isPlanet(object.data.type())) continue;
            if (object.data.radius() * camera.zoom() < 2) continue;
            var center = project(camera, viewport, x(object), y(object));
            var color = switch (object.data.type()) {
                case SUN -> 0x66D7A438;
                case EARTH -> 0x665A91B8;
                case MARS -> 0x66975D4C;
                case ASTEROID, RESOURCE_BODY -> 0x666F675E;
                case SURVEY_REGION -> 0x407F858D;
                case CRAFT, WAYPOINT -> 0x6693D7C8;
            };
            var radiusX = object.data.radius() * camera.zoom();
            var radiusY = radiusX * StarMapCamera.PLANE_TILT;
            if (!intersects(viewport, center.x(), center.y(), radiusX, radiusY)) continue;
            if (object.data.type() == SpaceObjects.ObjectType.SURVEY_REGION) {
                // Stable overlapping, feathered puffs merge into a continuous belt at system scale.
                var random = new Random(object.data.id().getLeastSignificantBits());
                discs.add(new StarMapObjectRenderer.Disc(center.x(), center.y(), radiusX, radiusY,
                        0x657F858D, 0x007F858D, circleSegments(radiusX)));
                for (int puff = 0; puff < 9; puff++) {
                    var angle = random.nextDouble() * Math.PI * 2;
                    var offset = 0.25 + random.nextDouble() * 0.35;
                    var size = 0.4 + random.nextDouble() * 0.25;
                    discs.add(new StarMapObjectRenderer.Disc(center.x() + Math.cos(angle) * radiusX * offset,
                            center.y() + Math.sin(angle) * radiusY * offset, radiusX * size, radiusY * size,
                            0x387F858D, 0x007F858D, circleSegments(radiusX * size)));
                }
            } else discs.add(new StarMapObjectRenderer.Disc(center.x(), center.y(), radiusX, radiusY, color,
                    circleSegments(radiusX)));
        }
        StarMapObjectRenderer.submit(graphics, discs, viewport);
    }

    void addReferenceLines(List<StarMapLineRenderer.Line> lines, StarMapCamera camera, Viewport viewport,
                           RocketStarMapWidget.NavigationSelection selectedTarget, Set<UUID> visibleIcons) {

        var sun = byId(SpaceSimulation.SUN_ID);
        if (sun != null) for (var object : objects) {
            if (object.data.type() == SpaceObjects.ObjectType.EARTH || object.data.type() == SpaceObjects.ObjectType.MARS) {
                addCircle(lines, camera, viewport, sun.data.x(), sun.data.y(),
                        Math.hypot(object.data.x() - sun.data.x(), object.data.y() - sun.data.y()), 0x553C5368, 0.55f);
            }
        }
        for (var object : objects) {
            if (!visibleIcons.contains(object.data.id()) && object.data.type() != SpaceObjects.ObjectType.SURVEY_REGION) continue;
            if (isPlanet(object.data.type())) continue;
            var objectX = x(object);
            var objectY = y(object);
            if (object.data.radius() * camera.zoom() >= 2) {
                var selected = selectedTarget != null && selectedTarget.objectId().equals(object.data.id())
                        && selectedTarget.orbit() == SpaceSimulation.OrbitBand.SURFACE;
                if (object.data.type() == SpaceObjects.ObjectType.SURVEY_REGION && !selected) continue;
                addCircle(lines, camera, viewport, objectX, objectY, SurveyRules.navigationRadius(object.data),
                        selected ? 0xDDF6C65B : 0x997C93A6, selected ? 1.25f : 0.8f);
            }
        }
        // Match the original draw order: solar orbits, surfaces, motion guides, then local orbits.
        addMotionLines(lines, camera, viewport);
        // An offscreen body can still have an orbit crossing the viewport.
        for (var object : objects) {
            var objectX = x(object);
            var objectY = y(object);
            for (var band : RocketFlightPlanRules.availableOrbits(object.data.type())) {
                if (band == SpaceSimulation.OrbitBand.SURFACE) continue;
                var radius = object.data.radius() + band.altitude();
                if (radius * camera.zoom() < 18) continue;
                var selected = selectedTarget != null && selectedTarget.objectId().equals(object.data.id()) && selectedTarget.orbit() == band;
                addCircle(lines, camera, viewport, objectX, objectY, radius,
                        selected ? 0xDDF6C65B : 0x77577A91, selected ? 1.2f : 0.65f);
            }
        }
    }

    private void addMotionLines(List<StarMapLineRenderer.Line> lines, StarMapCamera camera, Viewport viewport) {

        for (var object : objects) {
            var position = displayedPositions.get(object.data.id());
            if (position == null || Math.hypot(position.x() - object.data.x(), position.y() - object.data.y()) < 1)
                continue;
            var from = project(camera, viewport, object.data.x(), object.data.y());
            var to = project(camera, viewport, position.x(), position.y());
            if (!intersects(viewport, (from.x() + to.x()) * 0.5, (from.y() + to.y()) * 0.5,
                    Math.abs(to.x() - from.x()) * 0.5, Math.abs(to.y() - from.y()) * 0.5)) continue;
            lines.add(new StarMapLineRenderer.Line(from.x(), from.y(), to.x(), to.y(), 0x6685A7BC, 0.7f));
        }
    }

    List<Icon> groupIcons(StarMapCamera camera, Viewport viewport,
                                           RocketStarMapWidget.NavigationSelection selectedTarget) {

        var icons = new ArrayList<Icon>();
        for (var object : objects) {
            var type = object.data.type();
            if (type != SpaceObjects.ObjectType.ASTEROID && type != SpaceObjects.ObjectType.CRAFT) continue;
            if (fleetMarkers.contains(object.data.id())) continue;
            var position = project(camera, viewport, x(object), y(object));
            if (!intersects(viewport, position.x(), position.y(), 27, 27)) continue;
            icons.add(new Icon(object.data.id(), type, position.x(), position.y(),
                    selectedTarget != null && selectedTarget.objectId().equals(object.data.id())));
        }
        return icons;
    }

    Set<UUID> visibleIcons(StarMapCamera camera, Viewport viewport, Set<UUID> hiddenIcons) {

        var visible = new HashSet<UUID>();
        var drawnIcons = new HashSet<IconKey>();
        for (var object : objects) {
            if (hiddenIcons.contains(object.data.id()) || object.data.type() == SpaceObjects.ObjectType.SURVEY_REGION) continue;
            var position = project(camera, viewport, x(object), y(object));
            if (object.data.type() == SpaceObjects.ObjectType.CRAFT || object.data.type() == SpaceObjects.ObjectType.WAYPOINT) {
                if (!fleetMarkers.contains(object.data.id()) && intersects(viewport, position.x(), position.y(), 8, 8))
                    visible.add(object.data.id());
                continue;
            }
            var size = iconSize(object, camera);
            var radius = size / 2d;
            var renderRadius = radius + (isPlanet(object.data.type()) ? size * PLANET_TEXTURE_PADDING / ICON_TEXTURE_SIZE : 0);
            if (!intersects(viewport, position.x(), position.y(), renderRadius + 2, renderRadius + 2)) continue;
            // Match the PIP deduplication used when submitting block previews.
            var icon = new IconKey((float) (position.x() - radius), (float) (position.y() - radius),
                    size, object.widget.getState());
            if (drawnIcons.add(icon)) visible.add(object.data.id());
        }
        return Set.copyOf(visible);
    }

    private static float iconSize(Entry object, StarMapCamera camera) {

        var diameterScale = switch (object.data.type()) {
            case SUN, EARTH, MARS, RESOURCE_BODY -> 1;
            case ASTEROID -> 1.3;
            default -> 2;
        };
        return (float) (object.data.radius() * camera.zoom() * diameterScale);
    }

    Entry renderIcons(GuiGraphicsExtractor graphics, StarMapCamera camera, Viewport viewport,
                      RocketStarMapWidget.NavigationSelection selectedTarget, int mouseX, int mouseY, float delta,
                      boolean pointerInViewport, Set<UUID> visibleIcons) {

        Entry hovered = null;
        for (var object : objects) {
            var position = project(camera, viewport, x(object), y(object));
            if (object.data.type() == SpaceObjects.ObjectType.CRAFT || object.data.type() == SpaceObjects.ObjectType.WAYPOINT) {
                // Fleet markers already draw these craft; standalone docking targets use the same icon.
                if (!intersects(viewport, position.x(), position.y(), 8, 8)) continue;
                if (visibleIcons.contains(object.data.id())) {
                    graphics.item(object.data.type() == SpaceObjects.ObjectType.WAYPOINT ? waypointIcon : craftIcon,
                            (int) Math.round(position.x() - 8), (int) Math.round(position.y() - 8));
                    if (camera.zoom() >= CRAFT_NAME_ZOOM) graphics.text(Minecraft.getInstance().font, RocketStarMapWidget.objectName(object.data),
                            (int) Math.round(position.x() + 11), (int) Math.round(position.y() - 4), 0xFF9FB2C4, false);
                }
                if (pointerInViewport && Math.abs(mouseX - position.x()) <= 8 && Math.abs(mouseY - position.y()) <= 8)
                    hovered = object;
                continue;
            }
            if (object.data.type() == SpaceObjects.ObjectType.SURVEY_REGION) {
                var radiusX = Math.max(5, object.data.radius() * camera.zoom());
                var dx = (mouseX - position.x()) / radiusX;
                var dy = (mouseY - position.y()) / (radiusX * StarMapCamera.PLANE_TILT);
                if (pointerInViewport && dx * dx + dy * dy <= 1 && hovered == null) hovered = object;
                continue;
            }
            var size = iconSize(object, camera);
            var radius = size / 2d;
            // PIP block previews are expensive, so never submit one when it is fully clipped away.
            var renderRadius = radius + (isPlanet(object.data.type()) ? size * PLANET_TEXTURE_PADDING / ICON_TEXTURE_SIZE : 0);
            if (!intersects(viewport, position.x(), position.y(), renderRadius + 2, renderRadius + 2)) continue;
            if (visibleIcons.contains(object.data.id())) {
                graphics.pose().pushMatrix();
                graphics.pose().translate((float) (position.x() - radius), (float) (position.y() - radius));
                var scale = size / ICON_TEXTURE_SIZE;
                graphics.pose().scale(scale, scale);
                object.widget.render(graphics, mouseX, mouseY, delta);
                graphics.pose().popMatrix();
            }
            if (pointerInViewport && (isPlanet(object.data.type())
                    ? PlanetSurfaceProjection.contains(object.outline,
                            new RocketMapPaths.Point((mouseX - position.x()) / size, (mouseY - position.y()) / size))
                    : Math.abs(mouseX - position.x()) <= radius && Math.abs(mouseY - position.y()) <= radius)) hovered = object;
            if (object.data.type() != SpaceObjects.ObjectType.ASTEROID) {
                graphics.text(Minecraft.getInstance().font, RocketStarMapWidget.objectName(object.data),
                        (int) Math.round(position.x() + size / 2d + 3), (int) Math.round(position.y() - 4), 0xFF9FB2C4, false);
            }
            if (selectedTarget != null && selectedTarget.objectId().equals(object.data.id())
                    && selectedTarget.orbit() == SpaceSimulation.OrbitBand.SURFACE) {
                if (isPlanet(object.data.type())) {
                    var outlineLines = new ArrayList<StarMapLineRenderer.Line>();
                    for (var index = 0; index < object.outline.size(); index++) {
                        var a = object.outline.get(index);
                        var b = object.outline.get((index + 1) % object.outline.size());
                        outlineLines.add(new StarMapLineRenderer.Line(position.x() + a.x() * size, position.y() + a.y() * size,
                                position.x() + b.x() * size, position.y() + b.y() * size, 0xFFF6C65B, 1, true));
                    }
                    StarMapLineRenderer.submit(graphics, outlineLines, viewport.x(), viewport.y(), viewport.width(), viewport.height());
                    continue;
                }
                graphics.fill((int) Math.round(position.x() - size / 2f - 2), (int) Math.round(position.y() - size / 2f - 2),
                        (int) Math.round(position.x() + size / 2f + 2), (int) Math.round(position.y() - size / 2f), 0xFFF6C65B);
            }
        }
        return hovered;
    }

    RocketStarMapWidget.NavigationSelection findHoveredOrbit(double mouseX, double mouseY, StarMapCamera camera,
                                                             Viewport viewport, Entry hoveredObject) {

        var world = camera.unproject(mouseX, mouseY, viewport.x(), viewport.y(), viewport.width(), viewport.height());
        var closestDistance = 5d;
        RocketStarMapWidget.NavigationSelection closest = null;
        for (var object : objects) {
            var offsetX = world.x() - x(object);
            var offsetY = world.y() - y(object);
            var angle = Math.atan2(offsetY, offsetX);
            for (var band : RocketFlightPlanRules.availableOrbits(object.data.type())) {
                if (band == SpaceSimulation.OrbitBand.SURFACE) continue;
                var radius = object.data.radius() + band.altitude();
                if (radius * camera.zoom() < 18) continue;
                if (!circleIntersects(viewport, camera, x(object), y(object), radius)) continue;
                var ring = project(camera, viewport, x(object) + Math.cos(angle) * radius, y(object) + Math.sin(angle) * radius);
                var distance = Math.hypot(mouseX - ring.x(), mouseY - ring.y());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = new RocketStarMapWidget.NavigationSelection(object.data.id(), band);
                }
            }
        }
        return closest != null ? closest : hoveredObject == null ? null
                                           : new RocketStarMapWidget.NavigationSelection(hoveredObject.data.id(), SpaceSimulation.OrbitBand.SURFACE);
    }

    Entry byId(UUID id) {

        return objects.stream().filter(object -> object.data.id().equals(id)).findFirst().orElse(null);
    }

    RocketStarMapWidget.NavigationSelection outermostOrbit(Entry object) {

        var orbits = RocketFlightPlanRules.availableOrbits(object.data.type());
        return new RocketStarMapWidget.NavigationSelection(object.data.id(), orbits.getLast());
    }

    List<StarMapCamera.Point> focusPoints(RocketStarMapWidget.NavigationSelection selection) {

        var object = byId(selection.objectId());
        if (object == null) return List.of();
        var radius = SurveyRules.navigationRadius(object.data) + selection.orbit().altitude();
        var centerX = x(object);
        var centerY = y(object);
        return List.of(new StarMapCamera.Point(centerX - radius, centerY),
                new StarMapCamera.Point(centerX + radius, centerY),
                new StarMapCamera.Point(centerX, centerY - radius),
                new StarMapCamera.Point(centerX, centerY + radius));
    }

    StarMapTooltip.ArrivalPosition arrivalPosition(UUID objectId) {

        return displayedPositions.get(objectId);
    }

    private double x(Entry object) {

        var position = displayedPositions.get(object.data.id());
        return position == null ? object.data.x() : position.x();
    }

    private double y(Entry object) {

        var position = displayedPositions.get(object.data.id());
        return position == null ? object.data.y() : position.y();
    }

    private record FogPatch(double x, double y, double radius) {

    }

    private record IconKey(float x, float y, float size, BlockState state) {

    }

    /**
     * Visible map bounds in GUI pixels.
     */
    record Viewport(int x, int y, int width, int height) {

        int right() {

            return x + width;
        }

        int bottom() {

            return y + height;
        }
    }

    /**
     * Simulation data, reusable 3D icon, target selection, and tooltips for one map object.
     */
    record Entry(SpaceSimulation.SpaceObjectData data, BlockWidget widget, List<RocketMapPaths.Point> outline) {

    }
}
