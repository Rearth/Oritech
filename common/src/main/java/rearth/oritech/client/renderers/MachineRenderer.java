package rearth.oritech.client.renderers;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class MachineRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public MachineRenderer(String modelPath) {
        super(new MachineModel<>(modelPath));
    }
    
    public MachineRenderer(String modelPath, boolean glowing) {
        super(new MachineModel<>(modelPath));
        
        if (glowing) {
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }
    
    @Override
    protected void rotateBlock(Direction facing, MatrixStack poseStack) {
        
        if (facing.equals(Direction.UP)) {
            poseStack.translate(0, 0.5, -0.5);
        } else if (facing.equals(Direction.DOWN)) {
            poseStack.translate(0, 0.5, 0.5);
        }
        
        super.rotateBlock(facing, poseStack);
        
    }
    
    // this overrides a method from IBlockEntityRendererExtension on NF. Since this extension mixin is not available in common, we just declare the methode without\
    // the override annotation
    public Box getRenderBoundingBox(BlockEntity blockEntity) {
        return Box.of(blockEntity.getPos().toCenterPos(), 8, 8, 8);
    }
}


