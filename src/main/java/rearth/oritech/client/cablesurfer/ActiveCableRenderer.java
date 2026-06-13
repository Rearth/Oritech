package rearth.oritech.client.cablesurfer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.blocks.PowerPoleCableRenderer;
import rearth.oritech.client.renderers.util.RenderHelpers;

public class ActiveCableRenderer {

    // similar to geckolibs data tickets
    public static final ContextKey<CableRenderData> CABLE_DATA = new ContextKey<>(Oritech.id("zipline_cable"));

    // everything is in world space
    public record CableRenderData(Vec3 start, Vec3 end, Vec3 parallelStart, Vec3 parallelEnd, Vec3 cameraPos) {}

    // Extraction phase: capture the current zipline state into the level render state.
    public static void onExtractRenderState(ExtractLevelRenderStateEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || !ClientZiplineHandler.isZiplining(player)) return;

        var start = ClientZiplineHandler.getStartPos();
        var end = ClientZiplineHandler.getEndPos();
        if (start == null || end == null) return;

        var camPos = event.getCamera().position();
        var data = new CableRenderData(start, end, ClientZiplineHandler.getParallelStart(), ClientZiplineHandler.getParallelEnd(), camPos);

        event.getRenderState().setRenderData(CABLE_DATA, data);
    }

    // actual rendering submission
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        var data = event.getLevelRenderState().getRenderData(CABLE_DATA);
        if (data == null) return;

        var camPos = data.cameraPos();
        var start = data.start().subtract(camPos);
        var end = data.end().subtract(camPos);

        event.getSubmitNodeCollector().submitCustomGeometry(event.getPoseStack(), RenderTypes.entitySolid(PowerPoleCableRenderer.CABLE_TEXTURE), (pose, buffer) -> {

            var poseStack = new PoseStack();
            poseStack.last().pose().set(pose.pose());
            poseStack.last().normal().set(pose.normal());

            renderHangingCable(poseStack, buffer, start, end, 0.048f);

            if (data.parallelStart() != null && data.parallelEnd() != null) {
                renderHangingCable(poseStack, buffer, data.parallelStart().subtract(camPos), data.parallelEnd().subtract(camPos), 0.048f);
            }
        });
    }

    private static void renderHangingCable(PoseStack poseStack, VertexConsumer consumer, Vec3 startPos, Vec3 endPos, float thickness) {

        var totalOffset = endPos.subtract(startPos);
        float totalLength = (float) totalOffset.length();
        int segments = Mth.clamp((int) totalLength, 8, 48);

        var sag = Math.min(totalLength * 0.05f, 4);
        var currentPos = startPos;

        for (int i = 0; i < segments; i++) {
            float t = (float) (i + 1) / segments;

            // Linear
            var nextPos = startPos.add(totalOffset.scale(t));

            // Parabolic Sag
            var sagY = -sag * 4 * t * (1 - t);
            nextPos = nextPos.add(0, sagY, 0);

            var segmentDelta = nextPos.subtract(currentPos);

            // Draw segment
            drawSegment(poseStack, consumer, currentPos, segmentDelta, thickness);

            currentPos = nextPos;
        }
    }

    private static void drawSegment(PoseStack poseStack, VertexConsumer consumer, Vec3 startPos, Vec3 delta, float thickness) {
        poseStack.pushPose();
        poseStack.translate(startPos.x, startPos.y, startPos.z);
        poseStack.translate(0, 0.35f, 0);

        var xzLen = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        var yRot = (float) (-Math.atan2(-delta.x, delta.z));
        var xRot = (float) (-Math.atan2(delta.y, xzLen));

        poseStack.mulPose(Axis.YP.rotation(yRot));
        poseStack.mulPose(Axis.XP.rotation(xRot + (float) (Math.PI / 2)));

        float length = (float) delta.length() * 1.02f; // Slight overlap
        float r = thickness;

        int light = RenderHelpers.FULL_BRIGHT;

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal(); // Critical for rotating lighting

        // --- Side 1 (Front: Z+) ---
        // Normal: (0, 0, 1)
        addVertex(consumer, pose, normal, -r, 0, r, 0, 0, 0, 0, 1, light);
        addVertex(consumer, pose, normal, r, 0, r, 1, 0, 0, 0, 1, light);
        addVertex(consumer, pose, normal, r, length, r, 1, 1, 0, 0, 1, light);
        addVertex(consumer, pose, normal, -r, length, r, 0, 1, 0, 0, 1, light);

        // --- Side 2 (Back: Z-) ---
        // Normal: (0, 0, -1)
        addVertex(consumer, pose, normal, r, 0, -r, 0, 0, 0, 0, -1, light);
        addVertex(consumer, pose, normal, -r, 0, -r, 1, 0, 0, 0, -1, light);
        addVertex(consumer, pose, normal, -r, length, -r, 1, 1, 0, 0, -1, light);
        addVertex(consumer, pose, normal, r, length, -r, 0, 1, 0, 0, -1, light);

        // --- Side 3 (Left: X-) ---
        // Normal: (-1, 0, 0)
        addVertex(consumer, pose, normal, -r, 0, -r, 0, 0, -1, 0, 0, light);
        addVertex(consumer, pose, normal, -r, 0, r, 1, 0, -1, 0, 0, light);
        addVertex(consumer, pose, normal, -r, length, r, 1, 1, -1, 0, 0, light);
        addVertex(consumer, pose, normal, -r, length, -r, 0, 1, -1, 0, 0, light);

        // --- Side 4 (Right: X+) ---
        // Normal: (1, 0, 0)
        addVertex(consumer, pose, normal, r, 0, r, 0, 0, 1, 0, 0, light);
        addVertex(consumer, pose, normal, r, 0, -r, 1, 0, 1, 0, 0, light);
        addVertex(consumer, pose, normal, r, length, -r, 1, 1, 1, 0, 0, light);
        addVertex(consumer, pose, normal, r, length, r, 0, 1, 1, 0, 0, light);

        poseStack.popPose();
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normalMatrix,
                                  float x, float y, float z, float u, float v,
                                  float nx, float ny, float nz, int light) {

        var n = new Vector3f(nx, ny, nz);
        n.mul(normalMatrix);

        consumer.addVertex(pose, x, y, z)
                .setColor(50, 50, 50, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(n.x, n.y, n.z);
    }
}
