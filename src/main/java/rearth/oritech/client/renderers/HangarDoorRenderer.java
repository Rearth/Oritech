package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import rearth.oritech.block.blocks.decorative.HangarDoorBlock;
import rearth.oritech.block.entity.decorative.HangarDoorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HangarDoorRenderer extends GeoBlockRenderer<HangarDoorBlockEntity> {
    
    public HangarDoorRenderer(String modelPath) {
        super(new MachineModel<>(modelPath));
    }
    
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        var state = this.animatable.getBlockState();
        var surface = state.getValue(HangarDoorBlock.SURFACE);
        var rotated = state.getValue(HangarDoorBlock.ROTATED);
        
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