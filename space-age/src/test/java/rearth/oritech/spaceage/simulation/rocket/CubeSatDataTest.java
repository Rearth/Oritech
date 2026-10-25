package rearth.oritech.spaceage.simulation.rocket;

import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeSatDataTest {

    @BeforeAll
    static void bootstrap() {

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static ActiveRocketData rocket(long fuel, String controllerName, boolean scanner) {

        var engine = UUID.randomUUID();
        var top = UUID.randomUUID();
        var coupling = new StaticRocketSegment.CouplingData(new BlockPos(0, 1, 0), new BlockPos(0, 2, 0));
        var lower = new StaticRocketSegment(engine, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState())), Map.of(top, Set.of(coupling)), 4, 1);
        var upper = new StaticRocketSegment(top, Set.of(new StaticRocketSegment.BlockData(new BlockPos(0, 2, 0),
                (scanner ? SpaceAgeBlocks.SPACE_SCANNER : SpaceAgeBlocks.ANTENNA).get().defaultBlockState())), Map.of(engine, Set.of(coupling)), 4, 0);
        var upperResources = new DynamicRocketSegment(0, 5_000, 0, Set.of(engine));
        upperResources.crafters.put(new BlockPos(0, 2, 0), new RocketControllerState(controllerName, Optional.empty()));
        return new ActiveRocketData(Map.of(engine, lower, top, upper), Map.of(
                engine, new DynamicRocketSegment(fuel, 0, 1, Set.of(top)), top, upperResources));
    }

    @Test
    void fingerprintOnlyDependsOnTheBlockLayout() {

        var cube = CubeSatData.of(rocket(1_000, "a", false));
        assertEquals(cube.fingerprint(), CubeSatData.of(rocket(50, "b", false)).fingerprint(), "fuel and names are not part of the layout");
        assertNotEquals(cube.fingerprint(), CubeSatData.of(rocket(1_000, "a", true)).fingerprint(), "a different block is a different layout");
    }

    @Test
    void codecKeepsContentsAndResources() {

        var cube = CubeSatData.of(rocket(1_000, "probe", false)).withName("Probe");
        var decoded = CubeSatData.CODEC.parse(JsonOps.INSTANCE, CubeSatData.CODEC.encodeStart(JsonOps.INSTANCE, cube).getOrThrow()).getOrThrow();
        assertEquals(cube, decoded);
        var unfolded = decoded.unfold(UUID.randomUUID(), null);
        assertEquals(2, unfolded.getStaticSegments().size());
        assertEquals(1_000, unfolded.getDynamicSegments().values().stream().mapToLong(segment -> segment.availableFuelBurnTimeTicks).sum());
        assertTrue(unfolded.getDynamicSegments().values().stream().anyMatch(segment -> segment.crafters.values().stream()
                .anyMatch(controller -> controller.name.equals("probe"))));
        assertTrue(unfolded.getDynamicSegments().values().stream().allMatch(segment -> segment.getConnectedSegments().size() == 1));
        assertEquals(3, decoded.blockCount());
        assertEquals(3, decoded.size().getY());
    }

    @Test
    void carriedCubesUseTheirOwnPlanReferences() {

        var cube = CubeSatData.of(rocket(1_000, "", false));
        var host = rocket(1_000, "", false);
        var hostRefs = host.getStaticSegments().values().stream().map(SegmentRef::of).collect(Collectors.toSet());
        var first = RocketPayloads.segments(new BlockPos(0, 3, 0), cube);
        var second = RocketPayloads.segments(new BlockPos(1, 3, 0), cube);
        assertEquals(2, first.size());
        assertTrue(first.stream().noneMatch(hostRefs::contains), "cube segments never collide with the host");
        assertTrue(first.stream().noneMatch(second::contains), "two identical cubes stay distinguishable");
        assertEquals(first, RocketPayloads.segments(new BlockPos(0, 3, 0), cube), "references are stable for a cell");
        var unfolded = cube.unfold(UUID.randomUUID(), null);
        assertEquals(cube.parts().stream().map(part -> part.structure().planAnchor()).collect(Collectors.toSet()),
                unfolded.getStaticSegments().values().stream().map(StaticRocketSegment::planAnchor).collect(Collectors.toSet()),
                "a launched cube keeps the anchors of its layout");
    }
}
