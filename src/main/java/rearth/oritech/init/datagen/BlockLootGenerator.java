package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;

import java.util.HashSet;
import java.util.Set;

public class BlockLootGenerator extends FabricBlockLootTableProvider {
    
    public static Set<Block> autoRegisteredDrops = new HashSet<>();
    
    public BlockLootGenerator(FabricDataOutput dataOutput) {
        super(dataOutput);
    }
    
    @Override
    public void generate() {
        addOreDrop(BlockContent.NICKEL_ORE, ItemContent.RAW_NICKEL);
        addOreDrop(BlockContent.DEEPSLATE_NICKEL_ORE, ItemContent.RAW_NICKEL);
        addOreDrop(BlockContent.DEEPSLATE_PLATINUM_ORE, ItemContent.RAW_PLATINUM);
        addOreDrop(BlockContent.ENDSTONE_PLATINUM_ORE, ItemContent.RAW_PLATINUM);
        
        for (var block : autoRegisteredDrops) {
            addDrop(block);
        }
    }
    
    private void addOreDrop(Block block, Item item) {
        addDrop(block, oreDrops(block, item));
    }
}
