package rearth.oritech.client.renderers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.Oritech;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class MachineModel<T extends BlockEntity & GeoAnimatable> extends DefaultedBlockGeoModel<T> {
    public MachineModel(String subpath) {
        super(Oritech.id(subpath));
    }
    
    @Override
    public ResourceLocation getTextureResource(T animatable) {
        
        var color = "redstone";
        
        var base = super.getTextureResource(animatable);
        // return base;
        return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath().replace("models", "models/colored").replace(".png", "_" + color + ".png"));
    }
}
