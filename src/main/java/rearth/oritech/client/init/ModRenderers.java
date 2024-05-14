package rearth.oritech.client.init;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.*;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.FluidContent;

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
        BlockEntityRendererFactories.register(BlockEntitiesContent.BIO_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/bio_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.BASIC_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/basic_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.FUEL_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/fuel_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.LAVA_GENERATOR_ENTITY, ctx -> new MachineRenderer<>("models/lava_generator_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.BIG_SOLAR_ENTITY, ctx -> new SolarPanelRenderer<>("models/big_solar_panel_block"));
        
        // interactions
        BlockEntityRendererFactories.register(BlockEntitiesContent.LASER_ARM_ENTITY, ctx -> new LaserArmRenderer<>("models/laser_arm_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.DEEP_DRILL_ENTITY, ctx -> new MachineRenderer<>("models/deep_drill_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.DRONE_PORT_ENTITY, ctx -> new MachineRenderer<>("models/drone_port_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.PUMP_BLOCK, ctx -> new MachineRenderer<>("models/pump_block"));
        BlockEntityRendererFactories.register(BlockEntitiesContent.PLACER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.FERTILIZER_BLOCK_ENTITY, ctx -> new MachineGantryRenderer());
        BlockEntityRendererFactories.register(BlockEntitiesContent.SMALL_TANK_ENTITY, ctx -> new SmallTankRenderer());
        
        BlockEntityRendererFactories.register(BlockEntitiesContent.TECH_DOOR_ENTITY, ctx -> new MachineRenderer<>("models/tech_door"));
        
        // cutout renders
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.MACHINE_FRAME_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.FRAME_GANTRY_ARM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_PLACER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_DESTROYER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.BLOCK_FERTILIZER_HEAD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.MACHINE_FLUID_ADDON, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.CROP_FILTER_ADDON, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.LARGE_STORAGE_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.FERTILIZER_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.PLACER_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.DESTROYER_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlockContent.SMALL_TANK_BLOCK, RenderLayer.getCutout());
        
        // fluids
        FluidRenderHandlerRegistry.INSTANCE.register(FluidContent.STILL_OIL, FluidContent.FLOWING_OIL, new SimpleFluidRenderHandler(
          new Identifier("oritech:block/fluid/fluid_gas_dark"),
          new Identifier("oritech:block/fluid/fluid_gas_dark"),
          0x7a7a7a
        ));
        FluidRenderHandlerRegistry.INSTANCE.register(FluidContent.STILL_FUEL, FluidContent.FLOWING_FUEL, new SimpleFluidRenderHandler(
          new Identifier("oritech:block/fluid/fluid_strange_pale_2"),
          new Identifier("oritech:block/fluid/fluid_strange_pale_2"),
          0x2D3D48
        ));
        
        BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), FluidContent.STILL_OIL, FluidContent.FLOWING_OIL);

        Oritech.LOGGER.info("Registering Entities Renderers for " + Oritech.MOD_ID);
    }

}
