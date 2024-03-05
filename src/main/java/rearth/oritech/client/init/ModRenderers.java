package rearth.oritech.client.init;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.MachineGantryRenderer;
import rearth.oritech.client.renderers.MachineRenderer;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

public class ModRenderers {

    public static void registerRenderers() {

        BlockEntityRendererFactories.register(BlockEntitiesContent.PULVERIZER_ENTITY, ctx -> new MachineRenderer<>("models/pulverizer_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.GRINDER_ENTITY, ctx -> new MachineRenderer<>("models/grinder_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ASSEMBLER_ENTITY, ctx -> new MachineRenderer<>("models/assembler_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.FOUNDRY_ENTITY, ctx -> new MachineRenderer<>("models/foundry_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.POWERED_FURNACE_ENTITY, ctx -> new MachineRenderer<>("models/powered_furnace_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.PLACER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.MACHINE_FRAME_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.FRAME_GANTRY_ARM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_PLACER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_DESTROYER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_FERTILIZER_HEAD, RenderLayer.getCutout());

        Oritech.LOGGER.info("Registering Entities Renderers for " + Oritech.MOD_ID);
    }

}
