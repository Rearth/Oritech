package rearth.oritech.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.accelerator.BlackHoleBlockEntity;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.init.BlockContent;

import java.util.ArrayList;
import java.util.List;

public class BlackHoleRenderer implements BlockEntityRenderer<BlackHoleBlockEntity, BlackHoleRenderer.BlackHoleRenderState> {

    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final float OUTER_RING_BASE_SPEED = 1.1f;
    private final BlockModelResolver blockModelResolver;

    public BlackHoleRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public BlackHoleRenderState createRenderState() {
        return new BlackHoleRenderState();
    }

    @Override
    public void extractRenderState(BlackHoleBlockEntity blockEntity, BlackHoleRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        var level = (ClientLevel) blockEntity.getLevel();
        var time = level.getGameTime();
        var renderTime = time + (double) partialTicks;
        state.gameTime = (float) renderTime;
        state.updateOuterRotation(renderTime);

        var modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();

        state.pulledBlocks.clear();
        for (var pulledBlock : blockEntity.currentlyPulling) {
            if (pulledBlock.startedAt() + pulledBlock.pullTime() <= time || pulledBlock.state().isAir()) continue;

            var progress = (float) Math.pow((time + partialTicks - pulledBlock.startedAt()) / (float) pulledBlock.pullTime(), 1.3f);
            var startPos = Vec3.atLowerCornerOf(pulledBlock.from());
            var endPos = blockEntity.getBlockPos().getCenter();

            var pullOffset = endPos.subtract(startPos).scale(1 - progress);
            var pullRotation = progress * pulledBlock.pullTime() * 3;
            var orbitProgress = Math.clamp(progress, 0, 1);
            orbitProgress *= orbitProgress;
            var orbitRotation = orbitProgress * pulledBlock.pullTime() * 3;
            var pullScale = 1 - progress;

            // matrix for pulled block offset
            var tempStack = new PoseStack();
            tempStack.translate(0.5, 0.5, 0.5);
            tempStack.mulPose(Axis.YP.rotationDegrees(orbitRotation));
            tempStack.translate(-pullOffset.x, -pullOffset.y, -pullOffset.z);
            tempStack.mulPose(Axis.XP.rotationDegrees(pullRotation));
            tempStack.mulPose(Axis.ZP.rotationDegrees(pullRotation));
            tempStack.scale(pullScale, pullScale, pullScale);

            var pulledState = new BlockModelRenderState();
            blockModelResolver.update(pulledState, pulledBlock.state(), BLOCK_DISPLAY_CONTEXT);
            state.pulledBlocks.add(new PulledBlockRenderState(new Matrix4f(tempStack.last().pose()), pulledState));
        }

        state.growthScale = blockEntity.getGrowthScale();

        var innerState = BlockContent.BLACK_HOLE_INNER.get().defaultBlockState();
        RenderHelpers.ExtractStateModels(state.innerParts, modelSet.get(innerState), level, blockEntity.getBlockPos(), innerState);

        var middleState = BlockContent.BLACK_HOLE_MIDDLE.get().defaultBlockState();
        RenderHelpers.ExtractStateModels(state.middleParts, modelSet.get(middleState), level, blockEntity.getBlockPos(), middleState);

        var outerState = BlockContent.BLACK_HOLE_OUTER.get().defaultBlockState();
        RenderHelpers.ExtractStateModels(state.outerParts, modelSet.get(outerState), level, blockEntity.getBlockPos(), outerState);
    }

    @Override
    public void submit(BlackHoleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {

        // block being pulled in
        for (var pulledBlock : state.pulledBlocks) {
            if (pulledBlock.state().isEmpty()) continue;

            poseStack.pushPose();

            poseStack.mulPose(pulledBlock.matrix());

            pulledBlock.state().submitMultiLayer(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

            poseStack.popPose();
        }

        var timeDelta = state.gameTime;
        var rotationY = (timeDelta * 1.2f) % 360;
        var rotationX = Math.sin(timeDelta * 0.02) * 5;

        // inner / middle parts
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) rotationX));
        poseStack.scale(state.growthScale, state.growthScale, state.growthScale);
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), state.innerParts, new int[0], RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), state.middleParts, new int[0], RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();


        // outer ring has extra rotations
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);

        poseStack.mulPose(Axis.YP.rotationDegrees(state.outerRotationY));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) rotationX));
        poseStack.scale(state.growthScale, state.growthScale, state.growthScale);
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), state.outerParts, new int[0], RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static class BlackHoleRenderState extends BlockEntityRenderState {
        public float gameTime;
        public float growthScale = 1;
        public float outerRotationY;
        private double previousGameTime = Double.NaN;

        public List<PulledBlockRenderState> pulledBlocks = new ArrayList<>();

        public List<BlockStateModelPart> innerParts = new ArrayList<>();
        public List<BlockStateModelPart> middleParts = new ArrayList<>();
        public List<BlockStateModelPart> outerParts = new ArrayList<>();

        private void updateOuterRotation(double gameTime) {
            if (Double.isNaN(previousGameTime)) {
                outerRotationY = (float) ((gameTime * OUTER_RING_BASE_SPEED) % 360);
            } else {
                var elapsed = Math.max(0, gameTime - previousGameTime);
                outerRotationY = (float) ((outerRotationY + elapsed * OUTER_RING_BASE_SPEED * growthScale) % 360);
            }

            previousGameTime = gameTime;
        }
    }

    public record PulledBlockRenderState(Matrix4f matrix, BlockModelRenderState state) {
    }

    @Override
    public AABB getRenderBoundingBox(BlackHoleBlockEntity blockEntity) {
        return AABB.INFINITE;
    }
}
