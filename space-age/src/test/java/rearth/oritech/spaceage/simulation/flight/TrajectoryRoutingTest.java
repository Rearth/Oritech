package rearth.oritech.spaceage.simulation.flight;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.ReflectiveCodecBuilder;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddon;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ArrivalVelocityMode;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.AsteroidPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.FlightPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.MotionSample;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.ProcessingEstimate;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.mission.MissionForecast;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void timeLimitRetainsTheActualEstimatedDurationInsteadOfGenericInfeasibility() {

        var target = new SpaceObjectData(new UUID(80, 1), SpaceObjects.ObjectType.ASTEROID,
                3_000_000, 0, 100, 0, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(0, 0);
        var context = context(earth(0), target);
        var action = action(target.id(), 800).withMaxSpeed(1);
        assertFalse(RocketFlightNavigation.navigate(action, craft, context));
        assertEquals(TerminalState.INTEGRATION_TIME_LIMIT, craft.blockedState);
        assertNotNull(craft.navigationFailure);
        assertEquals("time_limit", craft.navigationFailure.reason());
        assertEquals(SpaceBalance.MAX_TRANSFER_SECONDS, craft.navigationFailure.limit());
        assertTrue(craft.navigationFailure.actual() > 2_900_000, "The slow leg has a concrete estimated duration");
        var path = craft.toPath(craft.blockedState, context);
        var saved = RocketFlightPathCalculator.CraftPath.CODEC.encodeStart(JsonOps.INSTANCE, path).getOrThrow();
        var restored = RocketFlightPathCalculator.CraftPath.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(path.failure(), restored.failure());
        var flight = new FlightPath(List.of(restored), List.of(), 0);
        assertEquals(path.failure(), MissionForecast.Navigation.forClient(flight).paths().getFirst().failure());
    }

    @Test
    void collectorRingAcceptsThe11590DeltaVLowThrustCraft() {

        var sun = new SpaceObjectData(SpaceSimulation.SUN_ID, SpaceObjects.ObjectType.SUN,
                0, 0, 250_000, 274, SpaceObjects.DetectionState.PRECISE);
        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                -3_000_000, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(-3_120_000, 0);
        craft.segments.put(new SegmentRef(BlockPos.ZERO), new RocketFlightPathState.Segment(
                1000, 1000, 0, 11590, 0, new RocketHardware(1, 0, 0, 0)));
        var context = context(earth, sun);
        assertEquals(11590, craft.availableDeltaV(context), 1e-6);
        var action = action(sun.id(), 801).withOrbit(OrbitBand.SOLAR);
        var route = RocketTransferRoute.solve(craft, context, action, sun, -500_000, 0);
        assertNotNull(route);
        assertNull(craft.navigationFailure);
        assertTrue(route.duration() < SpaceBalance.MAX_TRANSFER_SECONDS);
        assertTrue(route.gravity() < 0, "Earth departure gravity still applies, without a Sun braking penalty");
    }

    @Test
    void sunGravityIsZeroAtAndOutsideTheCollectorRingButStillAppliesInside() {

        var sun = new SpaceObjectData(SpaceSimulation.SUN_ID, SpaceObjects.ObjectType.SUN,
                0, 0, 250_000, 274, SpaceObjects.DetectionState.PRECISE);
        var context = context(sun);
        var outside = FlightCurve.between(-1_000_000, 0, -500_000, 0, 1, 0, 1, 0, 100_000);
        assertEquals(0, RocketTransferRoute.gravityForLeg(context, outside, 0), 1e-12);
        assertEquals(0, RocketTransferRoute.gravityAlong(context, outside, 0, 0), 1e-12);
        assertEquals(0, RocketTransferRoute.gravityAlong(context, outside, outside.length(), 0), 1e-12);
        var inside = FlightCurve.between(-500_000, 0, -300_000, 0, 1, 0, 1, 0, 60_000);
        assertTrue(RocketTransferRoute.gravityForLeg(context, inside, 0) > 0);
        assertTrue(RocketTransferRoute.gravityAlong(context, inside, inside.length(), 0) > 0);
        var bodyEarth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        assertTrue(RocketTransferRoute.gravityForLeg(context(bodyEarth), outside, 0) > 0,
                "The Sun-only cutoff must not remove other bodies' gravity");
        var craft = craft(-1_000_000, 0);
        craft.segments.put(new SegmentRef(BlockPos.ZERO), new RocketFlightPathState.Segment(
                1_000_000, 1000, 0, 100_000, 0, new RocketHardware(1, 0, 0, 0)));
        var arrival = action(sun.id(), 802).withOrbit(OrbitBand.SOLAR);
        var route = RocketTransferRoute.solve(craft, context, arrival, sun, -500_000, 0);
        assertNotNull(route, "Even a 0.001 m/s² craft can stop at the collector ring with enough propulsion/time");
        assertEquals(0, route.gravity(), 1e-12);
        assertNull(craft.navigationFailure);
        craft.x = -500_000;
        var departure = RocketTransferRoute.solve(craft, context, arrival, sun, -1_000_000, 0);
        assertNotNull(departure, "Departures and rendezvous routes also use the same zero-Sun-gravity region");
        assertEquals(0, departure.gravity(), 1e-12);
        assertNull(RocketTransferRoute.solve(craft, context, arrival.withOrbit(OrbitBand.SURFACE), sun, -250_000, 0),
                "The collector ring exemption does not make a low-thrust Sun-surface landing safe");
        assertEquals("braking_thrust", craft.navigationFailure.reason());
    }

    @Test
    void marsHighOrbitRoutesCompleteAfterFastEarthDepartures() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                -3_000_000, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var mars = new SpaceObjectData(SpaceSimulation.MARS_ID, SpaceObjects.ObjectType.MARS,
                5_000_000, 1_500_000, 45_000, 3.71f, SpaceObjects.DetectionState.PRECISE);
        var failures = new ArrayList<String>();
        for (var acceleration : new double[]{.1, 1, 10, 100}) {
            for (var initialSpeed : new double[]{0, 100, 500, 1000, 2500}) {
                for (var startTime : new double[]{0, 100, 100_000}) {
                    for (var poweredRadio : new boolean[]{false, true}) {
                        var craft = craft(-3_120_000, 0);
                        craft.time = startTime;
                        craft.velocityX = initialSpeed;
                        var hardware = poweredRadio ? new RocketHardware(0, 1, 0, 1) : new RocketHardware(1, 0, 0, 0);
                        var segment = new RocketFlightPathState.Segment(1000,
                                poweredRadio ? 0 : 1000 * acceleration, poweredRadio ? 1000 * acceleration : 0,
                                poweredRadio ? 0 : 11590 / acceleration, poweredRadio ? 11590 / acceleration : 0, hardware);
                        if (poweredRadio) {
                            // Keep propulsion and a long coast's radio bill funded independently of ion balance.
                            segment.rf = hardware.rfCost(segment.ionSeconds) + 2_000_000;
                            segment.ionSeconds = segment.rf / hardware.rfCost(1);
                        }
                        craft.segments.put(new SegmentRef(BlockPos.ZERO), segment);
                        var action = action(mars.id(), 803).withOrbit(OrbitBand.HIGH);
                        var success = RocketFlightNavigation.navigate(action, craft, context(earth, mars));
                        if (!success) failures.add("a=" + acceleration + " initial=" + initialSpeed + " start=" + startTime
                                + " radio=" + poweredRadio + " status=" + craft.blockedState + " velocity=" + Math.hypot(craft.velocityX, craft.velocityY)
                                + " time=" + craft.time + " deltaV=" + craft.availableDeltaV(context(earth, mars)));
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void mixedPropulsionWithRadioArrivesWithItsRemainingChemicalBudget() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                -3_000_000, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var mars = new SpaceObjectData(SpaceSimulation.MARS_ID, SpaceObjects.ObjectType.MARS,
                5_000_000, 1_500_000, 45_000, 3.71f, SpaceObjects.DetectionState.PRECISE);
        for (var ionSeconds : new double[]{25, 1000, 10_000}) {
            // Leaving Earth needs ~700 m/s here, so lower caps can't coast out of its gravity.
            for (var speedCap : new int[]{0, 1200, 2000}) {
                for (var antennas : new int[]{1, 4}) {
                    var craft = craft(-3_120_000, 0);
                    craft.velocityX = 500;
                    var hardware = new RocketHardware(1, 1, 0, antennas);
                    var segment = new RocketFlightPathState.Segment(1000, 1000, 1000, 10_000, ionSeconds, hardware);
                    segment.rf = hardware.rfCost(segment.ionSeconds);
                    craft.segments.put(new SegmentRef(BlockPos.ZERO), segment);
                    var success = RocketFlightNavigation.navigate(action(mars.id(), 804).withOrbit(OrbitBand.HIGH)
                            .withMaxSpeed(speedCap), craft, context(earth, mars));
                    assertTrue(success, "ion=" + ionSeconds + " cap=" + speedCap + " antennas=" + antennas
                            + " status=" + craft.blockedState + " speed=" + Math.hypot(craft.velocityX, craft.velocityY));
                    assertEquals(0, Math.hypot(craft.velocityX, craft.velocityY), 1e-6);
                    assertTrue(segment.chemicalSeconds > 0, "The spare chemical budget remains available after arrival");
                    assertEquals(mars.id(), craft.currentTarget);
                }
            }
        }
        var craft = craft(-3_120_000, 0);
        craft.velocityX = 500;
        assertFalse(RocketFlightNavigation.navigate(action(mars.id(), 804).withOrbit(OrbitBand.HIGH).withMaxSpeed(500),
                craft, context(earth, mars)));
        assertEquals("climb_speed", craft.navigationFailure.reason());
        assertEquals(500, craft.navigationFailure.actual(), 1e-9);
        assertTrue(craft.navigationFailure.limit() > 500);
    }

    @Test
    void insufficientRfForPropulsionAndRadioRejectsBeforeDrawingAFlight() {

        var target = new SpaceObjectData(SpaceSimulation.MARS_ID, SpaceObjects.ObjectType.MARS,
                8_000_000, 1_500_000, 45_000, 3.71f, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(0, 0);
        var hardware = new RocketHardware(0, 1, 0, 1);
        var segment = new RocketFlightPathState.Segment(1000, 0, 1_000_000, 0, 11.59, hardware);
        segment.rf = hardware.rfCost(segment.ionSeconds);
        craft.segments.put(new SegmentRef(BlockPos.ZERO), segment);
        assertFalse(RocketFlightNavigation.navigate(action(target.id(), 805).withOrbit(OrbitBand.HIGH), craft, context(target)));
        assertEquals(0, craft.time);
        assertEquals(hardware.rfCost(11.59), segment.rf, 1e-6);
        assertNotNull(craft.navigationFailure);
        assertEquals("radio_budget", craft.navigationFailure.reason());
        assertEquals(1, craft.samples.size(), "An impossible power schedule must not draw a completed transfer");
    }

    @Test
    void coordinateMappingIsContinuousPeriodicAndIndependentOfTheCard() {

        assertEquals(SpaceBalance.surfaceAngle(10, 20), SpaceBalance.surfaceAngle(2010, 20), 1e-12);
        assertEquals(Math.PI * 2 / 2000, SpaceBalance.surfaceAngle(1, 0) - SpaceBalance.surfaceAngle(0, 0), 1e-12);
        assertEquals(Math.cos(SpaceBalance.surfaceAngle(-1, 0)), Math.cos(SpaceBalance.surfaceAngle(1999, 0)), 1e-12);
    }

    @Test
    void surfaceTripsHaveRadialCurveEndpointsWithoutPlanetAvoidance() {

        var earth = earth(60_000);
        for (int landing : new int[]{1, 10, 250, 1000, 1999}) {
            var craft = craft(60_000, 0);
            var context = context(earth);
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
            assertEquals(1, departure.vx(), 1e-7);
            assertEquals(0, departure.vy(), 1e-7);
            assertEquals(-Math.cos(angle), arrival.vx(), 1e-7);
            assertEquals(-Math.sin(angle), arrival.vy(), 1e-7);
            assertTrue(RocketFlightNavigation.navigate(action, craft, context));
            assertEquals(Math.cos(angle) * earth.radius(), craft.x, .01);
            assertEquals(Math.sin(angle) * earth.radius(), craft.y, .01);
            assertTrue(craft.samples.size() < 210, "bounded shared curve samples");
        }
    }

    @Test
    void stoppedAsteroidRoundTripsReachEarthSurface() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                -3_000_000, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        for (int index = 0; index < 12; index++) {
            var angle = index * Math.PI / 6;
            var asteroid = new SpaceObjectData(new UUID(8, index), SpaceObjects.ObjectType.ASTEROID,
                    earth.x() + (float) (Math.cos(angle) * 300_000), (float) (Math.sin(angle) * 300_000), 100, 0, SpaceObjects.DetectionState.PRECISE);
            var craft = craft(earth.x() + 60_000, 0);
            var context = context(earth, asteroid);
            assertTrue(RocketFlightNavigation.navigate(action(asteroid.id(), 80 + index), craft, context), "outbound " + index);
            assertTrue(RocketFlightNavigation.navigate(action(earth.id(), 100 + index), craft, context),
                    "return " + index + ": " + craft.blockedState + " at " + craft.x + ", " + craft.y
                            + " speed " + Math.hypot(craft.velocityX, craft.velocityY));
        }
    }

    @Test
    void heavyParachuteCraftUsesTheNormalLandingCurve() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                -3_000_000, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var asteroid = new SpaceObjectData(new UUID(9, 9), SpaceObjects.ObjectType.ASTEROID,
                earth.x() - 300_000, -30_000, 100, 0, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(earth.x() + 60_000, 0);
        var segment = new RocketFlightPathState.Segment(248_140, 3_000_000, 0, 2_840, 0, new RocketHardware(12, 0, 0, 0));
        segment.parachutes = 3;
        craft.segments.put(new SegmentRef(BlockPos.ZERO), segment);
        var context = context(earth, asteroid);
        assertTrue(RocketFlightNavigation.navigate(action(asteroid.id(), 119), craft, context));
        var powered = craft.copyFor(Set.copyOf(craft.segments.keySet()));
        powered.segments.values().forEach(s -> s.parachutes = 0);
        var landing = action(earth.id(), 120);
        var route = RocketTransferRoute.solve(craft, context, landing, earth, earth.x() + earth.radius(), 0);
        var poweredRoute = RocketTransferRoute.solve(powered, context, landing, earth, earth.x() + earth.radius(), 0);
        assertNotNull(route);
        assertNotNull(poweredRoute);
        for (int index = 0; index <= 10; index++)
            assertEquals(poweredRoute.curve().atDistance(poweredRoute.curve().length() * index / 10, 1),
                    route.curve().atDistance(route.curve().length() * index / 10, 1));
        assertTrue(RocketFlightNavigation.navigate(landing, craft, context));
        assertTrue(RocketFlightNavigation.navigate(landing, powered, context(earth, asteroid)));
        assertEquals(earth.x() + earth.radius(), craft.x, .01);
        assertEquals(0, craft.velocityX, 1e-5);
        assertTrue(craft.samples.stream().noneMatch(sample -> sample.phase() == PathPhase.PARACHUTE));
        assertTrue(context.arrivalPredictions.getLast().parachutes().deltaVReduction() > 0);
        assertTrue(segment.chemicalSeconds > powered.segments.values().iterator().next().chemicalSeconds,
                "parachutes must reduce landing fuel consumption");
    }

    @Test
    void fastLowOrbitArrivalsHaveRoomToBrakeBeforeLanding() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        for (int i = 0; i < 8; i++) {
            var craft = craft(earth.radius() + OrbitBand.LOW.altitude(), 0);
            craft.segments.put(new SegmentRef(BlockPos.ZERO), new RocketFlightPathState.Segment(
                    100, 2_000, 0, 10_000, 0, new RocketHardware(1, 0, 0, 0)));
            craft.velocityX = Math.cos(i * Math.PI / 4) * 1500;
            craft.velocityY = Math.sin(i * Math.PI / 4) * 1500;
            craft.samples.clear();
            craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
            var context = context(earth);
            var landing = action(earth.id(), 140 + i);
            assertTrue(craft.availableDeltaV(context) > 100_000);
            var fuelBefore = craft.availableDeltaV(context);
            var route = RocketTransferRoute.solve(craft, context, landing, earth, earth.radius(), 0);
            assertNotNull(route);
            var start = route.curve().atDistance(0, 1500);
            assertEquals(craft.velocityX, start.vx(), 1e-6);
            assertEquals(craft.velocityY, start.vy(), 1e-6);
            assertEquals(fuelBefore, craft.availableDeltaV(context), 1e-6, "solving must not consume fuel");
            assertTrue(RocketFlightNavigation.navigate(landing, craft, context),
                    "heading " + i + ": " + craft.blockedState);
            assertEquals(earth.radius(), craft.x, .01);
            assertEquals(0, craft.y, .01);
            assertEquals(0, Math.hypot(craft.velocityX, craft.velocityY), 1e-5);
            assertTrue(craft.availableDeltaV(context) > 0);
        }
    }

    @Test
    void maximumOrbitArrivalDoesNotApplySurfaceGravityAcrossTheEntireBrakingLoop() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(-300_000, 30_000);
        craft.segments.put(new SegmentRef(BlockPos.ZERO), new RocketFlightPathState.Segment(
                100, 1_200, 0, 10_000, 0, new RocketHardware(1, 0, 0, 0)));
        var context = context(earth);
        var orbit = action(earth.id(), 152).withOrbit(OrbitBand.LOW).withVelocity(ArrivalVelocityMode.MAXIMUM, 0);
        assertTrue(RocketFlightNavigation.navigate(orbit, craft, context));
        var arrivalSpeed = Math.hypot(craft.velocityX, craft.velocityY);
        var landing = RocketTransferRoute.solve(craft, context, action(earth.id(), 153), earth, earth.radius(), 0);
        assertNotNull(landing);
        var vacuumStoppingDistance = arrivalSpeed * arrivalSpeed / (2 * 12 * landing.efficiency());
        assertTrue(landing.curve().length() < vacuumStoppingDistance * 1.5,
                "a long loop must not pay surface gravity over its entire braking distance");
        assertTrue(landing.gravity() < 1,
                "gravity work is bounded by the difference in endpoint potential");
        assertTrue(RocketFlightNavigation.navigate(action(earth.id(), 153), craft, context));
        assertEquals(0, Math.hypot(craft.velocityX, craft.velocityY), 1e-5);
    }

    @Test
    void longLandingTurnsStayRoundedAndApproachAlongTheSurfaceNormal() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        for (var heading : new double[]{0, .005, Math.PI - .005, Math.PI}) {
            var craft = craft(70_000, 0);
            craft.segments.put(new SegmentRef(BlockPos.ZERO), new RocketFlightPathState.Segment(
                    100, 2_000, 0, 10_000, 0, new RocketHardware(1, 0, 0, 0)));
            craft.velocityX = Math.cos(heading) * 6_000;
            craft.velocityY = Math.sin(heading) * 6_000;
            var route = RocketTransferRoute.solve(craft, context(earth), action(earth.id(), 151), earth, earth.radius(), 0);
            assertNotNull(route);
            var curve = route.curve();
            var width = 0d;
            for (int i = 0; i <= 1_000; i++) {
                var point = curve.atDistance(curve.length() * i / 1_000, 1);
                width = Math.max(width, Math.abs(point.y()));
            }
            assertTrue(width > curve.length() * .05,
                    "braking turns need width proportional to their length, not the short endpoint distance");
            var start = curve.atDistance(0, 1);
            assertEquals(Math.cos(heading), start.vx(), 1e-7);
            assertEquals(Math.sin(heading), start.vy(), 1e-7);
            var approach = curve.atDistance(curve.length() * .9999, 1);
            assertTrue(approach.vx() < -.99, "the last part of the approach should already point inward");
            var end = curve.atDistance(curve.length(), 1);
            assertEquals(earth.radius(), end.x(), 1e-7);
            assertEquals(0, end.y(), 1e-7);
            assertEquals(-1, end.vx(), 1e-7);
            assertEquals(0, end.vy(), 1e-7);
        }
    }

    @Test
    void anExtendedLandingApproachDoesNotAccelerateBeforeBraking() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(70_000, 0);
        craft.segments.put(new SegmentRef(BlockPos.ZERO), new RocketFlightPathState.Segment(
                100, 2_000, 0, 10_000, 0, new RocketHardware(1, 0, 0, 0)));
        craft.velocityX = -1500;
        craft.samples.clear();
        craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
        var context = context(earth);
        var landing = action(earth.id(), 150);
        var route = RocketTransferRoute.solve(craft, context, landing, earth, earth.radius(), 0);
        assertNotNull(route);
        var stoppingDistance = 1500 * 1500 / (2 * (20 * route.efficiency() - earth.surfaceGravity()));
        assertTrue(route.curve().length() <= stoppingDistance * 1.001,
                "the added approach should closely fit the full-power stopping distance");
        assertTrue(RocketFlightNavigation.navigate(landing, craft, context));
        assertTrue(craft.samples.stream().noneMatch(s -> s.phase() == PathPhase.ACCELERATE),
                "an approach extended to accommodate braking must not accelerate into the extra distance");
        assertTrue(craft.samples.stream().allMatch(s -> s.speedMetersPerSecond() <= 1500 + 1e-3),
                "only gravity over the residual coast may add speed");    }

    @Test
    void longerLandingApproachesStillRequireEnoughFuelAndThrust() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        for (var segment : List.of(
                new RocketFlightPathState.Segment(100, 900, 0, 100_000, 0, new RocketHardware(1, 0, 0, 0)),
                new RocketFlightPathState.Segment(100, 2_000, 0, 10, 0, new RocketHardware(1, 0, 0, 0)))) {
            var craft = craft(earth.radius() + OrbitBand.LOW.altitude(), 0);
            craft.segments.put(new SegmentRef(BlockPos.ZERO), segment);
            craft.velocityX = -1500;
            var context = context(earth);
            var fuelBefore = craft.availableDeltaV(context);
            assertNull(RocketTransferRoute.solve(craft, context, action(earth.id(), 149), earth, earth.radius(), 0));
            assertEquals(fuelBefore, craft.availableDeltaV(context), 1e-6);
        }
    }

    @Test
    void gravityMakesSurfaceAscentHarderThanFinalBraking() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(60_000, 0);
        var context = context(earth);
        var action = action(earth.id(), 30).withOrbit(OrbitBand.LOW);
        var route = RocketTransferRoute.solve(craft, context, action, earth, 160_000, 0);
        assertNotNull(route);
        assertEquals(-3.67875, route.gravity(), .001);
        assertTrue(route.burns().getFirst().seconds() > route.burns().getLast().seconds() * 1.5,
                "gravity must lengthen outward acceleration and assist final braking");

        var weakProfile = new RocketBurnProfile(List.of(new RocketBurnProfile.Interval(9, 10_000)));
        assertNull(CurveTransfer.solve(100_000, 0, 0, false, Double.POSITIVE_INFINITY,
                weakProfile, 1, -9.81), "9 m/s^2 cannot depart against 9.81 m/s^2 gravity");
    }

    @Test
    void stoppedDeparturesRememberTheirArrivalAndUseStableJitter() {

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
        var stopped = FlightMotion.position(craft.samples, craft.time + 1, MissionState.Position.surface());
        assertTrue(stopped.headingX() > .99, "arrival heading survives a tick past the stop");
        var position = new MissionState.Position(craft.x, craft.y, 0, 0, target.id(), OrbitBand.SURFACE,
                -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO), craft.headingX, craft.headingY);
        var encoded = MissionState.Position.CODEC.encodeStart(JsonOps.INSTANCE, position).getOrThrow();
        assertEquals(position, MissionState.Position.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        var child = craft.copyFor(Set.copyOf(craft.segments.keySet()));
        assertEquals(craft.headingX, child.headingX);
    }

    @Test
    void maximumArrivalRetainsMomentumAndTheReturnPaysForItsTurn() {

        var earth = earth(0);
        var target = new SpaceObjectData(new UUID(3, 3), SpaceObjects.ObjectType.ASTEROID, 1_000_000, 0, 0, 0, SpaceObjects.DetectionState.PRECISE);
        var context = context(earth, target);
        var craft = craft(0, 0);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 3).withVelocity(ArrivalVelocityMode.MAXIMUM, 0), craft, context));
        var endA = craft.samples.getLast();
        var history = List.copyOf(craft.samples);
        var fuel = craft.availableDeltaV(context);
        assertTrue(endA.velocityX() > 0);
        assertTrue(RocketFlightNavigation.navigate(action(earth.id(), 4).withVelocity(ArrivalVelocityMode.MAXIMUM, 0), craft, context));
        assertEquals(history, craft.samples.subList(0, history.size()), "B cannot alter A");
        assertTrue(craft.samples.stream().skip(history.size()).anyMatch(s -> s.x() > endA.x()), "inherited velocity must overshoot before reversing");
        assertTrue(craft.samples.stream().skip(history.size()).anyMatch(s -> s.y() < -100), "return takes the opposite side");
        assertTrue(craft.availableDeltaV(context) < fuel, "steering consumes propulsion");
    }

    @Test
    void abortsAndChildrenUseTheActualMotionState() {

        var earth = earth(0);
        var target = new SpaceObjectData(new UUID(4, 4), SpaceObjects.ObjectType.ASTEROID, 1_000_000, 0, 0, 0, SpaceObjects.DetectionState.PRECISE);
        var context = context(earth, target);
        var craft = craft(0, 0);
        var action = action(target.id(), 5).withAddons(List.of(new ActionAddon(new UUID(5, 5), ActionAddonType.TIME_BEFORE_ARRIVAL, 120)));
        assertTrue(RocketFlightNavigation.navigate(action, craft, context));
        assertEquals(1, context.navigationAborts.size());
        var abort = context.navigationAborts.getFirst();
        var at = FlightMotion.sample(craft.samples, abort.timeSeconds());
        assertEquals(craft.x, at.x(), 1e-6);
        assertEquals(craft.velocityX, at.vx(), 1e-6);
        var child = craft.copyFor(Set.copyOf(craft.segments.keySet()));
        assertEquals(craft.x, child.x);
        assertEquals(craft.velocityY, child.velocityY);
    }

    @Test
    void forecastPacketsPreserveMotionEventsAndAnAsteroidThatMissesEarth() {

        var context = context(earth(0));
        var craft = craft(0, 0);
        craft.velocityX = 10;
        craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
        craft.advance(false, 0, 0, 0, List.of(), 5);
        craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
        var path = craft.toPath(TerminalState.READY, context);
        var asteroid = new AsteroidPath(new UUID(9, 9), List.of(new MotionSample(5, 50, 0, 10), new MotionSample(10, 100, 0, 10)), null, 0);
        var estimate = new ProcessingEstimate(new UUID(8, 8), List.of(Identifier.parse("minecraft:gold_ingot")), 100, 400000, 1200000, "", true);
        var flight = new FlightPath(List.of(path), List.of(), 5, List.of(), List.of(asteroid), List.of(), List.of(), List.of(), List.of(estimate));
        NetworkManager.loadDefaultCodecs();
        MissionNetworking.registerForecastCodecs();
        var codec = ReflectiveCodecBuilder.create(MissionForecast.Navigation.class);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        try {
            var forecast = new MissionForecast.Navigation(flight);
            codec.encode(buffer, forecast);
            assertEquals(forecast, codec.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void customArrivalUsesTheOriginalApproachDirection() {

        var earth = earth(0);
        var target = new SpaceObjectData(new UUID(6, 6), SpaceObjects.ObjectType.ASTEROID, 1_000_000, 100_000, 0, 0, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(0, 0);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 6).withVelocity(ArrivalVelocityMode.CUSTOM, 100), craft, context(earth, target)));
        assertEquals(100, Math.hypot(craft.velocityX, craft.velocityY), 1e-6);
        assertEquals(.1, craft.velocityY / craft.velocityX, 1e-6);
    }

    @Test
    void movingAsteroidCustomArrivalUsesRelativeVelocity() {

        var target = new SpaceObjectData(new UUID(7, 7), SpaceObjects.ObjectType.ASTEROID,
                1_000_000, 100_000, 10, 5, 2_000, 0, 100, SpaceObjects.DetectionState.PRECISE, "Moving", List.of());
        var craft = craft(0, 0);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 7).withVelocity(ArrivalVelocityMode.CUSTOM, 10), craft, context(earth(0), target)),
                () -> "Moving arrival failed: " + craft.blockedState);
        assertEquals(10, Math.hypot(craft.velocityX - 10, craft.velocityY - 5), 1e-5);
        assertEquals(2_000, Math.hypot(craft.x - target.xAt(craft.time), craft.y - target.yAt(craft.time)), .01);
    }

    @Test
    void anUnpoweredRadialImpactRemainsAValidCoast() {

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

    @Test
    void cappedMaximumArrivalAcceleratesOnceThenCoastsAtTheConfiguredCap() {

        var target = new SpaceObjectData(new UUID(10, 10), SpaceObjects.ObjectType.SURVEY_REGION,
                8_000_000, 1_000_000, 200_000, 0, SpaceObjects.DetectionState.PRECISE);
        var craft = craft(0, 0);
        assertTrue(RocketFlightNavigation.navigate(action(target.id(), 10)
                .withVelocity(ArrivalVelocityMode.MAXIMUM, 0).withMaxSpeed(1000), craft, context(earth(0), target)));
        var coastStarted = false;
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

    @Test
    void unrestrictedMaximumArrivalBurnsContinuouslyWhileFuelIsAvailable() {

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

    @Test
    void highSpeedTurnsWithAbundantFuelRemainFeasibleAndKeepVelocityContinuous() {

        var target = new SpaceObjectData(new UUID(12, 12), SpaceObjects.ObjectType.SURVEY_REGION,
                500_000, 200_000, 20_000, 0, SpaceObjects.DetectionState.PRECISE);
        for (int i = 0; i < 16; i++) {
            var craft = craft(0, 0);
            craft.velocityX = Math.cos(i * Math.PI / 8) * 12_000;
            craft.velocityY = Math.sin(i * Math.PI / 8) * 12_000;
            craft.samples.clear();
            craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
            var vx = craft.velocityX;
            var vy = craft.velocityY;
            assertTrue(RocketFlightNavigation.navigate(action(target.id(), 12).withMaxSpeed(1000), craft, context(earth(0), target)),
                    "turn " + i + " failed: " + craft.blockedState);
            var first = new FlightMotion(craft.samples.get(0), craft.samples.get(1)).at(1e-8);
            assertEquals(vx, first.vx(), .01);
            assertEquals(vy, first.vy(), .01);
            for (int j = 1; j < craft.samples.size(); j++) {
                var a = craft.samples.get(j - 1);
                var b = craft.samples.get(j);
                var dt = b.timeSeconds() - a.timeSeconds();
                if (dt <= 1e-7) continue;
                var motion = new FlightMotion(a, b);
                assertEquals(b.x(), motion.at(1).x(), 1e-7);
                assertEquals(b.y(), motion.at(1).y(), 1e-7);
                assertEquals(a.velocityX(), motion.at(0).vx(), 1e-7);
                assertEquals(b.velocityY(), motion.at(1).vy(), 1e-7);
                if (b.phase() == PathPhase.BRAKE)
                    assertTrue(b.speedMetersPerSecond() <= a.speedMetersPerSecond() + .01);
                if (b.phase() == PathPhase.ACCELERATE)
                    assertTrue(b.speedMetersPerSecond() >= a.speedMetersPerSecond() - .01);
            }
        }
    }

    @Test
    void planetIntersectionsDoNotRejectUnrelatedNavigation() {

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
