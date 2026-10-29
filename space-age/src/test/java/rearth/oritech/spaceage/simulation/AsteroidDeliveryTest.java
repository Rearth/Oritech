package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.AsteroidDeliveries.Job;
import rearth.oritech.spaceage.simulation.SpaceObjects.AsteroidMaterial;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsteroidDeliveryTest {

    @Test
    void shapesUseExactVolumeAndHaveIrregularElongatedProfiles() {

        for (var volume : List.of(1, 3, 60, 1_000, 50_000, 250_000)) {
            var shape = AsteroidShape.columns(volume, 753);
            assertEquals(volume, shape.stream().mapToInt(AsteroidShape.Column::height).sum());
            assertEquals(0, shape.stream().mapToInt(AsteroidShape.Column::bottom).min().orElseThrow());
            assertEquals(shape, AsteroidShape.columns(volume, 753));
            var footprints = new HashSet<Long>();
            shape.forEach(column -> assertTrue(footprints.add((long) column.x() << 32 ^ column.z() & 0xffffffffL)));
            if (volume < 1_000) continue;
            assertTrue(shape.stream().mapToInt(AsteroidShape.Column::x).summaryStatistics().getMax()
                    > shape.stream().mapToInt(AsteroidShape.Column::z).summaryStatistics().getMax());
            assertTrue(shape.stream().map(AsteroidShape.Column::height).distinct().count() > 8);
            assertNotEquals(shape, AsteroidShape.columns(volume, 754));
        }
    }

    @Test
    void interruptedLargeDeliveryResumesWithoutRepeatedPositionsOrLostComposition() {

        var materials = List.of(new AsteroidMaterial(Identifier.parse("minecraft:stone"), 200_000),
                new AsteroidMaterial(DenseOres.Family.IRON.id(), 30_000),
                new AsteroidMaterial(Identifier.parse("minecraft:sculk"), 20_000));
        var job = new Job(new BlockPos(0, 100, 0), 37, materials, materials, 0, 0, 0);
        var positions = new HashSet<BlockPos>();
        var counts = new HashMap<Identifier, Integer>();
        for (var placed = 0; placed < 250_000; placed++) {
            assertFalse(job.finished());
            if (placed % 20_000 == 5_000) {
                var pos = job.position();
                var material = job.materialAt(pos);
                var encoded = Job.CODEC.encodeStart(NbtOps.INSTANCE, job).getOrThrow();
                job = Job.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
                assertEquals(pos, job.position());
                assertEquals(material, job.materialAt(pos));
            }
            var pos = job.position();
            assertTrue(positions.add(pos), "No already-placed coordinate may be revisited after reload");
            var selected = job.materialAt(pos);
            counts.merge(job.materials.get(selected).block(), 1, Integer::sum);
            job.placed(selected);
        }
        assertTrue(job.finished());
        for (var material : materials) assertEquals(material.amount(), counts.get(material.block()));
        assertTrue(job.remaining().stream().allMatch(material -> material.amount() == 0));
    }

    @Test
    void controlledClearanceAndCraterBottomUseDifferentReferences() {

        assertEquals(48, AsteroidDeliveries.landingBottom(48, null));
        assertEquals(103, AsteroidDeliveries.landingBottom(64, 100));
        assertEquals(120, AsteroidDeliveries.landingBottom(120, 100));
    }

    @Test
    void highAltitudeDepositsFlattenWithoutLosingVolume() {

        for (var limit : List.of(1, 10, 30)) {
            var shape = AsteroidShape.columns(50_000, 721, limit);
            assertEquals(50_000, shape.stream().mapToInt(AsteroidShape.Column::height).sum());
            assertTrue(shape.stream().allMatch(column -> column.bottom() >= 0 && column.bottom() + column.height() <= limit));
        }
    }

    @Test
    void densityDoesNotMultiplyPhysicalVolumeOrMiningLoot() {

        var stone = Identifier.parse("minecraft:stone");
        var materials = List.of(new AsteroidMaterial(stone, 80), new AsteroidMaterial(DenseOres.Family.IRON.id(), 20));
        assertEquals(100, materials.stream().mapToInt(AsteroidMaterial::amount).sum());
        assertEquals(160, DenseOres.mass(materials));
        assertEquals(100, AsteroidShape.columns(100, 1).stream().mapToInt(AsteroidShape.Column::height).sum());
        for (var host : List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore"))
            assertEquals(DenseOres.Family.IRON.id(), DenseOres.denseVariant(Identifier.parse(host)));
        assertEquals(DenseOres.Family.GOLD.id(), DenseOres.denseVariant(Identifier.parse("minecraft:nether_gold_ore")));
        assertEquals(DenseOres.Family.PLATINUM.id(), DenseOres.denseVariant(Identifier.parse("oritech:endstone_platinum_ore")));
        assertEquals(stone, DenseOres.denseVariant(stone));
        assertEquals(Identifier.parse("minecraft:sculk"), DenseOres.denseVariant(Identifier.parse("minecraft:sculk")));
    }
}
