package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceObjects.DetectionState;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ArrivalVelocityMode;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Segment;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferFeasibilityTest {

    private static final SegmentRef CORE = new SegmentRef(BlockPos.ZERO);

    private static Craft craft(double x, double y, double speed, double angle, Segment segment) {

        var craft = new Craft(new LinkedHashMap<>(Map.of(CORE, segment)), new HashMap<>(), x, y, 0);
        craft.branchId = new UUID(1, 1);
        craft.velocityX = Math.cos(angle) * speed;
        craft.velocityY = Math.sin(angle) * speed;
        craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
        return craft;
    }

    private static Segment chemical(double mass, double acceleration) {

        return new Segment(mass, mass * acceleration, 0, 20_000, 0, new RocketHardware(1, 0, 0, 0));
    }

    private static Context context(SpaceObjectData... objects) {

        var map = new HashMap<UUID, SpaceObjectData>();
        for (var object : objects) map.put(object.id(), object);
        return new Context(map, Map.of(), Map.of(), 1);
    }

    private static FlightPlanAction action(SpaceObjectData target, ArrivalVelocityMode mode, int arrival, int cap) {

        return new FlightPlanAction(new UUID(0, 301), ActionType.NAVIGATE_TO, List.of(), target.id(),
                OrbitBand.SURFACE, mode, arrival, cap, 0, 0, 0, 0, List.of());
    }

    private static void arrives(Craft craft, Context context, FlightPlanAction action, SpaceObjectData target, String scenario) {

        var before = craft.availableDeltaV(context);
        var speed = Math.hypot(craft.velocityX - target.velocityX(), craft.velocityY - target.velocityY());
        assertTrue(before > 4 * (speed + action.targetVelocity()), scenario + ": ample delta-v");
        assertTrue(RocketFlightNavigation.navigate(action, craft, context),
                () -> scenario + ": " + craft.blockedState + " at " + craft.x + ", " + craft.y
                        + " velocity=" + craft.velocityX + ", " + craft.velocityY);
        assertEquals(SurveyRules.navigationRadius(target) + action.orbit().altitude(),
                Math.hypot(craft.x - target.xAt(craft.time), craft.y - target.yAt(craft.time)), .01, scenario);
        var arrivalSpeed = Math.hypot(craft.velocityX - target.velocityX(), craft.velocityY - target.velocityY());
        if (action.velocityMode() != ArrivalVelocityMode.MAXIMUM)
            assertEquals(action.targetVelocity(), arrivalSpeed, 1e-5, scenario);
        assertTrue(craft.availableDeltaV(context) > 0, scenario + ": fuel remains");
        assertTrue(craft.availableDeltaV(context) <= before + 1e-6, scenario + ": fuel cannot increase");
        assertEquals(target.id(), craft.currentTarget, scenario);
        assertEquals(action.orbit(), craft.currentOrbit, scenario);
        for (var sample : craft.samples) {
            assertTrue(Double.isFinite(sample.x()) && Double.isFinite(sample.y())
                    && Double.isFinite(sample.velocityX()) && Double.isFinite(sample.velocityY())
                    && Double.isFinite(sample.timeSeconds()), scenario + ": finite motion");
        }
    }

    @Test
    void shortVacuumTransfersAcceptInwardSidewaysAndReversingMomentum() {

        for (int distance : new int[]{1, 1_000, 100_000}) {
            var target = new SpaceObjectData(new UUID(2, distance), ObjectType.SURVEY_REGION,
                    distance, 0, 0, 0, DetectionState.PRECISE);
            for (int speed : new int[]{300, 1_500, 12_000}) {
                for (int heading = 0; heading < 8; heading++) {
                    for (var mode : ArrivalVelocityMode.values()) {
                        var craft = craft(0, 0, speed, heading * Math.PI / 4, chemical(100, 20));
                        arrives(craft, context(target), action(target, mode, mode == ArrivalVelocityMode.CUSTOM ? 100 : 0, 0),
                                target, "vacuum distance=" + distance + " speed=" + speed + " heading=" + heading + " mode=" + mode);
                    }
                }
            }
        }
    }

    @Test
    void movingAsteroidApproachesRespectRelativeVelocityAndCruiseCaps() {

        var target = new SpaceObjectData(new UUID(3, 1), ObjectType.ASTEROID,
                25_000, -10_000, -200, 100, 2_000, 0, 100, DetectionState.PRECISE, "Moving", List.of());
        for (int cap : new int[]{0, 500, 2_000}) {
            for (int heading = 0; heading < 8; heading++) {
                for (var mode : ArrivalVelocityMode.values()) {
                    var craft = craft(0, 0, 1_500, heading * Math.PI / 4, chemical(100, 20));
                    craft.velocityX += target.velocityX();
                    craft.velocityY += target.velocityY();
                    craft.samples.clear();
                    craft.addSample(PathPhase.COAST, FlightPlanAction.NO_TARGET, FlightPlanAction.NO_TARGET);
                    var action = action(target, mode, mode == ArrivalVelocityMode.CUSTOM ? 100 : 0, cap).withOrbit(OrbitBand.TIGHT);
                    arrives(craft, context(target), action, target, "moving cap=" + cap + " heading=" + heading + " mode=" + mode);
                    if (cap > 0)
                        assertTrue(Math.hypot(craft.velocityX - target.velocityX(), craft.velocityY - target.velocityY()) <= cap + 1e-5,
                                "moving cap=" + cap + " heading=" + heading + " mode=" + mode
                                        + " actual=" + Math.hypot(craft.velocityX - target.velocityX(), craft.velocityY - target.velocityY()));
                    assertFalse(craft.destroyed);
                }
            }
        }
    }

    @Test
    void planetaryLandingsAcceptDifferentCoordinatesGravityAndInheritedSpeeds() {

        for (var planet : List.of(
                new SpaceObjectData(SpaceObjects.EARTH_ID, ObjectType.EARTH, -3_000_000, 0, 60_000, 9.81f, DetectionState.PRECISE),
                new SpaceObjectData(SpaceSimulation.MARS_ID, ObjectType.MARS, 5_000_000, 1_500_000, 45_000, 3.71f, DetectionState.PRECISE))) {
            for (int landing : new int[]{0, 250, 1_000}) {
                for (int speed : new int[]{300, 1_500, 6_000}) {
                    for (int heading : new int[]{0, 2, 4}) {
                        var craft = craft(planet.x() + planet.radius() + OrbitBand.LOW.altitude(), planet.y(),
                                speed, heading * Math.PI / 4, chemical(100, 40));
                        var action = action(planet, ArrivalVelocityMode.ZERO, 0, 0).withLanding(landing, 0, 0, 0);
                        arrives(craft, context(planet), action, planet,
                                planet.type() + " landing=" + landing + " speed=" + speed + " heading=" + heading);
                        assertFalse(craft.destroyed);
                        if (planet.type() == ObjectType.EARTH) {
                            var angle = SpaceBalance.surfaceAngle(landing, 0);
                            assertEquals(planet.x() + Math.cos(angle) * planet.radius(), craft.x, .01);
                            assertEquals(planet.y() + Math.sin(angle) * planet.radius(), craft.y, .01);
                        }
                    }
                }
            }
        }
    }

    @Test
    void shortApproachesBrakeAcrossChemicalOrIonExhaustion() {

        var target = new SpaceObjectData(new UUID(4, 1), ObjectType.SURVEY_REGION, 1_000, 0, 0, 0, DetectionState.PRECISE);
        for (boolean chemicalEndsFirst : new boolean[]{true, false}) {
            for (int heading = 0; heading < 8; heading++) {
                var segment = new Segment(100, 2_000, 1_000, chemicalEndsFirst ? 10 : 20_000,
                        chemicalEndsFirst ? 20_000 : 10, new RocketHardware(1, 1, 0, 0));
                segment.rf = segment.ionSeconds * 20 * SpaceBalance.ION_RF;
                var craft = craft(0, 0, 1_500, heading * Math.PI / 4, segment);
                arrives(craft, context(target), action(target, ArrivalVelocityMode.ZERO, 0, 0), target,
                        "engine exhaustion chemicalFirst=" + chemicalEndsFirst + " heading=" + heading);
                assertEquals(0, chemicalEndsFirst ? segment.chemicalSeconds : segment.ionSeconds, 1e-6);
                assertTrue(chemicalEndsFirst ? segment.ionSeconds > 0 : segment.chemicalSeconds > 0);
            }
        }
    }

    @Test
    void asteroidReturnPlansKeepEarlierLegsAndReachTheSurfaceAfterFastOrbitArrival() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, ObjectType.EARTH, 0, 0, 60_000, 9.81f, DetectionState.PRECISE);
        for (var coordinates : new int[][]{{300_000, 100_000}, {-300_000, 100_000}, {0, -300_000}}) {
            var asteroid = new SpaceObjectData(new UUID(6, coordinates[0]), ObjectType.ASTEROID,
                    coordinates[0], coordinates[1], 0, 0, 100, 0, 10, DetectionState.PRECISE, "Cargo", List.of());
            var craft = craft(earth.radius(), 0, 0, 0, chemical(8_000, 40));
            var context = context(earth, asteroid);
            var scenario = "round trip asteroid=" + coordinates[0] + ", " + coordinates[1];
            arrives(craft, context, action(asteroid, ArrivalVelocityMode.ZERO, 0, 0), asteroid, scenario + " outbound");
            craft.attachedAsteroid = asteroid;
            craft.asteroidAnchor = CORE;
            var outbound = List.copyOf(craft.samples);
            arrives(craft, context, action(earth, ArrivalVelocityMode.CUSTOM, 1_500, 2_000).withOrbit(OrbitBand.LOW),
                    earth, scenario + " orbit");
            assertEquals(outbound, craft.samples.subList(0, outbound.size()), scenario);
            var returnLeg = List.copyOf(craft.samples);
            arrives(craft, context, action(earth, ArrivalVelocityMode.ZERO, 0, 500), earth, scenario + " landing");
            assertEquals(returnLeg, craft.samples.subList(0, returnLeg.size()), scenario);
            assertFalse(craft.destroyed);
            assertEquals(asteroid, craft.attachedAsteroid);
        }
    }

    @Test
    void attachedAsteroidLandingsWorkWithAndWithoutParachutes() {

        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, ObjectType.EARTH, 0, 0, 60_000, 9.81f, DetectionState.PRECISE);
        for (int asteroidMass : new int[]{10, 100, 500}) {
            for (int parachutes : new int[]{0, 3}) {
                var asteroid = new SpaceObjectData(new UUID(5, asteroidMass), ObjectType.ASTEROID,
                        300_000, 0, 0, 0, 100, 0, asteroidMass, DetectionState.PRECISE, "Cargo", List.of());
                var mass = 8_000 + asteroidMass * SpaceBalance.KILOGRAMS_PER_ASTEROID_MASS;
                var segment = chemical(8_000, 40 * mass / 8_000);
                segment.parachutes = parachutes;
                var craft = craft(70_000, 0, 1_500, Math.PI, segment);
                craft.attachedAsteroid = asteroid;
                craft.asteroidAnchor = CORE;
                arrives(craft, context(earth, asteroid), action(earth, ArrivalVelocityMode.ZERO, 0, 0), earth,
                        "attached mass=" + asteroidMass + " parachutes=" + parachutes);
                assertFalse(craft.destroyed);
                assertEquals(asteroid, craft.attachedAsteroid);
            }
        }
    }
}
