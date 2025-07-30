package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import rearth.oritech.block.base.block.FrameInteractionBlock;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.block.entity.interaction.DestroyerBlockEntity;
import rearth.oritech.init.BlockContent;

public class MachineGantryRenderer implements BlockEntityRenderer<FrameInteractionBlockEntity> {
    
    private static final BlockState renderedBeam = BlockContent.FRAME_GANTRY_ARM.defaultBlockState();
    private static final float BEAM_DEPTH = 3 / 16f;
    private static final RandomSource renderRandom = RandomSource.create(100);
    
    @Override
    public int getViewDistance() {
        return 128;
    }
    
    @Override
    public boolean shouldRenderOffScreen(FrameInteractionBlockEntity blockEntity) {
        return true;
    }
    
    @Override
    public void render(FrameInteractionBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        
        var state = entity.getBlockState();
        if (!state.getValue(FrameInteractionBlock.HAS_FRAME) || entity.getAreaMin() == null || entity.getLastTarget() == null)
            return;
        
        var currentTarget = entity.getCurrentTarget();
        var renderedPosition = Vec3.atLowerCornerOf(currentTarget);
        
        var movingOffset = new Vec3(0, 0, 0);
        var random = entity.getLevel().random;
        
        if (entity.isMoving()) {
            var lastPosition = Vec3.atLowerCornerOf(entity.getLastTarget());
            var progress = entity.getCurrentProgress() / entity.getMoveTime();
            var offset = renderedPosition.subtract(lastPosition);
            renderedPosition = lastPosition.add(offset.scale(progress));
        } else {
            // apply slight shaking while working
            var offsetY = renderRandom.nextFloat() * 0.012 - 0.004;
            movingOffset = new Vec3(0, offsetY, 0);
        }
        
        
        renderedPosition = LaserArmRenderer.lerp(entity.lastRenderedPosition, renderedPosition, 0.04f);
        entity.lastRenderedPosition = renderedPosition;
        var targetOffset = renderedPosition.subtract(Vec3.atLowerCornerOf(entity.getBlockPos())).add(movingOffset);
        
        matrices.pushPose();
        matrices.translate(targetOffset.x(), targetOffset.y(), targetOffset.z());
        
        var pos = entity.getCurrentTarget(); // relevant for correct lighting, actual rendered position is determined by matrix
        
        Minecraft.getInstance().getBlockRenderer().renderBatched(
          entity.getMachineHead(),
          pos,
          entity.getLevel(),
          matrices,
          vertexConsumers.getBuffer(ItemBlockRenderTypes.getChunkRenderType(entity.getMachineHead())),
          true,
          random);
        
        matrices.popPose();
        
        matrices.pushPose();
        
        var length = entity.getAreaMax().getX() - entity.getAreaMin().getX() + 2 - BEAM_DEPTH * 2f;
        var target = new Vec3(entity.getAreaMin().getX() - 0.5 + BEAM_DEPTH, renderedPosition.y, renderedPosition.z).subtract(Vec3.atLowerCornerOf(entity.getBlockPos()));
        
        matrices.translate(target.x(), target.y(), target.z());
        matrices.scale(length, 1, 1);
        
        Minecraft.getInstance().getBlockRenderer().renderBatched(
          renderedBeam,
          pos,
          entity.getLevel(),
          matrices,
          vertexConsumers.getBuffer(RenderType.cutout()),
          true,
          random);
        
        matrices.popPose();
        
        var renderedItem = entity.getToolheadAdditionalRender();
        if (renderedItem != null) {
            matrices.pushPose();
            matrices.translate(targetOffset.x() + 0.4, targetOffset.y(), targetOffset.z() + 0.4);
            matrices.mulPose(Axis.YP.rotationDegrees(30));
            // matrices.scale(0.3f, 0.3f, 0.3f);
            
            Minecraft.getInstance().getItemRenderer().renderStatic(
              renderedItem,
              ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
              light,
              OverlayTexture.NO_OVERLAY,
              matrices,
              vertexConsumers,
              entity.getLevel(),
              0
            );
            
            matrices.popPose();
        }
        
        if (entity instanceof DestroyerBlockEntity destroyerBlock && destroyerBlock.range > 1) {
            
            var beamHeight = pos.getY() - destroyerBlock.quarryTarget.getY() - 1.3f;
            
            var beamInner = BlockContent.QUARRY_BEAM_INNER.defaultBlockState();
            var beamFrame = BlockContent.QUARRY_BEAM_TARGET.defaultBlockState();
            var beamRing = BlockContent.QUARRY_BEAM_RING.defaultBlockState();
            
            var offset = targetOffset.add(0, -1, 0);
            
            matrices.pushPose();
            matrices.translate(offset.x(), offset.y() - beamHeight + 1, offset.z());
            matrices.scale(1, beamHeight, 1);
            
            // outer beam
//            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
//              beamFrame,
//              pos,
//              entity.getWorld(),
//              matrices,
//              vertexConsumers.getBuffer(RenderLayers.getBlockLayer(beamFrame)),
//              true,
//              random);
            
            matrices.translate(0.5, 0, 0.5);
            var rotation = new Quaternionf(new AxisAngle4f((entity.getLevel().getGameTime() / 3f) % 360, 0, 1, 0));
            matrices.mulPose(rotation);
            matrices.translate(-0.5, 0, -0.5);
            
            // inner beam
            Minecraft.getInstance().getBlockRenderer().renderBatched(
              beamInner,
              pos,
              entity.getLevel(),
              matrices,
              vertexConsumers.getBuffer(ItemBlockRenderTypes.getChunkRenderType(beamInner)),
              true,
              random);
            
            matrices.popPose();
            
            // beam ring
            matrices.pushPose();
            var ringHeight = Math.sin((entity.getLevel().getGameTime() + tickDelta) / 4f);
            var heightOffset = beamHeight * 0.5 * ringHeight + beamHeight * 0.5;
            matrices.translate(offset.x(), offset.y() - heightOffset + 1, offset.z());
            
            // outer beam
            Minecraft.getInstance().getBlockRenderer().renderBatched(
              beamRing,
              pos,
              entity.getLevel(),
              matrices,
              vertexConsumers.getBuffer(ItemBlockRenderTypes.getChunkRenderType(beamRing)),
              true,
              random);
            matrices.popPose();
        }
    }
}
