package rearth.oritech.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ItemPipeTransferRenderer implements BlockEntityRenderer<ItemPipeInterfaceEntity, ItemPipeTransferRenderer.ItemPipeRenderState> {

    public ItemPipeTransferRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public ItemPipeRenderState createRenderState() {
        return new ItemPipeRenderState();
    }

    @Override
    public void extractRenderState(ItemPipeInterfaceEntity entity, ItemPipeRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);

        state.activeItems.clear();
        if (entity.activeStacks == null || entity.activeStacks.isEmpty()) return;

        var time = entity.getLevel().getGameTime() + partialTicks;
        var removedStacks = new HashSet<ItemPipeInterfaceEntity.RenderStackData>();

        for (var renderedStack : entity.activeStacks) {
            var age = time - renderedStack.startedAt();
            var neededTime = ItemPipeInterfaceEntity.calculatePathLength(renderedStack.pathLength());    // about 2 blocks/s, but much faster on longer paths
            var progress = age / neededTime;
            if (progress > 1) {
                removedStacks.add(renderedStack);
                continue;
            }

            progress = sigmoidFitted(progress);

            // get position in path at current progress (traverse path to current progress)
            var targetPathProgress = renderedStack.pathLength() * progress;
            var pathProgress = 0;
            var pathPosition = renderedStack.path().getFirst();
            Vec3 targetPos = Vec3.ZERO;

            for (var segment : renderedStack.path()) {
                var segmentDist = segment.distManhattan(pathPosition);

                if (pathProgress + segmentDist < targetPathProgress) {
                    pathProgress += segmentDist;
                    pathPosition = segment;
                } else {    // reaching or overshooting target
                    var remainingDist = targetPathProgress - pathProgress;
                    var targetOffset = Vec3.atLowerCornerOf(segment.subtract(pathPosition)).normalize().scale(remainingDist);
                    targetPos = Vec3.atLowerCornerOf(pathPosition).add(targetOffset);
                    break;
                }

            }

            var offset = targetPos.subtract(Vec3.atLowerCornerOf(entity.getBlockPos()));

            // pre-calculate and cache resolved ItemStackRenderState in extract phase
            var resolvedState = new ItemStackRenderState();
            var mc = Minecraft.getInstance();
            mc.getItemModelResolver().updateForTopItem(resolvedState, renderedStack.rendered(), ItemDisplayContext.GUI, entity.getLevel(), null, 0);

            state.activeItems.add(new RenderedItem(offset, resolvedState));
        }

        entity.activeStacks.removeAll(removedStacks);
    }

    @Override
    public void submit(ItemPipeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        for (var item : state.activeItems) {
            poseStack.pushPose();
            poseStack.translate(item.offset.x + 0.5, item.offset.y + 0.5, item.offset.z + 0.5);
            poseStack.scale(0.4f, 0.4f, 0.4f);
            poseStack.mulPose(Axis.YP.rotationDegrees(-140));
            poseStack.mulPose(Axis.XP.rotationDegrees(-30));

            item.itemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

            poseStack.popPose();
        }
    }

    private static double sigmoidFitted(double x) {
        return sigmoid((x - 0.5) * 2) + 0.5f;
    }

    private static double sigmoid(double x) {
        return x / (1 + Math.abs(x));
    }

    public static class ItemPipeRenderState extends BlockEntityRenderState {
        public final List<RenderedItem> activeItems = new ArrayList<>();
    }

    public record RenderedItem(Vec3 offset, ItemStackRenderState itemState) {}
}
