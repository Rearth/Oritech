package rearth.oritech.client.init;

import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.PulverizerBlockEntity;
import rearth.oritech.client.renderers.MachineRenderer;
import rearth.oritech.init.BlockEntitiesContent;

public class ModRenderers {

    public static void registerRenderers() {

        BlockEntityRendererFactories.register(BlockEntitiesContent.PULVERIZER_ENTITY, ctx -> new MachineRenderer<>("models/gem_station_anim"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.GRINDER_ENTITY, ctx -> new MachineRenderer<>("models/grav_magnet"));

        Oritech.LOGGER.info("Registering Entities Renderers for " + Oritech.MOD_ID);
    }

}
