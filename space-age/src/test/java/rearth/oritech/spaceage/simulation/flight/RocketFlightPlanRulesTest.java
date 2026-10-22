package rearth.oritech.spaceage.simulation.flight;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketLayout;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketFlightPlanRulesTest {

    private static SpaceSimulation.FlightPlanAction action(List<SpaceSimulation.ActionAddon> addons) {

        return new SpaceSimulation.FlightPlanAction(new UUID(1, 2), SpaceSimulation.ActionType.NAVIGATE_TO,
                List.of(), SpaceObjects.EARTH_ID, SpaceSimulation.OrbitBand.SURFACE,
                SpaceSimulation.ArrivalVelocityMode.ZERO, 0, 0, 100, 200, 32, -24, addons);
    }

    @Test
    void savedSegmentsWithoutPlanAnchorKeepTheirExistingReferences() {

        var position = new BlockPos(-2, 3, -4);
        var segment = new StaticRocketSegment(UUID.randomUUID(), Set.of(new StaticRocketSegment.BlockData(position,
                Blocks.IRON_BLOCK.defaultBlockState())), Map.of(), 5, 0);
        var json = StaticRocketSegment.CODEC.encodeStart(JsonOps.INSTANCE, segment).getOrThrow().getAsJsonObject();
        json.remove("plan_anchor");
        var restored = StaticRocketSegment.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(new SpaceSimulation.SegmentRef(position), SpaceSimulation.SegmentRef.of(restored));
        var resources = new DynamicRocketSegment(0, 0, 0, Set.of());
        resources.layout.put(position.below(), Blocks.GOLD_BLOCK.defaultBlockState());
        assertEquals(position, RocketLayout.current(restored, resources).planAnchor());
    }

    @Test
    void earthLandingOffsetOnlyUsesSelectedUncertainty() {

        var base = action(List.of());
        var withoutUncertainty = RocketFlightPlanRules.applyLandingUncertainty(base);
        assertEquals(0, withoutUncertainty.landingOffsetX());
        assertEquals(0, withoutUncertainty.landingOffsetZ());

        var distanceCondition = new SpaceSimulation.ActionAddon(UUID.randomUUID(),
                SpaceSimulation.ActionAddonType.DISTANCE_FROM_TARGET, 64);
        var withDistance = RocketFlightPlanRules.applyLandingUncertainty(action(List.of(distanceCondition)));
        assertEquals(0, withDistance.landingOffsetX());
        assertEquals(0, withDistance.landingOffsetZ());

        var uncertainty = new SpaceSimulation.ActionAddon(UUID.randomUUID(),
                SpaceSimulation.ActionAddonType.DESIRED_UNCERTAINTY, 256);
        var adjusted = RocketFlightPlanRules.applyLandingUncertainty(action(List.of(uncertainty)));
        assertTrue(Math.hypot(adjusted.landingOffsetX(), adjusted.landingOffsetZ()) <= 256);
        assertEquals(adjusted, RocketFlightPlanRules.applyLandingUncertainty(adjusted));
    }
}
