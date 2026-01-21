package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class UnstableContainerRenderer extends MachineRenderer<UnstableContainerBlockEntity> {
    
    public UnstableContainerRenderer(String modelPath) {
        super(modelPath);
    }
    
    @Override
    public void postRender(PoseStack poseStack, UnstableContainerBlockEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        
        var time = animatable.getLevel().getGameTime();
        var totalTime = time + partialTick;
        
        var rotationY = (totalTime * 10) % 360;
        
        if (animatable.capturedBlock == null) return;
        
        poseStack.pushPose();
        poseStack.scale(0.6f, 0.6f, 0.6f);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        poseStack.translate(-0.5, 0, -0.5);
        
        
        var renderManager = Minecraft.getInstance().getBlockRenderer();
        var renderedModel = renderManager.getBlockModel(animatable.capturedBlock);
        
        renderManager.getModelRenderer().tesselateBlock(
          animatable.getLevel(),
          renderedModel,
          Blocks.GLOWSTONE.defaultBlockState(),
          animatable.getBlockPos(),
          poseStack,
          bufferSource.getBuffer(ItemBlockRenderTypes.getChunkRenderType(animatable.capturedBlock)),
          true,
          animatable.getLevel().random,
          animatable.capturedBlock.getSeed(animatable.getBlockPos()),
          LightTexture.FULL_BRIGHT
        );
        
        poseStack.popPose();
    }
}
