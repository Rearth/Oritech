package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.interaction.PowerPoleEntity;
import rearth.oritech.util.Geometry;

public class PowerPoleCableRenderer implements BlockEntityRenderer<PowerPoleEntity> {
    
    public static final ResourceLocation CABLE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    
    public static final int CABLE_SEGMENT_COUNT = 12;
    
    @Override
    public void render(@NotNull PowerPoleEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        
        var connections = blockEntity.getConnections();
        if (connections.isEmpty() || blockEntity.isRemoved()) return;
        
        var camPos = Minecraft.getInstance().cameraEntity.blockPosition();
        var poleDist = blockEntity.getBlockPos().distSqr(camPos);
        
        var consumer = bufferSource.getBuffer(RenderType.entitySolid(CABLE_TEXTURE));
        var ownPos = blockEntity.getBlockPos();
        
        var ownFacing = blockEntity.getFacingForMultiblock();
        var ownSideVec = Vec3.atLowerCornerOf(Geometry.getForward(ownFacing));
        
        var centerPos = new Vec3(0.5, 0.5, 0.5);
        
        poseStack.pushPose();
        poseStack.translate(0, 0.35f, 0);
        
        for (var target : connections) {
            if (target == null) continue;
            
            var targetDist = target.pos().distSqr(camPos);
            if (targetDist < poleDist) continue;    // only render connections from the closer side
            
            var startWorldA = Vec3.atCenterOf(ownPos).add(ownSideVec);
            var startWorldB = Vec3.atCenterOf(ownPos).subtract(ownSideVec);
            
            var targetArmVec = Vec3.atLowerCornerOf(Geometry.getForward(target.facing()));
            
            var targetWorldA = Vec3.atCenterOf(target.pos()).add(targetArmVec);
            var targetWorldB = Vec3.atCenterOf(target.pos()).subtract(targetArmVec);
            
            var distDirect = startWorldA.distanceToSqr(targetWorldA) + startWorldB.distanceToSqr(targetWorldB);
            var distCross = startWorldA.distanceToSqr(targetWorldB) + startWorldB.distanceToSqr(targetWorldA);
            
            Vec3 targetForStartA;
            Vec3 targetForStartB;
            
            if (distDirect < distCross) {
                targetForStartA = targetWorldA;
                targetForStartB = targetWorldB;
            } else {
                targetForStartA = targetWorldB;
                targetForStartB = targetWorldA;
            }
            
            var localStartA = centerPos.add(ownSideVec);
            var localStartB = centerPos.subtract(ownSideVec);
            
            var localTargetA = targetForStartA.subtract(ownPos.getX(), ownPos.getY(), ownPos.getZ());
            var localTargetB = targetForStartB.subtract(ownPos.getX(), ownPos.getY(), ownPos.getZ());
            
            float thickness = 0.05f;
            
            renderHangingCable(poseStack, consumer, localStartA, localTargetA, thickness, packedLight);
            renderHangingCable(poseStack, consumer, localStartB, localTargetB, thickness, packedLight);
        }
        
        poseStack.popPose();
    }
    
    /**
     * Renders a cable hanging between two points in local render space.
     * @param startPos The start position relative to the BlockEntity origin (e.g. 0.5, 0.5, 0.5 is center).
     * @param endPos   The end position relative to the BlockEntity origin.
     */
    private void renderHangingCable(PoseStack poseStack, VertexConsumer consumer, Vec3 startPos, Vec3 endPos, float thickness, int packedLight) {
        poseStack.pushPose();
        
        // Calculate the full vector from start to end
        var totalOffset = endPos.subtract(startPos);
        
        var segments = CABLE_SEGMENT_COUNT;
        var totalLength = (float) totalOffset.length();
        var sag = totalLength * 0.05f; // Sag amount based on distance
        sag = Math.min(sag, 4);
        
        var currentPos = startPos; // Start drawing exactly at the start offset
        
        for (int i = 0; i < segments; i++) {
            var t = (float) (i + 1) / segments;
            
            // Linear interpolation from Start to End
            var nextPos = startPos.add(totalOffset.scale(t));
            
            // note: The same formula is also used in cablemath, but since some values are precalculated here its duplicated
            
            // Parabolic Sag to Y component
            // Formula: -4 * sag * t * (1-t)
            // We use (t * (1-t)) which peaks at t=0.5 with value 0.25. Multiplied by 4 gives 1.0.
            var sagY = -sag * 4 * t * (1 - t);
            nextPos = nextPos.add(0, sagY, 0);
            
            // Calculate vector for this specific segment
            var segmentDelta = nextPos.subtract(currentPos);
            
            // To prevent gaps between segments due to rotation, we can slightly overscale length,
            // or rely on the specific joint math. Simple scaling usually works fine.
            var drawDelta = segmentDelta.scale(1.02); // 2% overlap to hide cracks
            
            drawSegment(poseStack, consumer, currentPos, drawDelta, thickness, packedLight);
            
            currentPos = nextPos;
        }
        
        poseStack.popPose();
    }
    
