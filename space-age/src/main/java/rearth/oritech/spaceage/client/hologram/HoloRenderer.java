package rearth.oritech.spaceage.client.hologram;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.builders.UVPair;
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
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import rearth.oritech.spaceage.client.starmap.RocketStarMapWidget;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.InputEvent;
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

/** Shared local displays render from received snapshots, never from the flight solver. */
public final class HoloRenderer {

    private static final ContextKey<List<Display>> DISPLAYS = new ContextKey<>(OritechSpaceAge.id("holograms"));
    private static final Map<BlockPos, Cached> CACHE = new HashMap<>();
    private static final Map<BlockState, List<BakedQuad>> MODELS = new HashMap<>();
    private static Object modelSet;
    private static Object world;
    private static Cached aimed;
    private static UUID aimedObject;
    private static List<Component> inspection = List.of();
    private static boolean inspectionPinned;

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
        MODELS.clear();
        world = null;
        aimed = null;
        inspection = List.of();
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
            inspection = List.of();
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
        inspection = List.of();
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
            var s = data.settings();
            if (data.status().equals("empty")) labels.add(new Label(new Vec3(surface.width() / 2d, HoloView.PLANE_HEIGHT + .15, .2),
                    Component.translatable("screen.oritech_space_age.holo.empty", surface.width(), surface.depth())));
            // Subtle grid marks only the active solid rectangle, including on irregular pads.
            for (int x = 0; x <= surface.width(); x++) lines.add(new Line(new Vec3(x, HoloView.PLANE_HEIGHT, 0), new Vec3(x, HoloView.PLANE_HEIGHT, surface.depth()), 0x2852B5CB));
            for (int z = 0; z <= surface.depth(); z++) lines.add(new Line(new Vec3(0, HoloView.PLANE_HEIGHT, z), new Vec3(surface.width(), HoloView.PLANE_HEIGHT, z), 0x2852B5CB));
            for (var object : data.objects()) {
                if (object.type() == ObjectType.CRAFT || object.type() == ObjectType.WAYPOINT) continue;
                var point = HoloView.project(object.x(), object.y(), cached.frame, data);
                if (s.shows(HoloSettings.ORBITS) && object.type() != ObjectType.SURVEY_REGION)
                    for (var orbit : RocketFlightPlanRules.availableOrbits(object.type())) {
                        if (orbit.altitude() > 0) circle(lines, cached, object.x(), object.y(), SurveyRules.orbitRadius(object, orbit), 0x665889A1);
                    }
                if (object.type() == ObjectType.SURVEY_REGION) {
                    if (s.shows(HoloSettings.SURVEYS)) {
                        if (SurveyRules.beltSegment(object)) {
                            var outline = SurveyRules.outline(object);
                            for (int i = 1; i < outline.size(); i++) segment(lines, cached, outline.get(i - 1).x(), outline.get(i - 1).y(), outline.get(i).x(), outline.get(i).y(), 0x88777F8C);
                        } else circle(lines, cached, object.x(), object.y(), object.radius(), 0x88777F8C);
                    }
                    continue;
                }
                if (!s.shows(HoloSettings.BODIES) || !HoloView.inside(point, surface)) continue;
                var size = HoloView.modelDiameter(object, cached.frame, data);
                var edge = Math.min(Math.min(point.x(), surface.width() - point.x()), Math.min(point.y(), surface.depth() - point.y()));
                var alpha = Math.clamp(edge / Math.max(.01, size * .6), 0, 1);
                var planet = object.type() != ObjectType.ASTEROID;
                var position = new Vec3(point.x(), HoloView.PLANE_HEIGHT, point.y());
                models.add(new Miniature(model(block(object)), position, (float) (planet ? size / 2 : size), (float) alpha));
                var name = RocketStarMapWidget.objectName(object);
                var target = new Target(object.id(), position, size / 2 + .025, name, () -> HoloInspection.body(object, name));
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
                var color = selected ? 0xEE6DE9FF : 0x66698895;
                if (selected && s.shows(HoloSettings.PATHS)) {
                    for (int i = 1; i < craft.path().size(); i++) {
                        var a = craft.path().get(i - 1);
                        var b = craft.path().get(i);
                        segment(lines, cached, a.x(), a.y(), b.x(), b.y(), 0xAA5CC9EF);
                    }
                }
                var previous = cached.previous.craft().stream().filter(c -> c.id().equals(craft.id())).findFirst().orElse(craft);
                var progress = Math.clamp((time - cached.received) / 20, 0, 1);
                var point = HoloView.project(previous.x() + (craft.x() - previous.x()) * progress,
                        previous.y() + (craft.y() - previous.y()) * progress, cached.frame, data);
                var inside = HoloView.inside(point, surface);
                var px = Math.clamp(point.x(), .10, surface.width() - .10);
                var pz = Math.clamp(point.y(), .10, surface.depth() - .10);
                if (!inside && !selected) continue;
                var pos = new Vec3(px, HoloView.PLANE_HEIGHT, pz);
                var radius = .07 * s.markerSize();
                if (inside) {
                    lines.add(new Line(pos.add(-radius, -radius, 0), pos.add(0, radius, 0), color));
                    lines.add(new Line(pos.add(0, radius, 0), pos.add(radius, -radius, 0), color));
                    lines.add(new Line(pos.add(radius, -radius, 0), pos.add(-radius, -radius, 0), color));
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
            scenes.add(new Scene(cached, origin, lines, models, labels, targets));
        }
        var displays = new ArrayList<Display>();
        for (var scene : scenes) {
            for (var target : scene.targets) {
                var pinned = target.id.equals(scene.cached.selected);
                var hover = scene.cached == aimed && target == hovered;
                if (!pinned && !hover) continue;
                outline(scene.lines, target, pinned ? 0xFFFFCD58 : 0xFF91F4FF);
                if (scene.cached.snapshot.settings().shows(HoloSettings.LABELS))
                    scene.labels.add(new Label(target.pos.add(0, target.radius + .10, 0), target.name));
                if (hover || hovered == null && pinned && camera.distanceToSqr(scene.origin.add(target.pos)) < 144
                        && scene.origin.add(target.pos).subtract(camera).normalize().dot(look) > .3) {
                    inspection = target.details.get();
                    inspectionPinned = pinned;
                }
            }
            displays.add(new Display(scene.origin.subtract(camera), List.copyOf(scene.lines), List.copyOf(scene.models), List.copyOf(scene.labels)));
        }
        event.getRenderState().setRenderData(DISPLAYS, List.copyOf(displays));
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

    public static void hud(RenderGuiEvent.Post event) {

        var minecraft = Minecraft.getInstance();
        if (inspection.isEmpty() || minecraft.screen != null || minecraft.options.hideGui) return;
        var graphics = event.getGuiGraphics();
        var font = minecraft.font;
        var panelWidth = Math.min(208, graphics.guiWidth() / 2 - 40);
        var rows = new ArrayList<Component>();
        rows.add(HoloControllerScreen.text(inspectionPinned ? "pinned" : "hovered"));
        rows.addAll(inspection);
        rows.add(HoloControllerScreen.text(inspectionPinned ? "unpin_hint" : "pin_hint"));
        var wrapped = rows.stream().flatMap(row -> font.split(row, panelWidth - 16).stream()).limit(Math.max(4, (graphics.guiHeight() - 48) / 11)).toList();
        var panelHeight = wrapped.size() * 11 + 16;
        var x = graphics.guiWidth() - panelWidth - 12;
        var y = Math.max(12, (graphics.guiHeight() - panelHeight) / 2);
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xEA101E2B);
        var accent = inspectionPinned ? 0xFFFFCD58 : 0xFF91F4FF;
        graphics.fill(x, y, x + 2, y + panelHeight, accent);
        for (int i = 0; i < wrapped.size(); i++)
            graphics.text(font, wrapped.get(i), x + 8, y + 8 + i * 11, i < 2 ? accent : 0xFFE4EEF5, false);
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

        var clipped = HoloView.clip(HoloView.project(ax, ay, cached.frame, cached.snapshot),
                HoloView.project(bx, by, cached.frame, cached.snapshot), cached.snapshot.surface());
        if (clipped.size() == 2) lines.add(new Line(new Vec3(clipped.getFirst().x(), HoloView.PLANE_HEIGHT, clipped.getFirst().y()),
                new Vec3(clipped.getLast().x(), HoloView.PLANE_HEIGHT, clipped.getLast().y()), color));
    }

