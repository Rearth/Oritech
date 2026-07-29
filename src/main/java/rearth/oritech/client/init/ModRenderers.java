package rearth.oritech.client.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.PortalEntityRenderer;
import rearth.oritech.client.renderers.blocks.*;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.EntitiesContent;

public class ModRenderers {

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(EntitiesContent.PORTAL_ENTITY.get(), PortalEntityRenderer::new);

        // processing
        event.registerBlockEntityRenderer(BlockEntitiesContent.PULVERIZER.get(), ctx -> new MachineRenderer<>(ctx, "models/pulverizer"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.FRAGMENT_FORGE.get(), ctx -> new MachineRenderer<>(ctx, "models/fragment_forge"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.ASSEMBLER.get(), ctx -> new MachineRenderer<>(ctx, "models/assembler", false));
        event.registerBlockEntityRenderer(BlockEntitiesContent.FOUNDRY.get(), ctx -> new MachineRenderer<>(ctx, "models/foundry"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.INDUSTRIAL_CHILLER.get(), ctx -> new MachineRenderer<>(ctx, "models/industrial_chiller"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.CENTRIFUGE.get(), ctx -> new CentrifugeRenderer<>(ctx, "models/centrifuge"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.ATOMIC_FORGE.get(), ctx -> new MachineRenderer<>(ctx, "models/atomic_forge"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.POWERED_FURNACE.get(), ctx -> new MachineRenderer<>(ctx, "models/powered_furnace"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.REFINERY.get(), ctx -> new RefineryRenderer<>(ctx, "models/refinery"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.TAINTED_REFINERY.get(), ctx -> new TaintedRefineryRenderer<>(ctx, "models/tainted_refinery"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.REFINERY_CHAMBER_MODULE.get(), ctx -> new MachineRenderer<>(ctx, "models/refinery_chamber_module"));

        event.registerBlockEntityRenderer(BlockEntitiesContent.CYBERNETIC_AUGMENTATION_CENTER_BLOCK.get(), ctx -> new MachineRenderer<>(ctx, "models/cybernetic_augmentation_center"));

        // generators
        event.registerBlockEntityRenderer(BlockEntitiesContent.BIO_GENERATOR.get(), ctx -> new MachineRenderer<>(ctx, "models/bio_generator"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.BASIC_GENERATOR.get(), ctx -> new MachineRenderer<>(ctx, "models/basic_generator", false));
        event.registerBlockEntityRenderer(BlockEntitiesContent.FUEL_GENERATOR.get(), ctx -> new MachineRenderer<>(ctx, "models/fuel_generator"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.LAVA_GENERATOR.get(), ctx -> new MachineRenderer<>(ctx, "models/lava_generator"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.STEAM_ENGINE.get(), ctx -> new MachineRenderer<>(ctx, "models/steam_engine"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.BIG_SOLAR.get(), ctx -> new SolarPanelRenderer<>(ctx, "models/big_solar_panel"));

        // interactions
        event.registerBlockEntityRenderer(BlockEntitiesContent.ENDERIC_LASER.get(), ctx -> new EndericLaserRenderer<>(ctx, "models/enderic_laser"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.BEDROCK_EXTRACTOR.get(), ctx -> new MachineRenderer<>(ctx, "models/bedrock_extractor"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.DRONE_PORT.get(), ctx -> new MachineRenderer<>(ctx, "models/drone_port"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.TREE_CUTTER.get(), ctx -> new MachineRenderer<>(ctx, "models/tree_cutter"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.STABILIZED_ENCHANTER.get(), ctx -> new MachineRenderer<>(ctx, "models/stabilized_enchanter"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.PIPE_BOOSTER.get(), ctx -> new MachineRenderer<>(ctx, "models/pipe_booster"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.ARCANE_CATALYST_BLOCK.get(), ctx -> new MachineRenderer<>(ctx, "models/arcane_catalyst"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.PUMP.get(), ctx -> new MachineRenderer<>(ctx, "models/pump"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.PLACER.get(), MachineGantryRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.DESTROYER.get(), MachineGantryRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.FERTILIZER.get(), MachineGantryRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.PORTABLE_TANK.get(), PortableTankRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.CREATIVE_TANK.get(), PortableTankRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.ADDON_SPLICER.get(), ctx -> new AddonSplicerBlockRenderer<>(ctx, "models/addon_splicer"));

        event.registerBlockEntityRenderer(BlockEntitiesContent.SPAWNER_CONTROLLER.get(), SpawnerControllerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.PARTICLE_ACCELERATOR_BLOCK.get(), ParticleAcceleratorRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.BLACK_HOLE.get(), BlackHoleRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.ITEM_PIPE.get(), ItemPipeTransferRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.EQUIPMENT_CHARGER.get(), EquipmentChargerBlockRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.ENERGY_TRANSMISSION_POLE.get(), EnergyTransmissionPoleCableRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.SCHRODINGERS_SAFE_BLOCK.get(), ctx -> new SchrodingersSafeRenderer<>(ctx, "models/schrodingers_safe"));


        event.registerBlockEntityRenderer(BlockEntitiesContent.INDUSTRIAL_DOOR.get(), ctx -> new MachineRenderer<>(ctx, "models/industrial_door"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.HANGAR_DOOR.get(), ctx -> new HangarDoorRenderer<>(ctx, "models/hangar_door"));

        Oritech.LOGGER.info("Registered Entities Renderers for " + Oritech.MOD_ID);


    }

}
