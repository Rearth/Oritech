package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;

public class BlockLootGenerator extends FabricBlockLootTableProvider {
    
    public BlockLootGenerator(FabricDataOutput dataOutput) {
        super(dataOutput);
    }
    
    @Override
    public void generate() {
        addDrop(BlockContent.NICKEL_ORE, ItemContent.RAW_NICKEL);
        addDrop(BlockContent.DEEPSLATE_NICKEL_ORE, ItemContent.RAW_NICKEL);
        addDrop(BlockContent.DEEPSLATE_PLATINUM_ORE, ItemContent.RAW_PLATINUM);
        addDrop(BlockContent.ENDSTONE_PLATINUM_ORE, ItemContent.RAW_PLATINUM);
    }
}
