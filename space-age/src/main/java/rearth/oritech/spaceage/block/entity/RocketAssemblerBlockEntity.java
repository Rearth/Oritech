package rearth.oritech.spaceage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

        var segments = new HashMap<UUID, RocketFloodSegment>();
        segments.put(startSegment.id, startSegment);

        var openCouplings = new ArrayList<>(startSegment.couplings().stream().map(FoundCoupling::oppositeSide).toList());

        var limit = 60;

        while (!openCouplings.isEmpty() && limit-- > 0) {

            var candidate = openCouplings.removeFirst();

            // check if candidate is in another segment already (e.g. the second coupling connecting to this, or a coupling checked from the other side
            var alreadyConnected = segments.values().stream().anyMatch(segment -> segment.blocks.stream().anyMatch(foundBlock -> foundBlock.pos.equals(candidate)));
            if (alreadyConnected) continue;

            var segment = segmentFloodFill(candidate);
            if (segment.blocks.isEmpty()) continue;

            var couplingsValid = segmentCouplingsValid(segment);
            if (!couplingsValid) {
                OritechSpaceAge.LOGGER.warn("Couplings invalid! " + worldPosition);
                continue;
            }

            segments.put(segment.id, segment);

            for (var coupling : segment.couplings) {
                openCouplings.add(coupling.oppositeSide());
            }

        }

        System.out.println("Segments: " + segments.size());

        connectRocketSegments(segments);

        for (var segment : segments.values()) {
            System.out.println(segment);
            System.out.println("block count: " + segment.blocks.size() + " couplings: " + segment.couplings.size());
            System.out.println("connected segment count: " + segment.connectedSegments.size());
        }

    }

    // this is called after all segments are discovered, and connects them based on their couplings
    private void connectRocketSegments(Map<UUID, RocketFloodSegment> segments) {

        var segmentsByBlock = new HashMap<BlockPos, UUID>();
        for (var segment : segments.values()) {
            segment.connectedSegments.clear();
            for (var block : segment.blocks) {
                segmentsByBlock.put(block.pos, segment.id);
            }
        }

        for (var segment : segments.values()) {
            for (var coupling : segment.couplings) {
                var connectedSegmentId = segmentsByBlock.get(coupling.oppositeSide);
                if (connectedSegmentId == null || connectedSegmentId.equals(segment.id)) continue;

                segment.connectedSegments
                        .computeIfAbsent(connectedSegmentId, ignored -> new HashSet<>())
                        .add(coupling);
            }
        }
    }

    // searches and calculates engines, weight, fuel, etc.
    private void scanSegmentContent(RocketFloodSegment segment) {



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
        visited.add(start);
        var results = new HashSet<BlockPos>();

        while (!openPositions.isEmpty() && limit-- > 0) {

            var checkedPos = openPositions.removeFirst();

            var checkedState = level.getBlockState(checkedPos);

            if (!checkedState.is(SpaceAgeBlocks.ROCKET_PAD)) continue;

            results.add(checkedPos);

            for (var dir : directions) {
                if (dir.getAxis().isVertical()) continue;
                var nextPos = checkedPos.relative(dir);
                if (visited.add(nextPos)) openPositions.add(nextPos);
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
        visited.add(start);
        var results = new HashSet<FoundBlock>();
        var couplings = new HashSet<FoundCoupling>();

        while (!openPositions.isEmpty() && limit-- > 0) {

            var checkedElement = openPositions.removeFirst();
            var checkedPos = checkedElement.self();

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
                if (visited.add(nextPos)) openPositions.add(new FloodFillElement(nextPos, checkedPos));
            }

        }

        return new RocketFloodSegment(results, couplings, UUID.randomUUID());

    }

    private boolean isCoupling(BlockState state) {
        return state.is(SpaceAgeBlocks.ROCKET_COUPLING);
    }

    private boolean isValidRocketBlock(BlockState state, BlockPos pos) {
        return pos.getY() > this.worldPosition.getY() && !state.isAir();
    }

    private record FloodFillElement(BlockPos self, BlockPos source) {}

    private static final class RocketFloodSegment {

        private final Set<FoundBlock> blocks;
        private final Set<FoundCoupling> couplings;
        private final UUID id;
        private final Map<UUID, Set<FoundCoupling>> connectedSegments = new HashMap<>();    // contains all connected segments via
                                                                                            // id and the couplings that connect to it.

        private RocketFloodSegment(Set<FoundBlock> blocks, Set<FoundCoupling> couplings, UUID id) {
            this.blocks = blocks;
            this.couplings = couplings;
            this.id = id;
        }

        private Set<FoundCoupling> couplings() {
            return couplings;
        }

        @Override
        public String toString() {
            return "RocketFloodSegment{" +
                    "id=" + id +
                    ", blocks=" + blocks +
                    ", couplings=" + couplings +
                    ", connectedSegments=" + connectedSegments.entrySet().stream()
                            .map(entry -> entry.getKey() + " via " + entry.getValue())
                            .toList() +
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
