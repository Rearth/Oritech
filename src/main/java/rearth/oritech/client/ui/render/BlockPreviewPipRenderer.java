package rearth.oritech.client.ui.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Stubbed for the 26.1 migration. The previous implementation relied on
 * {@code Minecraft.getInstance().getBlockRenderer().renderSingleBlock(...)} and
 * {@code LightTexture.FULL_BRIGHT}, both of which were removed when the block
 * rendering pipeline was refactored around {@code ModelBlockRenderer} +
 * {@code FeatureRenderDispatcher} / {@code SubmitNodeStorage}.
 *
 * <p>The renderer remains registered so the rest of the screen wiring keeps
 * compiling and the PIP infrastructure functions; it just draws nothing for now.
 */
public class BlockPreviewPipRenderer extends PictureInPictureRenderer<BlockPreviewRenderState> {
    
    public BlockPreviewPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }
    
    @Override
    public Class<BlockPreviewRenderState> getRenderStateClass() {
        return BlockPreviewRenderState.class;
    }
    
    @Override
    protected void renderToTexture(BlockPreviewRenderState state, PoseStack poseStack) {
        // Stub: block-in-PIP rendering needs to be rebuilt against the new 26.1
        // block rendering pipeline. See the file-level javadoc for context.
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
