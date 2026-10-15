package rearth.oritech.client.ui.render;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import rearth.oritech.client.renderers.util.RenderHelpers;

/**
 * Renders block models and block entities into the GUI picture-in-picture target.
 */
public class BlockPreviewPipRenderer extends PictureInPictureRenderer<BlockPreviewRenderState> {
    private BlockPreviewRenderState cachedState;
    private Object cachedModel;
    private Object resolvedModels;
    private final Map<BlockState, PreviewBlockModel> models = new HashMap<>();

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    public BlockPreviewPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<BlockPreviewRenderState> getRenderStateClass() {
        return BlockPreviewRenderState.class;
    }

    @Override
    protected boolean textureIsReadyToBlit(BlockPreviewRenderState state) {
        var previous = cachedState;
        return state.cacheTexture() && previous != null
                && state.blocks().equals(previous.blocks())
                && state.rotationX() == previous.rotationX() && state.rotationY() == previous.rotationY()
                && state.centerX() == previous.centerX() && state.centerY() == previous.centerY()
                && state.centerZ() == previous.centerZ() && state.scale() == previous.scale()
                && cachedModel == Minecraft.getInstance().getModelManager().getBlockModelSet();
    }

    @Override
    protected void renderToTexture(BlockPreviewRenderState state, PoseStack poseStack) {
        var minecraft = Minecraft.getInstance();
        var featureDispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
        var submitNodes = featureDispatcher.getSubmitNodeStorage();
        var modelSet = minecraft.getModelManager().getBlockModelSet();
        if (resolvedModels != modelSet) {
            models.clear();
            resolvedModels = modelSet;
        }

        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);

        // GUI coordinates point down and the PIP base transform flips depth.
        poseStack.mulPose(BlockPreviewRenderState.modelRotation(state.rotationX(), state.rotationY()));
        poseStack.translate(-state.centerX(), -state.centerY(), -state.centerZ());

        for (var entry : state.blocks()) {
            poseStack.pushPose();
            // Block models occupy [0, 1]; offsets describe block centers.
            poseStack.translate(
                    entry.offset().getX() - 0.5f,
                    entry.offset().getY() - 0.5f,
                    entry.offset().getZ() - 0.5f
            );
            if (entry.scale() != 1f) {
                poseStack.translate(0.5f, 0.5f, 0.5f);
                poseStack.scale(entry.scale(), entry.scale(), entry.scale());
                poseStack.translate(-0.5f, -0.5f, -0.5f);
            }

            if (entry.state().getRenderShape() != RenderShape.INVISIBLE) {
                var modelState = models.computeIfAbsent(entry.state(), block -> {
                    var model = new PreviewBlockModel();
                    minecraft.getBlockModelResolver().update(model, block, DISPLAY_CONTEXT);
                    var worldModel = minecraft.getModelManager().getBlockStateModelSet().get(block);
                    // Athena's CTM loader lives in the world model; the GUI fallback only uses its particle texture.
                    if (worldModel.getClass().getPackageName().startsWith("earth.terrarium.athena.")) {
                        var parts = model.setupUnlitModel(worldModel.hasMaterialFlag(BlockAndTintGetter.EMPTY, BlockPos.ZERO, block, BakedQuad.FLAG_TRANSLUCENT));
                        worldModel.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, block, RandomSource.create(42), parts);
                    }
                    return model;
                });
                modelState.submit(poseStack, submitNodes, RenderHelpers.FULL_BRIGHT, entry.overlayCoords(), 0);
            }

            if (entry.entity() != null) {
                var blockEntityState = minecraft.getBlockEntityRenderDispatcher()
                        .tryExtractRenderState(entry.entity(), state.partialTick(), null, null);
                if (blockEntityState != null) {
                    blockEntityState.lightCoords = RenderHelpers.FULL_BRIGHT;
                    if (blockEntityState instanceof GeoRenderState geo)
                        geo.addGeckolibData(DataTickets.PACKED_LIGHT, RenderHelpers.FULL_BRIGHT);
                    var cameraState = minecraft.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
                    minecraft.getBlockEntityRenderDispatcher()
                            .submit(blockEntityState, poseStack, submitNodes, cameraState);
                }
            }
            poseStack.popPose();
        }

        featureDispatcher.renderAllFeatures();
        cachedState = state.cacheTexture() ? state : null;
        cachedModel = minecraft.getModelManager().getBlockModelSet();
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
