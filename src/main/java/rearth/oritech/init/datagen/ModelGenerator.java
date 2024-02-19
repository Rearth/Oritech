package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.block.Block;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;

public class ModelGenerator extends FabricModelProvider {
    public ModelGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.BANANA_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.PULVERIZER_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.GRINDER_BLOCK);
        blockStateModelGenerator.registerSimpleState(BlockContent.ASSEMBLER_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_CORE);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_SPEED_ADDON);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_EFFICIENCY_ADDON);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_INVENTORY_PROXY_ADDON);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.MACHINE_EXTENDER);
        blockStateModelGenerator.registerSimpleCubeAll(BlockContent.ADDON_INDICATOR_BLOCK);
        
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ItemContent.BANANA, Models.GENERATED);
    }
}
