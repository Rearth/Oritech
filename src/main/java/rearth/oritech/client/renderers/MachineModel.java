package rearth.oritech.client.renderers;

import net.minecraft.block.entity.BlockEntity;
import rearth.oritech.Oritech;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class MachineModel<T extends BlockEntity & GeoAnimatable> extends DefaultedBlockGeoModel<T> {
    public MachineModel(String subpath) {
        super(Oritech.id(subpath));
    }
    
}
