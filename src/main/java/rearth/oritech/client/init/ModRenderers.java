package rearth.oritech.client.init;

import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.MachineGantryRenderer;
import rearth.oritech.client.renderers.MachineRenderer;
import rearth.oritech.init.BlockEntitiesContent;

public class ModRenderers {

    public static void registerRenderers() {

        BlockEntityRendererFactories.register(BlockEntitiesContent.PULVERIZER_ENTITY, ctx -> new MachineRenderer<>("models/pulverizer_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.GRINDER_ENTITY, ctx -> new MachineRenderer<>("models/grinder_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ASSEMBLER_ENTITY, ctx -> new MachineRenderer<>("models/assembler_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.POWERED_FURNACE_ENTITY, ctx -> new MachineRenderer<>("models/powered_furnace_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.PLACER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());

        Oritech.LOGGER.info("Registering Entities Renderers for " + Oritech.MOD_ID);
    }

}
