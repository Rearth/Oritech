package rearth.oritech.client.renderers;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class MachineModel<T extends BlockEntity & GeoAnimatable> extends DefaultedBlockGeoModel<T> {
    public MachineModel(String subpath) {
        super(new Identifier(Oritech.MOD_ID, subpath));
    }
    
    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}
