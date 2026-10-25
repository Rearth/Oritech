package rearth.oritech.spaceage.simulation.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock;
import rearth.oritech.spaceage.block.cubesat.CubeSatBlockEntity;
import rearth.oritech.spaceage.block.vacuum_crafter.BlockPairController;
import rearth.oritech.spaceage.block.vacuum_crafter.VacuumCrafterBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.service.PhysicalCrafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Scans the rocket standing on a pad. Shared by the assembler launch and the splicer, which compacts rockets into cube-sats.
 * Scanning never changes the world; callers remove the scanned blocks once they commit.
 */
public final class RocketStructureScan {

    private final ServerLevel level;
    private final BlockPos pad;
    private final boolean shrink;
    private String issue = "";

    private RocketStructureScan(ServerLevel level, BlockPos pad, boolean shrink) {

        this.level = level;
        this.pad = pad;
        this.shrink = shrink;
    }

    /**
     * @param pad    the pad block fixing the local coordinates of a launch
     * @param shrink rejects cube-sats and moves the local origin to the rocket's lowest corner, so identical layouts match
     */
    public static Result scan(ServerLevel level, BlockPos pad, boolean shrink) {

        var scan = new RocketStructureScan(level, pad, shrink);
        var start = scan.findRocketStart();
        var result = start == null ? null : scan.gatherRocketData(start);
        return result == null ? new Result(null, Set.of(), scan.issue) : result;
    }

    public static void removeBlocks(ServerLevel level, Result result) {

        result.positions().forEach(pos -> {
            level.removeBlockEntity(pos);
            level.removeBlock(pos, false);
        });
    }

    private Result invalid(String issue) {

        this.issue = "status.oritech_space_age." + issue;
        return null;
    }

