package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.*;

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
        return SpaceCommunications.connection(craft, nodes, SpaceCommunications.routes(nodes));
    }

    @Test
    void constellationTiersExtendCommandCoverage() {
        var nodes = ground();
        assertEquals(SpaceBalance.GROUND_COMMAND_ALTITUDE,
                SpaceCommunications.network(OWNER, nodes).commandAltitude());

        addRing(nodes, OrbitBand.LOW);
        var low = SpaceCommunications.network(OWNER, nodes);
        assertTrue(low.lowComplete());
        assertEquals(SpaceBalance.LOW_COMMAND_ALTITUDE, low.commandAltitude());

        addRing(nodes, OrbitBand.HIGH);
        var high = SpaceCommunications.network(OWNER, nodes);
        assertTrue(high.highComplete());
        assertEquals(-1, high.commandAltitude());

        nodes.removeIf(SpaceCommunications.Node::ground);
        assertEquals(0, SpaceCommunications.network(OWNER, nodes).commandAltitude());
        assertFalse(link(node(1_000, 4, OrbitBand.LOW, -1, false, false), nodes).receiveCommands());
    }

    @Test
    void receptionUploadAndForwardingFollowDifferentAntennaRules() {
        var systemWide = ground();
        addRing(systemWide, OrbitBand.LOW);
        addRing(systemWide, OrbitBand.HIGH);

        var smallProbe = link(node(8_000_000, 1, OrbitBand.TIGHT, -1, false, false), systemWide);
        var largeProbe = link(node(8_000_000, 4, OrbitBand.TIGHT, -1, false, false), systemWide);
        assertTrue(smallProbe.receiveCommands());
        assertFalse(smallProbe.upload());
        assertTrue(largeProbe.receiveCommands());
        assertTrue(largeProbe.upload());
        assertEquals(SpaceBalance.ANTENNA_RANGE * 8, SpaceCommunications.uploadRange(4));

        var local = ground();
        var probe = node(500_000, 1, OrbitBand.TIGHT, -1, false, false);
        assertFalse(link(probe, local).upload());
        local.add(node(250_000, 1, OrbitBand.LOW, 0, true, false));
        assertTrue(link(probe, local).upload());
    }

    @Test
    void maintainingAnEarthDeploymentSlotEnablesRelayService() {
        var maintain = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var plan = FlightPlan.empty();
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(maintain))));
        var position = new MissionState.Position(-3_070_000, 0, 0, 0, SpaceObjects.EARTH_ID,
                OrbitBand.LOW, 2, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var mission = new MissionState(OWNER, new ActiveRocketData(Map.of(), Map.of()), plan, position,
                new SurveyKnowledge(), BlockPos.ZERO, 0);

        assertTrue(SpaceCommunications.isRelay(mission));

        mission.plan = plan.withBranches(List.of(plan.root().withActions(
                List.of(FlightPlanAction.create(ActionType.RELAY)))));
        assertFalse(SpaceCommunications.isRelay(mission));

        mission.plan = plan;
        mission.position = new MissionState.Position(position.x(), position.y(), 0, 0, SpaceObjects.EARTH_ID,
                OrbitBand.MEDIUM, -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        assertFalse(SpaceCommunications.isRelay(mission));
    }
}
