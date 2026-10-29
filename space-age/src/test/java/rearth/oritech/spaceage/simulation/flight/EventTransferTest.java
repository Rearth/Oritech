package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;

import java.util.LinkedHashMap;
import java.util.HashMap;

import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTransferTest {

    private Map<?, ?> previousSolar;

    @BeforeEach
    void solarData() {

        var maps = ((BaseMappedRegistry<Block>) BuiltInRegistries.BLOCK).getDataMaps();
        previousSolar = maps.get(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS);
        maps.put(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS,
                Map.of(Blocks.DAYLIGHT_DETECTOR.builtInRegistryHolder().unwrapKey().orElseThrow(), 100d));
    }

    @AfterEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void restoreData() {

        var maps = ((BaseMappedRegistry<Block>) BuiltInRegistries.BLOCK).getDataMaps();
        if (previousSolar == null) maps.remove(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS);
        else maps.put(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS, (Map) previousSolar);
    }

    @Test
    void smallGravityFieldsAreFoundBetweenGeometrySamples() {

        var body = new SpaceObjectData(SpaceSimulation.MARS_ID, SpaceObjects.ObjectType.MARS,
                1_250_000, 0, 100, 3.71f, SpaceObjects.DetectionState.PRECISE);
        var context = new RocketFlightPathState.Context(Map.of(body.id(), body), Map.of(), Map.of(), 1);
        var curve = FlightCurve.between(0, 0, 100_000_000, 0, 1, 0, 1, 0, 30_000_000);
        var cells = FlightEnvironment.along(curve, context, 0);
        assertTrue(cells.stream().anyMatch(cell -> cell.gravity() > .1));
        assertTrue(cells.stream().anyMatch(cell -> cell.gravity() < -.1));
        assertTrue(cells.stream().filter(cell -> cell.start() > 1_400_000).allMatch(cell -> cell.gravity() == 0));
    }

    @Test
    void fastInheritedSpeedCanBrakeWithWeakIonEnginesAndRadio() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                -3_000_000, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var mars = new SpaceObjectData(SpaceSimulation.MARS_ID, SpaceObjects.ObjectType.MARS,
                5_000_000, 1_500_000, 45_000, 3.71f, SpaceObjects.DetectionState.PRECISE);
        var hardware = new RocketHardware(0, 1, 0, 1);
        var segment = new RocketFlightPathState.Segment(1000, 0, 100, 0, 120_900, hardware);
        segment.rf = hardware.rfCost(segment.ionSeconds);
        var craft = new RocketFlightPathState.Craft(new LinkedHashMap<>(Map.of(new SegmentRef(BlockPos.ZERO), segment)),
                new HashMap<>(), -3_120_000, 0, 0);
        craft.branchId = new UUID(1, 1);
        craft.velocityX = 2500;
        var context = new RocketFlightPathState.Context(Map.of(earth.id(), earth, mars.id(), mars), Map.of(), Map.of(), 1);
        var action = new FlightPlanAction(new UUID(0, 803), ActionType.NAVIGATE_TO, List.of(), mars.id(), OrbitBand.HIGH,
                SpaceSimulation.ArrivalVelocityMode.ZERO, 0, 0, 0, 0, 0, 0, List.of());
        assertTrue(RocketFlightNavigation.navigate(action, craft, context), () -> "failure=" + craft.navigationFailure);
    }

    @Test
    void particleLaunchedIonCraftUsesVacuumThrustAndBrakesAcrossABroadSpeedRange() {

        var id = new UUID(900, 1);
        var blocks = new HashSet<StaticRocketSegment.BlockData>();
        for (int i = 0; i < 8; i++)
            blocks.add(new StaticRocketSegment.BlockData(new BlockPos(i, 0, 0),
                    SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()));
        blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.NAVIGATION_COMPUTER.get().defaultBlockState()));
        blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO.above(2), SpaceAgeBlocks.ANTENNA.get().defaultBlockState()));
        var structure = new StaticRocketSegment(id, blocks, Map.of(), 247, 8);
        var rocket = new ActiveRocketData(Map.of(id, structure), Map.of(id, new DynamicRocketSegment(0, 1_008_736, .33, Set.of())));
        rocket.setLaunchPosition(BlockPos.ZERO);
        var target = new SpaceObjectData(new UUID(901, 1), SpaceObjects.ObjectType.WAYPOINT,
                -500_000, 0, 0, 0, SpaceObjects.DetectionState.PRECISE);
        var objects = new ArrayList<>(new SpaceSimulation().createObjectData());
        objects.add(target);
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(target.id()).withOrbit(OrbitBand.SURFACE);
        var base = FlightPlan.empty();
        var plan = base.withBranches(List.of(base.root().withActions(List.of(action))));
        for (long particle : new long[]{5_000, 6_000, 9_000, 12_000, 15_000}) {
            var position = LaunchSpeedRules.position(objects, BlockPos.ZERO, particle);
            var path = RocketFlightPathCalculator.calculateFrom(rocket, objects, plan, position).paths().getFirst();
            assertEquals(TerminalState.READY, path.terminalState(), "particle=" + particle + " " + path.failure());
            assertEquals(0, path.samples().getLast().speedMetersPerSecond(), 1e-6);
            assertTrue(path.samples().stream().anyMatch(sample -> sample.phase() == PathPhase.BRAKE));
            assertTrue(path.samples().size() < 250, "One long transfer should need hundreds of events at most");
        }
        assertEquals(1_008_736, rocket.getDynamicSegments().get(id).rf(), "Trials must not consume real resources");
    }

    @Test
    void environmentalCellsKeepEarthGravityAndAtmosphereNearEarth() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                -3_000_000, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var context = new RocketFlightPathState.Context(Map.of(earth.id(), earth), Map.of(), Map.of(), 1);
        var curve = FlightCurve.between(-2_940_000, 0, -500_000, 0, 1, 0, 1, 0, 800_000);
        var cells = FlightEnvironment.along(curve, context, 0);
        assertTrue(cells.size() < 200);
        assertTrue(cells.getFirst().atmosphere());
        assertTrue(cells.stream().filter(cell -> cell.start() >= 10_000 + .001).noneMatch(FlightEnvironment.Cell::atmosphere));
        assertTrue(cells.stream().filter(cell -> cell.start() >= 60_000 + .001).allMatch(cell -> Math.abs(cell.gravity()) < 1e-12));
        var work = cells.stream().mapToDouble(cell -> cell.gravity() * (cell.end() - cell.start())).sum();
        assertEquals(-294_300, work, .1, "Local cells preserve total gravitational energy");
    }
}
