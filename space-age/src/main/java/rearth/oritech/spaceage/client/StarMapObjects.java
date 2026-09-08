package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Celestial object state and all object, surface, and orbit map rendering. */
final class StarMapObjects {

    // Reference rings need enough pieces to remain smooth while zoomed in.
    private static final int CIRCLE_SEGMENTS = 48;
    // Surface discs are simple background geometry; the foreground icons remain block previews.
    private static final int DISC_SEGMENTS = 28;

    // Each object keeps its 3D block widget so its familiar planet icon can be reused each frame.
    private final List<Entry> objects = new ArrayList<>();
    // Asteroids move only for the selected craft's completed navigation events.
    private final Map<UUID, StarMapTooltip.ArrivalPosition> displayedPositions = new HashMap<>();

    void setSnapshot(SpaceSimulation.FlightPlannerSnapshot snapshot) {
        objects.clear();
        for (var object : snapshot.objects()) {
            int size = object.type() == SpaceObjects.ObjectType.ASTEROID ? 12 : 18;
            objects.add(new Entry(object, new BlockWidget(0, 0, size, placeholderBlock(object.type()))));
        }
    }

    void updateArrivalPositions(RocketFlightPathCalculator.CraftPath selectedPath,
                                Map<UUID, SpaceSimulation.FlightPlanAction> actionsById,
                                SpaceSimulation.FlightPlannerSnapshot snapshot) {
        displayedPositions.clear();
        if (selectedPath == null) return;
        for (var moment : selectedPath.actionMoments()) {
            var action = actionsById.get(moment.actionId());
            if (!moment.completed() || action == null || action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) continue;
            var target = snapshot.objects().stream().filter(object -> object.id().equals(action.targetId())).findFirst().orElse(null);
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
        for (var object : objects) {
            if (object.data.radius() * camera.zoom() < 2) continue;
            var center = project(camera, viewport, x(object), y(object));
            var color = switch (object.data.type()) {
                case SUN -> 0x66D7A438;
                case EARTH -> 0x665A91B8;
                case MARS -> 0x66975D4C;
                case ASTEROID -> 0x666F675E;
            };
            var radiusX = object.data.radius() * camera.zoom();
            var radiusY = radiusX * StarMapCamera.PLANE_TILT;
            if (!intersects(viewport, center.x(), center.y(), radiusX, radiusY)) continue;
            discs.add(new StarMapObjectRenderer.Disc(center.x(), center.y(), radiusX, radiusY, color, DISC_SEGMENTS));
        }
        StarMapObjectRenderer.submit(graphics, discs, viewport);
    }

    void addReferenceLines(List<StarMapLineRenderer.Line> lines, StarMapCamera camera, Viewport viewport,
                           RocketStarMapWidget.NavigationSelection selectedTarget) {
        var sun = byId(SpaceSimulation.SUN_ID);
        if (sun != null) for (var object : objects) {
            if (object.data.type() == SpaceObjects.ObjectType.EARTH || object.data.type() == SpaceObjects.ObjectType.MARS) {
                addCircle(lines, camera, viewport, sun.data.x(), sun.data.y(),
                        Math.hypot(object.data.x() - sun.data.x(), object.data.y() - sun.data.y()), 0x553C5368, 0.55f);
            }
        }
        for (var object : objects) {
            var objectX = x(object);
            var objectY = y(object);
            if (object.data.radius() * camera.zoom() >= 2) {
                var selected = selectedTarget != null && selectedTarget.objectId().equals(object.data.id())
                        && selectedTarget.orbit() == SpaceSimulation.OrbitBand.SURFACE;
                addCircle(lines, camera, viewport, objectX, objectY, object.data.radius(),
                        selected ? 0xDDF6C65B : 0x997C93A6, selected ? 1.25f : 0.8f);
            }
        }
        // Match the original draw order: solar orbits, surfaces, motion guides, then local orbits.
        addMotionLines(lines, camera, viewport);
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
            if (position == null || Math.hypot(position.x() - object.data.x(), position.y() - object.data.y()) < 1) continue;
            var from = project(camera, viewport, object.data.x(), object.data.y());
            var to = project(camera, viewport, position.x(), position.y());
            if (!intersects(viewport, (from.x() + to.x()) * 0.5, (from.y() + to.y()) * 0.5,
                    Math.abs(to.x() - from.x()) * 0.5, Math.abs(to.y() - from.y()) * 0.5)) continue;
            lines.add(new StarMapLineRenderer.Line(from.x(), from.y(), to.x(), to.y(), 0x6685A7BC, 0.7f));
        }
    }

