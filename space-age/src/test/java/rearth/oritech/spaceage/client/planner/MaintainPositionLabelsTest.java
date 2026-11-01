package rearth.oritech.spaceage.client.planner;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;
import rearth.oritech.api.networking.ReflectiveCodecBuilder;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddon;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
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

    @Test
    void indefiniteAndConditionalHoldsExplainHowLaterCardsContinue() {

        var hold = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        assertEquals("screen.oritech_space_age.action.hold_following_warning",
                ((TranslatableContents) FlightPlannerLabels.stationKeepingContinuation(hold, true).getContents()).getKey());
        var conditional = hold.withAddons(List.of(ActionAddon.create(ActionAddonType.DURATION)));
        assertEquals("screen.oritech_space_age.action.continue_condition",
                ((TranslatableContents) FlightPlannerLabels.stationKeepingMode(conditional).getContents()).getKey());
        var estimate = new StationKeepingEstimate(hold.id(), 0, StationKeepingEnd.SUSTAINABLE, 0, StationKeepingEnd.SUSTAINABLE, 100, 10, 0, 0);
        assertEquals("screen.oritech_space_age.action.hold_no_end",
                ((TranslatableContents) FlightPlannerLabels.stationKeepingEnd(estimate).getContents()).getKey());
    }

    @Test
    void forecastPacketsKeepEnduranceAndUpkeepSeparateFromCompletion() {

        var estimate = new StationKeepingEstimate(UUID.randomUUID(), 60, StationKeepingEnd.DURATION, 600, StationKeepingEnd.RF, 5, 10, 3, .25);
        var codec = ReflectiveCodecBuilder.create(StationKeepingEstimate.class);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        try {
            codec.encode(buffer, estimate);
            assertEquals(estimate, codec.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
