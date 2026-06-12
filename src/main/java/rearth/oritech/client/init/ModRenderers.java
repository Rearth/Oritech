package rearth.oritech.client.init;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.blocks.*;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.HashMap;
import java.util.Map;

public class ModRenderers {

    public static Map<Block, RenderType> RENDER_LAYERS = new HashMap<>();  // this is used in client-specific parts to set render layers

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
        event.registerBlockEntityRenderer(BlockEntitiesContent.PLACER_BLOCK_ENTITY.get(), ctx -> new MachineGantryRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY.get(), ctx -> new MachineGantryRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY.get(), ctx -> new MachineGantryRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.SMALL_TANK_ENTITY.get(), ctx -> new SmallTankRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.CREATIVE_TANK_ENTITY.get(), ctx -> new SmallTankRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.SHRINKER_BLOCK_ENTITY.get(), ctx -> new ShrinkerBlockRenderer(ctx, "models, /shrinker_block"));

        event.registerBlockEntityRenderer(BlockEntitiesContent.SPAWNER_CONTROLLER_BLOCK_ENTITY.get(), ctx -> new SpawnerControllerRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.ACCELERATOR_CONTROLLER_BLOCK_ENTITY.get(), ctx -> new AcceleratorControllerRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.BLACK_HOLE_ENTITY.get(), BlackHoleRenderer::new);
        event.registerBlockEntityRenderer(BlockEntitiesContent.ITEM_PIPE_ENTITY.get(), ctx -> new ItemPipeTransferRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.CHARGER_BLOCK_ENTITY.get(), ctx -> new ChargerBlockRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.POWER_POLE_ENTITY.get(), ctx -> new PowerPoleCableRenderer(ctx));
        event.registerBlockEntityRenderer(BlockEntitiesContent.UNSTABLE_CONTAINER_BLOCK_ENTITY.get(), ctx -> new UnstableContainerRenderer<>(ctx, "models/unstable_container"));


        event.registerBlockEntityRenderer(BlockEntitiesContent.TECH_DOOR_ENTITY.get(), ctx -> new MachineRenderer<>(ctx, "models/tech_door"));
        event.registerBlockEntityRenderer(BlockEntitiesContent.HANGAR_DOOR_ENTITY.get(), ctx -> new HangarDoorRenderer(ctx, "models/hangar_door"));

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