    public static void submit(SubmitCustomGeometryEvent event) {

        var displays = event.getLevelRenderState().getRenderData(DISPLAYS);
        if (displays == null) return;
        var stack = event.getPoseStack();
        var collector = event.getSubmitNodeCollector();
        for (var display : displays) {
            stack.pushPose();
            stack.translate(display.offset.x, display.offset.y, display.offset.z);
            collector.submitCustomGeometry(stack, RenderTypes.lines(), (pose, consumer) -> {
                for (var line : display.lines) {
                    var delta = line.b.subtract(line.a).normalize();
                    consumer.addVertex(pose, (float) line.a.x, (float) line.a.y, (float) line.a.z).setColor(line.color)
                            .setNormal(pose, (float) delta.x, (float) delta.y, (float) delta.z).setLineWidth(1.5f);
                    consumer.addVertex(pose, (float) line.b.x, (float) line.b.y, (float) line.b.z).setColor(line.color)
                            .setNormal(pose, (float) delta.x, (float) delta.y, (float) delta.z).setLineWidth(1.5f);
                }
            });
            for (var model : display.models) {
                stack.pushPose();
                stack.translate(model.pos.x, model.pos.y, model.pos.z);
                stack.scale(model.scale, model.scale, model.scale);
                stack.translate(-.5, -.5, -.5);
                collector.submitCustomGeometry(stack, RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS), (pose, consumer) -> {
                    for (var quad : model.quads) {
                        var normal = quad.direction().getUnitVec3i();
                        for (int i = 0; i < 4; i++) {
                            var vertex = quad.position(i);
                            var uv = quad.packedUV(i);
                            consumer.addVertex(pose.pose(), vertex.x(), vertex.y(), vertex.z()).setColor(.48f, .88f, 1f, .70f * model.alpha)
                                    .setUv(UVPair.unpackU(uv), UVPair.unpackV(uv)).setLight(0xF000F0).setOverlay(OverlayTexture.NO_OVERLAY)
                                    .setNormal(pose, normal.getX(), normal.getY(), normal.getZ());
                        }
                    }
                });
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

    private static final class Cached {

        private HoloSnapshot snapshot = HoloSnapshot.EMPTY;
        private HoloSnapshot previous = HoloSnapshot.EMPTY;
        private Frame frame;
        private UUID selected;
        private long received;
        private double lastTime;
    }

    private record Target(UUID id, Vec3 pos, double radius, Component name, Supplier<List<Component>> details) {
    }

    private record Scene(Cached cached, Vec3 origin, List<Line> lines, List<Miniature> models, List<Label> labels, List<Target> targets) {
    }

    private record Display(Vec3 offset, List<Line> lines, List<Miniature> models, List<Label> labels) {
    }

    private record Line(Vec3 a, Vec3 b, int color) {
    }

    private record Miniature(List<BakedQuad> quads, Vec3 pos, float scale, float alpha) {
    }

    private record Label(Vec3 pos, Component text) {
    }
}
