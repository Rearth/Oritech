package rearth.oritech.client.renderers.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class BeamRenderer {
    
    public static int color(int r, int g, int b, int a) {
        return (a * 255) << 24
                 | (r * 255) << 16
                 | (g * 255) << 8
                 | (b * 255);
    }
    
    /**
     * Renders a solid, straight beam between two points using a rectangular prism geometry.
     * The beam is rotated to align with the vector provided.
     * <p>
     * The beam gradient is interpolated along the Y-axis of the model (length).
     *
     * @param poseStack   The matrix stack used for rendering.
     * @param consumer    The vertex consumer (buffer) to draw vertices into.
     * @param startPos    The starting position of the beam relative to the current PoseStack origin.
     * @param delta       The vector defining the direction and length of the beam (EndPos - StartPos).
     * @param thickness   The radius (half-width) of the beam.
     * @param packedLight The light value to render with (usually {@code LightTexture.FULL_BRIGHT} for glowing).
     * @param startColor  The ARGB color integer for the start of the beam (e.g. 0xB4FF0000).
     * @param endColor    The ARGB color integer for the end of the beam.
     */
    public static void renderStraightBeam(PoseStack poseStack, VertexConsumer consumer, Vec3 startPos, Vec3 delta, float thickness, int packedLight, int startColor, int endColor) {
        poseStack.pushPose();
        
        // Translate to the start of the beam
        poseStack.translate(startPos.x, startPos.y, startPos.z);
        
        // Calculate Yaw (Y-Axis rotation) and Pitch (X-Axis rotation) to align the beam
        // The beam model is defined along the Y-axis (Up), so we rotate it to match 'delta'
        double xzDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yRot = (float) (-Mth.atan2(-delta.x, delta.z));
        float xRot = (float) (-Mth.atan2(delta.y, xzDist));
        
        poseStack.mulPose(Axis.YP.rotation(yRot));
        poseStack.mulPose(Axis.XP.rotation(xRot + (float) (Math.PI / 2)));
        
        // Beam dimensions
        float length = (float) delta.length();
        float r = thickness; // Radius
        
        // Extract ARGB components
        int a1 = (startColor >> 24) & 0xFF;
        int r1 = (startColor >> 16) & 0xFF;
        int g1 = (startColor >> 8) & 0xFF;
        int b1 = (startColor) & 0xFF;
        
        int a2 = (endColor >> 24) & 0xFF;
        int r2 = (endColor >> 16) & 0xFF;
        int g2 = (endColor >> 8) & 0xFF;
        int b2 = (endColor) & 0xFF;
        
        PoseStack.Pose pose = poseStack.last();
        
        // Draw the 4 sides of the rectangular prism
        
        // Front
        addVertex(consumer, pose, -r, 0, r, r1, g1, b1, a1, 0, 1, packedLight);
        addVertex(consumer, pose, r, 0, r, r1, g1, b1, a1, 1, 1, packedLight);
        addVertex(consumer, pose, r, length, r, r2, g2, b2, a2, 1, 0, packedLight);
        addVertex(consumer, pose, -r, length, r, r2, g2, b2, a2, 0, 0, packedLight);
        
        // Back
        addVertex(consumer, pose, r, 0, -r, r1, g1, b1, a1, 0, 1, packedLight);
        addVertex(consumer, pose, -r, 0, -r, r1, g1, b1, a1, 1, 1, packedLight);
        addVertex(consumer, pose, -r, length, -r, r2, g2, b2, a2, 1, 0, packedLight);
        addVertex(consumer, pose, r, length, -r, r2, g2, b2, a2, 0, 0, packedLight);
        
        // Left
        addVertex(consumer, pose, -r, 0, -r, r1, g1, b1, a1, 0, 1, packedLight);
        addVertex(consumer, pose, -r, 0, r, r1, g1, b1, a1, 1, 1, packedLight);
        addVertex(consumer, pose, -r, length, r, r2, g2, b2, a2, 1, 0, packedLight);
        addVertex(consumer, pose, -r, length, -r, r2, g2, b2, a2, 0, 0, packedLight);
        
        // Right
        addVertex(consumer, pose, r, 0, r, r1, g1, b1, a1, 0, 1, packedLight);
        addVertex(consumer, pose, r, 0, -r, r1, g1, b1, a1, 1, 1, packedLight);
        addVertex(consumer, pose, r, length, -r, r2, g2, b2, a2, 1, 0, packedLight);
        addVertex(consumer, pose, r, length, r, r2, g2, b2, a2, 0, 0, packedLight);
        
        poseStack.popPose();
    }
    
    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int r, int g, int b, int a, float u, float v, int packedLight) {
        consumer.addVertex(pose.pose(), x, y, z)
          .setColor(r, g, b, a)
          .setUv(u, v)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(packedLight)
          .setNormal(pose, 0, 1, 0);
    }
}