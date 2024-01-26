package rearth.oritech.client.renderers;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.PulverizerBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MachineRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public MachineRenderer(String modelPath) {
        super(new MachineModel<>(modelPath));
    }

}


