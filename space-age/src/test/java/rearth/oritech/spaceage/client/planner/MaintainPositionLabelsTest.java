package rearth.oritech.spaceage.client.planner;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.StationKeepingEnd;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.StationKeepingEstimate;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaintainPositionLabelsTest {

    @Test
    void enduranceLabelUsesResourcesRatherThanTheChosenStopTime() {

        for (var limit : List.of(StationKeepingEnd.RF, StationKeepingEnd.FUEL)) {
            var estimate = new StationKeepingEstimate(UUID.randomUUID(), 10, StationKeepingEnd.DURATION, 600, limit, 0, 10, 5, 2);
            var text = (TranslatableContents) FlightPlannerLabels.stationKeepingDuration(estimate).getContents();
            assertEquals("screen.oritech_space_age.action.hold_endurance." + limit.name().toLowerCase(Locale.ROOT), text.getKey());
            var time = (Component) text.getArgs()[0];
            assertEquals("10.0", ((TranslatableContents) time.getContents()).getArgs()[0]);
        }
    }

}
