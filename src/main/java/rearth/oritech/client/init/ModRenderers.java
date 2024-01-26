package rearth.oritech.client.init;

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.PulverizerRenderer;
import rearth.oritech.init.BlockEntitiesContent;

public class ModRenderers {

    public static void registerRenderers() {

        BlockEntityRendererFactories.register(BlockEntitiesContent.PULVERIZER_ENTITY, ctx -> new PulverizerRenderer());

        Oritech.LOGGER.info("Registering Entities Renderers for " + Oritech.MOD_ID);
    }

}
