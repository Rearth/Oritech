package rearth.oritech.client.renderers.models;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.blocks.MachineRenderer;
import rearth.oritech.util.ColorableMachine;

import java.util.Locale;

public class MachineModel<T extends BlockEntity & GeoAnimatable> extends DefaultedBlockGeoModel<T> {

    public MachineModel(String subpath) {
        super(Oritech.id(subpath));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {

        var base = super.getTextureResource(renderState);

        var color = renderState.getOrDefaultGeckolibData(MachineRenderer.TEXTURE_OVERRIDE_TICKET, ColorableMachine.ColorVariant.ORANGE);

        if (color.equals(ColorableMachine.ColorVariant.ORANGE)) return base;

        var colorFileSuffix = color.toString().toLowerCase(Locale.ROOT);

        return Identifier.fromNamespaceAndPath(base.getNamespace(), base.getPath().replace("models", "models/colored").replace(".png", "_" + colorFileSuffix + ".png"));
    }
}
