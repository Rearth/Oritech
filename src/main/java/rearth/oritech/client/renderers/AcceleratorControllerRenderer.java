package rearth.oritech.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.client.init.ParticleContent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static rearth.oritech.client.renderers.LaserArmRenderer.CUSTOM_LINES;

public class AcceleratorControllerRenderer implements BlockEntityRenderer<AcceleratorControllerBlockEntity> {
    
    private record RenderedLine(float startedAt, List<Vec3d> positions) {
    }
    
    private final Map<AcceleratorControllerBlockEntity, RenderedLine> activeLines = new HashMap<>();
    
    @Override
    public int getRenderDistance() {
        return 128;
    }
    
    @Override
    public boolean rendersOutsideBoundingBox(AcceleratorControllerBlockEntity blockEntity) {
        return true;
    }
    
    @Override
    public void render(AcceleratorControllerBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        if (entity.displayTrail == null) {
            activeLines.remove(entity);
            return;
        }
        
        var lineConsumer = vertexConsumers.getBuffer(CUSTOM_LINES);
        var time = entity.getWorld().getTime() + tickDelta;
        
        // try adding new tail to lines
        var displayTrail = entity.displayTrail;
        if (!activeLines.containsKey(entity) || !activeLines.get(entity).positions.equals(displayTrail)) {
            activeLines.put(entity, new RenderedLine(time, displayTrail));
            ParticleContent.PARTICLE_MOVING.spawn(entity.getWorld(), displayTrail.getLast());
        }
        
        var activeLine = activeLines.get(entity);
        var line = activeLine.positions;
        var age = time - activeLine.startedAt;
        if (age >= 60) {
            if (entity.displayTrail.equals(activeLine.positions)) entity.displayTrail = null;
        }
        
        
        for (int i = 0; i < line.size() - 1; i++) {
            var start = line.get(i).subtract(Vec3d.of(entity.getPos()));
            var end = line.get(i + 1).subtract(Vec3d.of(entity.getPos()));
            
            var startPos = new Vector3f((float) start.x, (float) start.y, (float) start.z);
            var endPos = new Vector3f((float) end.x, (float) end.y, (float) end.z);
            
            var camPos = MinecraftClient.getInstance().cameraEntity.getPos();
            var camDist = camPos.subtract(line.get(i)).length();
            RenderSystem.lineWidth((float) (40 / Math.sqrt(camDist)));
            
            displayLine(matrices, light, overlay, startPos, endPos, lineConsumer, 1);
        }
        
    }
    
    private static void displayLine(MatrixStack matrices, int light, int overlay, Vector3f startPos, Vector3f endPos, VertexConsumer lineConsumer, float alpha) {
        
        matrices.push();
        var cross = new Vector3f(endPos).sub(startPos).normalize().cross(0, 1, 0);
        var scaledAlpha = (int) (alpha * 255);
        
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), startPos.x, startPos.y, startPos.z)
          .color(188, 22, 196, scaledAlpha)
          .light(light)
          .overlay(overlay)
          .normal(0, 1, 0);
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), endPos.x, endPos.y, endPos.z)
          .color(188, 22, 196, scaledAlpha)
          .light(light)
          .overlay(overlay)
          .normal(1, 0, 0);
        
        // render a second one at right angle to first one
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), startPos.x, startPos.y, startPos.z)
          .color(188, 22, 196, scaledAlpha)
          .light(light)
          .overlay(overlay)
          .normal(cross.x, cross.y, cross.z);
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), endPos.x, endPos.y, endPos.z)
          .color(188, 22, 196, scaledAlpha)
          .light(light)
          .overlay(overlay)
          .normal(cross.x, cross.y, cross.z);
        matrices.pop();
    }
}
