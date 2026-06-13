package rearth.oritech.client.renderers.blocks;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.blocks.decorative.HangarDoorBlock;
import rearth.oritech.block.entity.decorative.HangarDoorBlockEntity;
import rearth.oritech.client.renderers.models.MachineModel;

public class HangarDoorRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<HangarDoorBlockEntity, R> {

    public static final DataTicket<Direction> SURFACE_TICKET = DataTicket.create("hangar_door_surface", Direction.class);
    public static final DataTicket<Boolean> ROTATED_TICKET = DataTicket.create("hangar_door_rotated", Boolean.class);

    public HangarDoorRenderer(BlockEntityRendererProvider.Context context, String modelPath) {
        super(context, new MachineModel<>(modelPath));
    }

    @Override
    public void addRenderData(HangarDoorBlockEntity animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);
        var state = animatable.getBlockState();
        var surface = state.getValue(HangarDoorBlock.SURFACE);
        var rotated = state.getValue(HangarDoorBlock.ROTATED);
        renderState.addGeckolibData(SURFACE_TICKET, surface);
        renderState.addGeckolibData(ROTATED_TICKET, rotated);
    }

    @Override
    protected void tryRotateByBlockstate(RenderPassInfo<R> renderPassInfo, PoseStack poseStack) {
        var surface = renderPassInfo.getOrDefaultGeckolibData(SURFACE_TICKET, Direction.NORTH);
        var rotated = renderPassInfo.getOrDefaultGeckolibData(ROTATED_TICKET, false);

        if (rotated && surface.getAxis().isVertical()) {
            poseStack.translate(0, 0, -1);
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
            poseStack.translate(-0.5, 0, -0.5);
        }

        if (surface == Direction.DOWN) {
            poseStack.translate(0, 1, 0);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        } else if (surface == Direction.WEST) {
            poseStack.translate(0.5, 0.5, 0);
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        } else if (surface == Direction.EAST) {
            poseStack.translate(-0.5, 0.5, 0);
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(270));
        } else if (surface == Direction.SOUTH) {
            poseStack.translate(0, 0.5, -0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        } else if (surface == Direction.NORTH) {
            poseStack.translate(0, 0.5, 0.5);
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
        }

        if (rotated && surface.getAxis().isHorizontal())
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
    }

    public AABB getRenderBoundingBox(HangarDoorBlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 4, 4, 4);
    }
}
