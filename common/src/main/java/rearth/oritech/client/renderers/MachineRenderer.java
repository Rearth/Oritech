package rearth.oritech.client.renderers;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MachineRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public MachineRenderer(String modelPath) {
        super(new MachineModel<>(modelPath));
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
}