    private Result gatherRocketData(BlockPos start) {

        var startSegment = segmentFloodFill(start);
        if (!startSegment.fullyScanned || startSegment.blocks.isEmpty()) {
            OritechSpaceAge.LOGGER.warn("Unable to assemble invalid rocket at {}", pad);
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

            var couplingsValid = segment.fullyScanned;
            if (!couplingsValid) {
                OritechSpaceAge.LOGGER.warn("Unable to assemble rocket with invalid couplings at {}", pad);
                return null;
            }

            segments.put(segment.id, segment);

            for (var coupling : segment.couplings) {
                openCouplings.add(coupling.oppositeSide());
            }

        }

        if (!openCouplings.isEmpty()) {
            OritechSpaceAge.LOGGER.warn("Unable to assemble rocket at {}: coupling traversal limit reached", pad);
            return null;
        }

        connectRocketSegments(segments);

        if (!rocketConnectionsValid(segments)) {
            OritechSpaceAge.LOGGER.warn("Unable to assemble rocket with unconnected couplings at {}", pad);
            return null;
        }

        // Unconnected couplings are ordinary carried blocks and exposed docking ports.
        for (var segment : segments.values()) {
            var connected = new HashSet<FoundCoupling>();
            segment.connectedSegments.values().forEach(connected::addAll);
            segment.couplings.stream().filter(c -> !connected.contains(c)).forEach(c ->
                    segment.blocks.add(new FoundBlock(c.pos, level.getBlockState(c.pos))));
        }
        // Declared work cells stay owned by this segment, including empty freight destinations.
        var owners = new HashMap<BlockPos, UUID>();
        segments.values().forEach(segment -> segment.blocks.forEach(block -> owners.put(block.pos, segment.id)));
        var cargoCells = new HashSet<BlockPos>();
        for (var segment : segments.values()) {
            for (var controller : List.copyOf(segment.blocks)) {
                if (!(controller.state.getBlock() instanceof BlockPairController))
                    continue;
                for (var cell : BlockPairController.cells(controller.pos, controller.state)) {
                    var owner = owners.get(cell);
                    if (owner != null && !owner.equals(segment.id)) return invalid("cargo_segment");
                    var state = level.getBlockState(cell);
                    if (controller.state.is(SpaceAgeBlocks.CARGO)) {
                        if (!cargoCells.add(cell)) return invalid("cargo_overlap");
                        if (!PhysicalCrafting.passive(state)) return invalid("invalid_cargo_block");
                        segment.freight.add(cell);
                    }
                    // Internal couplings stay graph edges, even beside an unused crafter work face.
                    if (owner == null && !isCoupling(state)) {
                        segment.blocks.add(new FoundBlock(cell, state));
                        owners.put(cell, segment.id);
                    }
                }
            }
        }
        // Cube-sats only contain ordinary rockets, never other cube-sats.
        if (shrink && segments.values().stream().anyMatch(segment -> segment.blocks.stream().anyMatch(block -> block.state.is(SpaceAgeBlocks.CUBE_SAT))))
            return invalid("cube_sat_nested");

        var scannedSegments = new HashMap<UUID, ScannedSegmentData>();
        // One rollback transaction prevents aliases (including multiblock faces) counting a store twice.
        try (var transaction = Transaction.openRoot()) {
            var yields = new HashMap<FluidResource, Double>();
            level.recipeAccess().recipeMap().byType(RecipeContent.FUEL_GENERATOR.get()).forEach(holder -> {
                var input = holder.value().fluidInput().get();
                input.ingredient().fluids().forEach(fluid -> yields.merge(FluidResource.of(fluid), holder.value().time() / (double) input.amount(), Math::max));
            });
            var storageScan = new RocketStorageScan(yields);
            for (var segment : segments.values())
                scannedSegments.put(segment.id, scanSegmentContent(segment, transaction, storageScan));
        }

        var positions = new HashSet<BlockPos>();
        for (var segment : segments.values()) {
            segment.blocks.forEach(block -> positions.add(block.pos));
            segment.couplings.forEach(coupling -> positions.add(coupling.pos));
        }
        // The pad fixes local coordinates even when a new block changes the flood-fill start.
        var origin = shrink ? new BlockPos(positions.stream().mapToInt(BlockPos::getX).min().orElse(0), pad.getY() + 1,
                positions.stream().mapToInt(BlockPos::getZ).min().orElse(0)) : pad.above();
        var rocketData = createRocket(origin, segments, scannedSegments);
        OritechSpaceAge.LOGGER.debug("Scanned rocket with {} segments at {}", segments.size(), pad);
        return new Result(rocketData, Set.copyOf(positions), "");

    }

