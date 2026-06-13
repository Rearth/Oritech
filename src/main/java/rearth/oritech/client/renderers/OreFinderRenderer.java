package rearth.oritech.client.renderers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
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
    // empty biome/block tinting array
    private static final int[] NO_TINT = new int[0];

    public record OreRenderData(List<BlockPos> blocks) {}

    // extract highlighted positions and camera offset
    public static void onExtractRenderState(ExtractLevelRenderStateEvent event) {
        var level = Minecraft.getInstance().level;
        if (level == null || renderedBlocks == null || renderedBlocks.isEmpty()) return;

        var age = level.getGameTime() - receivedAt;
        if (age > 15) return;

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
            poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

            collector.submitBlockModel(poseStack, RenderTypes.cutoutMovingBlock(), parts, NO_TINT,
                    RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, OUTLINE_COLOR);

            poseStack.popPose();
        }

    }
}
