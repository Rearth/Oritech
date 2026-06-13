package rearth.oritech.client.renderers.blocks;

import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;
import rearth.oritech.client.renderers.models.MachineModel;

public class ShrinkerBlockRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<ShrinkerBlockEntity, R> {

    public ShrinkerBlockRenderer(BlockEntityRendererProvider.Context context, String modelPath) {
        super(context, new MachineModel<>(modelPath));
    }

    // todo check / update the green fluid part rendering
}
