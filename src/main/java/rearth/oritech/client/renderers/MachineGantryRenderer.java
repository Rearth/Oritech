package rearth.oritech.client.renderers;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import rearth.oritech.block.base.block.FrameInteractionBlock;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.block.entity.machines.interaction.DestroyerBlockEntity;
import rearth.oritech.init.BlockContent;

public class MachineGantryRenderer implements BlockEntityRenderer<FrameInteractionBlockEntity> {
    
    private static final BlockState renderedBeam = BlockContent.FRAME_GANTRY_ARM.getDefaultState();
    private static final float BEAM_DEPTH = 3 / 16f;
    private static final Random renderRandom = Random.create(100);
    
    @Override
    public void render(FrameInteractionBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        var state = entity.getCachedState();
        if (!state.get(FrameInteractionBlock.HAS_FRAME) || entity.getAreaMin() == null || entity.getLastTarget() == null)
            return;
        
        
        var renderedPosition = Vec3d.of(entity.getCurrentTarget());
        var movingOffset = new Vec3d(0, 0, 0);
        var time = entity.getWorld().getTime() + tickDelta;
        var passedTime = time - entity.getMoveStartedAt();
        var random = entity.getWorld().random;
        
        // check if currently moving, otherwise we can skip those calculations
        if (time < entity.getMoveStartedAt() + entity.getMoveTime() && entity.getMoveStartedAt() > 1) {
            var movementDoneAmount = passedTime / (float) entity.getMoveTime();
            var offset = Vec3d.of(entity.getCurrentTarget().subtract(entity.getLastTarget())).multiply(movementDoneAmount);
            renderedPosition = Vec3d.of(entity.getLastTarget()).add(offset);
        } else {
            // apply slight movement while working
            var offsetY = renderRandom.nextFloat() * 0.009 - 0.004;
            movingOffset = new Vec3d(0, offsetY, 0);
        }
        var targetOffset = renderedPosition.subtract(Vec3d.of(entity.getPos())).add(movingOffset);
        
        matrices.push();
        matrices.translate(targetOffset.getX(), targetOffset.getY(), targetOffset.getZ());
        
        var pos = entity.getCurrentTarget(); // relevant for correct lighting, actual rendered position is determined by matrix
        
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
          entity.getMachineHead(),
          pos,
          entity.getWorld(),
          matrices,
          vertexConsumers.getBuffer(RenderLayers.getBlockLayer(entity.getMachineHead())),
          true,
          random);
        
        matrices.pop();
        
        matrices.push();
        
        var length = entity.getAreaMax().getX() - entity.getAreaMin().getX() + 2 - BEAM_DEPTH * 2f;
        var target = new Vec3d(entity.getAreaMin().getX() - 0.5 + BEAM_DEPTH, renderedPosition.y, renderedPosition.z).subtract(Vec3d.of(entity.getPos()));
        
        matrices.translate(target.getX(), target.getY(), target.getZ());
        matrices.scale(length, 1, 1);
        
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
          renderedBeam,
          pos,
          entity.getWorld(),
          matrices,
          vertexConsumers.getBuffer(RenderLayers.getBlockLayer(renderedBeam)),
          true,
          random);
        
        matrices.pop();
        
        var renderedItem = entity.getToolheadAdditionalRender();
        if (renderedItem != null) {
            matrices.push();
            matrices.translate(targetOffset.getX() + 0.4, targetOffset.getY(), targetOffset.getZ() + 0.4);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30));
            // matrices.scale(0.3f, 0.3f, 0.3f);
            
            MinecraftClient.getInstance().getItemRenderer().renderItem(
              renderedItem,
              ModelTransformationMode.FIRST_PERSON_RIGHT_HAND,
              light,
              OverlayTexture.DEFAULT_UV,
              matrices,
              vertexConsumers,
              entity.getWorld(),
              0
            );
            
            matrices.pop();
        }
        
        if (entity instanceof DestroyerBlockEntity destroyerBlock && destroyerBlock.range > 1) {
            
            var beamHeight = pos.getY() - destroyerBlock.quarryTarget.getY() - 1.3f;
            
            var beamInner = BlockContent.QUARRY_BEAM_INNER.getDefaultState();
            var beamFrame = BlockContent.QUARRY_BEAM_TARGET.getDefaultState();
            var beamRing = BlockContent.QUARRY_BEAM_RING.getDefaultState();
            
            var offset = targetOffset.add(0, -1, 0);
            
            matrices.push();
            matrices.translate(offset.getX(), offset.getY() - beamHeight + 1, offset.getZ());
            matrices.scale(1, beamHeight, 1);
            
            // outer beam
            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
              beamFrame,
              pos,
              entity.getWorld(),
              matrices,
              vertexConsumers.getBuffer(RenderLayers.getBlockLayer(beamFrame)),
              true,
              random);
            
            matrices.translate(0.5, 0, 0.5);
            var rotation = new Quaternionf(new AxisAngle4f((entity.getWorld().getTime() / 3f) % 360, 0, 1, 0));
            matrices.multiply(rotation);
            matrices.translate(-0.5, 0, -0.5);
            
            // inner beam
            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
              beamInner,
              pos,
              entity.getWorld(),
              matrices,
              vertexConsumers.getBuffer(RenderLayers.getBlockLayer(beamInner)),
              true,
              random);
            
            matrices.pop();
            
            // beam ring
            matrices.push();
            var ringHeight = Math.sin((entity.getWorld().getTime() + tickDelta) / 4f);
            var heightOffset = beamHeight * 0.5 * ringHeight + beamHeight * 0.5;
            matrices.translate(offset.getX(), offset.getY() - heightOffset + 1, offset.getZ());
            
            // outer beam
            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
              beamRing,
              pos,
              entity.getWorld(),
              matrices,
              vertexConsumers.getBuffer(RenderLayers.getBlockLayer(beamRing)),
              true,
              random);
            matrices.pop();
        }
    }
}
