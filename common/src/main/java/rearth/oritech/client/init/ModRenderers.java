package rearth.oritech.client.init;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.*;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

public class ModRenderers {

    public static void registerRenderers() {

        // processing
        BlockEntityRendererFactories.register(BlockEntitiesContent.PULVERIZER_ENTITY, ctx -> new MachineRenderer<>("models/pulverizer_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.FRAGMENT_FORGE_ENTITY, ctx -> new MachineRenderer<>("models/fragment_forge_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ASSEMBLER_ENTITY, ctx -> new MachineRenderer<>("models/assembler_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.FOUNDRY_ENTITY, ctx -> new MachineRenderer<>("models/foundry_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.COOLER_ENTITY, ctx -> new MachineRenderer<>("models/cooler_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.CENTRIFUGE_ENTITY, ctx -> new MachineRenderer<>("models/centrifuge_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ATOMIC_FORGE_ENTITY, ctx -> new MachineRenderer<>("models/atomic_forge_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.POWERED_FURNACE_ENTITY, ctx -> new MachineRenderer<>("models/powered_furnace_block"));
        
        BlockEntityRendererFactories.register(BlockEntitiesContent.PLAYER_MODIFIER_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/test_block"));
        
        // generators
        BlockEntityRendererFactories.register(BlockEntitiesContent.BIO_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/bio_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.BASIC_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/basic_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.FUEL_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/fuel_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.LAVA_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/lava_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.STEAM_ENGINE_ENTITY, ctx -> new MachineRenderer<>("models/steam_engine_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.BIG_SOLAR_ENTITY, ctx -> new SolarPanelRenderer<>("models/big_solar_panel_block"));
        
        // interactions
        BlockEntityRendererFactories.register(BlockEntitiesContent.LASER_ARM_ENTITY, ctx -> new LaserArmRenderer<>("models/laser_arm_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.DEEP_DRILL_ENTITY, ctx -> new MachineRenderer<>("models/deep_drill_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.DRONE_PORT_ENTITY, ctx -> new MachineRenderer<>("models/drone_port_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.TREEFELLER_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/treefeller_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ENCHANTER_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/enchanter_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.PIPE_BOOSTER_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/pipe_booster_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.PARTICLE_COLLECTOR_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/particle_collector_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ENCHANTMENT_CATALYST_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/enchantment_catalyst_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.PUMP_BLOCK, ctx -> new MachineRenderer<>("models/pump_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.PLACER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.SMALL_TANK_ENTITY, ctx -> new SmallTankRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.CREATIVE_TANK_ENTITY, ctx -> new SmallTankRenderer());
        
        BlockEntityRendererFactories.register(BlockEntitiesContent.SPAWNER_CONTROLLER_BLOCK_ENTITY, ctx -> new SpawnerControllerRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.ACCELERATOR_CONTROLLER_BLOCK_ENTITY, ctx -> new AcceleratorControllerRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.BLACK_HOLE_ENTITY, ctx -> new BlackHoleRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.CHARGER_BLOCK_ENTITY, ctx -> new ChargerBlockRenderer());
        
        BlockEntityRendererFactories.register(BlockEntitiesContent.TECH_DOOR_ENTITY, ctx -> new MachineRenderer<>("models/tech_door"));
        
        // cutout renders
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.MACHINE_FRAME_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.REACTOR_ABSORBER_PORT, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.REACTOR_CONTROLLER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.FRAME_GANTRY_ARM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_PLACER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_DESTROYER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_FERTILIZER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.MACHINE_FLUID_ADDON, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.CROP_FILTER_ADDON, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.LARGE_STORAGE_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.FERTILIZER_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.PLACER_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.URANIUM_CRYSTAL, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.DESTROYER_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.SMALL_TANK_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.CREATIVE_TANK_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.INDUSTRIAL_GLASS_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.QUARRY_BEAM_TARGET, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.QUARRY_BEAM_RING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.WITHER_CROP_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.SPAWNER_CONTROLLER_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.ACCELERATOR_MOTOR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.ACCELERATOR_RING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.ACCELERATOR_CONTROLLER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLACK_HOLE_BLOCK, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLACK_HOLE_INNER, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLACK_HOLE_MIDDLE, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLACK_HOLE_OUTER, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.COOLER_BLOCK, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.REACTOR_ROD, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.REACTOR_DOUBLE_ROD, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.REACTOR_QUAD_ROD, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.REACTOR_REDSTONE_PORT, RenderLayer.getTranslucent());

        Oritech.LOGGER.info("Registering Entities Renderers for " + Oritech.MOD_ID);
    }

}
