package rearth.oritech.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.base.block.FrameInteractionBlock;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.block.entity.interaction.DestroyerBlockEntity;
import rearth.oritech.client.renderers.util.BeamRenderer;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.init.BlockContent;

public class MachineGantryRenderer implements BlockEntityRenderer<FrameInteractionBlockEntity, MachineGantryRenderer.GantryRenderState> {

    private static final BlockState renderedBeam = BlockContent.FRAME_GANTRY_ARM.get().defaultBlockState();
    private static final float BEAM_DEPTH = 3 / 16f;
    private static final RandomSource renderRandom = RandomSource.create(100);

    public MachineGantryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(FrameInteractionBlockEntity blockEntity) {
        return AABB.INFINITE;
    }

    @Override
    public GantryRenderState createRenderState() {
        return new GantryRenderState();
    }

    @Override
    public void extractRenderState(FrameInteractionBlockEntity entity, GantryRenderState gState, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, gState, partialTick, cameraPosition, breakProgress);

        var state = entity.getBlockState();
        if (!state.getValue(FrameInteractionBlock.HAS_FRAME) || entity.getAreaMin() == null || entity.getLastTarget() == null) {
            gState.hasArea = false;
            return;
        }
        gState.hasArea = true;

        var currentTarget = entity.getCurrentTarget();
        var renderedPosition = Vec3.atLowerCornerOf(currentTarget);
        var movingOffset = new Vec3(0, 0, 0);

        if (entity.isMoving()) {
            var lastPosition = Vec3.atLowerCornerOf(entity.getLastTarget());
            var progress = (entity.getCurrentProgress()) / entity.getMoveTime();
            progress = Math.min(progress, 1);
            var offset = renderedPosition.subtract(lastPosition);
            renderedPosition = lastPosition.add(offset.scale(progress));
        } else {
            // apply slight shaking while working
            var offsetY = renderRandom.nextFloat() * 0.012 - 0.004;
            movingOffset = new Vec3(0, offsetY, 0);
        }

        renderedPosition = EndericLaserRenderer.lerp(entity.lastRenderedPosition, renderedPosition, 0.1f);
        entity.lastRenderedPosition = renderedPosition;

        gState.targetOffset = renderedPosition.subtract(Vec3.atLowerCornerOf(entity.getBlockPos())).add(movingOffset);
        gState.machineHead = entity.getMachineHead();

        var mc = Minecraft.getInstance();
        var resolver = new BlockModelResolver(mc.getModelManager());

        // resolve the head block model state
        resolver.update(gState.headBlockState, gState.machineHead, BlockDisplayContext.create());

        gState.armLength = entity.getAreaMax().getX() - entity.getAreaMin().getX() + 2 - BEAM_DEPTH * 2f;
        gState.armTarget = new Vec3(entity.getAreaMin().getX() - 0.5 + BEAM_DEPTH, renderedPosition.y, renderedPosition.z).subtract(Vec3.atLowerCornerOf(entity.getBlockPos()));

        // resolve the arm block model state
        resolver.update(gState.armBlockState, renderedBeam, BlockDisplayContext.create());

        var renderedItem = entity.getToolheadAdditionalRender();
        if (renderedItem != null) {
            gState.hasToolhead = true;
            mc.getItemModelResolver().updateForTopItem(gState.toolheadState, renderedItem, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, entity.getLevel(), null, 0);
        } else {
            gState.hasToolhead = false;
        }

