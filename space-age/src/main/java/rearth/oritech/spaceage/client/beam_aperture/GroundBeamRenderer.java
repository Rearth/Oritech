package rearth.oritech.spaceage.client.beam_aperture;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.network.GroundBeamNetworking.Beams;
import rearth.oritech.spaceage.simulation.power.SpacePowerService.SurfaceBeam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An evenly filled orbital energy shower with a gentle downward travelling pulse.
 */
public final class GroundBeamRenderer {

    private static final ContextKey<Frame> BEAMS = new ContextKey<>(OritechSpaceAge.id("ground_beam_render"));
    private static final Identifier HAZE = OritechSpaceAge.id("textures/effect/ground_beam_haze.png");
    private static final Identifier FLOW = OritechSpaceAge.id("textures/effect/ground_beam_flow.png");
    private static final Identifier CONTACT = OritechSpaceAge.id("textures/effect/ground_beam_contact.png");
    private static final int ENERGY_COLOR = 0x67DEFF;
    private static final Map<SurfaceBeam, Double> visibility = new HashMap<>();
    private static ClientLevel world;
    private static long expires;
    private static double lastTime;
    private static List<SurfaceBeam> targets = List.of();

    private GroundBeamRenderer() {
    }

    public static void clear(ClientPlayerNetworkEvent.LoggingOut event) {

        world = null;
        targets = List.of();
        expires = 0;
        visibility.clear();
    }

    public static void receive(Beams payload) {

        var level = Minecraft.getInstance().level;
        if (world != level) {
            visibility.clear();
            lastTime = level == null ? 0 : level.getGameTime();
        }
        world = level;
        targets = payload.targets();
        expires = world == null ? 0 : world.getGameTime() + 30;
    }

    public static void extract(ExtractLevelRenderStateEvent event) {

        var level = event.getLevel();
        if (level != world || level.dimension() != Level.OVERWORLD) return;
        var time = level.getGameTime() + event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var fadeStep = Math.clamp(time - lastTime, 0, 5) / 10;
        lastTime = time;
        var active = level.getGameTime() <= expires ? targets : List.<SurfaceBeam>of();
        for (var beam : active) visibility.putIfAbsent(beam, 0d);
        visibility.replaceAll((beam, alpha) -> Math.clamp(alpha + (active.contains(beam) ? fadeStep : -fadeStep), 0, 1));
        visibility.entrySet().removeIf(entry -> entry.getValue() == 0 && !active.contains(entry.getKey()));
        var camera = event.getCamera().position();
        var range = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16d;
        var visible = new ArrayList<VisibleBeam>();
        visibility.forEach((beam, alpha) -> {
            if (Math.hypot(beam.target().getX() + .5 - camera.x, beam.target().getZ() + .5 - camera.z) <= range + Math.sqrt(2) * beam.radius())
                visible.add(new VisibleBeam(beam, alpha));
        });
        if (!visible.isEmpty())
            event.getRenderState().setRenderData(BEAMS, new Frame(List.copyOf(visible), camera, level.getMaxY() + 512, time));
    }

