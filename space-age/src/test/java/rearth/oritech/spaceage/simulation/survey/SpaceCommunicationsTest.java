package rearth.oritech.spaceage.simulation.survey;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceCommunicationsTest {

    private static final UUID OWNER = new UUID(1, 2);

    private static SpaceCommunications.Node node(double altitude, int antennas, OrbitBand band,
                                                 int slot, boolean relay, boolean ground) {

        return new SpaceCommunications.Node(UUID.randomUUID(), OWNER, -3_060_000 - altitude, 0,
                antennas, band, slot, relay, ground);
    }

    private static ArrayList<SpaceCommunications.Node> ground() {

        return new ArrayList<>(List.of(node(0, 1, OrbitBand.SURFACE, -1, true, true)));
    }

    private static void addRing(List<SpaceCommunications.Node> nodes, OrbitBand band) {

        for (int slot = 0; slot < SpaceBalance.slots(band); slot++) {
            nodes.add(node(band.altitude(), 1, band, slot, true, false));
        }
    }

    private static SpaceCommunications.Connection link(SpaceCommunications.Node craft,
                                                       List<SpaceCommunications.Node> network) {

        var nodes = new ArrayList<>(network);
        nodes.add(craft);
        return SpaceCommunications.connection(craft, nodes);
    }

    @Test
    void constellationTiersExtendCommunicationCoverage() {

        var nodes = ground();
        assertEquals(SpaceBalance.GROUND_COMMAND_ALTITUDE,
                SpaceCommunications.network(OWNER, nodes).coverageAltitude());

        addRing(nodes, OrbitBand.LOW);
        var low = SpaceCommunications.network(OWNER, nodes);
        assertTrue(low.lowComplete());
        assertEquals(SpaceBalance.LOW_COMMAND_ALTITUDE, low.coverageAltitude());

        addRing(nodes, OrbitBand.HIGH);
        var high = SpaceCommunications.network(OWNER, nodes);
        assertTrue(high.highComplete());
        assertEquals(-1, high.coverageAltitude());

        nodes.removeIf(SpaceCommunications.Node::ground);
        assertEquals(0, SpaceCommunications.network(OWNER, nodes).coverageAltitude());
        assertFalse(link(node(1_000, 4, OrbitBand.LOW, -1, false, false), nodes).connected());
    }

    @Test
    void bothDirectionsUseLowEarthOrbitAndTheSameConstellationCoverage() {

        var nodes = ground();
        assertTrue(link(node(OrbitBand.LOW.altitude(), 1, OrbitBand.LOW, -1, false, false), nodes).connected());
        assertFalse(link(node(OrbitBand.LOW.altitude() + 1, 8, OrbitBand.LOW, -1, false, false), nodes).connected());
        addRing(nodes, OrbitBand.LOW);
        var boundary = link(node(SpaceBalance.LOW_COMMAND_ALTITUDE, 1, OrbitBand.TIGHT, -1, false, false), nodes);
        assertTrue(boundary.connected());
        assertEquals(SpaceBalance.LOW_COMMAND_ALTITUDE, boundary.coverageAltitude());
        assertEquals(SpaceBalance.LOW_COMMAND_ALTITUDE, boundary.altitude());
        assertFalse(link(node(SpaceBalance.LOW_COMMAND_ALTITUDE + 1, 8, OrbitBand.LOW, -1, false, false), nodes).connected());

        addRing(nodes, OrbitBand.HIGH);
        assertTrue(link(node(8_000_000, 1, OrbitBand.TIGHT, -1, false, false), nodes).connected());
        assertEquals(-1, link(node(8_000_000, 1, OrbitBand.TIGHT, -1, false, false), nodes).coverageAltitude());
    }

    @Test
    void poweredAntennasAndOwnedGroundStationAreRequired() {

        var nodes = ground();
        var unpowered = link(node(10_000, 0, OrbitBand.LOW, -1, false, false), nodes);
        assertFalse(unpowered.connected());
        assertEquals("communication.oritech_space_age.no_craft_antenna", unpowered.reason());
        var noGround = link(node(10_000, 1, OrbitBand.LOW, -1, false, false), List.of());
        assertFalse(noGround.connected());
        assertEquals("communication.oritech_space_age.no_ground", noGround.reason());
        var otherOwner = new SpaceCommunications.Node(UUID.randomUUID(), UUID.randomUUID(), -3_070_000, 0,
                1, OrbitBand.LOW, -1, false, false);
        assertFalse(link(otherOwner, nodes).connected());
    }

    @Test
    void extraAntennasAndIndividualRelaysDoNotExtendDisplayedCoverage() {

        var nodes = ground();
        var probe = node(500_000, 8, OrbitBand.TIGHT, -1, false, false);
        nodes.add(node(250_000, 1, OrbitBand.LOW, 0, true, false));
        var connection = link(probe, nodes);
        assertFalse(connection.connected());
        assertEquals(SpaceBalance.GROUND_COMMAND_ALTITUDE, connection.coverageAltitude());
        assertEquals("communication.oritech_space_age.outside_coverage", connection.reason());
    }

    @Test
    void maintainingAnEarthDeploymentSlotEnablesRelayService() {

        var maintain = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var plan = FlightPlan.empty();
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(maintain))));
        var position = new MissionState.Position(-3_070_000, 0, 0, 0, SpaceObjects.EARTH_ID,
                OrbitBand.LOW, 2, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var mission = new MissionState(OWNER, new ActiveRocketData(Map.of(), Map.of()), plan, position,
                new SurveyKnowledge(), BlockPos.ZERO);

        assertTrue(SpaceCommunications.isRelay(mission));

        mission.plan = plan.withBranches(List.of(plan.root().withActions(
                List.of(FlightPlanAction.create(ActionType.TRANSMIT_INFORMATION)))));
        assertFalse(SpaceCommunications.isRelay(mission));

        mission.plan = plan;
        mission.position = new MissionState.Position(position.x(), position.y(), 0, 0, SpaceObjects.EARTH_ID,
                OrbitBand.MEDIUM, -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        assertFalse(SpaceCommunications.isRelay(mission));
    }
}
