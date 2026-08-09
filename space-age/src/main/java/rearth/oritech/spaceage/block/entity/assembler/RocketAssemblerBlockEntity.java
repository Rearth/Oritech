package rearth.oritech.spaceage.block.entity.assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.*;

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

        segments.values().forEach(this::scanSegmentContent);

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

    // searches and calculates engines, weight, fuel, energy, etc.
    private void scanSegmentContent(RocketFloodSegment segment) {

        // value is the burn time of the fuel type (per ml)
        var fuelTypes = new HashMap<FluidIngredient, Float>();
        var fuelGenRecipes = ((ServerLevel) level).recipeAccess().recipeMap().byType(RecipeContent.FUEL_GENERATOR.get());
        fuelGenRecipes.forEach(holder -> fuelTypes.put(holder.value().fluidInput().get().ingredient(), holder.value().time() / (float) holder.value().fluidInput().get().amount()));

        var detectedRF = 0L;
        var detectedFuels = new HashMap<FluidType, Long>(); // value is the total burn time available for the type on the segment
        var detectedEngines = 0;
        var weight = 0;
        var stoneWeight = Blocks.STONE.defaultBlockState().getDestroySpeed(level, worldPosition);

        for (var blockData : segment.blocks) {

            var worldPos = blockData.pos();
            var worldState = blockData.state();

            // fluid scan
            var fluidCandidate = level.getCapability(Capabilities.Fluid.BLOCK, worldPos, worldState, null, null);
            if (fluidCandidate != null) {
                try (var transaction = Transaction.openRoot()) {
                    var totalTaken = 0L;

                    for (var typeSet : fuelTypes.entrySet()) {
                        var typeIng = typeSet.getKey();
                        var typeBurnTime = typeSet.getValue();
                        for (var fluid : typeIng.fluids()) {
                            var fluidType = fluid.value().getFluidType();
                            var taken = fluidCandidate.extract(FluidResource.of(fluid), Integer.MAX_VALUE, transaction);
                            var takenBurnTime = (long) taken * typeBurnTime;
                            if (taken > 0)
                                detectedFuels.merge(fluidType, (long) takenBurnTime, Long::sum);

                            totalTaken += taken;
                        }
                    }

                    if (totalTaken > 0) {
                        weight += (int) (totalTaken * stoneWeight / FluidType.BUCKET_VOLUME);
                        // transaction.commit(); // todo enable again
                    }

                }
            }

            // energy scan
            var energyCandidate = level.getCapability(Capabilities.Energy.BLOCK, worldPos, worldState, null, null);
            if (energyCandidate != null) {
                detectedRF += energyCandidate.getAmountAsLong();
            }

            // weight scan
            weight += (int) Math.max(worldState.getDestroySpeed(level, worldPos), 0);

        }

        OritechSpaceAge.LOGGER.debug(
                "Collected stats for rocket segment {}: weight={}, fuels={}, energy={}, engines={}",
                segment.id, weight, detectedFuels, detectedRF, detectedEngines);
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

    private record FloodFillElement(BlockPos self, BlockPos source) {
    }

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
