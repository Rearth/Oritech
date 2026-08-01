package rearth.oritech.spaceage.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.List;
import java.util.Set;

public class SpaceAgeBlockLootProvider extends BlockLootSubProvider {

    private static final List<Block> BLOCKS = List.of(
            SpaceAgeBlocks.ROCKET_ASSEMBLER.get(),
            SpaceAgeBlocks.ROCKET_PAD.get(),
            SpaceAgeBlocks.ROCKET_ENGINE_TIER_1.get(),
            SpaceAgeBlocks.ROCKET_ENGINE_TIER_2.get(),
            SpaceAgeBlocks.ROCKET_ENGINE_TIER_3.get()
    );

    public SpaceAgeBlockLootProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, provider);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BLOCKS;
    }

    @Override
    protected void generate() {
        BLOCKS.forEach(this::dropSelf);
    }
}
