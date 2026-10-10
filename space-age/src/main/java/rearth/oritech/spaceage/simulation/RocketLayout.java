package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.block.BlockPairController;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.HardwareRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Current physical cells; launch topology and capacities do not change when freight is consumed.
 */
public final class RocketLayout {

    private RocketLayout() {
    }

    public static UUID segmentId(ActiveRocketData rocket, SpaceSimulation.SegmentRef ref) {

        return rocket.getOriginalSegments().entrySet().stream().filter(e -> SpaceSimulation.SegmentRef.of(e.getValue()).equals(ref))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public static BlockState state(ActiveRocketData rocket, SpaceSimulation.SegmentRef segment, BlockPos pos) {

        var id = segmentId(rocket, segment);
        if (id == null) return Blocks.AIR.defaultBlockState();
        var resources = rocket.getDynamicSegments().get(id);
        if (resources.layout.containsKey(pos)) return resources.layout.get(pos);
        return rocket.getOriginalSegments().get(id).blocks().stream().filter(b -> b.relativePos().equals(pos)).map(StaticRocketSegment.BlockData::state)
                .findFirst().orElse(Blocks.AIR.defaultBlockState());
    }

    public static RocketControllerState.Pair pair(ActiveRocketData rocket, HardwareRef controller) {

        var cells = BlockPairController.cells(controller.position(), state(rocket, controller.segment(), controller.position()));
        return new RocketControllerState.Pair(rocket.getRocketId(), controller, cells.get(0), cells.get(1));
    }

    public static boolean busy(ActiveRocketData rocket, RocketControllerState.Pair pair) {

        var id = segmentId(rocket, pair.controller().segment());
        if (id == null) return true;
        var reserved = rocket.getDynamicSegments().get(id).reserved;
        return reserved.containsKey(pair.left()) || reserved.containsKey(pair.right());
    }

    public static List<HardwareRef> controllers(ActiveRocketData rocket, boolean crafters) {

        var result = new ArrayList<HardwareRef>();
        rocket.getStaticSegments().values().forEach(s -> s.blocks().stream()
                .filter(b -> b.state().is(crafters ? SpaceAgeBlocks.VACUUM_CRAFTER.get() : SpaceAgeBlocks.CARGO.get()))
                .forEach(b -> result.add(new HardwareRef(SpaceSimulation.SegmentRef.of(s), b.relativePos()))));
        result.sort(Comparator.comparingLong((HardwareRef r) -> r.segment().anchor().asLong()).thenComparingLong(r -> r.position().asLong()));
        return List.copyOf(result);
    }

    public static StaticRocketSegment current(StaticRocketSegment original, DynamicRocketSegment resources) {

        if (resources.layout.isEmpty()) return original;
        var cells = new HashMap<BlockPos, BlockState>();
        original.blocks().forEach(b -> cells.put(b.relativePos(), b.state()));
        var weight = (double) original.staticWeight();
        var energy = original.payloadEnergyJoules();

        // Reserved ingredients still contribute mass and blast energy until the recipe finishes.
        for (var entry : resources.layout.entrySet()) {
            var previous = cells.getOrDefault(entry.getKey(), Blocks.AIR.defaultBlockState());
            var counted = resources.reserved.getOrDefault(entry.getKey(), entry.getValue());
            weight += mass(counted) - mass(previous);
            energy += explosive(counted) - explosive(previous);
            cells.put(entry.getKey(), entry.getValue());
        }
        var blocks = new HashSet<StaticRocketSegment.BlockData>();
        cells.forEach((pos, state) -> blocks.add(new StaticRocketSegment.BlockData(pos, state)));
        return new StaticRocketSegment(original.segmentId(), blocks, original.originalCouplings(), Math.max(0, Math.round(weight)),
                original.engineCount(), original.initialRF(), original.initialFuel(), Math.max(0, energy));
    }

    public static double mass(BlockState state) {

        return Math.max(0, (long) state.getBlock().defaultDestroyTime());
    }

    public static double explosive(BlockState state) {

        var value = state.getBlock().builtInRegistryHolder().getData(SpaceAgeDataMaps.ROCKET_EXPLOSIVES);
        return value == null ? 0 : value;
    }

    public static void setPair(ActiveRocketData rocket, RocketControllerState.Pair pair, BlockState first, BlockState second) {

        var resources = rocket.getDynamicSegments().get(segmentId(rocket, pair.controller().segment()));
        resources.layout.put(pair.left(), first);
        resources.layout.put(pair.right(), second);
    }
}
