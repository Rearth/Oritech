package rearth.oritech.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;
import rearth.oritech.util.ColorHelper;

public class SpawnerControllerRenderer implements BlockEntityRenderer<SpawnerControllerBlockEntity, SpawnerControllerRenderer.SpawnerRenderState> {

    public SpawnerControllerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public SpawnerRenderState createRenderState() {
        return new SpawnerRenderState();
    }

    @Override
    public void extractRenderState(SpawnerControllerBlockEntity entity, SpawnerRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);

        state.renderedEntity = entity.renderedEntity instanceof LivingEntity le ? le : null;
        state.hasCage = entity.hasCage;
        state.lightCoords = entity.getLevel() != null ? LevelRenderer.getLightCoords(entity.getLevel(), entity.getBlockPos()) : 15728880;

        if (state.renderedEntity != null && state.hasCage) {
            var progress = Math.min(1f, entity.collectedSouls / (float) entity.maxSouls);
            if (progress != 0)
                progress = (float) EndericLaserRenderer.lerp(entity.lastProgress, progress, 0.03f);
            entity.lastProgress = progress;

            // use ColorHelper to construct package-independent ARGB color
            state.color = ColorHelper.argb(1f - progress, 1f, 1f, (75f + 180f * progress) / 255f);
        }
    }

    @Override
    public void submit(SpawnerRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        if (state.renderedEntity != null && state.hasCage) {

            matrices.pushPose();
            matrices.translate(0, -Math.round(state.renderedEntity.getBbHeight() + 0.4f), 0);
            matrices.mulPose(Axis.YP.rotationDegrees(45));

            var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            var renderer = dispatcher.getRenderer(state.renderedEntity);

            if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingEntityRenderer) {
                matrices.scale(-1.0F, -1.0F, 1.0F);
                matrices.translate(0.0F, -1.501F, 0.0F);
                matrices.scale(0.9f, 0.9f, 0.9f);
                var model = livingEntityRenderer.getModel();
                var renderLayer = RenderTypes.entityTranslucentEmissive(
                        Identifier.withDefaultNamespace("textures/entity/beacon/beacon_beam.png"),
                        false);

                // submit the custom geometry with the matrices transformed specifically for the entity model preview
                collector.submitCustomGeometry(matrices, renderLayer, (pose, consumer) -> {
                    var localStack = new PoseStack();
                    localStack.last().pose().set(pose.pose());
                    localStack.last().normal().set(pose.normal());
                    model.renderToBuffer(localStack, consumer, state.lightCoords, OverlayTexture.NO_OVERLAY, state.color);
                });
            }
            matrices.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(SpawnerControllerBlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 6, 8, 6);
    }

    public static class SpawnerRenderState extends BlockEntityRenderState {
        public @Nullable LivingEntity renderedEntity;
        public boolean hasCage;
        public int color;
    }
}
