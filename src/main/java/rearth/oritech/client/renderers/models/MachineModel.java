package rearth.oritech.client.renderers.models;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.blocks.MachineRenderer;
import rearth.oritech.util.ColorableMachine;

public class MachineModel<T extends BlockEntity & GeoAnimatable> extends OritechBlockGeoModel<T> {

    public MachineModel(String subpath) {
        super(Oritech.id(subpath));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {

        var base = super.getTextureResource(renderState);

        var color = renderState.getOrDefaultGeckolibData(MachineRenderer.TEXTURE_OVERRIDE_TICKET, ColorableMachine.ColorVariant.ORANGE);

        return ColorableMachine.getTextureForColor(base, color);
    }
}
