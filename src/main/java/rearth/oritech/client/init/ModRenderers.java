package rearth.oritech.client.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.blocks.*;
import rearth.oritech.init.BlockEntitiesContent;

public class ModRenderers {

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

        // processing
        event.registerBlockEntityRenderer(BlockEntitiesContent.PULVERIZER_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/pulverizer_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.FRAGMENT_FORGE_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/fragment_forge_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.ASSEMBLER_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/assembler_block", false));
        event.registerBlockEntityRenderer(BlockEntitiesContent.FOUNDRY_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/foundry_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.COOLER_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/cooler_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.CENTRIFUGE_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/centrifuge_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.ATOMIC_FORGE_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/atomic_forge_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.POWERED_FURNACE_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/powered_furnace_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.REFINERY_ENTITY.get(), ctx -> new RefineryRenderer<>(ctx, "models/refinery_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.TAINTED_REFINERY_ENTITY.get(), ctx -> new TaintedRefineryRenderer<>(ctx, "models/tainted_refinery_block")); // todo
        event.registerBlockEntityRenderer(BlockEntitiesContent.REFINERY_MODULE_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/refinery_module_block"));

        event.registerBlockEntityRenderer(BlockEntitiesContent.PLAYER_MODIFIER_BLOCK_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/augment_application_block"));

        // generators
        event.registerBlockEntityRenderer(BlockEntitiesContent.BIO_GENERATOR_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/bio_generator_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.BASIC_GENERATOR_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/basic_generator_block", false));
        event.registerBlockEntityRenderer(BlockEntitiesContent.FUEL_GENERATOR_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/fuel_generator_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.LAVA_GENERATOR_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/lava_generator_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.STEAM_ENGINE_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/steam_engine_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.BIG_SOLAR_ENTITY.get(), ctx -> new SolarPanelRenderer<>(ctx, "models/big_solar_panel_block"));

        // interactions
        event.registerBlockEntityRenderer(BlockEntitiesContent.LASER_ARM_ENTITY.get(), ctx -> new LaserArmRenderer<>(ctx, "models/laser_arm_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.DEEP_DRILL_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/deep_drill_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.DRONE_PORT_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/drone_port_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.TREEFELLER_BLOCK_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/treefeller_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.ENCHANTER_BLOCK_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/enchanter_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.PIPE_BOOSTER_BLOCK_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/pipe_booster_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.ENCHANTMENT_CATALYST_BLOCK_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/enchantment_catalyst_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.PUMP_BLOCK.get(), ctx -> new MachineRenderer<>(ctx, "models/pump_block"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.PLACER_BLOCK_ENTITY.get(), MachineGantryRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY.get(), MachineGantryRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY.get(), MachineGantryRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.SMALL_TANK_ENTITY.get(), SmallTankRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.CREATIVE_TANK_ENTITY.get(), SmallTankRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.SHRINKER_BLOCK_ENTITY.get(), ctx -> new ShrinkerBlockRenderer<>(ctx, "models, /shrinker_block"));

        event.registerBlockEntityRenderer(BlockEntitiesContent.SPAWNER_CONTROLLER_BLOCK_ENTITY.get(), SpawnerControllerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.ACCELERATOR_CONTROLLER_BLOCK_ENTITY.get(), AcceleratorControllerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.BLACK_HOLE_ENTITY.get(), BlackHoleRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.ITEM_PIPE_ENTITY.get(), ItemPipeTransferRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.CHARGER_BLOCK_ENTITY.get(), ChargerBlockRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.POWER_POLE_ENTITY.get(), PowerPoleCableRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.UNSTABLE_CONTAINER_BLOCK_ENTITY.get(), ctx -> new UnstableContainerRenderer<>(ctx, "models/unstable_container"));


        event.registerBlockEntityRenderer(BlockEntitiesContent.TECH_DOOR_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/tech_door"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.HANGAR_DOOR_ENTITY.get(), ctx -> new HangarDoorRenderer<>(ctx, "models/hangar_door"));

        Oritech.LOGGER.info("Registered Entities Renderers for " + Oritech.MOD_ID);


    }

}
