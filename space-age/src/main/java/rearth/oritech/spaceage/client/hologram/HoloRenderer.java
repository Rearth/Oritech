package rearth.oritech.spaceage.client.hologram;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionHand;
import rearth.oritech.spaceage.client.starmap.RocketStarMapWidget;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterLevel;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.hologram.HoloSettings;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Point;
import rearth.oritech.spaceage.block.hologram.HoloView;
import rearth.oritech.spaceage.block.hologram.HoloView.Frame;
import rearth.oritech.spaceage.simulation.ResourceBodies;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.HoloNetworking.Projection;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector3f;

/** Shared local displays render from received snapshots, never from the flight solver. */
public final class HoloRenderer {

    private static final ContextKey<List<Display>> DISPLAYS = new ContextKey<>(OritechSpaceAge.id("holograms"));
    private static final Map<BlockPos, Cached> CACHE = new HashMap<>();
    private static final Map<BlockState, List<BakedQuad>> MODELS = new HashMap<>();
    private static final SubmitNodeStorage SUBMITS = new SubmitNodeStorage();
    private static final CustomFeatureRenderer GEOMETRY = new CustomFeatureRenderer();
    private static final TextFeatureRenderer TEXT = new TextFeatureRenderer();
    // The late pass draws into the composed world, not the separate item/entity transparency target.
    private static final RenderType LINES = RenderType.create("oritech_holo_lines",
            RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT).createRenderSetup());
    private static final float CALLOUT_SCALE = .012f;
    private static Object modelSet;
    private static Object world;
    private static Cached aimed;
    private static UUID aimedObject;

    private HoloRenderer() {
    }

    public static HoloSnapshot snapshot(BlockPos pos) {

        var cached = CACHE.get(pos);
        return cached == null ? HoloSnapshot.EMPTY : cached.snapshot;
    }

    public static void receive(Projection payload) {

        var level = Minecraft.getInstance().level;
        if (world != level) {
            CACHE.clear();
            world = level;
        }
        if (level == null) return;
        var cached = CACHE.computeIfAbsent(payload.pos(), ignored -> new Cached());
        cached.previous = cached.snapshot;
        cached.snapshot = payload.snapshot();
        cached.received = level.getGameTime();
        if (cached.frame == null) cached.frame = HoloView.frame(cached.snapshot, null);
    }

    public static void clear(ClientPlayerNetworkEvent.LoggingOut event) {

        CACHE.clear();
        SUBMITS.clear();
        MODELS.clear();
        world = null;
        aimed = null;
    }

    public static void interact(InputEvent.InteractionKeyMappingTriggered event) {

        if (Minecraft.getInstance().screen != null || !event.isUseItem() || aimed == null || aimed.snapshot.settings().locked()) return;
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            var clear = aimedObject.equals(aimed.selected);
            CACHE.values().forEach(cached -> cached.selected = null);
            if (!clear) aimed.selected = aimedObject;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static Point craftPosition(HoloSnapshot.Craft craft, Cached cached, double time) {

        var data = cached.snapshot;
        var route = new HoloPath(craft, data, cached.frame);
        var previous = cached.previous.craft().stream().filter(c -> c.id().equals(craft.id())).findFirst().orElse(craft);
        var progress = Math.clamp((time - cached.received) / 20, 0, 1);
        var sameTransfer = !craft.path().isEmpty() && !previous.path().isEmpty()
                && craft.path().getFirst().equals(previous.path().getFirst()) && craft.flightTime() >= previous.flightTime();
        var flightTime = sameTransfer ? previous.flightTime() + (craft.flightTime() - previous.flightTime()) * progress : craft.flightTime();
        return craft.destroyed() ? HoloView.surfacePosition(new Point(craft.x(), craft.y()), cached.frame, data)
                : route.position(flightTime, previous.x() + (craft.x() - previous.x()) * progress,
                        previous.y() + (craft.y() - previous.y()) * progress);
    }

    private static List<BakedQuad> model(BlockState state) {

        var minecraft = Minecraft.getInstance();
        var models = minecraft.getModelManager().getBlockStateModelSet();
        if (modelSet != models) {
            MODELS.clear();
            modelSet = models;
        }
        return MODELS.computeIfAbsent(state, key -> {
            var parts = new ArrayList<BlockStateModelPart>();
            models.get(key).collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, key, RandomSource.create(42), parts);
            var quads = new ArrayList<BakedQuad>();
            for (var part : parts) {
                quads.addAll(part.getQuads(null));
                for (var direction : Direction.values()) quads.addAll(part.getQuads(direction));
            }
            return List.copyOf(quads);
        });
    }

    static BlockState block(SpaceObjectData object) {

        return switch (object.type()) {
            case SUN -> SpaceAgeBlocks.SUN.get().defaultBlockState();
            case EARTH -> SpaceAgeBlocks.EARTH.get().defaultBlockState();
            case MARS -> SpaceAgeBlocks.MARS.get().defaultBlockState();
            case RESOURCE_BODY -> {
                var connection = Minecraft.getInstance().getConnection();
                var definition = connection == null ? null : ResourceBodies.find(connection.registryAccess(), object.id());
                yield definition == null ? SpaceAgeBlocks.ASTEROID_1.get().defaultBlockState()
                        : BuiltInRegistries.BLOCK.getValue(definition.modelBlock()).defaultBlockState();
            }
            default -> switch (Math.floorMod(object.id().hashCode(), 3)) {
                case 0 -> SpaceAgeBlocks.ASTEROID_1.get().defaultBlockState();
                case 1 -> SpaceAgeBlocks.ASTEROID_2.get().defaultBlockState();
                default -> SpaceAgeBlocks.ASTEROID_3.get().defaultBlockState();
            };
        };
    }

    public static void extract(ExtractLevelRenderStateEvent event) {

        var level = event.getLevel();
        if (world != level) {
            aimed = null;
            return;
        }
        var minecraft = Minecraft.getInstance();
        var camera = event.getCamera().position();
        var time = level.getGameTime() + event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        CACHE.entrySet().removeIf(e -> level.getGameTime() - e.getValue().received > 45
                || !level.getBlockState(e.getKey()).is(SpaceAgeBlocks.HOLO_CONTROLLER));
        var scenes = new ArrayList<Scene>();
        aimed = null;
        var aimDistance = 8d;
        var look = minecraft.player == null ? Vec3.ZERO : minecraft.player.getLookAngle();
        if (minecraft.player != null) aimDistance = Math.min(aimDistance,
                camera.distanceTo(minecraft.player.pick(8, event.getDeltaTracker().getGameTimeDeltaPartialTick(false), false).getLocation()));
        Target hovered = null;
        for (var entry : CACHE.entrySet()) {
            var cached = entry.getValue();
            var data = cached.snapshot;
            if ((!data.status().equals("ready") && !data.status().equals("empty")) || data.surface().width() == 0) continue;
            var elapsed = cached.lastTime == 0 ? 0 : (time - cached.lastTime) / 20;
            cached.lastTime = time;
            cached.frame = cached.frame.towards(HoloView.frame(data, cached.frame), elapsed, data.settings().response());
            var surface = data.surface();
            var origin = Vec3.atLowerCornerOf(surface.origin()).add(0, 1.04, 0);
            if (origin.distanceToSqr(camera) > 128 * 128) continue;
            var lines = new ArrayList<Line>();
            var models = new ArrayList<Miniature>();
            var labels = new ArrayList<Label>();
            var targets = new ArrayList<Target>();
            var emitters = new ArrayList<HoloEmitters.Beam>();
            var s = data.settings();
            if (data.status().equals("empty")) labels.add(new Label(new Vec3(surface.width() / 2d, HoloView.PLANE_HEIGHT + .15, .2),
                    Component.translatable("screen.oritech_space_age.holo.empty", surface.width(), surface.depth())));
            var hidden = cached.asteroids.hidden(data, cached.frame, cached.selected);
            for (var object : data.objects()) {
                if (hidden.contains(object.id())) continue;
                if (object.type() == ObjectType.CRAFT || object.type() == ObjectType.WAYPOINT) continue;
                var tug = data.craft().stream().filter(c -> c.id().equals(object.tugId())).findFirst().orElse(null);
                var mapped = tug == null ? new Point(object.x(), object.y()) : craftPosition(tug, cached, time);
                var point = HoloView.project(mapped.x(), mapped.y(), cached.frame, data);
                if (s.shows(HoloSettings.ORBITS) && object.type() != ObjectType.SURVEY_REGION && object.type() != ObjectType.ASTEROID)
                    for (var orbit : RocketFlightPlanRules.availableOrbits(object.type())) {
                        if (orbit.altitude() > 0) circle(lines, cached, object.x(), object.y(), SurveyRules.orbitRadius(object, orbit), 0x665889A1);
                    }
                if (object.type() == ObjectType.SURVEY_REGION) continue;
                if (!s.shows(HoloSettings.BODIES) || !HoloView.inside(point, surface)) continue;
                var size = HoloView.modelDiameter(object, cached.frame, data);
                var edge = Math.min(Math.min(point.x(), surface.width() - point.x()), Math.min(point.y(), surface.depth() - point.y()));
                var alpha = Math.clamp(edge / Math.max(.01, size * .6), 0, 1);
                var planet = object.type() != ObjectType.ASTEROID;
                var position = new Vec3(point.x() - (tug == null ? 0 : size / 2 + .10 * s.markerSize()), HoloView.PLANE_HEIGHT, point.y());
                models.add(new Miniature(model(block(object)), position, (float) (planet ? size / 2 : size), (float) alpha));
                emitters.add(new HoloEmitters.Beam(position, size / 2, alpha, object.id().hashCode()));
                var name = RocketStarMapWidget.objectName(object);
                var target = new Target(object.id(), position, size / 2 + .025, name,
                        () -> HoloInspection.body(object, name, tug == null ? null : tug.name()));
                targets.add(target);
                var hit = AABB.ofSize(origin.add(position), size, size, size).clip(camera, camera.add(look.scale(aimDistance)));
                if (hit.isPresent()) {
                    aimDistance = camera.distanceTo(hit.get());
                    aimed = cached;
                    aimedObject = object.id();
                    hovered = target;
                }
            }
            for (var craft : data.craft()) {
                var selected = HoloView.selected(s, craft);
                if (!selected && !s.shows(HoloSettings.OTHER_CRAFT)) continue;
                var color = craft.destroyed() ? (selected ? 0xDDDF7973 : 0x887F5656) : selected ? 0xEE6DE9FF : 0x66698895;
                var route = new HoloPath(craft, data, cached.frame);
                if (selected && s.shows(HoloSettings.PATHS)) {
                    for (var leg : craft.journey()) {
                        var points = route.journey(leg);
                        for (int i = 1; i < points.size(); i++) {
                            var a = points.get(i - 1);
                            var b = points.get(i);
                            segment(lines, cached, a.x(), a.y(), b.x(), b.y(), 0xAA739CAD, 2.25f);
                        }
                    }
                    if (craft.destroyed()) {
                        var points = route.active();
                        for (int i = 1; i < points.size(); i++) {
                            var a = points.get(i - 1);
                            var b = points.get(i);
                            segment(lines, cached, a.x(), a.y(), b.x(), b.y(), 0x997F6565, 2.25f);
                        }
                    } else activePath(lines, cached, route.active(), time / 20);
                }
                var mapped = craftPosition(craft, cached, time);
                var point = HoloView.project(mapped.x(), mapped.y(), cached.frame, data);
                var inside = HoloView.inside(point, surface);
                var px = Math.clamp(point.x(), .10, surface.width() - .10);
                var pz = Math.clamp(point.y(), .10, surface.depth() - .10);
                if (!inside && !selected) continue;
                var pos = new Vec3(px, HoloView.PLANE_HEIGHT, pz);
                var radius = .07 * s.markerSize();
                if (craft.destroyed()) {
                    var orientation = event.getRenderState().cameraRenderState.orientation;
                    var right = orientation.transform(new Vector3f(1, 0, 0));
                    var up = orientation.transform(new Vector3f(0, 1, 0));
                    var first = new Vec3(right.x + up.x, right.y + up.y, right.z + up.z).scale(radius);
                    var second = new Vec3(right.x - up.x, right.y - up.y, right.z - up.z).scale(radius);
                    lines.add(new Line(pos.subtract(first), pos.add(first), color, 2.25f));
                    lines.add(new Line(pos.subtract(second), pos.add(second), color, 2.25f));
                } else if (inside) {
                    emitters.add(new HoloEmitters.Beam(pos, radius, selected ? 1 : .45, craft.id().hashCode()));
                    models.add(new Miniature(model(SpaceAgeBlocks.SPACECRAFT_ICON.get().defaultBlockState()), pos,
                            (float) (radius * 32 / 44), selected ? 1f : .65f));
                } else {
                    var direction = new Vec3(point.x() - surface.width() / 2d, 0, point.y() - surface.depth() / 2d).normalize();
                    var back = pos.subtract(direction.scale(radius * 2));
                    var side = new Vec3(-direction.z, 0, direction.x).scale(radius);
                    lines.add(new Line(back.add(side), pos, color));
                    lines.add(new Line(pos, back.subtract(side), color));
                }
                var name = Component.literal(craft.name());
                var target = new Target(craft.id(), pos, radius + .025, name, () -> {
                    var destination = data.objects().stream().filter(o -> o.id().equals(craft.target())).findFirst()
                            .map(RocketStarMapWidget::objectName).orElseGet(() -> data.craft().stream().filter(c -> c.id().equals(craft.target()))
                                    .<Component>map(c -> Component.literal(c.name())).findFirst().orElse(HoloControllerScreen.text("free_flight")));
                    var details = new ArrayList<>(HoloInspection.craft(craft, data, destination));
                    if (!inside) details.add(Component.translatable("screen.oritech_space_age.holo.offscreen",
                            Math.round(Math.hypot(craft.x() - cached.frame.x(), craft.y() - cached.frame.y()))));
                    return List.copyOf(details);
                });
                targets.add(target);
                var hit = AABB.ofSize(origin.add(pos), .18, .18, .18).clip(camera, camera.add(look.scale(aimDistance)));
                if (hit.isPresent()) {
                    aimDistance = camera.distanceTo(hit.get());
                    aimed = cached;
                    aimedObject = craft.id();
                    hovered = target;
                }
            }
            scenes.add(new Scene(cached, origin, lines, models, labels, targets, HoloClouds.build(data, cached.frame), emitters));
        }
        Scene pinnedScene = null;
        Target pinnedTarget = null;
        for (var scene : scenes) {
            for (var target : scene.targets) {
                var pinned = target.id.equals(scene.cached.selected);
                var hover = scene.cached == aimed && target == hovered;
                if (pinned) {
                    pinnedScene = scene;
                    pinnedTarget = target;
                }
                if (pinned || hover) outline(scene.lines, target, pinned ? 0xFFFFCD58 : 0xFF91F4FF);
            }
        }
        var viewedScene = pinnedScene != null ? pinnedScene : scenes.stream().filter(scene -> scene.cached == aimed).findFirst().orElse(null);
        var viewed = pinnedTarget != null ? pinnedTarget : hovered;
        var displays = new ArrayList<Display>();
        for (var scene : scenes) {
            Callout callout = null;
            if (scene == viewedScene && viewed != null && scene.cached.snapshot.settings().shows(HoloSettings.LABELS))
                callout = callout(scene.lines, viewed, pinnedTarget != null, event);
            displays.add(new Display(scene.origin.subtract(camera), List.copyOf(scene.lines), List.copyOf(scene.models), List.copyOf(scene.labels), callout, scene.clouds, List.copyOf(scene.emitters), time / 20));
        }
        event.getRenderState().setRenderData(DISPLAYS, List.copyOf(displays));
    }

    private static Callout callout(List<Line> lines, Target target, boolean pinned, ExtractLevelRenderStateEvent event) {

        var font = Minecraft.getInstance().font;
        var content = target.details.get();
        var rows = content.stream().flatMap(row -> font.split(row, 170).stream()).toList();
        var width = rows.stream().mapToInt(font::width).max().orElse(80) + 16;
        var height = rows.size() * 11 + 12;
        var orientation = event.getRenderState().cameraRenderState.orientation;
        var right = orientation.transform(new Vector3f(1, 0, 0));
        var up = orientation.transform(new Vector3f(0, 1, 0));
        var rightVector = new Vec3(right.x, right.y, right.z);
        var upVector = new Vec3(up.x, up.y, up.z);
        var center = target.pos.add(0, target.radius + .35 + height * CALLOUT_SCALE / 2, 0)
                .add(rightVector.scale(target.radius + .25 + width * CALLOUT_SCALE / 2));
        var attachment = center.subtract(rightVector.scale(width * CALLOUT_SCALE / 2)).subtract(upVector.scale(height * CALLOUT_SCALE / 2 - .08));
        var elbow = target.pos.add(0, target.radius + .15, 0);
        var color = pinned ? 0xFFFFCD58 : 0xFF91F4FF;
        lines.add(new Line(target.pos, elbow, color));
        lines.add(new Line(elbow, attachment, color));
        return new Callout(center, rows, width, height, color);
    }

    private static void outline(List<Line> lines, Target target, int color) {

        var radius = target.radius;
        for (int axis = 0; axis < 3; axis++) {
            for (int a = -1; a <= 1; a += 2) {
                for (int b = -1; b <= 1; b += 2) {
                    var from = axis == 0 ? new Vec3(-radius, a * radius, b * radius)
                            : axis == 1 ? new Vec3(a * radius, -radius, b * radius) : new Vec3(a * radius, b * radius, -radius);
                    var to = axis == 0 ? from.add(radius * 2, 0, 0) : axis == 1 ? from.add(0, radius * 2, 0) : from.add(0, 0, radius * 2);
                    lines.add(new Line(target.pos.add(from), target.pos.add(to), color));
                }
            }
        }
    }

    private static void activePath(List<Line> lines, Cached cached, List<Point> points, double seconds) {

        var distance = 0d;
        var spacing = 2d;
        var pulseLength = .32;
        var phase = seconds * .7 % spacing;
        for (int i = 1; i < points.size(); i++) {
            var a = HoloView.project(points.get(i - 1).x(), points.get(i - 1).y(), cached.frame, cached.snapshot);
            var b = HoloView.project(points.get(i).x(), points.get(i).y(), cached.frame, cached.snapshot);
            var length = Math.hypot(b.x() - a.x(), b.y() - a.y());
            if (length < 1e-6) continue;
            var clipped = HoloView.clip(a, b, cached.snapshot.surface());
            if (clipped.size() == 2) {
                // Keep the active stroke above its dim forecast/history counterpart to avoid depth flicker.
                var from = new Vec3(clipped.getFirst().x(), HoloView.PLANE_HEIGHT + .008, clipped.getFirst().y());
                var to = new Vec3(clipped.getLast().x(), HoloView.PLANE_HEIGHT + .008, clipped.getLast().y());
                if (from.distanceToSqr(to) < 1e-12) {
                    distance += length;
                    continue;
                }
                lines.add(new Line(from, to, 0xEE5CE3FF, 3.5f));
                var start = distance + Math.hypot(clipped.getFirst().x() - a.x(), clipped.getFirst().y() - a.y());
                var end = start + from.distanceTo(to);
                var direction = new Vec3((b.x() - a.x()) / length, 0, (b.y() - a.y()) / length);
                // Split at distance boundaries, so even long, uneven guidance samples have a moving short pulse.
                for (var head = Math.ceil((start - phase) / spacing) * spacing + phase; head - pulseLength < end; head += spacing) {
                    var tail = Math.max(start, head - pulseLength);
                    var tip = Math.min(end, head);
                    if (tip <= tail) continue;
                    lines.add(new Line(from.add(direction.scale(tail - start)).add(0, .003, 0),
                            from.add(direction.scale(tip - start)).add(0, .003, 0), 0xFFF2FDFF, 4.5f));
                }
            }
            distance += length;
        }
    }

    private static void circle(List<Line> lines, Cached cached, double x, double y, double radius, int color) {

        var center = HoloView.project(x, y, cached.frame, cached.snapshot);
        var surface = cached.snapshot.surface();
        var projectedRadius = radius * Math.max(.1, Math.min(surface.width(), surface.depth()) - .5) / cached.frame.coverage();
        if (projectedRadius < .025 || center.x() + projectedRadius < 0 || center.y() + projectedRadius < 0
                || center.x() - projectedRadius > surface.width() || center.y() - projectedRadius > surface.depth()) return;
        for (int i = 0; i < 96; i++) {
            var a = i * Math.PI / 48;
            var b = (i + 1) * Math.PI / 48;
            segment(lines, cached, x + Math.cos(a) * radius, y + Math.sin(a) * radius, x + Math.cos(b) * radius, y + Math.sin(b) * radius, color);
        }
    }

    private static void segment(List<Line> lines, Cached cached, double ax, double ay, double bx, double by, int color) {

        segment(lines, cached, ax, ay, bx, by, color, 1.5f);
    }

    private static void segment(List<Line> lines, Cached cached, double ax, double ay, double bx, double by, int color, float width) {

        if (Math.hypot(bx - ax, by - ay) < 1e-6) return;
        var clipped = HoloView.clip(HoloView.project(ax, ay, cached.frame, cached.snapshot),
                HoloView.project(bx, by, cached.frame, cached.snapshot), cached.snapshot.surface());
        if (clipped.size() == 2) lines.add(new Line(new Vec3(clipped.getFirst().x(), HoloView.PLANE_HEIGHT, clipped.getFirst().y()),
                new Vec3(clipped.getLast().x(), HoloView.PLANE_HEIGHT, clipped.getLast().y()), color, width));
    }

    public static void submit(SubmitCustomGeometryEvent event) {

        SUBMITS.clear();
        var displays = event.getLevelRenderState().getRenderData(DISPLAYS);
        if (displays == null) return;
        var stack = event.getPoseStack();
        var collector = SUBMITS;
        for (var display : displays) {
            stack.pushPose();
            stack.translate(display.offset.x, display.offset.y, display.offset.z);
            collector.submitCustomGeometry(stack, LINES, (pose, consumer) -> {
                for (var line : display.lines) {
                    var delta = line.b.subtract(line.a).normalize();
                    consumer.addVertex(pose, (float) line.a.x, (float) line.a.y, (float) line.a.z).setColor(line.color)
                            .setNormal(pose, (float) delta.x, (float) delta.y, (float) delta.z).setLineWidth(line.width);
                    consumer.addVertex(pose, (float) line.b.x, (float) line.b.y, (float) line.b.z).setColor(line.color)
                            .setNormal(pose, (float) delta.x, (float) delta.y, (float) delta.z).setLineWidth(line.width);
                }
            });
            if (!display.emitters.isEmpty()) {
                collector.submitCustomGeometry(stack, LINES, (pose, consumer) -> {
                    for (var beam : display.emitters) HoloEmitters.render(pose, consumer, beam, display.seconds);
                });
            }
            if (!display.clouds.isEmpty()) {
                var orientation = event.getLevelRenderState().cameraRenderState.orientation;
                var right = orientation.transform(new Vector3f(1, 0, 0));
                var up = orientation.transform(new Vector3f(0, 1, 0));
                collector.submitCustomGeometry(stack, RenderTypes.debugQuads(), (pose, consumer) -> {
                    for (var puff : display.clouds) {
                        var center = puff.center();
                        for (int i = 0; i < 16; i++) {
                            var a = i * Math.PI / 8;
                            var b = (i + 1) * Math.PI / 8;
                            var first = HoloClouds.edge(puff, right, up, a);
                            var second = HoloClouds.edge(puff, right, up, b);
                            consumer.addVertex(pose, (float) center.x, (float) center.y, (float) center.z).setColor(0x3895ACBD);
                            consumer.addVertex(pose, (float) first.x, (float) first.y, (float) first.z).setColor(0x0095ACBD);
                            consumer.addVertex(pose, (float) second.x, (float) second.y, (float) second.z).setColor(0x0095ACBD);
                            consumer.addVertex(pose, (float) second.x, (float) second.y, (float) second.z).setColor(0x0095ACBD);
                        }
                    }
                });
            }
            for (var model : display.models) {
                stack.pushPose();
                stack.translate(model.pos.x, model.pos.y, model.pos.z);
                stack.scale(model.scale, model.scale, model.scale);
                stack.translate(-.5, -.5, -.5);
                collector.submitCustomGeometry(stack, RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS), (pose, consumer) -> {
                    for (var quad : model.quads) {
                        var normal = quad.direction().getUnitVec3i();
                        var sprite = quad.materialInfo().sprite();
                        // The wrapper also marks animated sprites visible for renderers such as Sodium.
                        var vertices = sprite.wrap(consumer);
                        for (int i = 0; i < 4; i++) {
                            var vertex = quad.position(i);
                            var uv = quad.packedUV(i);
                            vertices.addVertex(pose.pose(), vertex.x(), vertex.y(), vertex.z()).setColor(.78f, .95f, 1f, .88f * model.alpha)
                                    .setUv((UVPair.unpackU(uv) - sprite.getU0()) / (sprite.getU1() - sprite.getU0()),
                                            (UVPair.unpackV(uv) - sprite.getV0()) / (sprite.getV1() - sprite.getV0())).setLight(0xF000F0).setOverlay(OverlayTexture.NO_OVERLAY)
                                    .setNormal(pose, normal.getX(), normal.getY(), normal.getZ());
                        }
                    }
                });
                stack.popPose();
            }
            if (display.callout != null) {
                var callout = display.callout;
                stack.pushPose();
                stack.translate(callout.pos.x, callout.pos.y, callout.pos.z);
                stack.mulPose(event.getLevelRenderState().cameraRenderState.orientation);
                stack.scale(CALLOUT_SCALE, -CALLOUT_SCALE, CALLOUT_SCALE);
                collector.submitCustomGeometry(stack, RenderTypes.debugQuads(), (pose, consumer) -> {
                    var left = -callout.width / 2f;
                    var top = -callout.height / 2f;
                    var right = callout.width / 2f;
                    var bottom = callout.height / 2f;
                    consumer.addVertex(pose, left, top, -.02f).setColor(0xEA101E2B);
                    consumer.addVertex(pose, left, bottom, -.02f).setColor(0xEA101E2B);
                    consumer.addVertex(pose, right, bottom, -.02f).setColor(0xEA101E2B);
                    consumer.addVertex(pose, right, top, -.02f).setColor(0xEA101E2B);
                });
                for (int i = 0; i < callout.rows.size(); i++)
                    collector.submitText(stack, -callout.width / 2f + 8, -callout.height / 2f + 6 + i * 11,
                            callout.rows.get(i), false, Font.DisplayMode.NORMAL, 0xF000F0, i == 0 ? callout.color : 0xFFE4EEF5, 0, 0);
                stack.popPose();
            }
            for (var label : display.labels) {
                stack.pushPose();
                stack.translate(label.pos.x, label.pos.y, label.pos.z);
                stack.mulPose(event.getLevelRenderState().cameraRenderState.orientation);
                stack.scale(.009f, -.009f, .009f);
                collector.submitText(stack, -Minecraft.getInstance().font.width(label.text) / 2f, 0, label.text.getVisualOrderText(),
                        false, Font.DisplayMode.NORMAL, 0xF000F0, 0xFF9AEAFF, 0x50202B35, 0);
                stack.popPose();
            }
            stack.popPose();
        }
    }

    public static void render(AfterLevel event) {

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix().mul(event.getModelViewMatrix());
        try {
            for (var collection : SUBMITS.getSubmitsPerOrder().values()) {
                GEOMETRY.renderTranslucent(collection, buffers);
                buffers.endBatch();
                TEXT.renderTranslucent(collection, buffers);
                buffers.endBatch();
            }
        } finally {
            modelView.popMatrix();
            SUBMITS.clear();
            SUBMITS.endFrame();
        }
    }

    private static final class Cached {

        private HoloSnapshot snapshot = HoloSnapshot.EMPTY;
        private HoloSnapshot previous = HoloSnapshot.EMPTY;
        private Frame frame;
        private final HoloAsteroidGroups asteroids = new HoloAsteroidGroups();
        private UUID selected;
        private long received;
        private double lastTime;
    }

    private record Target(UUID id, Vec3 pos, double radius, Component name, Supplier<List<Component>> details) {
    }

    private record Scene(Cached cached, Vec3 origin, List<Line> lines, List<Miniature> models, List<Label> labels, List<Target> targets, List<HoloClouds.Puff> clouds, List<HoloEmitters.Beam> emitters) {
    }

    private record Display(Vec3 offset, List<Line> lines, List<Miniature> models, List<Label> labels, Callout callout, List<HoloClouds.Puff> clouds, List<HoloEmitters.Beam> emitters, double seconds) {
    }

    private record Callout(Vec3 pos, List<FormattedCharSequence> rows, int width, int height, int color) {
    }

    private record Line(Vec3 a, Vec3 b, int color, float width) {

        private Line(Vec3 a, Vec3 b, int color) {

            this(a, b, color, 1.5f);
        }
    }

    private record Miniature(List<BakedQuad> quads, Vec3 pos, float scale, float alpha) {
    }

    private record Label(Vec3 pos, Component text) {
    }
}
