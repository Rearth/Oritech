package rearth.oritech.generator;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.core.Direction;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;

public class ModelGenerator extends FabricModelProvider {
    
    public ModelGenerator(FabricDataOutput output) {
        super(output);
    }
    
    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.MACHINE_FRAME_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FLUID_PIPE);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ENERGY_PIPE);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.SUPERCONDUCTOR);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ITEM_PIPE);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.TRANSPARENT_ITEM_PIPE);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FLUID_PIPE_CONNECTION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ENERGY_PIPE_CONNECTION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.SUPERCONDUCTOR_CONNECTION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ITEM_PIPE_CONNECTION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FRAMED_FLUID_PIPE);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FRAMED_ENERGY_PIPE);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FRAMED_SUPERCONDUCTOR);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FRAMED_ITEM_PIPE);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FRAMED_FLUID_PIPE_CONNECTION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FRAMED_ENERGY_PIPE_CONNECTION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FRAMED_ITEM_PIPE_CONNECTION);
        blockStateModelGenerator.createTrivialCube(BlockContent.FLUID_PIPE_DUCT_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.ENERGY_PIPE_DUCT_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.SUPERCONDUCTOR_DUCT_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.ITEM_PIPE_DUCT_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ITEM_FILTER_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.SIMPLE_AUGMENT_STATION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ADVANCED_AUGMENT_STATION);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ARCANE_AUGMENT_STATION);
        
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.SMALL_STORAGE_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.LARGE_STORAGE_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.CREATIVE_STORAGE_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.SMALL_TANK_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.CREATIVE_TANK_BLOCK);
        
        blockStateModelGenerator.createNonTemplateHorizontalBlock(BlockContent.PLACER_BLOCK);
        blockStateModelGenerator.createNonTemplateHorizontalBlock(BlockContent.DESTROYER_BLOCK);
        blockStateModelGenerator.createNonTemplateHorizontalBlock(BlockContent.FERTILIZER_BLOCK);
        
        blockStateModelGenerator.createNonTemplateHorizontalBlock(BlockContent.POWER_POLE_BLOCK);
        
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.PUMP_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.CHARGER_BLOCK);
        
        blockStateModelGenerator.createTrivialCube(BlockContent.ADDON_INDICATOR_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.BLOCK_DESTROYER_HEAD);
        blockStateModelGenerator.createTrivialCube(BlockContent.BLOCK_PLACER_HEAD);
        blockStateModelGenerator.createTrivialCube(BlockContent.BLOCK_FERTILIZER_HEAD);
        blockStateModelGenerator.createTrivialCube(BlockContent.PUMP_TRUNK_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.TANK_ITEM_MODEL);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.CREATIVE_TANK_ITEM_MODEL);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.QUARRY_BEAM_INNER);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.QUARRY_BEAM_TARGET);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.QUARRY_BEAM_RING);
        
        // reactor section
        blockStateModelGenerator.createNonTemplateHorizontalBlock(BlockContent.REACTOR_CONTROLLER);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_ENERGY_PORT);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_REDSTONE_PORT);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_FUEL_PORT);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_ABSORBER_PORT);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_ROD);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_DOUBLE_ROD);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_QUAD_ROD);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_WALL, Blocks.BRICKS); // this is overridden by athena
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_VENT);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_REFLECTOR);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_HEAT_PIPE);
        blockStateModelGenerator.createTrivialCube(BlockContent.REACTOR_CONDENSER);
        
        blockStateModelGenerator.createAmethystCluster(BlockContent.URANIUM_CRYSTAL);
        
        blockStateModelGenerator.createTrivialCube(BlockContent.LOW_YIELD_NUKE);
        blockStateModelGenerator.createTrivialCube(BlockContent.NUKE);
        
        blockStateModelGenerator.createTrivialCube(BlockContent.REACTOR_COLD_INDICATOR_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.REACTOR_MEDIUM_INDICATOR_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.REACTOR_HOT_INDICATOR_BLOCK);
        
        // these blocks all use geckolib to render/display, so the only thing this really adds are block particles (e.g. when breaking)
        // the machine speed addon has a generic particle references that fits all machines well enough
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.PULVERIZER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FRAGMENT_FORGE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ASSEMBLER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FOUNDRY_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REFINERY_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REFINERY_MODULE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.COOLER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.CENTRIFUGE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ATOMIC_FORGE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.POWERED_FURNACE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.AUGMENT_APPLICATION_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.LASER_ARM_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.BIO_GENERATOR_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.FUEL_GENERATOR_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.BASIC_GENERATOR_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.LAVA_GENERATOR_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.BIG_SOLAR_PANEL_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.DEEP_DRILL_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.DRONE_PORT_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.SHRINKER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.TECH_DOOR, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.TECH_DOOR_HINGE, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.TREEFELLER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.STEAM_ENGINE_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.PIPE_BOOSTER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.PARTICLE_COLLECTOR_BLOCK, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_EXPLOSION_SMALL, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_EXPLOSION_MEDIUM, BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.REACTOR_EXPLOSION_LARGE, BlockContent.MACHINE_SPEED_ADDON);
        
        for (var fluid : FluidContent.FLUID_ATTRIBUTES)
            blockStateModelGenerator.createNonTemplateModelBlock(fluid.getBlock(), Blocks.WATER);
        
        blockStateModelGenerator.createTrivialCube(BlockContent.MACHINE_CORE_1);
        blockStateModelGenerator.createTrivialCube(BlockContent.MACHINE_CORE_2);
        blockStateModelGenerator.createTrivialCube(BlockContent.MACHINE_CORE_3);
        blockStateModelGenerator.createTrivialCube(BlockContent.MACHINE_CORE_4);
        blockStateModelGenerator.createTrivialCube(BlockContent.MACHINE_CORE_5);
        blockStateModelGenerator.createTrivialCube(BlockContent.MACHINE_CORE_6);
        blockStateModelGenerator.createTrivialCube(BlockContent.MACHINE_CORE_7);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.MACHINE_CORE_HIDDEN, Blocks.AIR);   // never visible
        
        blockStateModelGenerator.createTrivialCube(BlockContent.MACHINE_EXTENDER);
        
        //arcane
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.SPAWNER_CAGE_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.SPAWNER_CONTROLLER_BLOCK);
        blockStateModelGenerator.createCropBlock(BlockContent.WITHER_CROP_BLOCK, CropBlock.AGE, 0, 1, 1, 2, 3, 3, 4, 5);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ENCHANTER_BLOCK, BlockContent.MACHINE_SPEED_ADDON);   // uses geckolib
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.UNSTABLE_CONTAINER, BlockContent.MACHINE_SPEED_ADDON);   // uses geckolib
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ENCHANTMENT_CATALYST_BLOCK, BlockContent.MACHINE_SPEED_ADDON);   // uses geckolib
        
        // particle accelerator
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.ACCELERATOR_RING);
        blockStateModelGenerator.createNonTemplateHorizontalBlock(BlockContent.ACCELERATOR_MOTOR);
        blockStateModelGenerator.createNonTemplateHorizontalBlock(BlockContent.ACCELERATOR_CONTROLLER);
        blockStateModelGenerator.createNonTemplateHorizontalBlock(BlockContent.ACCELERATOR_SENSOR);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.BLACK_HOLE_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.BLACK_HOLE_INNER);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.BLACK_HOLE_MIDDLE);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.BLACK_HOLE_OUTER);
        
        // metals
        blockStateModelGenerator.createTrivialCube(BlockContent.NICKEL_ORE);
        blockStateModelGenerator.createTrivialCube(BlockContent.DEEPSLATE_NICKEL_ORE);
        blockStateModelGenerator.createTrivialCube(BlockContent.ENDSTONE_PLATINUM_ORE);
        blockStateModelGenerator.createTrivialCube(BlockContent.DEEPSLATE_PLATINUM_ORE);
        blockStateModelGenerator.createTrivialCube(BlockContent.DEEPSLATE_URANIUM_ORE);
        
        // NODES
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_REDSTONE);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_LAPIS);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_IRON);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_COAL);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_GOLD);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_EMERALD);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_DIAMOND);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_COPPER);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_NICKEL);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_PLATINUM);
        blockStateModelGenerator.createTrivialCube(BlockContent.RESOURCE_NODE_URANIUM);
        
        //decorative
        blockStateModelGenerator.createTrivialCube(BlockContent.INDUSTRIAL_GLASS_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.CAPACITOR_ADDON_EXTENDER);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.METAL_BEAM_BLOCK);
        blockStateModelGenerator.createNonTemplateModelBlock(BlockContent.METAL_GIRDER_BLOCK);
        blockStateModelGenerator.blockStateOutput.accept(createWallMountedState(BlockContent.CEILING_LIGHT));
        blockStateModelGenerator.blockStateOutput.accept(createWallMountedState(BlockContent.CEILING_LIGHT_HANGING));
        blockStateModelGenerator.blockStateOutput.accept(createButtonBlockState(BlockContent.TECH_BUTTON, ResourceLocation.fromNamespaceAndPath(Oritech.MOD_ID, "block/tech_button"), ResourceLocation.fromNamespaceAndPath(Oritech.MOD_ID, "block/tech_button_on")));
        registerLever(BlockContent.TECH_LEVER, blockStateModelGenerator);
        
        blockStateModelGenerator.createTrivialCube(BlockContent.STEEL_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.ENERGITE_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.NICKEL_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.BIOSTEEL_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.PLATINUM_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.ADAMANT_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.ELECTRUM_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.DURATIUM_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.BIOMASS_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.PLASTIC_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.FLUXITE_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.SILICON_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.RAW_NICKEL_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.RAW_PLATINUM_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.RAW_URANIUM_BLOCK);
        blockStateModelGenerator.createTrivialCube(BlockContent.URANIUM_DUST_BLOCK);
        
        var machinePlatingPool = blockStateModelGenerator.family(BlockContent.MACHINE_PLATING_BLOCK);
        machinePlatingPool.stairs(BlockContent.MACHINE_PLATING_STAIRS);
        machinePlatingPool.slab(BlockContent.MACHINE_PLATING_SLAB);
        machinePlatingPool.pressurePlate(BlockContent.MACHINE_PLATING_PRESSURE_PLATE);
        
        var ironPlatingPool = blockStateModelGenerator.family(BlockContent.IRON_PLATING_BLOCK);
        ironPlatingPool.stairs(BlockContent.IRON_PLATING_STAIRS);
        ironPlatingPool.slab(BlockContent.IRON_PLATING_SLAB);
        ironPlatingPool.pressurePlate(BlockContent.IRON_PLATING_PRESSURE_PLATE);
        
        var nickelPlatingPool = blockStateModelGenerator.family(BlockContent.NICKEL_PLATING_BLOCK);
        nickelPlatingPool.stairs(BlockContent.NICKEL_PLATING_STAIRS);
        nickelPlatingPool.slab(BlockContent.NICKEL_PLATING_SLAB);
        nickelPlatingPool.pressurePlate(BlockContent.NICKEL_PLATING_PRESSURE_PLATE);
        
        var carbonPlatingPool = blockStateModelGenerator.family(BlockContent.CARBON_PLATING_BLOCK);
        carbonPlatingPool.stairs(BlockContent.CARBON_PLATING_STAIRS);
        carbonPlatingPool.slab(BlockContent.CARBON_PLATING_SLAB);
        carbonPlatingPool.pressurePlate(BlockContent.CARBON_PLATING_PRESSURE_PLATE);
        
    }
    
    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_HELMET, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_CHESTPLATE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_LEGGINGS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_BOOTS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.CHAINSAW, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.HAND_DRILL, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.ELECTRIC_MACE, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.JETPACK, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.EXO_JETPACK, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.JETPACK_ELYTRA, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ToolsContent.JETPACK_EXO_ELYTRA, ModelTemplates.FLAT_ITEM);
        
        for (var fluid : FluidContent.FLUID_ATTRIBUTES)
            itemModelGenerator.generateFlatItem(fluid.getBucketItem(), ModelTemplates.FLAT_ITEM);
        
        for (var item : ItemContent.autoRegisteredModels) {
            itemModelGenerator.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
        }
        
    }
    
    public static BlockStateGenerator createWallMountedState(Block block) {
        return MultiVariantGenerator
                 .multiVariant(block, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(block)))
                 .with(PropertyDispatch.properties(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                               .select(AttachFace.FLOOR, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.FLOOR, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                               .select(AttachFace.FLOOR, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                               .select(AttachFace.FLOOR, Direction.NORTH, Variant.variant())
                               .select(AttachFace.WALL, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.WALL, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.WALL, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.WALL, Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.CEILING, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                               .select(AttachFace.CEILING, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                               .select(AttachFace.CEILING, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                               .select(AttachFace.CEILING, Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)));
    }
    
    // same as original method in BlockStateModelGenerator but without uvlock
    public static BlockStateGenerator createButtonBlockState(Block buttonBlock, ResourceLocation regularModelId, ResourceLocation pressedModelId) {
        return MultiVariantGenerator.multiVariant(buttonBlock)
                 .with(PropertyDispatch.property(BlockStateProperties.POWERED)
                               .select(false,
                                 Variant.variant().with(VariantProperties.MODEL, regularModelId)).select(true,
                     Variant.variant().with(VariantProperties.MODEL, pressedModelId)))
                 .with(PropertyDispatch.properties(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                               .select(AttachFace.FLOOR, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.FLOOR, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                               .select(AttachFace.FLOOR, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                               .select(AttachFace.FLOOR, Direction.NORTH, Variant.variant())
                               .select(AttachFace.WALL, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.WALL, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.WALL, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.WALL, Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                               .select(AttachFace.CEILING, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                               .select(AttachFace.CEILING, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                               .select(AttachFace.CEILING, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                               .select(AttachFace.CEILING, Direction.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)));
    }
    
    // basically the original registerLever, but with parameters
    public static void registerLever(Block block, BlockModelGenerators generator) {
        ResourceLocation identifier2 = ModelLocationUtils.getModelLocation(block);
        ResourceLocation identifier = ModelLocationUtils.getModelLocation(block, "_on");
        generator.createSimpleFlatItemModel(block);
        generator.blockStateOutput.accept(
          MultiVariantGenerator.multiVariant(block)
            .with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.POWERED, identifier, identifier2))
            .with(PropertyDispatch.properties(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                          .select(AttachFace.CEILING, Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                          .select(AttachFace.CEILING, Direction.EAST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                          .select(AttachFace.CEILING, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
                          .select(AttachFace.CEILING, Direction.WEST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                          .select(AttachFace.FLOOR, Direction.NORTH, Variant.variant())
                          .select(AttachFace.FLOOR, Direction.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                          .select(AttachFace.FLOOR, Direction.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                          .select(AttachFace.FLOOR, Direction.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))
                          .select(AttachFace.WALL, Direction.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                          .select(AttachFace.WALL, Direction.EAST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90))
                          .select(AttachFace.WALL, Direction.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))
                          .select(AttachFace.WALL, Direction.WEST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))));
    }
}
