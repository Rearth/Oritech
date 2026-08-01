package rearth.oritech.spaceage.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.RegistryReflectionUtil;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpaceAgeBlockLootProvider extends BlockLootSubProvider {

    public SpaceAgeBlockLootProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, provider);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return getAutoDropBlocks();
    }

    @Override
    protected void generate() {
        getAutoDropBlocks().forEach(this::dropSelf);
    }

    @SuppressWarnings("unchecked")
    private static List<Block> getAutoDropBlocks() {
        var blocks = new ArrayList<Block>();

        RegistryReflectionUtil.IterateFields(SpaceAgeBlocks.class, DeferredBlock.class, (field, identifier, value) -> {
            if (field.isAnnotationPresent(BlockContent.NoBlockItem.class)
                    || field.isAnnotationPresent(BlockContent.NoAutoDrop.class)) return;

            blocks.add(((DeferredBlock<? extends Block>) value).get());
        });

        return blocks;
    }
}
