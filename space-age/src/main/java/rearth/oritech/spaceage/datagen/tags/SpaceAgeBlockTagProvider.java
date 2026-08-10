package rearth.oritech.spaceage.datagen.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.registries.DeferredBlock;
import rearth.oritech.util.RegistryReflectionUtil;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.concurrent.CompletableFuture;

public class SpaceAgeBlockTagProvider extends BlockTagsProvider {

    public SpaceAgeBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, OritechSpaceAge.MOD_ID);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addTags(HolderLookup.Provider provider) {
        var pickaxeTag = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        var ironToolTag = tag(BlockTags.NEEDS_IRON_TOOL);

        RegistryReflectionUtil.IterateFields(SpaceAgeBlocks.class, DeferredBlock.class, (field, identifier, value) -> {
            var block = ((DeferredBlock<? extends net.minecraft.world.level.block.Block>) value).get();
            pickaxeTag.add(block);
            ironToolTag.add(block);
        });
    }
}
