package rearth.oritech.client.init;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.Block;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.*;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.HashMap;
import java.util.Map;

public class ModRenderers {
    
    public static Map<Block, RenderType> RENDER_LAYERS = new HashMap<>();  // this is used in client-specific parts to set render layers

    public static void registerRenderers() {

        // processing
        BlockEntityRenderers.register(BlockEntitiesContent.PULVERIZER_ENTITY, ctx -> new MachineRenderer<>("models/pulverizer_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.FRAGMENT_FORGE_ENTITY, ctx -> new MachineRenderer<>("models/fragment_forge_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.ASSEMBLER_ENTITY, ctx -> new MachineRenderer<>("models/assembler_block", false));
        BlockEntityRenderers.register(BlockEntitiesContent.FOUNDRY_ENTITY, ctx -> new MachineRenderer<>("models/foundry_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.COOLER_ENTITY, ctx -> new MachineRenderer<>("models/cooler_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.CENTRIFUGE_ENTITY, ctx -> new MachineRenderer<>("models/centrifuge_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.ATOMIC_FORGE_ENTITY, ctx -> new MachineRenderer<>("models/atomic_forge_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.POWERED_FURNACE_ENTITY, ctx -> new MachineRenderer<>("models/powered_furnace_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.REFINERY_ENTITY, ctx -> new RefineryRenderer<>("models/refinery_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.REFINERY_MODULE_ENTITY, ctx -> new MachineRenderer<>("models/refinery_module_block"));
        
        BlockEntityRenderers.register(BlockEntitiesContent.PLAYER_MODIFIER_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/augment_application_block"));
        
        // generators
        BlockEntityRenderers.register(BlockEntitiesContent.BIO_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/bio_generator_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.BASIC_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/basic_generator_block", false));
        BlockEntityRenderers.register(BlockEntitiesContent.FUEL_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/fuel_generator_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.LAVA_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/lava_generator_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.STEAM_ENGINE_ENTITY, ctx -> new MachineRenderer<>("models/steam_engine_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.BIG_SOLAR_ENTITY, ctx -> new SolarPanelRenderer<>("models/big_solar_panel_block"));
        
        // interactions
        BlockEntityRenderers.register(BlockEntitiesContent.LASER_ARM_ENTITY, ctx -> new LaserArmRenderer<>("models/laser_arm_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.DEEP_DRILL_ENTITY, ctx -> new MachineRenderer<>("models/deep_drill_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.DRONE_PORT_ENTITY, ctx -> new MachineRenderer<>("models/drone_port_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.TREEFELLER_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/treefeller_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.ENCHANTER_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/enchanter_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.PIPE_BOOSTER_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/pipe_booster_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.PARTICLE_COLLECTOR_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/particle_collector_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.ENCHANTMENT_CATALYST_BLOCK_ENTITY, ctx -> new MachineRenderer<>("models/enchantment_catalyst_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.PUMP_BLOCK, ctx -> new MachineRenderer<>("models/pump_block"));
        BlockEntityRenderers.register(BlockEntitiesContent.PLACER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.SMALL_TANK_ENTITY, ctx -> new SmallTankRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.CREATIVE_TANK_ENTITY, ctx -> new SmallTankRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.SHRINKER_BLOCK_ENTITY, ctx -> new ShrinkerBlockRenderer("models/shrinker_block"));
        
        BlockEntityRenderers.register(BlockEntitiesContent.SPAWNER_CONTROLLER_BLOCK_ENTITY, ctx -> new SpawnerControllerRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.ACCELERATOR_CONTROLLER_BLOCK_ENTITY, ctx -> new AcceleratorControllerRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.BLACK_HOLE_ENTITY, ctx -> new BlackHoleRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.ITEM_PIPE_ENTITY, ctx -> new ItemPipeTransferRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.CHARGER_BLOCK_ENTITY, ctx -> new ChargerBlockRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.POWER_POLE_ENTITY, ctx -> new PowerPoleLineRenderer());
        BlockEntityRenderers.register(BlockEntitiesContent.UNSTABLE_CONTAINER_BLOCK_ENTITY, ctx -> new UnstableContainerRenderer("models/unstable_container"));
        
        BlockEntityRenderers.register(BlockEntitiesContent.TECH_DOOR_ENTITY, ctx -> new MachineRenderer<>("models/tech_door"));
        
        // cutout renders
        RENDER_LAYERS.put(BlockContent.MACHINE_FRAME_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.REACTOR_ABSORBER_PORT, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.REACTOR_CONTROLLER, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.FRAME_GANTRY_ARM, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.BLOCK_PLACER_HEAD, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.BLOCK_DESTROYER_HEAD, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.BLOCK_FERTILIZER_HEAD, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.MACHINE_FLUID_ADDON, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.CROP_FILTER_ADDON, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.LARGE_STORAGE_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.FERTILIZER_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.PLACER_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.URANIUM_CRYSTAL, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.DESTROYER_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.SMALL_TANK_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.CREATIVE_TANK_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.INDUSTRIAL_GLASS_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.QUARRY_BEAM_TARGET, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.QUARRY_BEAM_RING, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.WITHER_CROP_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.SPAWNER_CONTROLLER_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.SHRINKER_BLOCK, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.ACCELERATOR_MOTOR, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.ACCELERATOR_RING, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.ACCELERATOR_CONTROLLER, RenderType.cutout());
        RENDER_LAYERS.put(BlockContent.BLACK_HOLE_BLOCK, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.BLACK_HOLE_INNER, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.BLACK_HOLE_MIDDLE, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.BLACK_HOLE_OUTER, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.COOLER_BLOCK, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.REACTOR_ROD, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.REACTOR_DOUBLE_ROD, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.REACTOR_QUAD_ROD, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.REACTOR_REDSTONE_PORT, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.TRANSPARENT_ITEM_PIPE, RenderType.translucent());
        RENDER_LAYERS.put(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION, RenderType.translucent());

        Oritech.LOGGER.info("Registering Entities Renderers for " + Oritech.MOD_ID);
        
        
    }

}
