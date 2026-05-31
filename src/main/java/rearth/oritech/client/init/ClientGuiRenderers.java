package rearth.oritech.client.init;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import rearth.oritech.Oritech;
import rearth.oritech.client.ui.render.BlockPreviewPipRenderer;
import rearth.oritech.client.ui.render.BlockPreviewRenderState;

@EventBusSubscriber(modid = Oritech.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientGuiRenderers {

    private ClientGuiRenderers() {
    }

    @SubscribeEvent
    public static void registerPipRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(BlockPreviewRenderState.class, BlockPreviewPipRenderer::new);
    }
}
