package rearth.oritech.spaceage.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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

    public static final DataMapType<Block, Double> SPACE_SOLAR_GENERATORS = DataMapType.builder(
            OritechSpaceAge.id("space_solar_generators"), Registries.BLOCK, Codec.DOUBLE).synced(Codec.DOUBLE, false).build();
    public static final DataMapType<Block, Double> BEAM_APERTURE_MATERIALS = DataMapType.builder(
            OritechSpaceAge.id("beam_aperture_materials"), Registries.BLOCK, Codec.DOUBLE).synced(Codec.DOUBLE, false).build();

    public static final DataMapType<Block, BlockState> BEAM_SCORCHING = DataMapType.builder(
                    OritechSpaceAge.id("beam_scorching"), Registries.BLOCK, BlockState.CODEC)
            .synced(BlockState.CODEC, false).build();
    public static final DataMapType<Block, BlockState> BEAM_MELTING = DataMapType.builder(
                    OritechSpaceAge.id("beam_melting"), Registries.BLOCK, BlockState.CODEC)
            .synced(BlockState.CODEC, false).build();

    private SpaceAgeDataMaps() {
    }

    public static void register(RegisterDataMapTypesEvent event) {

        event.register(ROCKET_EXPLOSIVES);
        event.register(SPACE_SOLAR_GENERATORS);
        event.register(BEAM_APERTURE_MATERIALS);
        event.register(BEAM_SCORCHING);
        event.register(BEAM_MELTING);
    }
}
