package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.item.tools.ToolsContent;

public class ModelGenerator extends FabricModelProvider {
    public ModelGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BANANA_BLOCK);
        
        blockStateModelGenerator.registerSimpleState(BlockContent.MACHINE_FRAME_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.FLUID_PIPE);
        blockStateModelGenerator.registerSimpleState(BlockContent.ENERGY_PIPE);
        blockStateModelGenerator.registerSimpleState(BlockContent.ITEM_PIPE);
        blockStateModelGenerator.registerSimpleState(BlockContent.FLUID_PIPE_CONNECTION);
        blockStateModelGenerator.registerSimpleState(BlockContent.ENERGY_PIPE_CONNECTION);
        blockStateModelGenerator.registerSimpleState(BlockContent.ITEM_PIPE_CONNECTION);
        blockStateModelGenerator.registerSimpleState(BlockContent.ITEM_FILTER_BLOCK);
        
        blockStateModelGenerator.registerSimpleState(BlockContent.SMALL_STORAGE_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.LARGE_STORAGE_BLOCK);
        
        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(BlockContent.PLACER_BLOCK);
        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(BlockContent.DESTROYER_BLOCK);
        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(BlockContent.FERTILIZER_BLOCK);
        
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.PUMP_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.ADDON_INDICATOR_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BLOCK_DESTROYER_HEAD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BLOCK_PLACER_HEAD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BLOCK_FERTILIZER_HEAD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.PUMP_TRUNK_BLOCK);
        
        // these blocks all use geckolib to render/display, so the only thing this really adds are block particles (e.g. when breaking)
        // the machine speed addon has a generic particle references that fits all machines well enough
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.PULVERIZER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.FRAGMENT_FORGE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.ASSEMBLER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.FOUNDRY_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.CENTRIFUGE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.ATOMIC_FORGE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.POWERED_FURNACE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.LASER_ARM_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.BIO_GENERATOR_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.FUEL_GENERATOR_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.BASIC_GENERATOR_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.LAVA_GENERATOR_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.BIG_SOLAR_PANEL_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.DEEP_DRILL_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.DRONE_PORT_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_BASIC);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_GOOD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_EXTENDER);
        
        // metals
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.NICKEL_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.DEEPSLATE_NICKEL_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.ENDSTONE_PLATINUM_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.DEEPSLATE_PLATINUM_ORE);
        
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ItemContent.BANANA, Models.GENERATED);
        itemModelGenerator.register(ItemContent.TARGET_DESIGNATOR, Models.GENERATED);
        itemModelGenerator.register(ItemContent.TEST_ENERGY_ITEM, Models.GENERATED);
        itemModelGenerator.register(ItemContent.OIL_BUCKET, Models.GENERATED);
        
        
        itemModelGenerator.register(ToolsContent.EXO_HELMET, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.EXO_CHESTPLATE, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.EXO_LEGGINGS, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.EXO_BOOTS, Models.GENERATED);
        // itemModelGenerator.registerArmor((ArmorItem) ToolsContent.EXO_HELMET);   // this seems to generate it compatible with armor trims
        
        // nickel
        itemModelGenerator.register(ItemContent.NICKEL_INGOT, Models.GENERATED);
        itemModelGenerator.register(ItemContent.RAW_NICKEL, Models.GENERATED);
        itemModelGenerator.register(ItemContent.NICKEL_CLUMP, Models.GENERATED);
        itemModelGenerator.register(ItemContent.SMALL_NICKEL_CLUMP, Models.GENERATED);
        itemModelGenerator.register(ItemContent.NICKEL_DUST, Models.GENERATED);
        itemModelGenerator.register(ItemContent.SMALL_NICKEL_DUST, Models.GENERATED);
        itemModelGenerator.register(ItemContent.NICKEL_GEM, Models.GENERATED);
        itemModelGenerator.register(ItemContent.NICKEL_NUGGET, Models.GENERATED);
        // platinum
        itemModelGenerator.register(ItemContent.PLATINUM_INGOT, Models.GENERATED);
        itemModelGenerator.register(ItemContent.RAW_PLATINUM, Models.GENERATED);
        itemModelGenerator.register(ItemContent.PLATINUM_CLUMP, Models.GENERATED);
        itemModelGenerator.register(ItemContent.SMALL_PLATINUM_CLUMP, Models.GENERATED);
        itemModelGenerator.register(ItemContent.PLATINUM_DUST, Models.GENERATED);
        itemModelGenerator.register(ItemContent.SMALL_PLATINUM_DUST, Models.GENERATED);
        itemModelGenerator.register(ItemContent.PLATINUM_GEM, Models.GENERATED);
        itemModelGenerator.register(ItemContent.PLATINUM_NUGGET, Models.GENERATED);
        // fluxite
        itemModelGenerator.register(ItemContent.FLUXITE, Models.GENERATED);
    }
}
