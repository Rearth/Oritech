package rearth.oritech.init.datamap;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import rearth.oritech.Oritech;

public class DataMapContent {

    // attaches a quality multiplier to blocks that the unstable container can capture
    // data files are located at <namespace>/data_maps/block/unstable_container_source.json
    public static final DataMapType<Block, UnstableContainerSource> UNSTABLE_CONTAINER_SOURCE = DataMapType.builder(
            Oritech.id("unstable_container_source"),
            Registries.BLOCK,
            UnstableContainerSource.CODEC
    ).synced(UnstableContainerSource.CODEC, false).build();

    public static void registerDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(UNSTABLE_CONTAINER_SOURCE);
    }
}


