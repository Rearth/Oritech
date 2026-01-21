package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.renderers.util.BeamRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
        
        var bePos = Vec3.atLowerCornerOf(entity.getBlockPos());
        var baseThickness = 0.07f;
        var beamConsumer = vertexConsumers.getBuffer(RenderType.eyes(LaserArmRenderer.BEAM_TEXTURE));
        
        for (int i = 0; i < line.size() - 1; i++) {
            var pointCurrent = line.get(i);
            var pointNext = line.get(i + 1);
            
            // to local space
            var startLocal = pointCurrent.subtract(bePos);
            var endLocal = pointNext.subtract(bePos);
            
            Vec3 delta = endLocal.subtract(startLocal);
            
            BeamRenderer.renderStraightBeam(
              matrices, beamConsumer, startLocal, delta,
              baseThickness * 0.3f,
              LightTexture.FULL_BRIGHT,
              LaserArmRenderer.CORE_COLOR_START,
              LaserArmRenderer.CORE_COLOR_END
            );
            
            // render glow
            BeamRenderer.renderStraightBeam(
              matrices, beamConsumer, startLocal, delta,
              baseThickness,
              LightTexture.FULL_BRIGHT,
              LaserArmRenderer.GLOW_COLOR_START,
              LaserArmRenderer.GLOW_COLOR_END
            );
        }
        
    }
}
