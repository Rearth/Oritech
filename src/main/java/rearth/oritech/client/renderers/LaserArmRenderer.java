package rearth.oritech.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.HashMap;

import static net.minecraft.client.render.RenderPhase.VIEW_OFFSET_Z_LAYERING;

public class LaserArmRenderer<T extends LaserArmBlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    
    public LaserArmRenderer(String modelPath) {
        super(new LaserArmModel<>(modelPath));
    }
    
    // Modified RenderLayer.LINES
    public static final RenderLayer.MultiPhase CUSTOM_LINES = RenderLayer.of("lines", VertexFormats.LINES, VertexFormat.DrawMode.LINES, 1536, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.LINES_PROGRAM).layering(VIEW_OFFSET_Z_LAYERING).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).target(RenderPhase.ITEM_ENTITY_TARGET).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).build(false));
    private static final HashMap<LaserArmBlockEntity, Vec3d> cachedOffsets = new HashMap<>();
    
    @Override
    public void postRender(MatrixStack matrices, T laserEntity, BakedGeoModel model, VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(matrices, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        
        if (laserEntity.getCurrentTarget() == null || !laserEntity.isFiring()) return;
        
        var startOffset = new Vector3f(0, 1.55f, 0);
        var startPos = Vec3d.of(laserEntity.getPos()).add(0.5, 1.55, 0.5);
        
        var targetPos = Vec3d.of(laserEntity.getCurrentTarget()).add(0, 0.5, 0);   // convert to block center
        if (laserEntity.isTargetingAtomicForge()) { // adjust so the beam end faces one of the corner pillars
            var moveX = 0.5;
            var moveZ = 0.5;
            if (startPos.x < targetPos.x) moveX = -0.5;
            if (startPos.z < targetPos.z) moveZ = -0.5;
            targetPos = targetPos.add(moveX, 0.5, moveZ);
        } else if (laserEntity.isTargetingDeepdrill()) {
            var offset = cachedOffsets.computeIfAbsent(laserEntity, id -> idToOffset(id.getPos(), 0.7f));
            targetPos = targetPos.add(0, 1, 1).add(offset);
        }
        
        if (laserEntity.lastRenderPosition == null) laserEntity.lastRenderPosition = targetPos;
        targetPos = lerp(laserEntity.lastRenderPosition, targetPos, 0.06f);
        laserEntity.lastRenderPosition = targetPos;
        
        var targetPosOffset = targetPos.subtract(Vec3d.of(laserEntity.getPos()));
        
        var forward = targetPos.subtract(startPos).normalize();
        if (!laserEntity.isTargetingEnergyContainer() && !laserEntity.isTargetingBuddingAmethyst())
            ParticleContent.LASER_BEAM_EFFECT.spawn(laserEntity.getWorld(), startPos.add(forward), new ParticleContent.LineData(startPos.add(forward), targetPos.add(0.5, 0, 0.5).subtract(forward.multiply(0.6))));
        
        var cross = forward.crossProduct(new Vec3d(0, 1, 0));
        
        matrices.push();
        var lineConsumer = bufferSource.getBuffer(CUSTOM_LINES);
        
        // to prevent line from becoming too big when further away, as the size seems to be in screen space
        var camPos = MinecraftClient.getInstance().cameraEntity.getPos();
        var camDist = camPos.subtract(startPos).length();
        var widthMultiplier = 1f;
        if (camDist > 20)
            widthMultiplier = (float) (camDist / 20f);
        RenderSystem.lineWidth((float) (Math.sin((laserEntity.getWorld().getTime() + partialTick) * 0.3) * 2 + 7) / widthMultiplier);
        
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), startOffset.x, startOffset.y, startOffset.z)
          .color(138, 242, 223, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal(0, 1, 0);
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), (float) targetPosOffset.x, (float) targetPosOffset.y, (float) targetPosOffset.z)
          .color(19, 91, 80, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal(1, 0, 0);
        
        // render a second one at right angle to first one
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), startOffset.x, startOffset.y, startOffset.z)
          .color(138, 242, 223, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal((float) cross.x, (float) cross.y, (float) cross.z);
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), (float) targetPosOffset.x, (float) targetPosOffset.y, (float) targetPosOffset.z)
          .color(19, 91, 80, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal((float) cross.x, (float) cross.y, (float) cross.z);
        
        matrices.pop();
    }
    
    private static Vec3d idToOffset(BlockPos source, float range) {
        var random = Random.create(source.asLong());
        return new Vec3d((random.nextFloat() * 2 - 1) * range, (random.nextFloat() * 2 - 1) * range, (random.nextFloat() * 2 - 1) * range);
    }
    
    private static Vec3d lerp(Vec3d a, Vec3d b, float f) {
        return new Vec3d(lerp(a.x, b.x, f), lerp(a.y, b.y, f), lerp(a.z, b.z, f));
    }
    
    private static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
}


