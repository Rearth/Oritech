package rearth.oritech.spaceage.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DataMapProvider;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.simulation.SpaceBalance;

import java.util.concurrent.CompletableFuture;

public final class SpaceAgeDataMapProvider extends DataMapProvider {

    public SpaceAgeDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {

        super(output, registries);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {

        builder(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS).add(BlockContent.BIG_SOLAR_PANEL.getId(), 128.0, false);
        var glass = builder(SpaceAgeDataMaps.BEAM_APERTURE_MATERIALS);
        glass.add(Tags.Blocks.GLASS_BLOCKS_CHEAP, 1.0, false);
        glass.add(BlockContent.INDUSTRIAL_GLASS.getId(), 1.25, false);
        glass.add(SpaceAgeBlocks.CRYSTAL_GLASS.getId(), 4.0, false);
        var scorching = builder(SpaceAgeDataMaps.BEAM_SCORCHING);
        scorching.add(Blocks.GRASS_BLOCK.builtInRegistryHolder(), Blocks.DIRT.defaultBlockState(), false);
        scorching.add(Blocks.DIRT.builtInRegistryHolder(), Blocks.COARSE_DIRT.defaultBlockState(), false);
        scorching.add(Blocks.SAND.builtInRegistryHolder(), Blocks.GLASS.defaultBlockState(), false);
        scorching.add(Blocks.STONE.builtInRegistryHolder(), Blocks.BLACKSTONE.defaultBlockState(), false);
        var explosives = builder(SpaceAgeDataMaps.ROCKET_EXPLOSIVES);
        explosives.add(Blocks.TNT.builtInRegistryHolder(), SpaceBalance.TNT_ENERGY_JOULES, false);
        explosives.add(BlockContent.LOW_YIELD_NUCLEAR_EXPLOSION_DEVICE.getId(), SpaceBalance.TNT_ENERGY_JOULES * 100, false);
        explosives.add(BlockContent.MANHATTAN_MODULE.getId(), SpaceBalance.TNT_ENERGY_JOULES * 1000, false);
    }
}
