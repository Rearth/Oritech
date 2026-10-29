package rearth.oritech.spaceage.block.assembler;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlannerSnapshot;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketAssemblerMenuTest {

    @Test
    void snapshotsPreserveNewEditsAndIgnorePreviousRocketResponses() {

        var menu = new RocketAssemblerMenu(SpaceAgeMenus.ROCKET_ASSEMBLER.get(), 1, BlockPos.ZERO);
        var rocket = new ActiveRocketData(Map.of(), Map.of());
        menu.setPreview(rocket);
        var system = UUID.randomUUID();
        var saved = FlightPlan.empty();
        var snapshot = new FlightPlannerSnapshot(system, rocket.getRocketId(), List.of(), saved);
        menu.setFlightPlannerSnapshot(snapshot);
        var edited = saved.withName("New name");
        menu.setDraftFlightPlan(edited);
        menu.setFlightPlannerSnapshot(snapshot);
        assertEquals(edited, menu.getDraftFlightPlan());
        assertTrue(menu.isDraftFlightPlanDirty());

        var card = saved.withName("Loaded from card");
        menu.setFlightPlannerSnapshot(new FlightPlannerSnapshot(system, rocket.getRocketId(), List.of(), card), true);
        assertEquals(card, menu.getDraftFlightPlan());
        assertTrue(menu.isDraftFlightPlanDirty(), "Loading a card must save its plan when the assembler closes");

        var replacement = new ActiveRocketData(Map.of(), Map.of());
        menu.setPreview(replacement);
        menu.setDraftFlightPlan(edited);
        menu.setFlightPlannerSnapshot(snapshot, true);
        assertNull(menu.getFlightPlannerSnapshot());
        assertEquals(edited, menu.getDraftFlightPlan());
        assertTrue(menu.isDraftFlightPlanDirty());
    }
}