        if (entity instanceof DestroyerBlockEntity destroyer && (!destroyer.isMoving() || destroyer.range > 1) && !destroyer.quarryTarget.equals(BlockPos.ZERO)) {
            gState.isDestroyerQuarry = true;
            var pos = currentTarget;
            gState.destroyerBeamHeight = pos.getY() - destroyer.quarryTarget.getY() - 1.3f;
            gState.destroyerBeamOffset = gState.targetOffset.add(0, -1, 0);

            var baseThickness = 0.035f;
            gState.thickness = (float) (baseThickness * 2 + Math.sin((entity.getLevel().getGameTime() + partialTick) * 0.54) * 0.02f);
            gState.ringHeightSine = (float) Math.sin((entity.getLevel().getGameTime() + partialTick) / 4f);

            var beamRing = BlockContent.QUARRY_BEAM_RING.get().defaultBlockState();
            resolver.update(gState.ringBlockState, beamRing, BlockDisplayContext.create());
        } else {
            gState.isDestroyerQuarry = false;
        }
    }

    @Override
    public void submit(GantryRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        if (!state.hasArea) return;

        // 1. Submit Machine Head
        matrices.pushPose();
        matrices.translate(state.targetOffset.x(), state.targetOffset.y(), state.targetOffset.z());
        state.headBlockState.submit(matrices, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();

        // 2. Submit Gantry Arm
        matrices.pushPose();
        matrices.translate(state.armTarget.x(), state.armTarget.y(), state.armTarget.z());
        matrices.scale(state.armLength, 1, 1);
        state.armBlockState.submit(matrices, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();

        // 3. Submit Toolhead Item
        if (state.hasToolhead) {
            matrices.pushPose();
            matrices.translate(state.targetOffset.x() + 0.4, state.targetOffset.y(), state.targetOffset.z() + 0.4);
            matrices.mulPose(Axis.YP.rotationDegrees(30));
            state.toolheadState.submit(matrices, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }

        // 4. Submit Destroyer Quarry Beam
        if (state.isDestroyerQuarry) {
            var offset = state.destroyerBeamOffset;
            var baseThickness = 0.035f;

            var beamTexture = EndericLaserRenderer.BEAM_TEXTURE;
            var renderType = RenderTypes.eyes(beamTexture);

            collector.submitCustomGeometry(matrices, renderType, (pose, consumer) -> {
                // inner core
                BeamRenderer.renderStraightBeam(
                        pose, consumer, offset.add(0.5, 1, 0.5), new Vec3(0, -state.destroyerBeamHeight - 1, 0),
                        baseThickness,
                        RenderHelpers.FULL_BRIGHT,
                        EndericLaserRenderer.CORE_COLOR_START,
                        EndericLaserRenderer.CORE_COLOR_END
                );

                // render glow overlay
                BeamRenderer.renderStraightBeam(
                        pose, consumer, offset.add(0.5, 1, 0.5), new Vec3(0, -state.destroyerBeamHeight - 1, 0),
                        state.thickness,
                        RenderHelpers.FULL_BRIGHT,
                        EndericLaserRenderer.GLOW_COLOR_START,
                        EndericLaserRenderer.GLOW_COLOR_END
                );
            });

            // beam ring
            matrices.pushPose();
            var ringHeight = state.ringHeightSine;
            var heightOffset = state.destroyerBeamHeight * 0.5 * ringHeight + state.destroyerBeamHeight * 0.5;
            matrices.translate(offset.x(), offset.y() - heightOffset + 1, offset.z());
            state.ringBlockState.submit(matrices, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }
    }

    public static class GantryRenderState extends BlockEntityRenderState {
        public boolean hasArea;
        public Vec3 targetOffset;
        public BlockState machineHead;
        public float armLength;
        public Vec3 armTarget;
        public boolean hasToolhead;
        public final ItemStackRenderState toolheadState = new ItemStackRenderState();

        // destroyer quarry beam
        public boolean isDestroyerQuarry;
        public Vec3 destroyerBeamOffset;
        public float destroyerBeamHeight;
        public float thickness;
        public float ringHeightSine;

        // block render states
        public final BlockModelRenderState headBlockState = new BlockModelRenderState();
        public final BlockModelRenderState armBlockState = new BlockModelRenderState();
        public final BlockModelRenderState ringBlockState = new BlockModelRenderState();
    }
}
