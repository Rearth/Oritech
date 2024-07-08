package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.datagen.data.TagContent;

import java.util.concurrent.CompletableFuture;

public class BlockTagGenerator extends FabricTagProvider.BlockTagProvider {
    
    public BlockTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }
    
    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        
        var pickaxeBuilder = getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE);
        
        for (var block : BlockLootGenerator.autoRegisteredDrops) {
            pickaxeBuilder.add(block);
        }
        pickaxeBuilder.add(BlockContent.ENERGY_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.FLUID_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.ITEM_PIPE_CONNECTION);
        pickaxeBuilder.add(BlockContent.SMALL_TANK_BLOCK);
        pickaxeBuilder.add(BlockContent.SMALL_STORAGE_BLOCK);
        pickaxeBuilder.add(BlockContent.PUMP_TRUNK_BLOCK);
        
        pickaxeBuilder
          .add(BlockContent.NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE)
          .add(BlockContent.ENDSTONE_PLATINUM_ORE);
        
        getOrCreateTagBuilder(ConventionalBlockTags.ORES)
          .add(BlockContent.NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE)
          .add(BlockContent.ENDSTONE_PLATINUM_ORE);
        
        getOrCreateTagBuilder(BlockTags.NEEDS_STONE_TOOL)
          .add(BlockContent.NICKEL_ORE)
          .add(BlockContent.DEEPSLATE_NICKEL_ORE);
        
        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
          .add(BlockContent.DEEPSLATE_PLATINUM_ORE)
          .add(BlockContent.ENDSTONE_PLATINUM_ORE);
        
        getOrCreateTagBuilder(TagContent.DRILL_MINEABLE)
          .addOptionalTag(BlockTags.PICKAXE_MINEABLE)
          .addOptionalTag(BlockTags.SHOVEL_MINEABLE);
        
        getOrCreateTagBuilder(TagContent.RESOURCE_NODES)
          .add(BlockContent.RESOURCE_NODE_COPPER)
          .add(BlockContent.RESOURCE_NODE_IRON)
          .add(BlockContent.RESOURCE_NODE_NICKEL)
          .add(BlockContent.RESOURCE_NODE_GOLD)
          .add(BlockContent.RESOURCE_NODE_REDSTONE)
          .add(BlockContent.RESOURCE_NODE_LAPIS)
          .add(BlockContent.RESOURCE_NODE_EMERALD)
          .add(BlockContent.RESOURCE_NODE_DIAMOND)
          .add(BlockContent.RESOURCE_NODE_COAL)
          .add(BlockContent.RESOURCE_NODE_PLATINUM);
    }
}