    private void drawSegment(PoseStack poseStack, VertexConsumer consumer, Vec3 startPos, Vec3 delta, float thickness, int packedLight) {
        poseStack.pushPose();
        
        // Move to start of segment
        poseStack.translate(startPos.x, startPos.y, startPos.z);
        
        // Calculate rotations to align the Y-axis (length) with the segment delta
        var xzLen = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        var yRot = (float) (-Math.atan2(-delta.x, delta.z)); // Standard MC Yaw calculation
        var xRot = (float) (-Math.atan2(delta.y, xzLen));    // Standard MC Pitch calculation
        
        poseStack.mulPose(Axis.YP.rotation(yRot));
        poseStack.mulPose(Axis.XP.rotation(xRot + (float) (Math.PI / 2)));
        
        // Draw the Box
        float length = (float) delta.length();
        float r = thickness;
        
        int red = 50, green = 50, blue = 50, alpha = 255;
        var pose = poseStack.last();
        
        // --- Side 1 (Front) ---
        addVertex(consumer, pose, -r, 0, r, red, green, blue, alpha, 0, 0, 0, 0, 1, packedLight);
        addVertex(consumer, pose, r, 0, r, red, green, blue, alpha, 1, 0, 0, 0, 1, packedLight);
        addVertex(consumer, pose, r, length, r, red, green, blue, alpha, 1, 1, 0, 0, 1, packedLight);
        addVertex(consumer, pose, -r, length, r, red, green, blue, alpha, 0, 1, 0, 0, 1, packedLight);
        
        // --- Side 2 (Back) ---
        addVertex(consumer, pose, r, 0, -r, red, green, blue, alpha, 0, 0, 0, 0, -1, packedLight);
        addVertex(consumer, pose, -r, 0, -r, red, green, blue, alpha, 1, 0, 0, 0, -1, packedLight);
        addVertex(consumer, pose, -r, length, -r, red, green, blue, alpha, 1, 1, 0, 0, -1, packedLight);
        addVertex(consumer, pose, r, length, -r, red, green, blue, alpha, 0, 1, 0, 0, -1, packedLight);
        
        // --- Side 3 (Left) ---
        addVertex(consumer, pose, -r, 0, -r, red, green, blue, alpha, 0, 0, -1, 0, 0, packedLight);
        addVertex(consumer, pose, -r, 0, r, red, green, blue, alpha, 1, 0, -1, 0, 0, packedLight);
        addVertex(consumer, pose, -r, length, r, red, green, blue, alpha, 1, 1, -1, 0, 0, packedLight);
        addVertex(consumer, pose, -r, length, -r, red, green, blue, alpha, 0, 1, -1, 0, 0, packedLight);
        
        // --- Side 4 (Right) ---
        addVertex(consumer, pose, r, 0, r, red, green, blue, alpha, 0, 0, 1, 0, 0, packedLight);
        addVertex(consumer, pose, r, 0, -r, red, green, blue, alpha, 1, 0, 1, 0, 0, packedLight);
        addVertex(consumer, pose, r, length, -r, red, green, blue, alpha, 1, 1, 1, 0, 0, packedLight);
        addVertex(consumer, pose, r, length, r, red, green, blue, alpha, 0, 1, 1, 0, 0, packedLight);
        
        poseStack.popPose();
    }
    
    private void addVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int r, int g, int b, int a, float u, float v, float nx, float ny, float nz, int packedLight) {
        consumer.addVertex(pose.pose(), x, y, z)
          .setColor(r, g, b, a)
          .setUv(u, v)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(packedLight)
          .setNormal(pose, nx, ny, nz);
    }
    
    @Override
    public int getViewDistance() {
        return 256;
    }
    
    @Override
    public boolean shouldRenderOffScreen(PowerPoleEntity blockEntity) {
        return true;
    }
}