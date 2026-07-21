package rearth.oritech.client.renderers;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.FrameGraphSetupEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.util.RenderHelpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles block outline highlights for the ore-scanner augment.
 */
public class OreFinderRenderer {

    // set by scanner augment
    public static List<BlockPos> renderedBlocks;
    public static long receivedAt;

    // render data context key
    public static final ContextKey<OreRenderData> ORE_DATA = new ContextKey<>(Oritech.id("ore_finder"));

    // teal outline color (with alpha, directs geometry to outline buffer)
    private static final int OUTLINE_COLOR = 0xFF8AF2DF;
    // Preserve the normal block shader, but only draw model fragments hidden
    // behind existing world geometry. Based on Just Dire Things' 26.1 ore X-ray.
    private static final RenderPipeline ORE_XRAY_PIPELINE = RenderPipelines.SOLID_BLOCK.toBuilder()
            .withLocation(Oritech.id("pipeline/ore_xray"))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
            .build();
    private static final RenderType ORE_XRAY = RenderType.create(
            "oritech_ore_finder_fill",
            RenderSetup.builder(ORE_XRAY_PIPELINE)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());
    // empty biome/block tinting array
    private static final int[] NO_TINT = new int[0];

    public record OreRenderData(List<BlockPos> blocks) {}

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ORE_XRAY_PIPELINE);
    }

    public static void onFrameGraphSetup(FrameGraphSetupEvent event) {
        if (hasActiveHighlights()) {
            event.enableOutlineProcessing();
        }
    }

    // extract highlighted positions and camera offset
    public static void onExtractRenderState(ExtractLevelRenderStateEvent event) {
        if (!hasActiveHighlights()) return;

        var data = new OreRenderData(new ArrayList<>(renderedBlocks));
        event.getRenderState().setRenderData(ORE_DATA, data);
    }

    // submit highlighted block models to renderer with see-through glow
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        var data = event.getLevelRenderState().getRenderData(ORE_DATA);
        if (data == null) return;

        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        var random = level.getRandom();
        var poseStack = event.getPoseStack();
        var collector = event.getSubmitNodeCollector();
        var camPos = event.getLevelRenderState().cameraRenderState.pos;

        for (var pos : data.blocks()) {
            var state = level.getBlockState(pos);
            if (state.isAir()) continue;

            var model = modelSet.get(state);
            var parts = new ArrayList<BlockStateModelPart>();
            random.setSeed(42L);
            model.collectParts(random, parts);
            if (parts.isEmpty()) continue;

            poseStack.pushPose();
            // align coordinates relative to camera
            poseStack.translate(pos.getX() - camPos.x - 0.01, pos.getY() - camPos.y - 0.01, pos.getZ() - camPos.z - 0.01);

            // Draw the occluded ore surfaces with the normal block shader, then
            // add the outline in a separate target for a readable silhouette.
            collector.submitBlockModel(poseStack, ORE_XRAY, parts, NO_TINT,
                    RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            collector.submitBlockModel(poseStack, RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS), parts, NO_TINT,
                    RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, OUTLINE_COLOR);

            poseStack.popPose();
        }

    }

    private static boolean hasActiveHighlights() {
        var level = Minecraft.getInstance().level;
        return level != null
                && renderedBlocks != null
                && !renderedBlocks.isEmpty()
                && level.getGameTime() - receivedAt <= 15;
    }
}
