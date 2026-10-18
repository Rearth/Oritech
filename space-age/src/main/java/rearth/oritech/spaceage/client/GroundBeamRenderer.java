package rearth.oritech.spaceage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.client.renderers.blocks.EndericLaserRenderer;
import rearth.oritech.client.renderers.util.BeamRenderer;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.network.GroundBeamNetworking.Beams;
import rearth.oritech.spaceage.simulation.SpacePowerService.SurfaceBeam;

import java.util.List;

/** Narrow Enderic centerline plus a translucent cylinder showing the complete ground footprint. */
public final class GroundBeamRenderer {

    private static final ContextKey<Frame> BEAMS = new ContextKey<>(OritechSpaceAge.id("ground_beam_render"));
    private static ClientLevel world;
    private static long expires;
    private static List<SurfaceBeam> targets = List.of();

    private GroundBeamRenderer() {}

    public static void clear(ClientPlayerNetworkEvent.LoggingOut event) {

        world = null;
        targets = List.of();
        expires = 0;
    }

    public static void receive(Beams payload) {

        world = Minecraft.getInstance().level;
        targets = payload.targets();
        expires = world == null ? 0 : world.getGameTime() + 30;
    }

    public static void extract(ExtractLevelRenderStateEvent event) {

        var level = event.getLevel();
        if (level != world || level.dimension() != Level.OVERWORLD || level.getGameTime() > expires) return;
        var camera = event.getCamera().position();
        var range = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16d;
        var visible = targets.stream().filter(beam -> Math.hypot(beam.target().getX() + .5 - camera.x, beam.target().getZ() + .5 - camera.z) <= range + beam.radius()).toList();
        if (!visible.isEmpty()) event.getRenderState().setRenderData(BEAMS, new Frame(visible, camera, level.getMaxY() + 512));
    }

    public static void submit(SubmitCustomGeometryEvent event) {

        var data = event.getLevelRenderState().getRenderData(BEAMS);
        if (data == null || !OritechClientConfig.renderBeams()) return;
        var stack = event.getPoseStack();
        stack.pushPose();
        stack.translate(-data.camera.x, -data.camera.y, -data.camera.z);
        for (var beam : data.targets) {
            var pos = beam.target();
            var start = new Vec3(pos.getX() + .5, data.top, pos.getZ() + .5);
            var delta = new Vec3(0, pos.getY() - data.top, 0);
            event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.entityTranslucentEmissive(EndericLaserRenderer.BEAM_TEXTURE),
                    (pose, consumer) -> renderFootprint(pose, consumer, start, pos.getY(), beam.radius()));
            event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.eyes(EndericLaserRenderer.BEAM_TEXTURE),
                    (pose, consumer) -> BeamRenderer.renderStraightBeam(pose, consumer, start, delta, .12f,
                            RenderHelpers.FULL_BRIGHT, EndericLaserRenderer.CORE_COLOR_START, EndericLaserRenderer.CORE_COLOR_END));
        }
        stack.popPose();
    }

    private static void renderFootprint(PoseStack.Pose pose, VertexConsumer consumer, Vec3 top, double bottom, double radius) {

        // Fixed radius is intentional: this is the ground collection area, not a simulated atmospheric cone.
        for (int side = 0; side < 32; side++) {
            var angle = side * Math.PI * 2 / 32;
            var next = (side + 1) * Math.PI * 2 / 32;
            var x1 = top.x + Math.cos(angle) * radius;
            var z1 = top.z + Math.sin(angle) * radius;
            var x2 = top.x + Math.cos(next) * radius;
            var z2 = top.z + Math.sin(next) * radius;
            vertex(pose, consumer, x1, bottom, z1, 0, 1);
            vertex(pose, consumer, x2, bottom, z2, 1, 1);
            vertex(pose, consumer, x2, top.y, z2, 1, 0);
            vertex(pose, consumer, x1, top.y, z1, 0, 0);
            // The bottom disc makes the footprint visible while standing inside the beam.
            vertex(pose, consumer, top.x, bottom + .02, top.z, .5f, .5f);
            vertex(pose, consumer, x2, bottom + .02, z2, 1, 1);
            vertex(pose, consumer, x1, bottom + .02, z1, 0, 1);
            vertex(pose, consumer, top.x, bottom + .02, top.z, .5f, .5f);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, double x, double y, double z, float u, float v) {

        consumer.addVertex(pose.pose(), (float) x, (float) y, (float) z).setColor(0x248AF2DF).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(RenderHelpers.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    private record Frame(List<SurfaceBeam> targets, Vec3 camera, int top) {}
}
