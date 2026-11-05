package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.block.trophy.TrophyInscription;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpaceAgeAdvancementsTest {

    private static final UUID OWNER = UUID.randomUUID();

    private static MissionState craft(UUID owner, UUID body, OrbitBand orbit, int slot) {

        return new MissionState(owner, new ActiveRocketData(Map.of(), Map.of()), FlightPlan.empty(),
                new MissionState.Position(0, 0, 0, 0, body, orbit, slot, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO)),
                new SurveyKnowledge(), BlockPos.ZERO);
    }

    private static List<MissionState> ring(OrbitBand orbit) {

        var body = orbit == OrbitBand.SOLAR ? SpaceSimulation.SUN_ID : SpaceObjects.EARTH_ID;
        var result = new ArrayList<MissionState>();
        for (int slot = 0; slot < SpaceBalance.slots(body, orbit); slot++) result.add(craft(OWNER, body, orbit, slot));
        return result;
    }

    @Test
    void eachSupportedOrbitCompletesIndependently() {

        for (var orbit : List.of(OrbitBand.LOW, OrbitBand.HIGH, OrbitBand.SOLAR)) {
            var fleet = ring(orbit);
            var last = fleet.removeLast();
            assertNull(SpaceAgeAdvancements.completedOrbit(OWNER, fleet));
            fleet.add(last);
            assertEquals(orbit, SpaceAgeAdvancements.completedOrbit(OWNER, fleet));
        }
    }

    @Test
    void duplicatesOtherOwnersWrongBodiesAndInvalidSlotsDoNotFillAHole() {

        var fleet = ring(OrbitBand.LOW);
        fleet.removeLast();
        fleet.add(craft(OWNER, SpaceObjects.EARTH_ID, OrbitBand.LOW, 0));
        fleet.add(craft(UUID.randomUUID(), SpaceObjects.EARTH_ID, OrbitBand.LOW, 5));
        fleet.add(craft(OWNER, SpaceSimulation.MARS_ID, OrbitBand.LOW, 5));
        fleet.add(craft(OWNER, SpaceObjects.EARTH_ID, OrbitBand.LOW, -1));
        fleet.add(craft(OWNER, SpaceObjects.EARTH_ID, OrbitBand.LOW, 6));
        assertNull(SpaceAgeAdvancements.completedOrbit(OWNER, fleet));
        var destroyed = craft(OWNER, SpaceObjects.EARTH_ID, OrbitBand.LOW, 5);
        destroyed.ended = true;
        fleet.add(destroyed);
        assertNull(SpaceAgeAdvancements.completedOrbit(OWNER, fleet));
    }

    @Test
    void firstCompletionSurvivesTheFleetLeavingAndWorldPersistence() {

        var missions = new MissionSavedData();
        var systems = new SpaceSimulationSavedData();
        for (var craft : ring(OrbitBand.HIGH)) missions.craft.put(craft.rocket.getRocketId(), craft);
        SpaceAgeAdvancements.recordCompletedOrbits(missions, systems);
        missions.craft.clear();
        SpaceAgeAdvancements.recordCompletedOrbits(missions, systems);
        var system = systems.getOrCreate(OWNER);
        var encoded = SpaceSimulation.CODEC.encodeStart(JsonOps.INSTANCE, system).getOrThrow();
        var restored = SpaceSimulation.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(OrbitBand.HIGH, restored.completedOrbit.orElseThrow());
    }

    @Test
    void inscriptionCodecPreservesThePlayersNameAndCompletedOrbit() {

        var inscription = new TrophyInscription("Commander \u2605", OrbitBand.SOLAR);
        var encoded = TrophyInscription.CODEC.encodeStart(JsonOps.INSTANCE, inscription).getOrThrow();
        assertEquals(inscription, TrophyInscription.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }
}
