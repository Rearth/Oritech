package rearth.oritech.client.renderers;

import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class ItemPipeTransferRenderer implements BlockEntityRenderer<ItemPipeInterfaceEntity> {
    
    @Override
    public void render(ItemPipeInterfaceEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        
        if (entity.activeStacks == null || entity.activeStacks.isEmpty()) return;
        
        var time = entity.getLevel().getGameTime() + tickDelta;
        var removedStacks = new HashSet<ItemPipeInterfaceEntity.RenderStackData>();
        
        for (var renderedStack : entity.activeStacks) {
            var age = time - renderedStack.startedAt();
            var neededTime = ItemPipeInterfaceEntity.calculatePathLength(renderedStack.pathLength());    // about 2 blocks/s, but much faster on longer paths
            var progress = age / neededTime;
            if (progress > 1) {
                removedStacks.add(renderedStack);
                continue;
            }
            
            progress = sigmoidFitted(progress);
            
            // get position in path at current progress (traverse path to current progress)
            var targetPathProgress = renderedStack.pathLength() * progress;
            var pathProgress = 0;
            var pathPosition = renderedStack.path().getFirst();
            Vec3 targetPos = Vec3.ZERO;
            
            for (var segment : renderedStack.path()) {
                var segmentDist = segment.distManhattan(pathPosition);
                
                if (pathProgress + segmentDist < targetPathProgress) {
                    pathProgress += segmentDist;
                    pathPosition = segment;
                } else {    // reaching or overshooting target
                    var remainingDist = targetPathProgress - pathProgress;
                    var targetOffset = Vec3.atLowerCornerOf(segment.subtract(pathPosition)).normalize().scale(remainingDist);
                    targetPos = Vec3.atLowerCornerOf(pathPosition).add(targetOffset);
                    break;
                }
                
            }
            
            var offset = targetPos.subtract(Vec3.atLowerCornerOf(entity.getBlockPos()));
            
            matrices.pushPose();
            matrices.translate(offset.x + 0.5, offset.y + 0.5, offset.z + 0.5);
            matrices.scale(0.4f, 0.4f, 0.4f);
            matrices.mulPose(Axis.YP.rotationDegrees(-140));
            matrices.mulPose(Axis.XP.rotationDegrees(-30));
            
            var renderedItem = renderedStack.rendered();
            
            Minecraft.getInstance().getItemRenderer().renderStatic(
              renderedItem,
              ItemDisplayContext.GUI,
              light,
              OverlayTexture.NO_OVERLAY,
              matrices,
              vertexConsumers,
              entity.getLevel(),
              0
            );
            
            matrices.popPose();
            
        }
        
        entity.activeStacks.removeAll(removedStacks);
        
    }
    
    private static double sigmoidFitted(double x) {
        return sigmoid((x - 0.5) * 2) + 0.5f;
    }
    
    private static double sigmoid(double x) {
        return x / (1 + Math.abs(x));
    }
}
