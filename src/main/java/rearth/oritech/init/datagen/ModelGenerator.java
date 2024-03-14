package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;

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
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.PLACER_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.DESTROYER_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.FERTILIZER_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.PUMP_BLOCK);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.ADDON_INDICATOR_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BLOCK_DESTROYER_HEAD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BLOCK_PLACER_HEAD);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BLOCK_FERTILIZER_HEAD);
        
        blockStateModelGenerator.registerSimpleState(BlockContent.PULVERIZER_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.GRINDER_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.ASSEMBLER_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.FOUNDRY_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.CENTRIFUGE_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.ATOMIC_FORGE_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.POWERED_FURNACE_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.LASER_ARM_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.TEST_GENERATOR_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.BASIC_GENERATOR_BLOCK);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_BASIC);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_GOOD);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_EXTENDER);
        
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ItemContent.BANANA, Models.GENERATED);
        itemModelGenerator.register(ItemContent.TARGET_DESIGNATOR, Models.GENERATED);
    }
}
