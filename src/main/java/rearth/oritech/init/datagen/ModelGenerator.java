package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;
import net.minecraft.item.Item;
import rearth.oritech.init.BlockContent;
import rearth.oritech.item.tools.ToolsContent;

import java.util.HashSet;
import java.util.Set;

public class ModelGenerator extends FabricModelProvider {
    
    public static Set<Item> autoRegisteredModels = new HashSet<>();
    
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
        blockStateModelGenerator.registerSimpleState(BlockContent.SMALL_TANK_BLOCK);
        
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
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_1);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_2);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_3);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_4);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_5);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_6);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_7);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_EXTENDER);
        
        // metals
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.NICKEL_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.DEEPSLATE_NICKEL_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.ENDSTONE_PLATINUM_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.DEEPSLATE_PLATINUM_ORE);
        
        // NODES
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_REDSTONE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_LAPIS);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_IRON);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_GOLD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_EMERALD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_DIAMOND);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_COPPER);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_NICKEL);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_PLATINUM);
        
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        
        itemModelGenerator.register(ToolsContent.EXO_HELMET, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.EXO_CHESTPLATE, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.EXO_LEGGINGS, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.EXO_BOOTS, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.CHAINSAW, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.HAND_DRILL, Models.GENERATED);
        // itemModelGenerator.registerArmor((ArmorItem) ToolsContent.EXO_HELMET);   // this seems to generate it compatible with armor trims
        
        for (var item : autoRegisteredModels) {
            itemModelGenerator.register(item, Models.GENERATED);
        }
        
    }
}
