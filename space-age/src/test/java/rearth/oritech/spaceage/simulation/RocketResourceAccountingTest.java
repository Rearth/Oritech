package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import net.minecraft.core.BlockPos;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import java.util.Map;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Segment;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddon;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketResourceAccountingTest {

    @Test
    void portableBatteriesGiveIonCraftUsefulVacuumEndurance() {

        // One 250 kN engine, three default 1 MRF batteries, 20 tonnes including their mass.
        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState())), Map.of(), 20, 1, 3_000_000, 0);
        var resources = new DynamicRocketSegment(0, 3_000_000, 0, Set.of());
        var rocket = new ActiveRocketData(Map.of(id, segment), Map.of(id, resources));
        var performance = RocketPerformanceCalculator.calculate(rocket);
        assertEquals(250_000, performance.thrustNewtons());
        assertEquals(1_500, performance.availableBurnSeconds());
        assertEquals(18_750, performance.availableDeltaVMetersPerSecond());
        var hardware = RocketHardware.of(segment);
        hardware.burn(resources, 1_500);
        assertEquals(0, resources.rf(), 1e-6);
        assertEquals(0, hardware.ionSeconds(resources));
        // Additional engines improve acceleration but share the same total stored RF impulse.
        assertEquals(750, new RocketHardware(0, 2, 0, 0).ionSeconds(new DynamicRocketSegment(0, 3_000_000, 0, Set.of())));
    }

    @Test
    void tinyBurnsMatchOneContinuousBurnAndPreviewIncludingReload() {

        var hardware = new RocketHardware(2, 3, 0, 0);
        var continuous = new DynamicRocketSegment(1_000, 10_000_000, 50, Set.of());
        var live = continuous.copy();
        var preview = new Segment(100, 20, 30, hardware.chemicalSeconds(live), hardware.ionSeconds(live), hardware);
        preview.fuelMass = 50;
        preview.fuelTicks = live.fuel();
        preview.rf = live.rf();
        var seconds = .000037;
        for (int i = 0; i < 10_000; i++) {
            hardware.burn(live, seconds);
            preview.consume(seconds);
            if (i == 4_999) live = DynamicRocketSegment.CODEC.parse(JsonOps.INSTANCE,
                    DynamicRocketSegment.CODEC.encodeStart(JsonOps.INSTANCE, live).getOrThrow()).getOrThrow();
        }
        hardware.burn(continuous, seconds * 10_000);
        assertEquals(continuous.fuel(), live.fuel(), 1e-7);
        assertEquals(continuous.rf(), live.rf(), 1e-4);
        assertEquals(continuous.currentFuelWeight, live.currentFuelWeight, 1e-7);
        assertEquals(preview.fuelTicks, live.fuel(), 1e-7);
        assertEquals(preview.rf, live.rf(), 1e-4);
        assertEquals(preview.fuelMass, live.currentFuelWeight, 1e-7);
        assertEquals(live.fuel(), live.copy().fuel());
        assertEquals(live.rf(), live.copy().rf());
        assertTrue(live.fuel() < 1_000, "sub-tick burns must still consume fuel");
    }

    @Test
    void fractionalRemaindersDepleteWithoutNegativeResourcesOrMass() {

        var resources = new DynamicRocketSegment(1, 1, 10, Set.of());
        resources.consumeFuel(.3);
        resources.spendRF(.7);
        assertEquals(.7, resources.fuel(), 1e-12);
        assertEquals(.3, resources.rf(), 1e-12);
        assertEquals(7, resources.currentFuelWeight, 1e-12);
        resources.consumeFuel(2);
        resources.spendRF(2);
        assertEquals(0, resources.fuel());
        assertEquals(0, resources.rf());
        assertEquals(0, resources.currentFuelWeight);
    }

    @Test
    void stationKeepingEstimateAndLiveThresholdUseTheSameBudgets() {

        var hardware = new RocketHardware(1, 0, 0, 1);
        var budget = new StationServiceRules.Budget(hardware, 100, 1_000, 100, 1_000);
        var lowFuel = new ActionAddon(new UUID(1, 1), ActionAddonType.LOW_FUEL, 25);
        var estimate = StationServiceRules.estimate(List.of(budget), .01, lowFuel);
        var expectedFuelSeconds = 75 / .2;
        var expectedRFSeconds = 1_000 / StationServiceRules.rfPerSecond(hardware, .01);
        assertEquals(Math.min(expectedFuelSeconds, expectedRFSeconds), estimate.seconds(), 1e-6);
        var chemicalOnly = new RocketHardware(1, 0, 0, 0);
        budget = new StationServiceRules.Budget(chemicalOnly, 100, 0, 100, 0);
        estimate = StationServiceRules.estimate(List.of(budget), .01, lowFuel);
        assertEquals(expectedFuelSeconds, estimate.seconds(), 1e-6);
        assertTrue(estimate.conditionReached());
        assertFalse(StationServiceRules.conditionReached(List.of(budget), lowFuel, .01, expectedFuelSeconds - .05));
        var live = new DynamicRocketSegment(100, 0, 10, Set.of());
        for (int i = 0; i < 375; i++) chemicalOnly.burn(live, .01);
        var current = new StationServiceRules.Budget(chemicalOnly, live.fuel(), live.rf(), 100, 0);
        assertEquals(25, live.fuel(), 1e-9);
        assertTrue(StationServiceRules.conditionReached(List.of(current), lowFuel, 0, 0));
    }
}
