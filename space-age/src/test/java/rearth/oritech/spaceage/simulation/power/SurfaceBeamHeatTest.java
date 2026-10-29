package rearth.oritech.spaceage.simulation.power;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SurfaceBeamHeatTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void meltingRequiresHighHeatAndTakesPriorityOverScorching() {

        var maps = ((BaseMappedRegistry<Block>) BuiltInRegistries.BLOCK).getDataMaps();
        var previousScorching = maps.get(SpaceAgeDataMaps.BEAM_SCORCHING);
        var previousMelting = maps.get(SpaceAgeDataMaps.BEAM_MELTING);
        try {
            maps.put(SpaceAgeDataMaps.BEAM_SCORCHING, Map.of(
                    Blocks.STONE.builtInRegistryHolder().unwrapKey().orElseThrow(), Blocks.BLACKSTONE.defaultBlockState(),
                    Blocks.GRASS_BLOCK.builtInRegistryHolder().unwrapKey().orElseThrow(), Blocks.DIRT.defaultBlockState(),
                    Blocks.SAND.builtInRegistryHolder().unwrapKey().orElseThrow(), Blocks.GLASS.defaultBlockState()));
            maps.put(SpaceAgeDataMaps.BEAM_MELTING, Map.of(
                    Blocks.STONE.builtInRegistryHolder().unwrapKey().orElseThrow(), Blocks.LAVA.defaultBlockState(),
                    Blocks.BLACKSTONE.builtInRegistryHolder().unwrapKey().orElseThrow(), Blocks.LAVA.defaultBlockState(),
                    Blocks.WATER.builtInRegistryHolder().unwrapKey().orElseThrow(), Blocks.AIR.defaultBlockState(),
                    Blocks.ICE.builtInRegistryHolder().unwrapKey().orElseThrow(), Blocks.AIR.defaultBlockState()));

            assertNull(SurfaceBeamData.heatedState(Blocks.STONE.defaultBlockState(), 9.99));
            assertEquals(Blocks.BLACKSTONE.defaultBlockState(), SurfaceBeamData.heatedState(Blocks.STONE.defaultBlockState(), 24.99));
            assertEquals(Blocks.LAVA.defaultBlockState(), SurfaceBeamData.heatedState(Blocks.STONE.defaultBlockState(), 25));
            assertEquals(Blocks.LAVA.defaultBlockState(), SurfaceBeamData.heatedState(Blocks.BLACKSTONE.defaultBlockState(), 25));
            assertNull(SurfaceBeamData.heatedState(Blocks.WATER.defaultBlockState(), 24.99));
            assertEquals(Blocks.AIR.defaultBlockState(), SurfaceBeamData.heatedState(Blocks.WATER.defaultBlockState(), 25));
            assertEquals(Blocks.AIR.defaultBlockState(), SurfaceBeamData.heatedState(Blocks.ICE.defaultBlockState(), 25));
            assertEquals(Blocks.GLASS.defaultBlockState(), SurfaceBeamData.heatedState(Blocks.SAND.defaultBlockState(), 25),
                    "Unmapped melting materials still use their scorching conversion");
            assertNull(SurfaceBeamData.heatedState(Blocks.GRASS_BLOCK.defaultBlockState(), 4.99));
            assertEquals(Blocks.DIRT.defaultBlockState(), SurfaceBeamData.heatedState(Blocks.GRASS_BLOCK.defaultBlockState(), 5));
            assertNull(SurfaceBeamData.heatedState(Blocks.BEDROCK.defaultBlockState(), 100));
        } finally {
            if (previousScorching == null) maps.remove(SpaceAgeDataMaps.BEAM_SCORCHING);
            else maps.put(SpaceAgeDataMaps.BEAM_SCORCHING, previousScorching);
            if (previousMelting == null) maps.remove(SpaceAgeDataMaps.BEAM_MELTING);
            else maps.put(SpaceAgeDataMaps.BEAM_MELTING, previousMelting);
        }
    }
}
