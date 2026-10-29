package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Segment;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketControllerState;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment.BlockData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightResourcesTest {

    private static final SegmentRef REF = new SegmentRef(BlockPos.ZERO);

    private static Segment segment() {

        var structure = new StaticRocketSegment(new UUID(780, 1), Set.of(
                new BlockData(BlockPos.ZERO, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()),
                new BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState())),
                Map.of(), 10, 2, 1000, 200);
        var resources = new DynamicRocketSegment(200, 1000, 2, Set.of());
        resources.setResources(199.5, 999.25);
        return new Segment(structure, resources);
    }

    private static Craft craft(Segment segment) {

        return new Craft(new HashMap<>(Map.of(REF, segment)), new HashMap<>(), 0, 0, 0);
    }

    @Test
    void eventPlaybackUsesLiveResourceRulesAndMassImmediately() {

        var segment = segment();
        var craft = craft(segment);
        var live = segment.physical.copy();
        var events = List.of(new FlightResourceRate(40, 400), new FlightResourceRate(0, -20_000),
                new FlightResourceRate(4000, 20_000));
        for (var rate : events) {
            EventTransfer.apply(craft, Map.of(REF, rate), .05);
            rate.apply(live, .05);
            assertEquals(live.fuel(), segment.physical.fuel());
            assertEquals(live.rf(), segment.rf());
            assertEquals(10_000 + live.currentFuelWeight * SpaceBalance.KILOGRAMS_PER_WEIGHT_UNIT, craft.mass());
            assertEquals(segment.hardware.ionSeconds(live), segment.ionSeconds());
            assertEquals(segment.hardware.chemicalSeconds(live), segment.chemicalSeconds());
        }
        assertEquals(10_000, craft.mass());
    }

    @Test
    void chargedIonEnginesBecomeAvailableWithoutSynchronizingAnotherBudget() {

        var segment = segment();
        segment.physical.spendRF(segment.rf());
        segment.physical.consumeFuel(segment.physical.fuel());
        assertEquals(0, segment.thrust(false));
        segment.physical.receiveRF(800);
        assertEquals(2, segment.ionSeconds());
        assertEquals(SpaceBalance.ION_THRUST_NEWTONS, segment.thrust(false));
    }

    @Test
    void numericalTrialsOmitCargoAndBranchCopiesRetainIndependentCargo() {

        var segment = segment();
        segment.physical.layout.put(BlockPos.ZERO.above(2), Blocks.IRON_BLOCK.defaultBlockState());
        segment.physical.crafters.put(BlockPos.ZERO.above(3), new RocketControllerState());
        segment.refreshLayout();
        var craft = craft(segment);
        var trial = craft.copyForFlight().segments.get(REF);
        var branch = craft.copyFor(Set.of(REF)).segments.get(REF);
        assertTrue(trial.physical.layout.isEmpty());
        assertTrue(trial.physical.crafters.isEmpty());
        assertEquals(segment.currentStructure(), trial.currentStructure());
        assertEquals(segment.mass(), trial.mass());
        assertEquals(segment.physical.fuel(), trial.physical.fuel());
        assertFalse(branch.physical.layout.isEmpty());
        trial.physical.consumeFuel(100);
        branch.physical.crafters.get(BlockPos.ZERO.above(3)).name = "Child";
        assertEquals(199.5, segment.physical.fuel());
        assertEquals("", segment.physical.crafters.get(BlockPos.ZERO.above(3)).name);
    }

    @Test
    void layoutRefreshUpdatesEngineHardwareForTheNextLeg() {

        var segment = segment();
        segment.physical.layout.put(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        segment.refreshLayout();
        assertEquals(0, segment.hardware.chemical());
        assertEquals(SpaceBalance.ION_THRUST_NEWTONS, segment.thrust(false));
        assertEquals(199.5, segment.physical.fuel(), "Removing an engine does not burn its remaining fuel");
    }
}
