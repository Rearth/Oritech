package rearth.oritech.spaceage.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import rearth.oritech.spaceage.OritechSpaceAge;

public final class SpaceAgeDataMaps {

    /**
     * Chemical payload energy in joules; entries may reference block tags.
     */
    public static final DataMapType<Block, Double> ROCKET_EXPLOSIVES = DataMapType.builder(
                    OritechSpaceAge.id("rocket_explosives"), Registries.BLOCK, Codec.DOUBLE)
            .synced(Codec.DOUBLE, false).build();

    private SpaceAgeDataMaps() {
    }

    public static void register(RegisterDataMapTypesEvent event) {

        event.register(ROCKET_EXPLOSIVES);
    }
}
