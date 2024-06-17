package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryWrapper;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BlockLootGenerator extends FabricBlockLootTableProvider {
    
    public static Set<Block> autoRegisteredDrops = new HashSet<>();
    
    protected BlockLootGenerator(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
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
        
        addDrop(BlockContent.ITEM_PIPE_CONNECTION, BlockContent.ITEM_PIPE);
        addDrop(BlockContent.FLUID_PIPE_CONNECTION, BlockContent.FLUID_PIPE);
        addDrop(BlockContent.ENERGY_PIPE_CONNECTION, BlockContent.ENERGY_PIPE);
    }
    
    private void addOreDrop(Block block, Item item) {
        addDrop(block, oreDrops(block, item));
    }
}