    Entry renderIcons(GuiGraphicsExtractor graphics, StarMapCamera camera, Viewport viewport,
                      RocketStarMapWidget.NavigationSelection selectedTarget, int mouseX, int mouseY, float delta,
                      boolean pointerInViewport) {
        Entry hovered = null;
        for (var object : objects) {
            var position = project(camera, viewport, x(object), y(object));
            var minimum = object.data.type() == SpaceObjects.ObjectType.ASTEROID ? 10 : 16;
            var size = (int) Math.clamp(object.data.radius() * camera.zoom() * 2, minimum, 54);
            var radius = size / 2d;
            // PIP block previews are expensive, so never submit one when it is fully clipped away.
            if (!intersects(viewport, position.x(), position.y(), radius + 2, radius + 2)) continue;
            object.widget.setPosition((int) Math.round(position.x() - radius), (int) Math.round(position.y() - radius));
            object.widget.setSize(size, size);
            object.widget.render(graphics, mouseX, mouseY, delta);
            if (pointerInViewport && object.widget.isMouseOver(mouseX, mouseY)) hovered = object;
            if (object.data.type() != SpaceObjects.ObjectType.ASTEROID) {
                graphics.text(Minecraft.getInstance().font, RocketStarMapWidget.objectName(object.data),
                        (int) Math.round(position.x() + size / 2d + 3), (int) Math.round(position.y() - 4), 0xFF9FB2C4, false);
            }
            if (selectedTarget != null && selectedTarget.objectId().equals(object.data.id())
                    && selectedTarget.orbit() == SpaceSimulation.OrbitBand.SURFACE) {
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
        var radius = object.data.radius() + selection.orbit().altitude();
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

    private static void addCircle(List<StarMapLineRenderer.Line> lines, StarMapCamera camera, Viewport viewport,
                                  double centerX, double centerY, double radius, int color, float width) {
        if (!circleIntersects(viewport, camera, centerX, centerY, radius)) return;
        var previous = project(camera, viewport, centerX + radius, centerY);
        for (var index = 1; index <= CIRCLE_SEGMENTS; index++) {
            var angle = Math.PI * 2 * index / CIRCLE_SEGMENTS;
            var next = project(camera, viewport, centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius);
            lines.add(new StarMapLineRenderer.Line(previous.x(), previous.y(), next.x(), next.y(), color, width));
            previous = next;
        }
    }

    private static StarMapCamera.Point project(StarMapCamera camera, Viewport viewport, double x, double y) {
        return camera.project(x, y, viewport.x(), viewport.y(), viewport.width(), viewport.height());
    }

    private static BlockState placeholderBlock(SpaceObjects.ObjectType type) {
        return switch (type) {
            case EARTH -> Blocks.GRASS_BLOCK.defaultBlockState();
            case SUN -> Blocks.GOLD_BLOCK.defaultBlockState();
            case MARS -> Blocks.RED_SAND.defaultBlockState();
            case ASTEROID -> Blocks.IRON_ORE.defaultBlockState();
        };
    }

    private static boolean intersects(Viewport viewport, double x, double y, double radiusX, double radiusY) {
        return x + radiusX >= viewport.x() && x - radiusX < viewport.right()
                && y + radiusY >= viewport.y() && y - radiusY < viewport.bottom();
    }

    private static boolean circleIntersects(Viewport viewport, StarMapCamera camera, double x, double y, double radius) {
        var center = project(camera, viewport, x, y);
        var radiusX = radius * camera.zoom();
        return intersects(viewport, center.x(), center.y(), radiusX, radiusX * StarMapCamera.PLANE_TILT);
    }

    private double x(Entry object) {
        var position = displayedPositions.get(object.data.id());
        return position == null ? object.data.x() : position.x();
    }

    private double y(Entry object) {
        var position = displayedPositions.get(object.data.id());
        return position == null ? object.data.y() : position.y();
    }

    /** Visible map bounds in GUI pixels. */
    record Viewport(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
    }

    /** Simulation data, reusable 3D icon, target selection, and tooltips for one map object. */
    record Entry(SpaceSimulation.SpaceObjectData data, BlockWidget widget) {
    }
}
