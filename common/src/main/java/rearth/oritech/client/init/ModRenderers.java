package rearth.oritech.client.init;

import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.*;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.HashMap;
import java.util.Map;

public class ModRenderers {
    
    public static Map<Block, RenderLayer> RENDER_LAYERS = new HashMap<>();  // this is used in client-specific parts to set render layers

    public static void registerRenderers() {

        // processing
        BlockEntityRendererFactories.register(BlockEntitiesContent.PULVERIZER_ENTITY, ctx -> new MachineRenderer<>("models/pulverizer_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.FRAGMENT_FORGE_ENTITY, ctx -> new MachineRenderer<>("models/fragment_forge_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ASSEMBLER_ENTITY, ctx -> new MachineRenderer<>("models/assembler_block", true));
        BlockEntityRendererFactories.register(BlockEntitiesContent.FOUNDRY_ENTITY, ctx -> new MachineRenderer<>("models/foundry_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.COOLER_ENTITY, ctx -> new MachineRenderer<>("models/cooler_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.CENTRIFUGE_ENTITY, ctx -> new MachineRenderer<>("models/centrifuge_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.ATOMIC_FORGE_ENTITY, ctx -> new MachineRenderer<>("models/atomic_forge_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.POWERED_FURNACE_ENTITY, ctx -> new MachineRenderer<>("models/powered_furnace_block"));
        
        BlockEntityRendererFactories.register(BlockEntitiesContent.PLAYER_MODIFIER_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/augment_application_block"));
        
        // generators
        BlockEntityRendererFactories.register(BlockEntitiesContent.BIO_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/bio_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.BASIC_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/basic_generator_block", true));
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
        BlockEntityRendererFactories.register(BlockEntitiesContent.ITEM_PIPE_ENTITY, ctx -> new ItemPipeTransferRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.CHARGER_BLOCK_ENTITY, ctx -> new ChargerBlockRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.UNSTABLE_CONTAINER_BLOCK_ENTITY, ctx -> new UnstableContainerRenderer("models/unstable_container"));
        
        BlockEntityRendererFactories.register(BlockEntitiesContent.TECH_DOOR_ENTITY, ctx -> new MachineRenderer<>("models/tech_door"));
        
        // cutout renders
        RENDER_LAYERS.put(BlockContent.MACHINE_FRAME_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.REACTOR_ABSORBER_PORT, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.REACTOR_CONTROLLER, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.FRAME_GANTRY_ARM, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.BLOCK_PLACER_HEAD, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.BLOCK_DESTROYER_HEAD, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.BLOCK_FERTILIZER_HEAD, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.MACHINE_FLUID_ADDON, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.CROP_FILTER_ADDON, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.LARGE_STORAGE_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.FERTILIZER_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.PLACER_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.URANIUM_CRYSTAL, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.DESTROYER_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.SMALL_TANK_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.CREATIVE_TANK_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.INDUSTRIAL_GLASS_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.QUARRY_BEAM_TARGET, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.QUARRY_BEAM_RING, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.WITHER_CROP_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.SPAWNER_CONTROLLER_BLOCK, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.ACCELERATOR_MOTOR, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.ACCELERATOR_RING, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.ACCELERATOR_CONTROLLER, RenderLayer.getCutout());
        RENDER_LAYERS.put(BlockContent.BLACK_HOLE_BLOCK, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.BLACK_HOLE_INNER, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.BLACK_HOLE_MIDDLE, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.BLACK_HOLE_OUTER, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.COOLER_BLOCK, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.REACTOR_ROD, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.REACTOR_DOUBLE_ROD, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.REACTOR_QUAD_ROD, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.REACTOR_REDSTONE_PORT, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.TRANSPARENT_ITEM_PIPE, RenderLayer.getTranslucent());
        RENDER_LAYERS.put(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION, RenderLayer.getTranslucent());

        Oritech.LOGGER.info("Registering Entities Renderers for " + Oritech.MOD_ID);
    }

}
