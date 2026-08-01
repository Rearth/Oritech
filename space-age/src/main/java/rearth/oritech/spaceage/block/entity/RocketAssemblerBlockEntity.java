package rearth.oritech.spaceage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RocketAssemblerBlockEntity extends BlockEntity {

    public RocketAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceAgeBlockEntities.ROCKET_ASSEMBLER.get(), pos, state);
    }

    public void assemble() {

        OritechSpaceAge.LOGGER.debug("Starting assembling process");

        collectRocketSegments();

    }

    private void collectRocketSegments() {

        // start at first found block above connected pads
        // todo offset by assembler orientation

        var padBlocks = padFloodFill(worldPosition.north());
        var start = worldPosition;

        for (var padBlock : padBlocks) {

            var above = padBlock.above();

            var checkedState = level.getBlockState(above);
            if (checkedState.isAir() || isCoupling(checkedState)) continue;

            OritechSpaceAge.LOGGER.debug("Found start: " + above);
            start = above;

            break;
        }

        if (start.equals(worldPosition)) return;


        var startSegment = segmentFloodFill(start);
        var couplingsValid = segmentCouplingsValid(startSegment);

        var segments = new HashSet<RocketFloodSegment>();
        segments.add(startSegment);

        var openCouplings = new ArrayList<>(startSegment.couplings().stream().map(FoundCoupling::oppositeSide).toList());
        startSegment.couplings().forEach(coupling -> openCouplings.add(coupling.oppositeSide()));

        var limit = 60;

        while (!openCouplings.isEmpty() && limit-- > 0) {

            var candidate = openCouplings.removeFirst();

            // check if candidate is in another segment already (e.g. the second coupling connecting to this, or a coupling checked from the other side
            var alreadyConnected = segments.stream().anyMatch(segment -> segment.blocks.stream().anyMatch(foundBlock -> foundBlock.pos.equals(candidate)));
            if (alreadyConnected) continue;

            var segment = segmentFloodFill(candidate);

            segments.add(segment);

            for (var coupling : segment.couplings) {
                openCouplings.add(coupling.oppositeSide());
            }

        }

        System.out.println("Segments: " + segments.size());

        for (var segment : segments) {
            System.out.println(segment);
            System.out.println("block count: " + segment.blocks.size() + " couplings: " + segment.couplings.size());
            System.out.println("couplings okay: " + segment);
        }

    }

    // ensures no couples connect to the segment itself
    private boolean segmentCouplingsValid(RocketFloodSegment segment) {
        for (var coupling : segment.couplings) {
            if (segment.blocks.stream().anyMatch(block -> block.pos.equals(coupling.oppositeSide))) return false;
        }

        return true;
    }

    private Set<BlockPos> padFloodFill(BlockPos start) {

        var directions = Direction.values();
        var limit = 1000;

        var openPositions = new ArrayList<BlockPos>();
        openPositions.add(start);

        var visited = new HashSet<BlockPos>();
        var results = new HashSet<BlockPos>();

        while (!openPositions.isEmpty() && limit-- > 0) {

            var checkedPos = openPositions.removeFirst();
            visited.add(checkedPos);

            var checkedState = level.getBlockState(checkedPos);

            if (!checkedState.is(SpaceAgeBlocks.ROCKET_PAD)) continue;

            results.add(checkedPos);

            for (var dir : directions) {
                if (dir.getAxis().isVertical()) continue;
                var nextPos = checkedPos.relative(dir);
                if (visited.contains(nextPos)) continue;
                openPositions.add(nextPos);
            }

        }

        return results;

    }

    private RocketFloodSegment segmentFloodFill(BlockPos start) {

        var directions = Direction.values();
        var limit = 1000;

        var openPositions = new ArrayList<FloodFillElement>();
        openPositions.add(new FloodFillElement(start, start));

        var visited = new HashSet<BlockPos>();
        var results = new HashSet<FoundBlock>();
        var couplings = new HashSet<FoundCoupling>();

        while (!openPositions.isEmpty() && limit-- > 0) {

            var checkedElement = openPositions.removeFirst();
            var checkedPos = checkedElement.self();
            visited.add(checkedPos);

            var checkedState = level.getBlockState(checkedPos);

            if (isCoupling(checkedState)) {

                var source = checkedElement.source();
                var offset = checkedPos.subtract(source);
                if (offset.distManhattan(BlockPos.ZERO) != 1) {
                    OritechSpaceAge.LOGGER.error("Error during rocket calculations");
                    break;
                }

                couplings.add(new FoundCoupling(checkedPos, checkedPos.offset(offset)));
                continue;
            }

            if (!isValidRocketBlock(checkedState, checkedPos)) continue;

            results.add(new FoundBlock(checkedPos, checkedState));

            for (var dir : directions) {
                var nextPos = checkedPos.relative(dir);
                if (visited.contains(nextPos)) continue;
                openPositions.add(new FloodFillElement(nextPos, checkedPos));
            }

        }

        return new RocketFloodSegment(results, couplings);

    }

    private boolean isCoupling(BlockState state) {
        return state.is(SpaceAgeBlocks.ROCKET_COUPLING);
    }

    private boolean isValidRocketBlock(BlockState state, BlockPos pos) {
        return pos.getY() > this.worldPosition.getY() && !state.isAir();
    }

    private record FloodFillElement(BlockPos self, BlockPos source) {}

    private record RocketFloodSegment(Set<FoundBlock> blocks, Set<FoundCoupling> couplings) {

        @Override
        public String toString() {
            return "RocketFloodSegment{" +
                    "blocks=" + blocks +
                    ", couplings=" + couplings +
                    '}';
        }
    }

    private record FoundCoupling(BlockPos pos, BlockPos oppositeSide) {
        @Override
        public String toString() {
            return "FoundCoupling{" +
                    "pos=" + pos +
                    ", oppositeSide=" + oppositeSide +
                    '}';
        }
    }

    private record FoundBlock(BlockPos pos, BlockState state) {

        @Override
        public String toString() {
            return "FoundBlock{" +
                    "pos=" + pos +
                    ", state=" + state +
                    '}';
        }
    }

}
