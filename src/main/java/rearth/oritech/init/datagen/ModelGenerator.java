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
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.PLACER_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.DESTROYER_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.FERTILIZER_BLOCK);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.ADDON_INDICATOR_BLOCK);
        
        blockStateModelGenerator.registerSimpleState(BlockContent.PULVERIZER_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.GRINDER_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.ASSEMBLER_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.POWERED_FURNACE_BLOCK);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_BASIC);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE_GOOD);
        
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_EXTENDER);
        
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ItemContent.BANANA, Models.GENERATED);
    }
}
