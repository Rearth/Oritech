package rearth.oritech.spaceage.simulation.power;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.mission.MissionController;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings.HardwareRef;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Controlled hardware isolates network integration from the separate real-block/datamap GameTests. */
class EventPowerNetworkTest {

    private static final UUID OWNER = new UUID(0, 1);

    @TestFactory
    Stream<DynamicTest> differentConstellationsAndBatteryPhasesConservePower() {

        var tests = new ArrayList<DynamicTest>();
        for (var topology : List.of("chain", "merge", "ring", "tail_into_ring", "islands", "long_chain", "large_merge", "large_ring")) {
            for (var reserve : List.of(0, 20, 100)) {
                for (var charge : List.of("empty", "mixed", "full")) {
                    tests.add(DynamicTest.dynamicTest(topology + " / reserve " + reserve + " / " + charge,
                            () -> verify(topology, reserve, charge)));
                }
            }
        }
        return tests.stream();
    }

    @TestFactory
    Stream<DynamicTest> inputEventsMatchFineSteppingAcrossDifferentNetworks() {

        return Stream.iterate(0, i -> i + 1).limit(48).map(seed -> DynamicTest.dynamicTest("event sequence " + seed, () -> {
            var coarse = network(seed);
            var fine = network(seed);
            var previous = 0d;
            for (var time : new double[]{.125, 1, 2.75, 5, 11.5, 20, 33, 50, 75, 100, 160, 250}) {
                coarse.power.advance(time);
                for (var tick = previous + .125; tick < time; tick += .125) {
                    fine.power.update(null, fine, tick);
                }
                fine.power.update(null, fine, time);
                compare(coarse, fine, time);
                mutate(coarse, seed, time);
                mutate(fine, seed, time);
                coarse.power.update(null, coarse, time);
                fine.power.update(null, fine, time);
                compare(coarse, fine, time);
                previous = time;
            }
            coarse.power.advance(1_000_000);
            fine.power.advance(1_000_000);
            compare(coarse, fine, 1_000_000);
        }));
    }

    @Test
    void fullyCoveredRelayChainsAreLosslessAndUnpoweredCyclesStayEmpty() {

        var data = new MissionSavedData();
        var states = new ArrayList<MissionState>();
        for (int i = 0; i < 12; i++) {
            var station = station(new ControlledRocket(i, 0, 10_000_000, i == 0 ? 1000 : 0, 1));
            states.add(station);
            data.craft.put(station.rocket.getRocketId(), station);
        }
        for (int i = 0; i < states.size() - 1; i++) program(states.get(i), states.get(i + 1), 0);
        data.power.update(null, data, 100_000);
        data.power.advance(100_200);
        for (int i = 0; i < states.size() - 1; i++) {
            var report = states.get(i).powerReport;
            assertEquals(1000 - (i + 1) * 10, report.sent(), 1e-6);
            assertEquals(report.sent(), report.accepted(), 1e-6, "Only each relay's actual antenna service consumes RF");
            assertEquals(1, report.outgoing().coverage(), 1e-12);
        }
        var loop = new MissionSavedData();
        for (int i = 0; i < 8; i++) {
            var station = station(new ControlledRocket(i, 0, 10_000_000, 0, 1));
            loop.craft.put(station.rocket.getRocketId(), station);
        }
        var nodes = List.copyOf(loop.craft.values());
        for (int i = 0; i < nodes.size(); i++) program(nodes.get(i), nodes.get((i + 1) % nodes.size()), 0);
        loop.power.update(null, loop, 0);
        loop.power.advance(1_000_000);
        assertEquals(0, energy(loop));
        nodes.forEach(station -> assertEquals(0, station.powerReport.sent()));
    }

