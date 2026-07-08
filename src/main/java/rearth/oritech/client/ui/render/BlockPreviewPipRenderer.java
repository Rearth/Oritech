package rearth.oritech.client.ui.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import rearth.oritech.client.renderers.util.RenderHelpers;

/**
 * Renders block models and block entities into the GUI picture-in-picture target.
 */
public class BlockPreviewPipRenderer extends PictureInPictureRenderer<BlockPreviewRenderState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    public BlockPreviewPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<BlockPreviewRenderState> getRenderStateClass() {
        return BlockPreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(BlockPreviewRenderState state, PoseStack poseStack) {
        var minecraft = Minecraft.getInstance();
        var featureDispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
        var submitNodes = featureDispatcher.getSubmitNodeStorage();
        var modelState = new BlockModelRenderState();

        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        // GUI coordinates point down and the PIP base transform flips depth.
        poseStack.scale(1f, -1f, -1f);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.rotationX()));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotationY()));
        poseStack.translate(-state.centerX(), -state.centerY(), -state.centerZ());

        for (var entry : state.blocks()) {
            poseStack.pushPose();
            // Block models occupy [0, 1]; offsets describe block centers.
            poseStack.translate(
                    entry.offset().getX() - 0.5f,
                    entry.offset().getY() - 0.5f,
                    entry.offset().getZ() - 0.5f
            );

            minecraft.getBlockModelResolver().update(modelState, entry.state(), DISPLAY_CONTEXT);
            modelState.submit(poseStack, submitNodes, RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);

            if (entry.entity() != null) {
                var blockEntityState = minecraft.getBlockEntityRenderDispatcher()
                        .tryExtractRenderState(entry.entity(), state.partialTick(), null, null);
                if (blockEntityState != null) {
                    var cameraState = minecraft.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
                    minecraft.getBlockEntityRenderDispatcher()
                            .submit(blockEntityState, poseStack, submitNodes, cameraState);
                }
            }
            poseStack.popPose();
        }

        featureDispatcher.renderAllFeatures();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "oritech_block_preview";
    }
}
