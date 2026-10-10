package rearth.oritech.spaceage.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.DataMapProvider;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.simulation.SpaceBalance;

import java.util.concurrent.CompletableFuture;

public final class SpaceAgeDataMapProvider extends DataMapProvider {

    public SpaceAgeDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {

        super(output, registries);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {

        var explosives = builder(SpaceAgeDataMaps.ROCKET_EXPLOSIVES);
        explosives.add(Blocks.TNT.builtInRegistryHolder(), SpaceBalance.TNT_ENERGY_JOULES, false);
        explosives.add(BlockContent.LOW_YIELD_NUCLEAR_EXPLOSION_DEVICE.getId(), SpaceBalance.TNT_ENERGY_JOULES * 100, false);
        explosives.add(BlockContent.MANHATTAN_MODULE.getId(), SpaceBalance.TNT_ENERGY_JOULES * 1000, false);
    }
}
