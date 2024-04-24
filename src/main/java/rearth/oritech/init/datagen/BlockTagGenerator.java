package rearth.oritech.init.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import rearth.oritech.init.BlockContent;

import java.util.concurrent.CompletableFuture;

public class BlockTagGenerator extends FabricTagProvider.BlockTagProvider {
    
    public BlockTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }
    
    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
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
    }
}
