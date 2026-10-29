package rearth.oritech.spaceage.client.starmap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentConfiguration;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarMapTooltipTest {

    private static final SegmentRef SEGMENT = new SegmentRef(BlockPos.ZERO);
    private static final SpaceSimulation.SpaceObjectData EARTH = new SpaceSimulation.SpaceObjectData(
            SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH, 0, 0, 60_000, 9.81f,
            SpaceObjects.DetectionState.PRECISE);

    private static List<Component> tooltip(UUID branch, FlightPlanAction action, FlightPlan plan,
                                           Map<SegmentRef, String> defaults, Map<UUID, Map<SegmentRef, String>> names) {

        var sample = new PathSample(10, 0, 0, 100, 100, 0, PathPhase.BRAKE,
                SpaceObjects.EARTH_ID, action.id(), 1, Set.of(SEGMENT), Set.of(SEGMENT), FlightPlanAction.NO_TARGET);
        return StarMapTooltip.build(null, null, null, null, new StarMapTooltip.PathPoint(branch, sample, 100),
                plan, defaults, names, id -> id.equals(EARTH.id()) ? EARTH : null);
    }

    @Test
    void reusedPlansResolveActionsAndSegmentNamesWithinTheHoveredCraft() {

        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var navigate = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID);
        var wait = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        // Repeated launches share both action IDs and relative segment anchors.
        var plan = new FlightPlan(List.of(
                new FlightPlanBranch(first, FlightPlanBranch.NO_PARENT, List.of(navigate, wait)),
                new FlightPlanBranch(second, FlightPlanBranch.NO_PARENT, List.of(wait, navigate))), List.of());
        var names = Map.of(first, Map.of(SEGMENT, "S1"), second, Map.of(SEGMENT, "Relay engine"));
        for (var branch : List.of(first, second)) {
            var lines = tooltip(branch, navigate, plan, Map.of(), names);
            var actions = lines.stream().filter(line -> line.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("screen.oritech_space_age.path_action")).toList();
            assertEquals(List.of(Component.translatable("screen.oritech_space_age.path_action", first.equals(branch) ? 1 : 2,
                    RocketStarMapWidget.objectName(EARTH))), actions);
            var expectedName = names.get(branch).get(SEGMENT);
            assertEquals(2, lines.stream().filter(line -> line.equals(Component.literal("• " + expectedName))).count());
            assertFalse(lines.stream().anyMatch(line -> line.getString().contains("Unknown segment")));
        }
    }

    @Test
    void singleCraftPreviewKeepsConfiguredAndDefaultSegmentNames() {

        var branch = UUID.randomUUID();
        var navigate = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID);
        var plan = new FlightPlan(List.of(new FlightPlanBranch(branch, FlightPlanBranch.NO_PARENT, List.of(navigate))), List.of());
        assertTrue(tooltip(branch, navigate, plan, Map.of(SEGMENT, "S1"), Map.of()).contains(Component.literal("• S1")));
        var named = plan.withSegmentConfigurations(List.of(new SegmentConfiguration(SEGMENT, "Booster", false, List.of(1))));
        assertTrue(tooltip(branch, navigate, named, Map.of(SEGMENT, "S1"), Map.of()).contains(Component.literal("• Booster")));
    }
}