    private BlockPos findRocketStart() {

        // start at first found block above connected pads
        for (var padBlock : padFloodFill()) {

            var above = padBlock.above();

            var checkedState = level.getBlockState(above);
            if (checkedState.isAir() || isCoupling(checkedState)) continue;

            OritechSpaceAge.LOGGER.debug("Found start: " + above);
            return above;
        }

        return null;
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
    private ScannedSegmentData scanSegmentContent(RocketFloodSegment segment, Transaction transaction, RocketStorageScan storageScan) {

        long detectedRF = 0;
        long detectedFuel = 0;
        long rfCapacity = 0;
        long fuelCapacity = 0;
        var fuelWeight = 0d;
        var detectedEngines = 0;
        long staticWeight = 0;
        for (var blockData : segment.blocks) {
            var worldPos = blockData.pos();
            var worldState = blockData.state();
            var budget = segment.freight.contains(worldPos) ? new RocketStorageScan.Budget(0, 0, 0, 0, 0) : storageScan.probe(
                    level.getCapability(Capabilities.Fluid.BLOCK, worldPos, worldState, null, null),
                    level.getCapability(Capabilities.Energy.BLOCK, worldPos, worldState, null, null), transaction);
            detectedFuel += budget.fuel();
            detectedRF += budget.rf();
            fuelCapacity += budget.fuelCapacity();
            fuelWeight += budget.fuelWeight();
            rfCapacity += budget.rfCapacity();

            if (worldState.getBlock() instanceof RocketEngineBlock) {
                detectedEngines++;
            }

            // weight scan, folded cube-sats have a constant mass
            staticWeight += worldState.is(SpaceAgeBlocks.CUBE_SAT) ? (long) RocketLayout.mass(worldState)
                    : (long) Math.max(worldState.getDestroySpeed(level, worldPos), 0);

        }

        return new ScannedSegmentData(detectedFuel, detectedRF,
                fuelWeight, staticWeight, detectedEngines, fuelCapacity, rfCapacity);
    }

    // creates the actual ActiveRocketData, along with its segment data instances
    private ActiveRocketData createRocket(BlockPos origin, Map<UUID, RocketFloodSegment> segments,
                                          Map<UUID, ScannedSegmentData> scannedSegments) {

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
                            coupling.pos.subtract(origin), coupling.oppositeSide.subtract(origin), level.getBlockState(coupling.pos)));
                }
                couplings.put(connectedSegmentId, couplingData);
            });

            var structure = new StaticRocketSegment(segment.id, blocks, couplings, scannedData.staticWeight,
                    scannedData.engineCount, scannedData.availableRF, scannedData.availableFuelBurnTimeTicks);
            staticSegments.put(segment.id, new StaticRocketSegment(segment.id, blocks, couplings, scannedData.staticWeight,
                    scannedData.engineCount, scannedData.availableRF, scannedData.availableFuelBurnTimeTicks,
                    RocketExplosives.installed(structure), StaticRocketSegment.couplingAnchor(couplings)));
            var resources = new DynamicRocketSegment(
                    scannedData.availableFuelBurnTimeTicks,
                    scannedData.availableRF,
                    scannedData.currentFuelWeight,
                    segment.connectedSegments.keySet(), Map.of(), scannedData.fuelCapacity, scannedData.rfCapacity);
            dynamicSegments.put(segment.id, resources);
            for (var block : segment.blocks) {
                var entity = level.getBlockEntity(block.pos);
                if (entity instanceof VacuumCrafterBlockEntity crafter)
                    resources.crafters.put(block.pos.subtract(origin), new RocketControllerState(crafter.name(), Optional.empty()));
                // Carried cube-sats keep their contents, unlike other block entities.
                if (entity instanceof CubeSatBlockEntity cube && cube.data() != null)
                    resources.payloads.put(block.pos.subtract(origin), cube.data());
            }
        }

        var rocketData = new ActiveRocketData(staticSegments, dynamicSegments);
        rocketData.setLaunchPosition(origin);
        rocketData.setLaunchPadCenter(RocketRecovery.center(padFloodFill()).above());
        return rocketData;
    }

    private boolean rocketConnectionsValid(Map<UUID, RocketFloodSegment> segments) {

        for (var segment : segments.values()) {

            for (var connectedSegmentId : segment.connectedSegments.keySet()) {
                var connectedSegment = segments.get(connectedSegmentId);
                if (connectedSegment == null || !connectedSegment.connectedSegments.containsKey(segment.id))
                    return false;
            }
        }

        return true;
    }

    // returns all horizontally found pad blocks
    private Set<BlockPos> padFloodFill() {

        var directions = Direction.values();
        var limit = 1000;

        var openPositions = new ArrayList<BlockPos>();
        openPositions.add(pad);

        var visited = new HashSet<BlockPos>();
        visited.add(pad);
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

        return pos.getY() > pad.getY() && !state.isAir();
    }

    /**
     * @param rocket    the scanned rocket, or null if the scan failed
     * @param positions all world positions of the rocket, including couplings
     * @param issue     an optional translation key explaining a failed scan
     */
    public record Result(ActiveRocketData rocket, Set<BlockPos> positions, String issue) {

    }

    private record FloodFillElement(BlockPos self, BlockPos source) {

    }

    private record ScannedSegmentData(long availableFuelBurnTimeTicks, long availableRF, double currentFuelWeight,
                                      long staticWeight, int engineCount, long fuelCapacity, long rfCapacity) {

    }

    private static final class RocketFloodSegment {

        private final Set<BlockPos> freight = new HashSet<>();
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
