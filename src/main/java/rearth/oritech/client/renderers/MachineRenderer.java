package rearth.oritech.client.renderers;

import net.minecraft.block.entity.BlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MachineRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public MachineRenderer(String modelPath) {
        super(new MachineModel<>(modelPath));
    }
}


