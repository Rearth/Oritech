package rearth.oritech.spaceage.block.assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.BlockPairController;
import rearth.oritech.spaceage.block.VacuumCrafterBlockEntity;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeRecipes;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.MissionController;
import rearth.oritech.spaceage.simulation.MissionSavedData;
import rearth.oritech.spaceage.simulation.PhysicalCrafting;
import rearth.oritech.spaceage.simulation.RocketControllerState;
import rearth.oritech.spaceage.simulation.RocketDocking;
import rearth.oritech.spaceage.simulation.RocketExplosives;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.RocketPerformanceCalculator;
import rearth.oritech.spaceage.simulation.RocketStorageScan;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;
import rearth.oritech.spaceage.simulation.StaticRocketSegment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class RocketAssemblerBlockEntity extends BlockEntity implements MenuProvider {

    private String scanIssue = "";

    public RocketAssemblerBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.ROCKET_ASSEMBLER.get(), pos, state);
    }

    public String scanIssue() {

        return scanIssue;
    }

    private ActiveRocketData invalidCargo(String issue) {

        scanIssue = "status.oritech_space_age." + issue;
        return null;
    }

    public boolean assemble(ServerPlayer player, SpaceSimulation.FlightPlan plan) {

        OritechSpaceAge.LOGGER.debug("Starting assembling process");

        if (!(level instanceof ServerLevel) || level.dimension() != Level.OVERWORLD)
            return false;

        var start = findRocketStart();
        if (start == null) return false;

        // Launch readiness must be checked before fluids are committed and blocks are removed. The second scan is
        // intentional: previews are side-effect free, while only a launch that passed validation may consume data.
        var preview = gatherRocketData(start, false);
        if (preview == null) {
            if (!scanIssue.isEmpty()) player.sendOverlayMessage(Component.translatable(scanIssue));
            return false;
        }
        var readiness = RocketPerformanceCalculator.getLaunchReadiness(preview, plan);
        if (readiness != RocketPerformanceCalculator.LaunchReadiness.READY) {
            OritechSpaceAge.LOGGER.warn("Rocket launch at {} failed: {}", worldPosition, readiness.failureReason());
            return false;
        }

        var system = SpaceSimulationSavedData.getForPlayer(player);
        var validation = RocketFlightPlanRules.inspect(plan, preview, system.createObjectData(), true, null,
                ((ServerLevel) level).recipeAccess().recipeMap().byType(SpaceAgeRecipes.VACUUM_CRAFTING.get()),
                RocketDocking.targets(MissionSavedData.get(player.level().getServer()), player.getUUID()));
        if (!validation.valid()) {
            player.sendOverlayMessage(validation.issues().getFirst().description());
            return false;
        }
        var validated = validation.plan();
        if (validated == null || validated.root().actions().isEmpty()
                || validated.branches().stream().mapToInt(b -> b.actions().size()).sum() != plan.branches().stream().mapToInt(b -> b.actions().size()).sum())
            return false;
        validated = MissionController.resolveOrbitSlots(validated, MissionSavedData.get(player.level().getServer()), player.getUUID(), preview.getRocketId());
        var first = validated.root().actions().getFirst();
        if (first.type() != SpaceSimulation.ActionType.NAVIGATE_TO)
            return false;
        var firstPlan = validated.withBranches(List.of(validated.root().withActions(List.of(first))));
        var predicted = RocketFlightPathCalculator.calculateFrom(preview, system.createObjectData(), firstPlan, null,
                ((ServerLevel) level).recipeAccess().recipeMap().byType(SpaceAgeRecipes.VACUUM_CRAFTING.get()),
                RocketDocking.targets(MissionSavedData.get(player.level().getServer()), player.getUUID()));
        if (predicted.paths().isEmpty() || predicted.paths().getFirst().terminalState().preventsLaunch()) return false;
        var result = gatherRocketData(start, true);
        if (result == null) {
            OritechSpaceAge.LOGGER.warn("Rocket Assembly Failed");
            return false;
        }

        MissionController.launch((ServerLevel) level, player.getUUID(), result, validated, start);
        return true;
    }

    public ActiveRocketData createPreview() {

        if (!(level instanceof ServerLevel)) return null;

        var start = findRocketStart();
        return start == null ? null : gatherRocketData(start, false);
    }

    private ActiveRocketData gatherRocketData(BlockPos start, boolean consumeResources) {

        scanIssue = "";

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
                    if (owner != null && !owner.equals(segment.id)) return invalidCargo("cargo_segment");
                    var state = level.getBlockState(cell);
                    if (controller.state.is(SpaceAgeBlocks.CARGO)) {
                        if (!cargoCells.add(cell)) return invalidCargo("cargo_overlap");
                        if (!PhysicalCrafting.passive(state)) return invalidCargo("invalid_cargo_block");
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
        var scannedSegments = new HashMap<UUID, ScannedSegmentData>();
        // One rollback transaction prevents aliases (including multiblock faces) counting a store twice.
        try (var transaction = Transaction.openRoot()) {
            var yields = new HashMap<FluidResource, Double>();
            ((ServerLevel) level).recipeAccess().recipeMap().byType(RecipeContent.FUEL_GENERATOR.get()).forEach(holder -> {
                var input = holder.value().fluidInput().get();
                input.ingredient().fluids().forEach(fluid -> yields.merge(FluidResource.of(fluid), holder.value().time() / (double) input.amount(), Math::max));
            });
            var storageScan = new RocketStorageScan(yields);
            for (var segment : segments.values())
                scannedSegments.put(segment.id, scanSegmentContent(segment, transaction, storageScan));
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

            // weight scan
            staticWeight += (long) Math.max(worldState.getDestroySpeed(level, worldPos), 0);

        }

        return new ScannedSegmentData(detectedFuel, detectedRF,
                fuelWeight, staticWeight, detectedEngines, fuelCapacity, rfCapacity);
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
                            coupling.pos.subtract(origin), coupling.oppositeSide.subtract(origin), level.getBlockState(coupling.pos)));
                }
                couplings.put(connectedSegmentId, couplingData);
            });

            var structure = new StaticRocketSegment(segment.id, blocks, couplings, scannedData.staticWeight,
                    scannedData.engineCount, scannedData.availableRF, scannedData.availableFuelBurnTimeTicks);
            staticSegments.put(segment.id, new StaticRocketSegment(segment.id, blocks, couplings, scannedData.staticWeight,
                    scannedData.engineCount, scannedData.availableRF, scannedData.availableFuelBurnTimeTicks,
                    RocketExplosives.installed(structure)));
            dynamicSegments.put(segment.id, new DynamicRocketSegment(
                    scannedData.availableFuelBurnTimeTicks,
                    scannedData.availableRF,
                    scannedData.currentFuelWeight,
                    segment.connectedSegments.keySet(), Map.of(), scannedData.fuelCapacity, scannedData.rfCapacity));
            for (var block : segment.blocks) {
                if (level.getBlockEntity(block.pos) instanceof VacuumCrafterBlockEntity crafter)
                    dynamicSegments.get(segment.id).crafters.put(block.pos.subtract(origin), new RocketControllerState(crafter.name(), Optional.empty()));
            }
        }

        var rocketData = new ActiveRocketData(staticSegments, dynamicSegments);
        rocketData.setLaunchPosition(origin);
        if (removeBlocks) {
            var blocksToRemove = new HashSet<BlockPos>();
            for (var segment : segments.values()) {
                segment.blocks.forEach(block -> blocksToRemove.add(block.pos));
                segment.couplings.forEach(coupling -> blocksToRemove.add(coupling.pos));
            }
            blocksToRemove.forEach(pos -> {
                level.removeBlockEntity(pos);
                level.removeBlock(pos, false);
            });
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
