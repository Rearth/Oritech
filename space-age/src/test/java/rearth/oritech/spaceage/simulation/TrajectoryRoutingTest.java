package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.*;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.*;

class TrajectoryRoutingTest {
    private static final UUID BRANCH = new UUID(1, 1);
    private static SpaceObjectData earth(double radius) {
        return new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH, 0, 0, (float) radius, 0, SpaceObjects.DetectionState.PRECISE);
    }
    private static RocketFlightPathState.Craft craft(double x, double y) {
        var ref = new SegmentRef(BlockPos.ZERO);
        var segment = new RocketFlightPathState.Segment(100, 1000, 0, 100_000, 0, new RocketHardware(1, 0, 0, 0));
        var craft = new RocketFlightPathState.Craft(new LinkedHashMap<>(Map.of(ref, segment)), new HashMap<>(), x, y, 0);
        craft.branchId = BRANCH;
        craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
        return craft;
    }
    private static RocketFlightPathState.Context context(SpaceObjectData... objects) {
        var map = new HashMap<UUID, SpaceObjectData>();
        for (var object : objects) map.put(object.id(), object);
        return new RocketFlightPathState.Context(map, Map.of(), Map.of(), 1);
    }
    private static FlightPlanAction action(UUID target, int id) {
        return new FlightPlanAction(new UUID(0, id), ActionType.NAVIGATE_TO, List.of(), target, OrbitBand.SURFACE,
                ArrivalVelocityMode.ZERO, 0, 0, 0, 0, 0, 0, List.of());
    }
    @Test void coordinateMappingIsContinuousPeriodicAndIndependentOfTheCard() {
        assertEquals(SpaceBalance.surfaceAngle(10, 20), SpaceBalance.surfaceAngle(2010, 20), 1e-12);
        assertEquals(Math.PI * 2 / 2000, SpaceBalance.surfaceAngle(1, 0) - SpaceBalance.surfaceAngle(0, 0), 1e-12);
        assertEquals(Math.cos(SpaceBalance.surfaceAngle(-1, 0)), Math.cos(SpaceBalance.surfaceAngle(1999, 0)), 1e-12);
    }
    @Test void surfaceTripsHaveRadialCurveEndpointsWithoutPlanetAvoidance() {
        var earth = earth(60_000);
        for (int landing : new int[] {1, 10, 250, 1000, 1999}) {
            var craft = craft(60_000, 0); var context = context(earth);
            var action = action(earth.id(), 2).withLanding(landing, 0, 0, 0);
            var angle = SpaceBalance.surfaceAngle(landing, 0);
            var route = RocketTransferRoute.solve(craft, context, action, earth,
                    Math.cos(angle) * earth.radius(), Math.sin(angle) * earth.radius());
            assertNotNull(route);
            if (landing == 1000) {
                var middle = route.curve().atDistance(route.curve().length() * .5, 1);
                assertTrue(Math.abs(middle.y()) > 100, "opposite radial tangents must not fold into a straight line");
            }
            var departure = route.curve().atDistance(0, 1);
            var arrival = route.curve().atDistance(route.curve().length(), 1);
            assertEquals(1, departure.vx(), 1e-7); assertEquals(0, departure.vy(), 1e-7);
            assertEquals(-Math.cos(angle), arrival.vx(), 1e-7);
            assertEquals(-Math.sin(angle), arrival.vy(), 1e-7);
            assertTrue(RocketFlightNavigation.navigate(action, craft, context));
            assertEquals(Math.cos(angle) * earth.radius(), craft.x, .01);
            assertEquals(Math.sin(angle) * earth.radius(), craft.y, .01);
            assertTrue(craft.samples.size() < 210, "bounded shared curve samples");
        }
    }

    @Test void gravityMakesSurfaceAscentHarderThanFinalBraking() {
        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(60_000, 0);
        var context = context(earth);
        var action = action(earth.id(), 30).withOrbit(OrbitBand.LOW);
        var route = RocketTransferRoute.solve(craft, context, action, earth, 160_000, 0);
        assertNotNull(route);
        assertTrue(route.departureGravity() < -9.8);
        assertTrue(route.burns().getFirst().seconds() > route.burns().getLast().seconds() * 2,
                "gravity must lengthen outward acceleration and assist final braking");

        var weakProfile = new RocketBurnProfile(List.of(new RocketBurnProfile.Interval(9, 10_000)));
        assertNull(CurveTransfer.solve(100_000, 0, 0, false, Double.POSITIVE_INFINITY,
                weakProfile, 1, -9.81, -1), "9 m/s^2 cannot depart against 9.81 m/s^2 gravity");
    }
    @Test void stoppedDeparturesRememberTheirArrivalAndUseStableJitter() {
        var target = new SpaceObjectData(new UUID(20, 20), SpaceObjects.ObjectType.SURVEY_REGION,
                1_000_000, 0, 0, 0, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(0, 0);
        var context = context(earth(0), target);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 20), craft, context));
        assertEquals(0, Math.hypot(craft.velocityX, craft.velocityY), 1e-5);
        assertTrue(Math.hypot(craft.headingX, craft.headingY) > .99);
        var next = action(earth(0).id(), 21);
        var route = RocketTransferRoute.solve(craft, context, next, earth(0), 0, 0);
        var repeated = RocketTransferRoute.solve(craft, context, next, earth(0), 0, 0);
        var departure = route.curve().atDistance(0, 1);
        assertEquals(departure, repeated.curve().atDistance(0, 1));
        assertTrue(Math.abs(departure.vy()) > .05, "stopped return should separate from the arrival line");
        assertTrue(departure.vx() < -.93, "jitter remains slight");
        var stopped = MissionController.sample(craft.samples, craft.time + 1, MissionState.Position.surface());
        assertTrue(stopped.headingX() > .99, "arrival heading survives a tick past the stop");
        var position = new MissionState.Position(craft.x, craft.y, 0, 0, target.id(), OrbitBand.SURFACE,
                -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO), craft.headingX, craft.headingY);
        var encoded = MissionState.Position.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, position).getOrThrow();
        assertEquals(position, MissionState.Position.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow());
        var child = craft.copyFor(Set.copyOf(craft.segments.keySet()));
        assertEquals(craft.headingX, child.headingX);
    }

    @Test void maximumArrivalRetainsMomentumAndTheReturnPaysForItsTurn() {
        var earth = earth(0);
        var target = new SpaceObjectData(new UUID(3, 3), SpaceObjects.ObjectType.ASTEROID, 1_000_000, 0, 0, 0, SpaceObjects.DetectionState.PRECISE);
        var context = context(earth, target); var craft = craft(0, 0);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 3).withVelocity(ArrivalVelocityMode.MAXIMUM, 0), craft, context));
        var endA = craft.samples.getLast(); var history = List.copyOf(craft.samples);
        double fuel = craft.availableDeltaV(context);
        assertTrue(endA.velocityX() > 0);
        assertTrue(RocketFlightNavigation.navigate(action(earth.id(), 4).withVelocity(ArrivalVelocityMode.MAXIMUM, 0), craft, context));
        assertEquals(history, craft.samples.subList(0, history.size()), "B cannot alter A");
        assertTrue(craft.samples.stream().skip(history.size()).anyMatch(s -> s.x() > endA.x()), "inherited velocity must overshoot before reversing");
        assertTrue(craft.samples.stream().skip(history.size()).anyMatch(s -> s.y() < -100), "return takes the opposite side");
        assertTrue(craft.availableDeltaV(context) < fuel, "steering consumes propulsion");
    }
    @Test void abortsAndChildrenUseTheActualMotionState() {
        var earth = earth(0);
        var target = new SpaceObjectData(new UUID(4, 4), SpaceObjects.ObjectType.ASTEROID, 1_000_000, 0, 0, 0, SpaceObjects.DetectionState.PRECISE);
        var context = context(earth, target); var craft = craft(0, 0);
        var action = action(target.id(), 5).withAddons(List.of(new ActionAddon(new UUID(5, 5), ActionAddonType.TIME_BEFORE_ARRIVAL, 120)));
        assertTrue(RocketFlightNavigation.navigate(action, craft, context));
        assertEquals(1, context.navigationAborts.size());
        var abort = context.navigationAborts.getFirst();
        var at = FlightMotion.sample(craft.samples, abort.timeSeconds());
        assertEquals(craft.x, at.x(), 1e-6); assertEquals(craft.velocityX, at.vx(), 1e-6);
        var child = craft.copyFor(Set.copyOf(craft.segments.keySet()));
        assertEquals(craft.x, child.x); assertEquals(craft.velocityY, child.velocityY);
    }
    @Test void forecastPacketsPreserveMotionEventsAndAnAsteroidThatMissesEarth() {
        var context = context(earth(0)); var craft = craft(0, 0);
        craft.velocityX = 10;
        craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
        craft.advance(false, 0, 0, 0, List.of(), 5);
        craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
        var path = craft.toPath(TerminalState.READY, context);
        var asteroid = new AsteroidPath(new UUID(9, 9), List.of(new MotionSample(5, 50, 0, 10), new MotionSample(10, 100, 0, 10)), null, 0);
        var flight = new FlightPath(List.of(path), List.of(), 5, List.of(), List.of(asteroid), List.of());
        rearth.oritech.api.networking.NetworkManager.loadDefaultCodecs();
        rearth.oritech.spaceage.network.MissionNetworking.registerForecastCodecs();
        var codec = rearth.oritech.api.networking.ReflectiveCodecBuilder.create(MissionForecast.Navigation.class);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY, net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
        try {
            var forecast = new MissionForecast.Navigation(flight);
            codec.encode(buffer, forecast);
            assertEquals(forecast, codec.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test void customArrivalUsesTheOriginalApproachDirection() {
        var earth = earth(0);
        var target = new SpaceObjectData(new UUID(6, 6), SpaceObjects.ObjectType.ASTEROID, 1_000_000, 100_000, 0, 0, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(0, 0);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 6).withVelocity(ArrivalVelocityMode.CUSTOM, 100), craft, context(earth, target)));
        assertEquals(100, Math.hypot(craft.velocityX, craft.velocityY), 1e-6);
        assertEquals(.1, craft.velocityY / craft.velocityX, 1e-6);
    }

    @Test void movingAsteroidCustomArrivalUsesRelativeVelocity() {
        var target = new SpaceObjectData(new UUID(7, 7), SpaceObjects.ObjectType.ASTEROID,
                1_000_000, 100_000, 10, 5, 2_000, 0, 100, SpaceObjects.DetectionState.PRECISE, "Moving", List.of());
        var craft = craft(0, 0);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 7).withVelocity(ArrivalVelocityMode.CUSTOM, 10), craft, context(earth(0), target)),
                () -> "Moving arrival failed: " + craft.blockedState);
        assertEquals(10, Math.hypot(craft.velocityX - 10, craft.velocityY - 5), 1e-5);
        assertEquals(2_000, Math.hypot(craft.x - target.xAt(craft.time), craft.y - target.yAt(craft.time)), .01);
    }

    @Test void anUnpoweredRadialImpactRemainsAValidCoast() {
        var craft = craft(100_000, 0);
        craft.velocityX = -100;
        craft.segments.values().forEach(s -> s.chemicalSeconds = 0);
        craft.samples.clear();
        craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
        assertTrue(RocketFlightNavigation.navigate(action(SpaceObjects.EARTH_ID, 8)
                .withVelocity(ArrivalVelocityMode.MAXIMUM, 0), craft, context(earth(60_000))));
        assertEquals(400, craft.time, 1e-5);
        assertEquals(-100, craft.velocityX, 1e-7);
    }

    @Test void cappedMaximumArrivalAcceleratesOnceThenCoastsAtTheConfiguredCap() {
        var target = new SpaceObjectData(new UUID(10, 10), SpaceObjects.ObjectType.SURVEY_REGION,
                8_000_000, 1_000_000, 200_000, 0, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(0, 0);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 10)
                .withVelocity(ArrivalVelocityMode.MAXIMUM, 0).withMaxSpeed(1000), craft, context(earth(0), target)));
        boolean coastStarted = false;
        for (int i = 1; i < craft.samples.size(); i++) {
            var sample = craft.samples.get(i);
            if (sample.timeSeconds() <= craft.samples.get(i - 1).timeSeconds()) continue;
            assertNotEquals(PathPhase.BRAKE, sample.phase());
            assertNotEquals(PathPhase.REDIRECT, sample.phase());
            if (sample.firingSegments().isEmpty()) {
                coastStarted = true;
                assertEquals(1000, sample.speedMetersPerSecond(), 1e-5);
            } else assertFalse(coastStarted, "No hidden midpoint may restart acceleration");
        }
        assertTrue(coastStarted);
    }

    @Test void unrestrictedMaximumArrivalBurnsContinuouslyWhileFuelIsAvailable() {
        var target = new SpaceObjectData(new UUID(11, 11), SpaceObjects.ObjectType.SURVEY_REGION,
                8_000_000, 1_000_000, 200_000, 0, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(0, 0);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 11)
                .withVelocity(ArrivalVelocityMode.MAXIMUM, 0), craft, context(earth(0), target)));
        for (int i = 1; i < craft.samples.size(); i++) {
            var sample = craft.samples.get(i);
            if (sample.timeSeconds() - craft.samples.get(i - 1).timeSeconds() < 1e-7) continue;
            assertEquals(PathPhase.ACCELERATE, sample.phase());
            assertFalse(sample.firingSegments().isEmpty());
        }
    }

    @Test void highSpeedTurnsWithAbundantFuelRemainFeasibleAndKeepVelocityContinuous() {
        var target = new SpaceObjectData(new UUID(12, 12), SpaceObjects.ObjectType.SURVEY_REGION,
                500_000, 200_000, 20_000, 0, SpaceObjects.DetectionState.PRECISE);
        for (int i = 0; i < 16; i++) {
            var craft = craft(0, 0);
            craft.velocityX = Math.cos(i * Math.PI / 8) * 12_000;
            craft.velocityY = Math.sin(i * Math.PI / 8) * 12_000;
            craft.samples.clear();
            craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
            double vx = craft.velocityX, vy = craft.velocityY;
            assertTrue(RocketFlightNavigation.navigate(action(target.id(), 12).withMaxSpeed(1000), craft, context(earth(0), target)),
                    "turn " + i + " failed: " + craft.blockedState);
            var first = new FlightMotion(craft.samples.get(0), craft.samples.get(1)).at(1e-8);
            assertEquals(vx, first.vx(), .01); assertEquals(vy, first.vy(), .01);
            for (int j = 1; j < craft.samples.size(); j++) {
                var a = craft.samples.get(j - 1); var b = craft.samples.get(j);
                double dt = b.timeSeconds() - a.timeSeconds();
                if (dt <= 1e-7) continue;
                var motion = new FlightMotion(a, b);
                assertEquals(b.x(), motion.at(1).x(), 1e-7);
                assertEquals(b.y(), motion.at(1).y(), 1e-7);
                assertEquals(a.velocityX(), motion.at(0).vx(), 1e-7);
                assertEquals(b.velocityY(), motion.at(1).vy(), 1e-7);
                if (b.phase() == PathPhase.BRAKE) assertTrue(b.speedMetersPerSecond() <= a.speedMetersPerSecond() + .01);
                if (b.phase() == PathPhase.ACCELERATE) assertTrue(b.speedMetersPerSecond() >= a.speedMetersPerSecond() - .01);
            }
        }
    }

    @Test void planetIntersectionsDoNotRejectUnrelatedNavigation() {
        var craft = craft(60_010, 0);
        craft.velocityX = -1000;
        craft.samples.clear();
        craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
        var target = new SpaceObjectData(new UUID(13, 13), SpaceObjects.ObjectType.SURVEY_REGION,
                500_000, 200_000, 20_000, 0, SpaceObjects.DetectionState.PRECISE);
        var context = context(earth(60_000), target);
        assertTrue(craft.availableDeltaV(context) > 100_000);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 13), craft, context));
        assertEquals(0, Math.hypot(craft.velocityX, craft.velocityY), 1e-5);
    }

}
