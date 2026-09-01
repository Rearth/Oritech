package rearth.oritech.spaceage.block.assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.joml.Vector2i;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.*;

import java.util.*;

public class RocketAssemblerBlockEntity extends BlockEntity implements MenuProvider {

    public RocketAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceAgeBlockEntities.ROCKET_ASSEMBLER.get(), pos, state);
    }

    public boolean assemble() {

        OritechSpaceAge.LOGGER.debug("Starting assembling process");

        if (!(level instanceof ServerLevel)) return false;

        var start = findRocketStart();
        if (start == null) return false;

        var result = gatherRocketData(start, true);

        if (result == null) {
            OritechSpaceAge.LOGGER.warn("Rocket Assembly Failed");
            return false;
        }

        var flightPlan = new RocketFlightPlan(start, new Vector2i(20, 1000));

        RocketSimulationController.launchRocket((ServerLevel) level, result, flightPlan);
        return true;
    }

    public ActiveRocketData createPreview() {
        if (!(level instanceof ServerLevel)) return null;

        var start = findRocketStart();
        return start == null ? null : gatherRocketData(start, false);
    }

    private ActiveRocketData gatherRocketData(BlockPos start, boolean consumeResources) {


        var startSegment = segmentFloodFill(start);
        if (!startSegment.fullyScanned || startSegment.blocks.isEmpty() || !segmentCouplingsValid(startSegment)) {
            OritechSpaceAge.LOGGER.warn("Unable to assemble invalid rocket at {}", worldPosition);
            return null;
        }

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

            var couplingsValid = segment.fullyScanned && segmentCouplingsValid(segment);
            if (!couplingsValid) {
                OritechSpaceAge.LOGGER.warn("Unable to assemble rocket with invalid couplings at {}", worldPosition);
                return null;
            }

            segments.put(segment.id, segment);

            for (var coupling : segment.couplings) {
                openCouplings.add(coupling.oppositeSide());
            }

        }

        if (!openCouplings.isEmpty()) {
            OritechSpaceAge.LOGGER.warn("Unable to assemble rocket at {}: coupling traversal limit reached", worldPosition);
            return null;
        }

        connectRocketSegments(segments);

        if (!rocketConnectionsValid(segments)) {
            OritechSpaceAge.LOGGER.warn("Unable to assemble rocket with unconnected couplings at {}", worldPosition);
            return null;
        }

        var scannedSegments = new HashMap<UUID, ScannedSegmentData>();
        for (var segment : segments.values()) {
            scannedSegments.put(segment.id, scanSegmentContent(segment, consumeResources));
        }

        var rocketData = createRocket(start, segments, scannedSegments, consumeResources);
        OritechSpaceAge.LOGGER.debug("Assembled rocket with {} segments at {}", segments.size(), worldPosition);
        return rocketData;

    }

    private BlockPos findRocketStart() {

        // start at first found block above connected pads
        var facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        var padBlocks = padFloodFill(worldPosition.relative(facing));
        var start = worldPosition;

        for (var padBlock : padBlocks) {

            var above = padBlock.above();

            var checkedState = level.getBlockState(above);
            if (checkedState.isAir() || isCoupling(checkedState)) continue;

            OritechSpaceAge.LOGGER.debug("Found start: " + above);
            start = above;

            break;
        }

        if (start.equals(worldPosition)) return null;
        return start;
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
    private ScannedSegmentData scanSegmentContent(RocketFloodSegment segment, boolean consumeResources) {

        // value is the burn time of the fuel type (per ml)
        var fuelTypes = new HashMap<FluidIngredient, Float>();
        var fuelGenRecipes = ((ServerLevel) level).recipeAccess().recipeMap().byType(RecipeContent.FUEL_GENERATOR.get());
        fuelGenRecipes.forEach(holder -> fuelTypes.put(holder.value().fluidInput().get().ingredient(), holder.value().time() / (float) holder.value().fluidInput().get().amount()));

        var detectedRF = 0L;
        var detectedFuels = new HashMap<FluidType, Long>(); // value is the total burn time available for the type on the segment
        var detectedEngines = 0;
        var staticWeight = 0L;
        var fuelWeight = 0f;

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
                            if (taken > 0) {
                                detectedFuels.merge(fluidType, (long) takenBurnTime, Long::sum);

                                // add weight
                                var amountInBuckets = taken / (float) FluidType.BUCKET_VOLUME;
                                var densityRelativeToWater = Math.max(fluidType.getDensity(), 0) / 1000f;
                                fuelWeight += amountInBuckets * densityRelativeToWater;
                            }

                            totalTaken += taken;
                        }
                    }

                    if (consumeResources && totalTaken > 0) {
                        transaction.commit();
                    }

                }
            }

            // energy scan
            var energyCandidate = level.getCapability(Capabilities.Energy.BLOCK, worldPos, worldState, null, null);
            if (energyCandidate != null) {
                detectedRF += energyCandidate.getAmountAsLong();
            }

            if (worldState.getBlock() instanceof RocketEngineBlock) {
                detectedEngines++;
            }

            // weight scan
            staticWeight += (long) Math.max(worldState.getDestroySpeed(level, worldPos), 0);

        }

        OritechSpaceAge.LOGGER.debug(
                "Collected stats for rocket segment {}: weight={}, fuelWeight={}, fuels={}, energy={}, engines={}",
                segment.id, staticWeight, detectedFuels, fuelWeight, detectedRF, detectedEngines);

        var availableFuelBurnTime = detectedFuels.values().stream().mapToLong(Long::longValue).sum();
        return new ScannedSegmentData(availableFuelBurnTime, detectedRF, (long) Math.ceil(fuelWeight), staticWeight, detectedEngines);
    }

    // removes the blocks from the world, and create the actual ActiveRocketData, along with its segment data instances
    private ActiveRocketData createRocket(BlockPos origin, Map<UUID, RocketFloodSegment> segments,
                                          Map<UUID, ScannedSegmentData> scannedSegments, boolean removeBlocks) {

        var staticSegments = new HashMap<UUID, StaticRocketSegment>();
        var dynamicSegments = new HashMap<UUID, DynamicRocketSegment>();

        for (var segment : segments.values()) {
            var scannedData = scannedSegments.get(segment.id);
            if (scannedData == null) {
                throw new IllegalStateException("Missing scanned data for rocket segment " + segment.id);
            }

            var blocks = new HashSet<StaticRocketSegment.BlockData>();
            for (var block : segment.blocks) {
                blocks.add(new StaticRocketSegment.BlockData(block.pos.subtract(origin), block.state));
            }

            var couplings = new HashMap<UUID, Set<StaticRocketSegment.CouplingData>>();
            segment.connectedSegments.forEach((connectedSegmentId, foundCouplings) -> {
                var couplingData = new HashSet<StaticRocketSegment.CouplingData>();
                for (var coupling : foundCouplings) {
                    couplingData.add(new StaticRocketSegment.CouplingData(
                            coupling.pos.subtract(origin), coupling.oppositeSide.subtract(origin)));
                }
                couplings.put(connectedSegmentId, couplingData);
            });

            staticSegments.put(segment.id, new StaticRocketSegment(
                    segment.id, blocks, couplings, scannedData.staticWeight, scannedData.engineCount));
            dynamicSegments.put(segment.id, new DynamicRocketSegment(
                    scannedData.availableFuelBurnTimeTicks,
                    scannedData.availableRF,
                    scannedData.currentFuelWeight,
                    segment.connectedSegments.keySet()));
        }

        var rocketData = new ActiveRocketData(staticSegments, dynamicSegments);

        if (removeBlocks) {
            var blocksToRemove = new HashSet<BlockPos>();
            for (var segment : segments.values()) {
                segment.blocks.forEach(block -> blocksToRemove.add(block.pos));
                segment.couplings.forEach(coupling -> blocksToRemove.add(coupling.pos));
            }
            blocksToRemove.forEach(pos -> level.removeBlock(pos, false));
        }

        return rocketData;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new RocketAssemblerMenu(syncId, playerInventory, this, createPreview());
    }

    // ensures no couples connect to the segment itself
    private boolean segmentCouplingsValid(RocketFloodSegment segment) {
        for (var coupling : segment.couplings) {
            if (segment.blocks.stream().anyMatch(block -> block.pos.equals(coupling.oppositeSide))) return false;
        }

        return true;
    }

    private boolean rocketConnectionsValid(Map<UUID, RocketFloodSegment> segments) {
        for (var segment : segments.values()) {
            var connectedCouplingCount = segment.connectedSegments.values().stream().mapToInt(Set::size).sum();
            if (connectedCouplingCount != segment.couplings.size()) return false;

            for (var connectedSegmentId : segment.connectedSegments.keySet()) {
                var connectedSegment = segments.get(connectedSegmentId);
                if (connectedSegment == null || !connectedSegment.connectedSegments.containsKey(segment.id))
                    return false;
            }
        }

        return true;
    }

    // returns all horizontally found pad blocks
    private Set<BlockPos> padFloodFill(BlockPos start) {

        var directions = Direction.values();
        var limit = 1000;

        var openPositions = new ArrayList<BlockPos>();
        openPositions.add(start);

        var visited = new HashSet<BlockPos>();
        visited.add(start);
        var results = new LinkedHashSet<BlockPos>();

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

    // finds and scans all blocks of a segment  (e.g. stops the fill at any couplings)
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

        return new RocketFloodSegment(results, couplings, UUID.randomUUID(), openPositions.isEmpty());

    }

    private boolean isCoupling(BlockState state) {
        return state.is(SpaceAgeBlocks.ROCKET_COUPLING);
    }

    private boolean isValidRocketBlock(BlockState state, BlockPos pos) {
        return pos.getY() > this.worldPosition.getY() && !state.isAir();
    }

    private record FloodFillElement(BlockPos self, BlockPos source) {
    }

    private record ScannedSegmentData(long availableFuelBurnTimeTicks, long availableRF, long currentFuelWeight,
                                      long staticWeight, int engineCount) {
    }

    private static final class RocketFloodSegment {

        private final Set<FoundBlock> blocks;
        private final Set<FoundCoupling> couplings;
        private final UUID id;
        private final boolean fullyScanned;
        private final Map<UUID, Set<FoundCoupling>> connectedSegments = new HashMap<>();    // contains all connected segments via segmentId and the couplings (on itself) that connect to it.

        private RocketFloodSegment(Set<FoundBlock> blocks, Set<FoundCoupling> couplings, UUID id, boolean fullyScanned) {
            this.blocks = blocks;
            this.couplings = couplings;
            this.id = id;
            this.fullyScanned = fullyScanned;
        }

        private Set<FoundCoupling> couplings() {
            return couplings;
        }

        @Override
        public String toString() {
            return "RocketFloodSegment{" +
                    "segmentId=" + id +
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
