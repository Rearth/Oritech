package rearth.oritech.client.cablesurfer;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import rearth.oritech.client.renderers.PowerPoleCableRenderer;

public class ActiveCableRenderer {
    
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player != null && !ClientZiplineHandler.isZiplining(player)) return;
        
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        
        Vec3 start = ClientZiplineHandler.getStartPos();
        Vec3 end = ClientZiplineHandler.getEndPos();
        
        // Get Parallel
        Vec3 parStart = ClientZiplineHandler.getParallelStart();
        Vec3 parEnd = ClientZiplineHandler.getParallelEnd();
        
        if (start == null || end == null) return;
        
        var consumer = bufferSource.getBuffer(RenderType.entitySolid(PowerPoleCableRenderer.CABLE_TEXTURE));
        
        renderHangingCable(poseStack, consumer, start.subtract(camPos), end.subtract(camPos), 0.048f);
        
        if (parStart != null && parEnd != null) {
            renderHangingCable(poseStack, consumer, parStart.subtract(camPos), parEnd.subtract(camPos), 0.048f);
        }
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
        
        int light = LightTexture.FULL_BRIGHT;
        
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