    public static void submit(SubmitCustomGeometryEvent event) {

        var data = event.getLevelRenderState().getRenderData(BEAMS);
        if (data == null || !OritechClientConfig.renderBeams()) return;
        var stack = event.getPoseStack();
        stack.pushPose();
        var fullEffects = OritechClientConfig.renderFullEffects();
        for (var visible : data.targets) {
            var beam = visible.beam;
            var pos = beam.target();
            var dx = data.camera.x - pos.getX() - .5;
            var dz = data.camera.z - pos.getZ() - .5;
            var height = data.top - pos.getY();
            var phase = Math.floorMod(pos.asLong(), 997) / 997d * Math.PI * 2;
            var alpha = visible.alpha;
            stack.pushPose();
            stack.translate(-dx, pos.getY() - data.camera.y, -dz);
            // World-aligned faces preserve the square silhouette from every viewing angle.
            event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.eyes(HAZE), (pose, consumer) -> {
                var radius = beam.radius();
                ribbon(pose, consumer, new Vec3(1, 0, 0), 0, radius, radius, height, 16, 0, .12 * alpha, data.time, phase);
                ribbon(pose, consumer, new Vec3(-1, 0, 0), 0, -radius, radius, height, 16, 0, .12 * alpha, data.time, phase);
                ribbon(pose, consumer, new Vec3(0, 0, 1), -radius, 0, radius, height, 16, 0, .12 * alpha, data.time, phase);
                ribbon(pose, consumer, new Vec3(0, 0, -1), radius, 0, radius, height, 16, 0, .12 * alpha, data.time, phase);
            });
            if (fullEffects) {
                event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.eyes(FLOW), (pose, consumer) -> {
                    // World-anchored cells fill the square, including its corners. Bound geometry for very large footprints.
                    var spacing = Math.max(2, beam.radius() / 8);
                    var cells = (int) Math.ceil(beam.radius() / spacing);
                    for (int x = -cells; x < cells; x++) {
                        for (int z = -cells; z < cells; z++) {
                            var seed = Math.sin(x * 127.1 + z * 311.7 + phase) * 43758.5453;
                            var variation = seed - Math.floor(seed);
                            var px = (x + .3 + variation * .4) * spacing;
                            var pz = (z + .7 - variation * .4) * spacing;
                            var distance = Math.max(Math.abs(px), Math.abs(pz));
                            if (distance >= beam.radius()) continue;
                            var edge = Math.min(1, (beam.radius() - distance) / (beam.radius() * .12));
                            var shimmer = 1 + .08 * Math.sin(data.time * .035 + variation * Math.PI * 2);
                            var cellAngle = Math.atan2(dx - px, dz - pz);
                            var cellRight = new Vec3(Math.cos(cellAngle), 0, -Math.sin(cellAngle));
                            ribbon(pose, consumer, cellRight, px, pz, Math.min(spacing * .45, beam.radius() - distance), Math.min(height, 80),
                                    10 + variation * 6, data.time * (.004 + variation * .002) + variation,
                                    .27 * alpha * edge * shimmer, data.time, phase);
                        }
                    }
                });
            }
            event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.eyes(CONTACT), (pose, consumer) ->
                    contact(pose, consumer, beam.radius(), .025, .20 * alpha, data.time, phase));
            stack.popPose();
        }
        stack.popPose();
    }

    private static void ribbon(PoseStack.Pose pose, VertexConsumer consumer, Vec3 right, double centerX, double centerZ,
                               double radius, double height, double repeat, double scroll, double alpha, double time, double phase) {

        var x = right.x * radius;
        var z = right.z * radius;
        var offset = scroll - Math.floor(scroll);
        // Short sections keep the travelling brightness and width changes smooth.
        for (double bottom = 0; bottom < height; bottom += 4) {
            var top = Math.min(height, bottom + 4);
            var lowerPulse = pulse(bottom, time, phase);
            var upperPulse = pulse(top, time, phase);
            var lowerAlpha = alpha * Math.min(1, (height - bottom) / Math.min(height, 32)) * lowerPulse;
            var upperAlpha = alpha * Math.min(1, (height - top) / Math.min(height, 32)) * upperPulse;
            // Swell the entire shower by up to 2.8%, tapering to the true footprint at ground level.
            var lowerWidth = 1 + .08 * (lowerPulse - 1) * Math.min(1, bottom / 8);
            var upperWidth = 1 + .08 * (upperPulse - 1) * Math.min(1, top / 8);
            var v1 = (float) (bottom / repeat + offset);
            var v2 = (float) (top / repeat + offset);
            vertex(pose, consumer, (centerX - x) * lowerWidth, bottom, (centerZ - z) * lowerWidth, 0, v1, lowerAlpha, ENERGY_COLOR);
            vertex(pose, consumer, (centerX + x) * lowerWidth, bottom, (centerZ + z) * lowerWidth, 1, v1, lowerAlpha, ENERGY_COLOR);
            vertex(pose, consumer, (centerX + x) * upperWidth, top, (centerZ + z) * upperWidth, 1, v2, upperAlpha, ENERGY_COLOR);
            vertex(pose, consumer, (centerX - x) * upperWidth, top, (centerZ - z) * upperWidth, 0, v2, upperAlpha, ENERGY_COLOR);
        }
    }

    private static double pulse(double height, double time, double phase) {

        // Keep only the bright lobe of every fifth cycle; positive time moves it downward.
        var position = height + time * 6 + phase / (Math.PI * 2) * 160;
        var local = position - Math.floor(position / 160) * 160;
        return local < 48 ? 1 + .35 * Math.max(0, Math.sin(Math.PI * 2 * local / 48)) : 1;
    }

    private static void contact(PoseStack.Pose pose, VertexConsumer consumer, double radius, double y, double alpha, double time, double phase) {

        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                contactVertex(pose, consumer, radius, y, x, z, alpha, time, phase);
                contactVertex(pose, consumer, radius, y, x, z + 1, alpha, time, phase);
                contactVertex(pose, consumer, radius, y, x + 1, z + 1, alpha, time, phase);
                contactVertex(pose, consumer, radius, y, x + 1, z, alpha, time, phase);
            }
        }
    }

    private static void contactVertex(PoseStack.Pose pose, VertexConsumer consumer, double radius, double y,
                                      int x, int z, double alpha, double time, double phase) {

        var shimmer = 1 + .08 * Math.sin(x * 1.7 + z * 2.3 + time * .03 + phase);
        vertex(pose, consumer, (x / 4d - 1) * radius, y, (z / 4d - 1) * radius,
                x / 8f, z / 8f, alpha * shimmer * pulse(0, time, phase), ENERGY_COLOR);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, double x, double y, double z,
                               float u, float v, double alpha, int color) {

        consumer.addVertex(pose.pose(), (float) x, (float) y, (float) z).setColor((int) (Math.clamp(alpha, 0, 1) * 255) << 24 | color).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(RenderHelpers.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    private record VisibleBeam(SurfaceBeam beam, double alpha) {
    }

    private record Frame(List<VisibleBeam> targets, Vec3 camera, int top, double time) {
    }
}
