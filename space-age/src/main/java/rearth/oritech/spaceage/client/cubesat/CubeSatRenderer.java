package rearth.oritech.spaceage.client.cubesat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.oritech.spaceage.block.cubesat.CubeSatBlockEntity;
import rearth.oritech.spaceage.simulation.rocket.CubeSatData;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws a miniature of the contained rocket inside the cube-sat's glass frame.
 */
public class CubeSatRenderer implements BlockEntityRenderer<CubeSatBlockEntity, CubeSatRenderer.State> {

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    // Share of the frame used by the miniature.
    private static final float INNER_SIZE = 0.75f;
    // Renderer instances are recreated on resource reload, so cached models never outlive their resources.
    private final Map<Long, Miniature> miniatures = new LinkedHashMap<>();

    public CubeSatRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static Miniature miniature(CubeSatData data) {

        var size = data.size();
        var scale = INNER_SIZE / Math.max(1, Math.max(size.getX(), Math.max(size.getY(), size.getZ())));
        var offset = new Vec3((1 - size.getX() * scale) / 2, (1 - size.getY() * scale) / 2, (1 - size.getZ() * scale) / 2);
        var resolver = Minecraft.getInstance().getBlockModelResolver();
        var models = new HashMap<BlockState, BlockModelRenderState>();
        var cells = new LinkedHashMap<BlockPos, Cell>();
        for (var part : data.parts()) {
            for (var block : part.structure().blocks())
                cells.put(block.relativePos(), new Cell(block.relativePos(), model(models, resolver, block.state())));
            part.structure().originalCouplings().values().forEach(couplings -> couplings.forEach(coupling ->
                    cells.put(coupling.relativePos(), new Cell(coupling.relativePos(), model(models, resolver, coupling.state())))));
        }
        return new Miniature(List.copyOf(cells.values()), scale, offset);
    }

    private static BlockModelRenderState model(HashMap<BlockState, BlockModelRenderState> models,
                                               BlockModelResolver resolver, BlockState state) {

        return models.computeIfAbsent(state, ignored -> {
            var result = new BlockModelRenderState();
            resolver.update(result, state, DISPLAY_CONTEXT);
            return result;
        });
    }

    @Override
    public State createRenderState() {

        return new State();
    }

    @Override
    public void extractRenderState(CubeSatBlockEntity entity, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {

        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        var data = entity.data();
        if (data == null) {
            state.cells = List.of();
            return;
        }
        var miniature = miniatures.get(data.fingerprint());
        if (miniature == null) {
            miniature = miniature(data);
            if (miniatures.size() >= 64) miniatures.remove(miniatures.keySet().iterator().next());
            miniatures.put(data.fingerprint(), miniature);
        }
        state.cells = miniature.cells();
        state.scale = miniature.scale();
        state.offset = miniature.offset();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {

        if (state.cells.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(state.offset.x, state.offset.y, state.offset.z);
        poseStack.scale(state.scale, state.scale, state.scale);
        for (var cell : state.cells) {
            poseStack.pushPose();
            poseStack.translate(cell.pos().getX(), cell.pos().getY(), cell.pos().getZ());
            cell.model().submitMultiLayer(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private record Miniature(List<Cell> cells, float scale, Vec3 offset) {

    }

    private record Cell(BlockPos pos, BlockModelRenderState model) {

    }

    public static class State extends BlockEntityRenderState {

        private List<Cell> cells = List.of();
        private float scale;
        private Vec3 offset = Vec3.ZERO;
    }
}
