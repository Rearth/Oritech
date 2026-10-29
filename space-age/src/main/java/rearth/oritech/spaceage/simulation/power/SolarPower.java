package rearth.oritech.spaceage.simulation.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketLayout;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.util.Geometry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SolarPower {

    private SolarPower() {
    }

    public static double multiplier(double x, double y) {

        var ratio = SpaceBalance.SOLAR_REFERENCE_DISTANCE / Math.max(SpaceBalance.SOLAR_MIN_DISTANCE, Math.hypot(x, y));
        return ratio * ratio;
    }

    public static double rating(Iterable<StaticRocketSegment> segments) {

        var blocks = new HashMap<BlockPos, BlockState>();
        for (var segment : segments)
            for (var block : segment.blocks()) blocks.put(block.relativePos(), block.state());
        var total = 0d;
        for (var entry : blocks.entrySet()) {
            var state = entry.getValue();
            var rating = state.getBlock().builtInRegistryHolder().getData(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS);
            if (rating == null || !Double.isFinite(rating) || rating <= 0) continue;
            if (state.is(BlockContent.BIG_SOLAR_PANEL) && !completePanel(blocks, entry.getKey())) continue;
            total += rating;
        }
        return total;
    }

    private static boolean completePanel(Map<BlockPos, BlockState> blocks, BlockPos centre) {

        for (var offset : BigSolarPanelEntity.CORE_POSITIONS) {
            var pos = centre.offset(Geometry.rotatePosition(offset, Direction.NORTH));
            if (!(blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()).getBlock() instanceof MachineCoreBlock))
                return false;
        }
        return true;
    }

    public static boolean sunRegion(MissionState.Position position) {

        return position.target().equals(SpaceSimulation.SUN_ID) || Math.hypot(position.x(), position.y()) <= SpaceBalance.SOLAR_MIN_DISTANCE + 1;
    }

    public static boolean stationary(MissionState state) {

        return !state.ended && !(state.position.orbit() == OrbitBand.SURFACE
                && (state.position.target().equals(SpaceObjects.EARTH_ID) || state.position.target().equals(SpaceSimulation.MARS_ID)
                || state.position.target().equals(SpaceSimulation.SUN_ID)))
                && (state.action() == null || state.action().type() != ActionType.NAVIGATE_TO || state.navigationBlocked())
                && Math.hypot(state.position.vx(), state.position.vy()) < .001;
    }

    public static double output(MissionState state) {

        var account = SpacePowerService.account(state.rocket);
        return account == null ? state.rocket.powerHardware().solar() * multiplier(state.position.x(), state.position.y()) : account.solarGeneration();
    }

    public static double receive(ActiveRocketData rocket, double amount) {

        var remaining = amount;
        for (var id : rocket.getDynamicSegments().keySet().stream().sorted().toList())
            remaining -= rocket.getDynamicSegments().get(id).receiveRF(remaining);
        return amount - remaining;
    }

    public static double spend(ActiveRocketData rocket, double amount) {

        var remaining = amount;
        for (var id : rocket.getDynamicSegments().keySet().stream().sorted().toList()) {
            var segment = rocket.getDynamicSegments().get(id);
            var spent = Math.min(remaining, segment.rf());
            segment.spendRF(spent);
            remaining -= spent;
        }
        return amount - remaining;
    }

    public static List<StaticRocketSegment> structures(RocketFlightPathState.Craft craft) {

        return craft.segments.values().stream().filter(segment -> segment.structure != null)
                .map(segment -> segment.physical == null ? segment.structure : RocketLayout.current(segment.structure, segment.physical)).toList();
    }

    public static double output(RocketFlightPathState.Craft craft) {

        if (craft.currentOrbit == OrbitBand.SURFACE && (craft.currentTarget.equals(SpaceObjects.EARTH_ID)
                || craft.currentTarget.equals(SpaceSimulation.MARS_ID) || craft.currentTarget.equals(SpaceSimulation.SUN_ID)))
            return 0;
        return rating(structures(craft)) * multiplier(craft.x, craft.y);
    }
}
