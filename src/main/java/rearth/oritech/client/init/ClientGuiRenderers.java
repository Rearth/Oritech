package rearth.oritech.client.init;

import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import rearth.oritech.client.ui.render.BlockPreviewPipRenderer;
import rearth.oritech.client.ui.render.BlockPreviewRenderState;

public final class ClientGuiRenderers {

    private ClientGuiRenderers() {
    }

    public static void registerPipRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(BlockPreviewRenderState.class, BlockPreviewPipRenderer::new);
    }
}
