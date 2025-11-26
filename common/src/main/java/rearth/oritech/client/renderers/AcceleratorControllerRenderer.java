package rearth.oritech.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.client.init.ParticleContent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.Vec3;

import static rearth.oritech.client.renderers.LaserArmRenderer.CUSTOM_LINES;


public class AcceleratorControllerRenderer implements BlockEntityRenderer<AcceleratorControllerBlockEntity> {
    
    private record RenderedLine(float startedAt, List<Vec3> positions) {
    }
    
    private final Map<Long, RenderedLine> activeLines = new HashMap<>();
    
    @Override
    public int getViewDistance() {
        return 128;
    }
    
    @Override
    public boolean shouldRenderOffScreen(AcceleratorControllerBlockEntity blockEntity) {
        return true;
    }
    @Override
    public void render(AcceleratorControllerBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        
        if (entity.displayTrail == null) {
            activeLines.remove(entity.getBlockPos().asLong());
            return;
        }
        
        var lineConsumer = vertexConsumers.getBuffer(CUSTOM_LINES);
        var time = entity.getLevel().getGameTime() + tickDelta;
        
        // try adding new tail to lines
        var displayTrail = entity.displayTrail;
        if (!activeLines.containsKey(entity.getBlockPos().asLong()) || !activeLines.get(entity.getBlockPos().asLong()).positions.equals(displayTrail)) {
            activeLines.put(entity.getBlockPos().asLong(), new RenderedLine(time, displayTrail));
            ParticleContent.PARTICLE_MOVING.spawn(entity.getLevel(), displayTrail.getLast());
        }
        
        var activeLine = activeLines.get(entity.getBlockPos().asLong());
        var line = activeLine.positions;
        var age = time - activeLine.startedAt;
        if (age >= 60) {
            if (entity.displayTrail.equals(activeLine.positions)) entity.displayTrail = null;
        }
        
        
        for (int i = 0; i < line.size() - 1; i++) {
            var start = line.get(i).subtract(Vec3.atLowerCornerOf(entity.getBlockPos()));
            var end = line.get(i + 1).subtract(Vec3.atLowerCornerOf(entity.getBlockPos()));
            
            var startPos = new Vector3f((float) start.x, (float) start.y, (float) start.z);
            var endPos = new Vector3f((float) end.x, (float) end.y, (float) end.z);
            
            var camPos = Minecraft.getInstance().cameraEntity.position();
            var camDist = camPos.subtract(line.get(i)).length();
            RenderSystem.lineWidth((float) (40 / Math.sqrt(camDist)));
            
            displayLine(matrices, light, overlay, startPos, endPos, lineConsumer, 1);
        }
        
    }
    
    private static void displayLine(PoseStack matrices, int light, int overlay, Vector3f startPos, Vector3f endPos, VertexConsumer lineConsumer, float alpha) {
        
        matrices.pushPose();
        var cross = new Vector3f(endPos).sub(startPos).normalize().cross(0, 1, 0);
        var scaledAlpha = (int) (alpha * 255);
        
        lineConsumer.addVertex(matrices.last().pose(), startPos.x, startPos.y, startPos.z)
          .setColor(188, 22, 196, scaledAlpha)
          .setLight(light)
          .setOverlay(overlay)
          .setNormal(0, 1, 0);
        lineConsumer.addVertex(matrices.last().pose(), endPos.x, endPos.y, endPos.z)
          .setColor(188, 22, 196, scaledAlpha)
          .setLight(light)
          .setOverlay(overlay)
          .setNormal(1, 0, 0);
        
        // render a second one at right angle to first one
        lineConsumer.addVertex(matrices.last().pose(), startPos.x, startPos.y, startPos.z)
          .setColor(188, 22, 196, scaledAlpha)
          .setLight(light)
          .setOverlay(overlay)
          .setNormal(cross.x, cross.y, cross.z);
        lineConsumer.addVertex(matrices.last().pose(), endPos.x, endPos.y, endPos.z)
          .setColor(188, 22, 196, scaledAlpha)
          .setLight(light)
          .setOverlay(overlay)
          .setNormal(cross.x, cross.y, cross.z);
        matrices.popPose();
    }
}
