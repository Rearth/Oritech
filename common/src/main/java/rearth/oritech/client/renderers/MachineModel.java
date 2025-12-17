package rearth.oritech.client.renderers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.Oritech;
import rearth.oritech.util.ColorableMachine;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class MachineModel<T extends BlockEntity & GeoAnimatable> extends DefaultedBlockGeoModel<T> {
    public MachineModel(String subpath) {
        super(Oritech.id(subpath));
    }
    
    @Override
    public ResourceLocation getTextureResource(T animatable) {
        
        if (animatable instanceof ColorableMachine colorableMachine && colorableMachine.supportRecoloring()) {
            var color = colorableMachine.getCurrentColor();
            var base = super.getTextureResource(animatable);
            
            if (color.equals(ColorableMachine.ColorVariant.ORANGE)) return base;
            
            var colorFileSuffix = color.toString().toLowerCase();
            
            return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath().replace("models", "models/colored").replace(".png", "_" + colorFileSuffix + ".png"));
        } else {
            return super.getTextureResource(animatable);
        }
    }
    
    
    public ResourceLocation getBaseTexturePath(T animatable) {
            return super.getTextureResource(animatable);
    }
}
