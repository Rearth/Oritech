package rearth.oritech.spaceage.datagen.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.concurrent.CompletableFuture;

public class SpaceAgeBlockTagProvider extends BlockTagsProvider {

    public SpaceAgeBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, OritechSpaceAge.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                SpaceAgeBlocks.ROCKET_ASSEMBLER.get(),
                SpaceAgeBlocks.ROCKET_PAD.get(),
                SpaceAgeBlocks.ROCKET_ENGINE_TIER_1.get(),
                SpaceAgeBlocks.ROCKET_ENGINE_TIER_2.get(),
                SpaceAgeBlocks.ROCKET_ENGINE_TIER_3.get()
        );

        tag(BlockTags.NEEDS_IRON_TOOL).add(
                SpaceAgeBlocks.ROCKET_ASSEMBLER.get(),
                SpaceAgeBlocks.ROCKET_PAD.get(),
                SpaceAgeBlocks.ROCKET_ENGINE_TIER_1.get(),
                SpaceAgeBlocks.ROCKET_ENGINE_TIER_2.get(),
                SpaceAgeBlocks.ROCKET_ENGINE_TIER_3.get()
        );
    }
}
