package rearth.oritech.client.renderers;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class UnstableContainerRenderer extends MachineRenderer<UnstableContainerBlockEntity> {
    
    public UnstableContainerRenderer(String modelPath) {
        super(modelPath);
    }
    
    @Override
    public void postRender(MatrixStack poseStack, UnstableContainerBlockEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        
        var time = animatable.getWorld().getTime();
        var totalTime = time + partialTick;
        
        var rotationY = (totalTime * 10) % 360;
        
        if (animatable.capturedBlock == null) return;
        
        poseStack.push();
        poseStack.scale(0.6f, 0.6f, 0.6f);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
        poseStack.translate(-0.5, 0, -0.5);
        
        
        var renderManager = MinecraftClient.getInstance().getBlockRenderManager();
        var renderedModel = renderManager.getModel(animatable.capturedBlock);
        
        renderManager.getModelRenderer().render(
          animatable.getWorld(),
          renderedModel,
          Blocks.GLOWSTONE.getDefaultState(),
          animatable.getPos(),
          poseStack,
          bufferSource.getBuffer(RenderLayers.getBlockLayer(animatable.capturedBlock)),
          true,
          animatable.getWorld().random,
          animatable.capturedBlock.getRenderingSeed(animatable.getPos()),
          LightmapTextureManager.MAX_LIGHT_COORDINATE
        );
        
        poseStack.pop();
    }
}
