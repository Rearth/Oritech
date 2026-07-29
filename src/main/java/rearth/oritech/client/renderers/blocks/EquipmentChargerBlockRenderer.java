package rearth.oritech.client.renderers.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.interaction.EquipmentChargerBlockEntity;

public class EquipmentChargerBlockRenderer implements BlockEntityRenderer<EquipmentChargerBlockEntity, EquipmentChargerBlockRenderer.EquipmentChargerRenderState> {

    public EquipmentChargerBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public EquipmentChargerRenderState createRenderState() {
        return new EquipmentChargerRenderState();
    }

    @Override
    public void extractRenderState(EquipmentChargerBlockEntity entity, EquipmentChargerRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);

        var stack = entity.inventory.getItem(0);
        if (stack.isEmpty()) {
            state.hasItem = false;
            return;
        }

        state.hasItem = true;
        var mc = Minecraft.getInstance();
        mc.getItemModelResolver().updateForTopItem(state.itemState, stack, ItemDisplayContext.GROUND, entity.getLevel(), null, 0);
    }

    @Override
    public void submit(EquipmentChargerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        if (!state.hasItem) return;

        poseStack.pushPose();
        poseStack.translate(0.5f, 8 / 16f, 0.5f);

        state.itemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }

    public static class EquipmentChargerRenderState extends BlockEntityRenderState {
        public boolean hasItem;
        public final ItemStackRenderState itemState = new ItemStackRenderState();
    }
}
