package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.block.Block;
import net.minecraft.block.CropBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.data.client.*;
import net.minecraft.item.Item;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;

import java.util.HashSet;
import java.util.Set;

public class ModelGenerator extends FabricModelProvider {
    
    public static Set<Item> autoRegisteredModels = new HashSet<>();
    
    public ModelGenerator(FabricDataOutput output) {
        super(output);
    }

    public static TextureMap stairsOrientable(Block block, Block baseBlock) {
        return new TextureMap().put(TextureKey.FRONT, TextureMap.getSubId(block, "_front")).put(TextureKey.SIDE, TextureMap.getSubId(block, "_side")).put(TextureKey.TOP, TextureMap.getSubId(block, "_top")).put(TextureKey.BACK, TextureMap.getId(baseBlock)).put(TextureKey.BOTTOM, TextureMap.getId(baseBlock));
    }
    
    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        
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
        blockStateModelGenerator.registerSimpleState(BlockContent.CREATIVE_STORAGE_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.SMALL_TANK_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.CREATIVE_TANK_BLOCK);
        
        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(BlockContent.PLACER_BLOCK);
        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(BlockContent.DESTROYER_BLOCK);
        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(BlockContent.FERTILIZER_BLOCK);
        
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.PUMP_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.ADDON_INDICATOR_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BLOCK_DESTROYER_HEAD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BLOCK_PLACER_HEAD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BLOCK_FERTILIZER_HEAD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.PUMP_TRUNK_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.QUARRY_BEAM_INNER);
        blockStateModelGenerator.registerSimpleState(BlockContent.QUARRY_BEAM_TARGET);
        blockStateModelGenerator.registerSimpleState(BlockContent.QUARRY_BEAM_RING);
        
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
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.TECH_DOOR, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.TECH_DOOR_HINGE, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.TREEFELLER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.STEAM_ENGINE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_1);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_2);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_3);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_4);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_5);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_6);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_7);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_EXTENDER);
        
        //arcane
        blockStateModelGenerator.registerSimpleState(BlockContent.SPAWNER_CAGE_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.SPAWNER_CONTROLLER_BLOCK);
        blockStateModelGenerator.registerCrop(BlockContent.WITHER_CROP_BLOCK, CropBlock.AGE, 0, 0, 1, 2, 3, 3, 4, 5);
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.ENCHANTER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);   // uses geckolib
        blockStateModelGenerator.registerStateWithModelReference(BlockContent.ENCHANTMENT_CATALYST_BLOCK, BlockContent.MACHINE_SPEED_ADDON);   // uses geckolib
        
        // particle accelerator
        blockStateModelGenerator.registerSimpleState(BlockContent.ACCELERATOR_RING);
        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(BlockContent.ACCELERATOR_MOTOR);
        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(BlockContent.ACCELERATOR_CONTROLLER);
        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(BlockContent.ACCELERATOR_SENSOR);
        blockStateModelGenerator.registerSimpleState(BlockContent.BLACK_HOLE_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.BLACK_HOLE_INNER);
        blockStateModelGenerator.registerSimpleState(BlockContent.BLACK_HOLE_MIDDLE);
        blockStateModelGenerator.registerSimpleState(BlockContent.BLACK_HOLE_OUTER);
        
        // metals
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.NICKEL_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.DEEPSLATE_NICKEL_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.ENDSTONE_PLATINUM_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.DEEPSLATE_PLATINUM_ORE);
        
        // NODES
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_REDSTONE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_LAPIS);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_IRON);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_COAL);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_GOLD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_EMERALD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_DIAMOND);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_COPPER);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_NICKEL);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.RESOURCE_NODE_PLATINUM);
        
        //decorative
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.INDUSTRIAL_GLASS_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.CAPACITOR_ADDON_EXTENDER);
        blockStateModelGenerator.registerSimpleState(BlockContent.METAL_BEAM_BLOCK);
        blockStateModelGenerator.blockStateCollector.accept(createWallMountedState(BlockContent.CEILING_LIGHT));
        blockStateModelGenerator.blockStateCollector.accept(createWallMountedState(BlockContent.CEILING_LIGHT_HANGING));
        blockStateModelGenerator.blockStateCollector.accept(createButtonBlockState(BlockContent.TECH_BUTTON, Identifier.of(Oritech.MOD_ID, "block/tech_button"), Identifier.of(Oritech.MOD_ID, "block/tech_button_on")));
        registerLever(BlockContent.TECH_LEVER, blockStateModelGenerator);
        
        var machinePlatingPool = blockStateModelGenerator.registerCubeAllModelTexturePool(BlockContent.MACHINE_PLATING_BLOCK);
        machinePlatingPool.stairs(BlockContent.MACHINE_PLATING_STAIRS);
        machinePlatingPool.slab(BlockContent.MACHINE_PLATING_SLAB);
        machinePlatingPool.pressurePlate(BlockContent.MACHINE_PLATING_PRESSURE_PLATE);
        
        var ironPlatingPool = blockStateModelGenerator.registerCubeAllModelTexturePool(BlockContent.IRON_PLATING_BLOCK);
        ironPlatingPool.stairs(BlockContent.IRON_PLATING_STAIRS);
        ironPlatingPool.slab(BlockContent.IRON_PLATING_SLAB);
        ironPlatingPool.pressurePlate(BlockContent.IRON_PLATING_PRESSURE_PLATE);
        
        var nickelPlatingPool = blockStateModelGenerator.registerCubeAllModelTexturePool(BlockContent.NICKEL_PLATING_BLOCK);
        nickelPlatingPool.stairs(BlockContent.NICKEL_PLATING_STAIRS);
        nickelPlatingPool.slab(BlockContent.NICKEL_PLATING_SLAB);
        nickelPlatingPool.pressurePlate(BlockContent.NICKEL_PLATING_PRESSURE_PLATE);
        
        
    }
    
    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        
        itemModelGenerator.register(ToolsContent.EXO_HELMET, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.EXO_CHESTPLATE, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.EXO_LEGGINGS, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.EXO_BOOTS, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.CHAINSAW, Models.GENERATED);
        itemModelGenerator.register(ToolsContent.HAND_DRILL, Models.GENERATED);
        
        itemModelGenerator.register(ItemContent.ORITECH_GUIDE, Models.GENERATED);
        
        for (var item : autoRegisteredModels) {
            itemModelGenerator.register(item, Models.GENERATED);
        }
        
    }
    
    public static BlockStateSupplier createWallMountedState(Block block) {
        return VariantsBlockStateSupplier
                 .create(block, BlockStateVariant.create().put(VariantSettings.MODEL, ModelIds.getBlockModelId(block)))
                 .coordinate(BlockStateVariantMap.create(Properties.BLOCK_FACE, Properties.HORIZONTAL_FACING)
                               .register(BlockFace.FLOOR, Direction.EAST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90))
                               .register(BlockFace.FLOOR, Direction.WEST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270))
                               .register(BlockFace.FLOOR, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180))
                               .register(BlockFace.FLOOR, Direction.NORTH, BlockStateVariant.create())
                               .register(BlockFace.WALL, Direction.EAST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90).put(VariantSettings.X, VariantSettings.Rotation.R90))
                               .register(BlockFace.WALL, Direction.WEST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270).put(VariantSettings.X, VariantSettings.Rotation.R90))
                               .register(BlockFace.WALL, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180).put(VariantSettings.X, VariantSettings.Rotation.R90))
                               .register(BlockFace.WALL, Direction.NORTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R90))
                               .register(BlockFace.CEILING, Direction.EAST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270).put(VariantSettings.X, VariantSettings.Rotation.R180))
                               .register(BlockFace.CEILING, Direction.WEST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90).put(VariantSettings.X, VariantSettings.Rotation.R180))
                               .register(BlockFace.CEILING, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R180))
                               .register(BlockFace.CEILING, Direction.NORTH, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180).put(VariantSettings.X, VariantSettings.Rotation.R180)));
    }
    
    // same as original method in BlockStateModelGenerator but without uvlock
    public static BlockStateSupplier createButtonBlockState(Block buttonBlock, Identifier regularModelId, Identifier pressedModelId) {
        return VariantsBlockStateSupplier.create(buttonBlock)
                 .coordinate(BlockStateVariantMap.create(Properties.POWERED)
                               .register(false,
                                 BlockStateVariant.create().put(VariantSettings.MODEL, regularModelId)).register(true,
                     BlockStateVariant.create().put(VariantSettings.MODEL, pressedModelId)))
                 .coordinate(BlockStateVariantMap.create(Properties.BLOCK_FACE, Properties.HORIZONTAL_FACING)
                               .register(BlockFace.FLOOR, Direction.EAST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90))
                               .register(BlockFace.FLOOR, Direction.WEST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270))
                               .register(BlockFace.FLOOR, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180))
                               .register(BlockFace.FLOOR, Direction.NORTH, BlockStateVariant.create())
                               .register(BlockFace.WALL, Direction.EAST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90).put(VariantSettings.X, VariantSettings.Rotation.R90))
                               .register(BlockFace.WALL, Direction.WEST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270).put(VariantSettings.X, VariantSettings.Rotation.R90))
                               .register(BlockFace.WALL, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180).put(VariantSettings.X, VariantSettings.Rotation.R90))
                               .register(BlockFace.WALL, Direction.NORTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R90))
                               .register(BlockFace.CEILING, Direction.EAST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270).put(VariantSettings.X, VariantSettings.Rotation.R180))
                               .register(BlockFace.CEILING, Direction.WEST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90).put(VariantSettings.X, VariantSettings.Rotation.R180))
                               .register(BlockFace.CEILING, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R180))
                               .register(BlockFace.CEILING, Direction.NORTH, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180).put(VariantSettings.X, VariantSettings.Rotation.R180)));
    }
    
    // basically the original registerLever, but with parameters
    public static void registerLever(Block block, BlockStateModelGenerator generator) {
        Identifier identifier2 = ModelIds.getBlockModelId(block);
        Identifier identifier = ModelIds.getBlockSubModelId(block, "_on");
        generator.registerItemModel(block);
        generator.blockStateCollector.accept(
          VariantsBlockStateSupplier.create(block)
            .coordinate(BlockStateModelGenerator.createBooleanModelMap(Properties.POWERED, identifier, identifier2))
            .coordinate(BlockStateVariantMap.create(Properties.BLOCK_FACE, Properties.HORIZONTAL_FACING)
                          .register(BlockFace.CEILING, Direction.NORTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R180).put(VariantSettings.Y, VariantSettings.Rotation.R180))
                          .register(BlockFace.CEILING, Direction.EAST, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R180).put(VariantSettings.Y, VariantSettings.Rotation.R270))
                          .register(BlockFace.CEILING, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R180))
                          .register(BlockFace.CEILING, Direction.WEST, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R180).put(VariantSettings.Y, VariantSettings.Rotation.R90))
                          .register(BlockFace.FLOOR, Direction.NORTH, BlockStateVariant.create())
                          .register(BlockFace.FLOOR, Direction.EAST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90))
                          .register(BlockFace.FLOOR, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180))
                          .register(BlockFace.FLOOR, Direction.WEST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270))
                          .register(BlockFace.WALL, Direction.NORTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R90))
                          .register(BlockFace.WALL, Direction.EAST, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R90).put(VariantSettings.Y, VariantSettings.Rotation.R90))
                          .register(BlockFace.WALL, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R90).put(VariantSettings.Y, VariantSettings.Rotation.R180))
                          .register(BlockFace.WALL, Direction.WEST, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R90).put(VariantSettings.Y, VariantSettings.Rotation.R270))));
    }
}
