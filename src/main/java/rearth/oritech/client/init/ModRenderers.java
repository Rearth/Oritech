package rearth.oritech.client.init;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.LaserArmRenderer;
import rearth.oritech.client.renderers.MachineGantryRenderer;
import rearth.oritech.client.renderers.MachineRenderer;
import rearth.oritech.client.renderers.SolarPanelRenderer;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

public class ModRenderers {

    public static void registerRenderers() {

        // processing
        BlockEntityRendererFactories.register(BlockEntitiesContent.PULVERIZER_ENTITY, ctx -> new MachineRenderer<>("models/pulverizer_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.FRAGMENT_FORGE_ENTITY, ctx -> new MachineRenderer<>("models/fragment_forge_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ASSEMBLER_ENTITY, ctx -> new MachineRenderer<>("models/assembler_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.FOUNDRY_ENTITY, ctx -> new MachineRenderer<>("models/foundry_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.CENTRIFUGE_ENTITY, ctx -> new MachineRenderer<>("models/centrifuge_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ATOMIC_FORGE_ENTITY, ctx -> new MachineRenderer<>("models/atomic_forge_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.POWERED_FURNACE_ENTITY, ctx -> new MachineRenderer<>("models/powered_furnace_block"));
        
        // generators
        BlockEntityRendererFactories.register(BlockEntitiesContent.TEST_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/test_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.BASIC_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/test_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.BIG_SOLAR_ENTITY, ctx -> new SolarPanelRenderer<>("models/big_solar_panel_block"));
        
        // interactions
        BlockEntityRendererFactories.register(BlockEntitiesContent.LASER_ARM_BLOCK, ctx -> new LaserArmRenderer<>("models/laser_arm_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.PLACER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        
        // cutout renders
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.MACHINE_FRAME_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.FRAME_GANTRY_ARM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_PLACER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_DESTROYER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_FERTILIZER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.MACHINE_FLUID_ADDON, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.CROP_FILTER_ADDON, RenderLayer.getCutout());

        Oritech.LOGGER.info("Registering Entities Renderers for " + Oritech.MOD_ID);
    }

}