    @Test
    void emptyBatteryOnlySendsSolarSurplusAndTelemetryUsesTheSameInstant() {

        var data = new MissionSavedData();
        var source = station(new ControlledRocket(0, 1000, 211_000_000, 118, 1));
        var target = station(new ControlledRocket(1, 0, 211_000_000, 0, 1));
        ((ControlledRocket) source.rocket).power = new SpacePowerHardware(118, 1083, 3, 4d);
        program(source, target, 0);
        data.craft.put(source.rocket.getRocketId(), source);
        data.craft.put(target.rocket.getRocketId(), target);
        data.power.update(null, data, 0);
        assertEquals(-975, source.powerReport.net(), 1e-7, "Battery burst is valid only while stored RF remains");
        data.power.advance(2);
        var rf = source.rocket.getDynamicSegments().values().iterator().next();
        assertEquals(0, rf.rf(), 1e-7);
        assertEquals(108, source.powerReport.sent(), 1e-7);
        assertEquals(0, source.powerReport.net(), 1e-7);
        for (var snapshot : List.of(source.summary(2), source.telemetry(2))) {
            var encoded = MissionState.Telemetry.CODEC.encodeStart(JsonOps.INSTANCE, snapshot).getOrThrow();
            var restored = MissionState.Telemetry.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
            assertEquals(0, restored.rocket().getDynamicSegments().values().iterator().next().rf(), 1e-7);
            assertEquals(108, restored.power().sent(), 1e-7);
            assertEquals(0, restored.power().net(), 1e-7);
        }
        var oldPlan = source.plan;
        source.connected = true;
        var replacement = FlightPlan.empty();
        replacement = replacement.withBranches(List.of(replacement.root().withActions(List.of(
                FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH)))));
        assertFalse(MissionController.replace(source, replacement, new SpaceSimulation()), "An empty beaming station cannot accept an impossible departure");
        assertEquals(oldPlan, source.plan);
        data.power.advance(1_000_000);
        assertEquals(0, rf.rf(), 1e-7);
        assertEquals(108, source.powerReport.sent(), 1e-7);
    }

    @Test
    void independentSegmentsDrainInOrderAndCopyReloadKeepFractionalCharge() {

        var data = new MissionSavedData();
        var source = station(new ControlledRocket(0, 13, 100, 0, 1));
        var extra = new UUID(2, 0);
        var original = source.rocket.getOriginalSegments().values().iterator().next();
        var blocks = new HashSet<>(original.blocks());
        blocks.add(new StaticRocketSegment.BlockData(new BlockPos(0, 1, 0), SpaceAgeBlocks.ANTENNA.get().defaultBlockState()));
        var first = new StaticRocketSegment(original.segmentId(), blocks, Map.of(), 1, 0, 13, 0);
        var second = new StaticRocketSegment(extra, Set.of(new StaticRocketSegment.BlockData(new BlockPos(2, 0, 0),
                Blocks.GLASS.defaultBlockState())), Map.of(), 1, 0, 30, 0);
        source.rocket = new ActiveRocketData(source.rocket.getRocketId(), Map.of(first.segmentId(), first, extra, second),
                Map.of(first.segmentId(), new DynamicRocketSegment(0, 13, 0, Set.of(), Map.of(), 0, 100),
                        extra, new DynamicRocketSegment(0, 30, 0, Set.of(), Map.of(), 0, 100)), null);
        data.craft.put(source.rocket.getRocketId(), source);
        data.power.update(null, data, 0);
        data.power.advance(1.375);
        assertEquals(29.25, energy(data), 1e-7);
        assertEquals(0, source.rocket.getDynamicSegments().get(source.rocket.getRocketId()).rf(), 1e-7);
        var encoded = ActiveRocketData.CODEC.encodeStart(JsonOps.INSTANCE, source.rocket).getOrThrow();
        var restored = ActiveRocketData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(29.25, restored.getDynamicSegments().values().stream().mapToDouble(DynamicRocketSegment::rf).sum(), 1e-7);
        data.power.advance(10);
        assertEquals(0, energy(data), 1e-7);
        assertEquals(0, source.powerReport.net(), 1e-7);
        assertEquals(Double.POSITIVE_INFINITY, data.power.nextEvent());
    }

    private static MissionSavedData network(int seed) {

        var data = new MissionSavedData();
        var random = new Random(seed);
        var count = 4 + seed % 9;
        for (int i = 0; i < count; i++) {
            var capacity = 100 + random.nextInt(5000);
            var rocket = new ControlledRocket(i, random.nextBoolean() ? 0 : capacity, capacity,
                    random.nextBoolean() ? random.nextDouble(200) : 0, .05 + random.nextDouble(.95));
            var state = station(rocket);
            data.craft.put(rocket.getRocketId(), state);
        }
        var states = List.copyOf(data.craft.values());
        for (var state : states) program(state, states.get(random.nextInt(count)), seed % 3 == 0 ? 0 : seed % 3 == 1 ? 20 : 100);
        data.power.update(null, data, 0);
        return data;
    }

    private static MissionState station(ControlledRocket rocket) {

        return new MissionState(OWNER, rocket, FlightPlan.empty(), new MissionState.Position(-3_000_000, 0, 0, 0,
                SpaceObjects.EARTH_ID, OrbitBand.LOW, -1, 1, FlightPlanAction.NO_TARGET,
                SegmentRef.of(rocket.getOriginalSegments().values().iterator().next())), new SurveyKnowledge(), BlockPos.ZERO);
    }

    private static void mutate(MissionSavedData data, int seed, double time) {

        var states = List.copyOf(data.craft.values());
        var source = states.get(seed % states.size());
        var segment = source.rocket.getDynamicSegments().values().iterator().next();
        var rocket = (ControlledRocket) source.rocket;
        if (time == .125) segment.spendRF(700);
        else if (time == 1) segment.receiveRF(123.25);
        else if (time == 2.75) rocket.power = new SpacePowerHardware(0, 123, 3, 4d * .1);
        else if (time == 5) {
            var p = source.position;
            source.position = new MissionState.Position(-1_500_000, 0, 0, 0, p.target(), p.orbit(), p.slot(), p.stage(), p.asteroid(), p.anchor());
        } else if (time == 11.5) rocket.power = new SpacePowerHardware(137.125, 750, 5, 4d);
        else if (time == 20) program(source, states.get((seed + 1) % states.size()), 20);
        else if (time == 33) source.plan = FlightPlan.empty();
        else if (time == 50) program(source, states.get((seed + 2) % states.size()), 0);
        else if (time == 75) source.ended = true;
        else if (time == 100) source.ended = false;
        else if (time == 160) segment.spendRF(segment.rf());
        else if (time == 250) data.craft.remove(source.rocket.getRocketId());
    }

    private static void compare(MissionSavedData coarse, MissionSavedData fine, double time) {

        assertEquals(fine.craft.keySet(), coarse.craft.keySet());
        for (var entry : coarse.craft.entrySet()) {
            var first = entry.getValue();
            var second = fine.craft.get(entry.getKey());
            assertEquals(second.rocket.getDynamicSegments().values().iterator().next().rf(),
                    first.rocket.getDynamicSegments().values().iterator().next().rf(), 1e-4, "Stepping changed RF at " + time);
            assertEquals(second.powerReport.sent(), first.powerReport.sent(), 1e-4, "Stepping changed transmission at " + time);
            assertEquals(second.powerReport.net(), first.powerReport.net(), 1e-4, "Stepping changed battery rate at " + time);
            var rf = first.rocket.getDynamicSegments().values().iterator().next().rf();
            var capacity = first.rocket.getDynamicSegments().values().iterator().next().rfCapacity;
            var rawNet = first.powerReport.solar() + first.powerReport.incoming() - first.powerReport.services() - first.powerReport.sent();
            assertEquals(rf >= capacity - 1e-5 ? Math.min(0, rawNet) : rawNet, first.powerReport.net(), 1e-4,
                    "Battery rate disagrees with generation and consumption at " + time);
            assertEquals(first.powerReport.outgoing().incident(), first.powerReport.outgoing().missed()
                    + first.powerReport.outgoing().capacityLoss() + first.powerReport.outgoing().storageLoss() + first.powerReport.accepted(), 1e-4);
            if (!first.ended && rf <= 1e-6) {
                assertTrue(first.powerReport.net() >= -1e-5, "Empty battery discharges at " + time + " rf=" + first.rocket.getDynamicSegments().values().iterator().next().rf() + " report=" + first.powerReport);
                assertTrue(first.powerReport.sent() + first.powerReport.services() <= first.powerReport.solar() + first.powerReport.incoming() + 1e-4);
            }
        }
        assertEquals(fine.power.revisions(), coarse.power.revisions(), "Repeated input checks must not add power events");
    }

    private static void verify(String topology, int reserve, String charge) {

        var count = topology.startsWith("large") || topology.equals("long_chain") ? 16 : 5;
        var data = new MissionSavedData();
        var states = new ArrayList<MissionState>();
        for (int i = 0; i < count; i++) {
            var capacity = 5000 + i * 100;
            var rf = charge.equals("empty") ? 0 : charge.equals("full") ? capacity : i % 2 == 0 ? capacity * reserve / 100 : capacity;
            var rocket = new ControlledRocket(i, rf, capacity, i == 0 ? 118 : 0, i % 3 == 0 ? 1 : .4);
            var state = new MissionState(OWNER, rocket, FlightPlan.empty(), new MissionState.Position(-3_000_000, 0, 0, 0,
                    SpaceObjects.EARTH_ID, OrbitBand.LOW, -1, 1, FlightPlanAction.NO_TARGET,
                    SegmentRef.of(rocket.getOriginalSegments().values().iterator().next())), new SurveyKnowledge(), BlockPos.ZERO);
            data.craft.put(rocket.getRocketId(), state);
            states.add(state);
        }
        for (int i = 0; i < count; i++) {
            var target = switch (topology) {
                case "ring", "large_ring" -> (i + 1) % count;
                case "merge", "large_merge" -> count - 1;
                case "tail_into_ring" -> i == count - 1 ? 2 : i + 1;
                case "islands" -> i % 2 == 0 && i + 1 < count ? i + 1 : i;
                default -> i + 1;
            };
            if (target >= count || target == i) continue;
            program(states.get(i), states.get(target), reserve);
        }
        data.power.update(null, data, 0);
        var previousTime = 0d;
        var previousEnergy = energy(data);
        for (var time : new double[]{0, .01, .5, 1, 1.5, 2.5, 5, 10, 20, 50, 100, 500, 1000}) {
            data.power.advance(time);
            var currentEnergy = energy(data);
            assertTrue(currentEnergy <= previousEnergy + 118 * (time - previousTime) + 1e-3,
                    "Network created more RF than its solar supply at " + time);
            for (var state : states) {
                var rf = state.rocket.getDynamicSegments().values().iterator().next().rf();
                var report = state.powerReport;
                assertTrue(Double.isFinite(rf) && rf >= 0 && rf <= 6600, "Invalid battery at " + time);
                var rawNet = report.solar() + report.incoming() - report.services() - report.sent();
                var expectedNet = rf >= state.rocket.getDynamicSegments().values().iterator().next().rfCapacity - 1e-5 ? Math.min(0, rawNet) : rawNet;
                assertEquals(expectedNet, report.net(), 1e-3,
                        "Published rates disagree with battery change at " + time + " for " + state.rocket.getRocketId());
                if (rf < 1e-5) {
                    assertTrue(report.net() >= -1e-5, "An empty battery claims to be discharging at " + time);
                    assertTrue(report.sent() + report.services() <= report.solar() + report.incoming() + 1e-3,
                            "An empty craft sends more RF than it receives/generates at " + time);
                }
                assertTrue(report.accepted() <= report.delivered() + 1e-3, "Receiver accepts energy that was never transmitted");
            }
            previousTime = time;
            previousEnergy = currentEnergy;
        }
    }

    private static void program(MissionState source, MissionState target, int reserve) {

        var action = FlightPlanAction.create(ActionType.BEAM_POWER).withTarget(target.rocket.getRocketId());
        action = action.withSettings(action.settings().withBeam(new RocketServiceSettings.Beam(reserve)));
        source.plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(action))), List.of());
    }

    private static double energy(MissionSavedData data) {

        return data.craft.values().stream().flatMap(state -> state.rocket.getDynamicSegments().values().stream()).mapToDouble(DynamicRocketSegment::rf).sum();
    }

    private static final class ControlledRocket extends ActiveRocketData {

        private SpacePowerHardware power;
        private final UUID segment;

        private ControlledRocket(int index, long rf, long capacity, double generation, double capture) {

            this(new UUID(1, index), rf, capacity, generation, capture);
        }

        private ControlledRocket(UUID id, long rf, long capacity, double generation, double capture) {

            super(id, Map.of(id, new StaticRocketSegment(id, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                    Blocks.GLASS.defaultBlockState())), Map.of(), 1, 0, rf, 0)),
                    Map.of(id, new DynamicRocketSegment(0, rf, 0, Set.of(), Map.of(), 0, capacity)), null);
            segment = id;
            power = new SpacePowerHardware(generation, 1000 + id.getLeastSignificantBits() * 23, 3, 4d * capture);
        }

        @Override
        public SpacePowerHardware powerHardware() {

            return power;
        }

        @Override
        public Map<UUID, RocketHardware> hardware() {

            return Map.of(segment, new RocketHardware(0, 0, 0, 1));
        }

        @Override
        public List<HardwareRef> processingModules() {

            return List.of();
        }
    }
}
