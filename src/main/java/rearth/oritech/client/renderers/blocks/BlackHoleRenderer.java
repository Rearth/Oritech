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
        state.gameTime = time + partialTicks;

        var modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();

        if (blockEntity.currentlyPullingFrom != null && blockEntity.currentlyPulling != null
                && blockEntity.pullingStartedAt + blockEntity.pullTime > time && !blockEntity.currentlyPulling.isAir()) {

            var progress = (float) Math.pow((time + partialTicks - blockEntity.pullingStartedAt) / (float) blockEntity.pullTime, 1.3f);
            var startPos = Vec3.atLowerCornerOf(blockEntity.currentlyPullingFrom);
            var endPos = blockEntity.getBlockPos().getCenter();

            var pullOffset = endPos.subtract(startPos).scale(1 - progress);
            var pullRotationY = progress * blockEntity.pullTime * 3;
            var pullScale = 1 - progress;

            // matrix for pulled block offset
            var tempStack = new PoseStack();
            tempStack.translate(0.5, 0.5, 0.5);
            tempStack.mulPose(Axis.YP.rotationDegrees(pullRotationY));
            tempStack.translate(-pullOffset.x, -pullOffset.y, -pullOffset.z);
            tempStack.mulPose(Axis.XP.rotationDegrees(pullRotationY));
            tempStack.mulPose(Axis.ZP.rotationDegrees(pullRotationY));
            tempStack.scale(pullScale, pullScale, pullScale);

            state.pullMatrix = new Matrix4f(tempStack.last().pose());
            blockModelResolver.update(state.pulledState, blockEntity.currentlyPulling, BLOCK_DISPLAY_CONTEXT);
        } else {
            state.pullMatrix = null;
        }

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
        if (state.pullMatrix != null && !state.pulledState.isEmpty()) {
            poseStack.pushPose();

            poseStack.mulPose(state.pullMatrix);

            state.pulledState.submitMultiLayer(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

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
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), state.innerParts, new int[0], RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), state.middleParts, new int[0], RenderHelpers.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();


        // outer hold has extra rotations
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);

        // Recalculate rotationY at the 1.1f speed factor from your original code
        rotationY = (timeDelta * 1.1f) % 360;

        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) rotationX));
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

        public @Nullable Matrix4f pullMatrix = null;
        public BlockModelRenderState pulledState = new BlockModelRenderState();

        public List<BlockStateModelPart> innerParts = new ArrayList<>();
        public List<BlockStateModelPart> middleParts = new ArrayList<>();
        public List<BlockStateModelPart> outerParts = new ArrayList<>();
    }
}
