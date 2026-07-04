package rearth.oritech.block.entity.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.apache.commons.lang3.time.StopWatch;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeConnectionBlock;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.*;

public class ItemPipeInterfaceEntity extends ExtractablePipeInterfaceEntity {
    private static final int TRANSFER_AMOUNT = OritechConfig.itemPipeTransferAmount.get();
    private static final int TRANSFER_PERIOD = OritechConfig.itemPipeIntervalDuration.get();

    private List<BlockCapabilityCache<ResourceHandler<ItemResource>, Direction>> filteredItemTargetsCached;

    // item path cache (invalidated on network update)
    private final HashMap<BlockPos, Tuple<ArrayList<BlockPos>, Integer>> cachedTransferPaths = new HashMap<>();
    private final boolean renderItems;

    private static final HashMap<BlockPos, Long> blockedUntil = new HashMap<>();   // used to fake item movement in transparent pipes

    // client only
    public Set<RenderStackData> activeStacks = new HashSet<>();

    public ItemPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ITEM_PIPE_ENTITY.get(), pos, state);
        this.renderItems = state.getBlock().equals(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION);

    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        var block = (ExtractablePipeConnectionBlock) state.getBlock();
        if (level.isClientSide() || !block.isExtractable(state))
            return;

        var boosted = isBoostAvailable();

        // boosted pipe works every tick, otherwise only every N tick
        if (level.getGameTime() % TRANSFER_PERIOD != 0 && !boosted)
            return;

        var data = ItemPipeBlock.ITEM_PIPE_DATA.get(level.dimension().identifier());
        if (data == null) return;   // this should never happen

        var targets = findNetworkTargets(pos, data);

        if (targets == null) {
            System.err.println("Yeah your pipe network likely is too long (or something else errored. At: " + this.getBlockPos());
            return;
        }

        refreshTargetCaches(level, targets);

        var moveCapacity = isBoostAvailable() ? 64 : TRANSFER_AMOUNT;
        // var hasMotor = state.getValue(ItemPipeConnectionBlock.HAS_MOTOR);   // todo benchmark / decide if motor needs to be kept

        // do the whole thing for each direction (neighboring item container) items are taken from (usually just 1, but could be multiple)
        // tries to extract from each side (for each slot on that source machine) and then tries to insert it to any matching containers
        for (var machineDirection : data.getMachineDirections(pos)) {
            if (!block.isSideExtracting(state, machineDirection)) continue;

            var sourcePos = pos.relative(machineDirection);
            var accessDirection = machineDirection.getOpposite();

            // blocking for visual delays if needed
            var blockedTimer = blockedUntil.getOrDefault(sourcePos, 0L);
            if (level.getGameTime() < blockedTimer) continue;

            if (blockedTimer > 0)   // if timer has expired but was set
                blockedUntil.remove(sourcePos);

            var machineMoveCapacity = moveCapacity;

            var sourceBlock = level.getBlockState(sourcePos);

            var sourceContainer = level.getCapability(Capabilities.Item.BLOCK, sourcePos, sourceBlock, null, accessDirection);
            if (sourceContainer == null) continue;

            // one transaction per machine that is being extracted from
            try (var transaction = Transaction.openRoot()) {
                var moved = 0;
                // do the whole thing for each slot in the source container
                for (int i = 0; i < sourceContainer.size(); i++) {
                    var extractedResource = sourceContainer.getResource(i);
                    if (extractedResource.isEmpty() || machineMoveCapacity <= 0) continue;

                    // with directly canceled transaction just to figure out how much can be moved / extracted
                    var availableAmount = 0;
                    try (var simulation = Transaction.open(transaction)) {
                        availableAmount = sourceContainer.extract(i, extractedResource, machineMoveCapacity, simulation);
                        if (availableAmount <= 0) continue;
                    }


                    // go through all targets and try to insert
                    for (var cachedTarget : filteredItemTargetsCached) {
                        var targetContainer = cachedTarget.getCapability();
                        if (targetContainer == null) continue;

                        var inserted = targetContainer.insert(extractedResource, availableAmount, transaction);
                        var taken = sourceContainer.extract(i, extractedResource, inserted, transaction);

                        if (taken != inserted) {
                            Oritech.LOGGER.warn("Item Pipe Insertion Error! Handler Misbehaving. At: {}, inserted: {}, extracted: {},  amount: {}. From pipe at: {}",
                                    cachedTarget.pos(), inserted, taken, availableAmount, worldPosition);
                            return;  // this should never happen
                        }

                        availableAmount -= inserted;
                        moved += inserted;

                        if (inserted > 0) {
                            onItemMoved(worldPosition, sourcePos, cachedTarget.pos(), data.getNetworkNodes(worldPosition), level, extractedResource.getItem(), inserted);
                            machineMoveCapacity -= moved;
                        }

                        // this slot's extracted amount is fully distributed, no point probing the remaining (farther) targets
                        // or if there's been enough moved already
                        if (availableAmount <= 0 || machineMoveCapacity <= 0) break;
                    }
                }
                if (moved > 0) {
                    transaction.commit();
                    onBoostUsed();
                }

            }
        }

    }

    private void refreshTargetCaches(Level level, Set<GenericPipeInterfaceEntity.PipeNetworkTarget> targets) {
        var netHash = targets.hashCode();
        if (netHash == filteredTargetsNetHash && filteredItemTargetsCached != null) {
            return;
        }

        filteredItemTargetsCached = targets.stream()
                .filter(target -> {
                    var pipeState = level.getBlockState(target.getPipePos());
                    if (!(pipeState.getBlock() instanceof ItemPipeConnectionBlock itemBlock))
                        return true;
                    var extracting = itemBlock.isSideExtracting(pipeState, target.getPipeFacing());
                    return !extracting;
                })
                .map(target -> BlockCapabilityCache.create(Capabilities.Item.BLOCK, (ServerLevel) level, target.machinePos(), target.insertedFrom()))
                .sorted(Comparator.comparingInt(target -> target.pos().distManhattan(worldPosition)))
                .toList();

        filteredTargetsNetHash = netHash;
        cachedTransferPaths.clear();
    }

    private void onItemMoved(BlockPos startPos, BlockPos from, BlockPos to, Set<BlockPos> network, Level level, Item moved, int movedCount) {
        if (!renderItems) return;
        var path = cachedTransferPaths.computeIfAbsent(to, ignored -> calculatePath(startPos, from, to, network, level));
        if (path == null) return;

        var codedPath = path.getA();
        var pathLength = 0;
        for (int i = 0; i < codedPath.size() - 1; i++) {
            var pathPos = codedPath.get(i);
            var nextPathPos = codedPath.get(i + 1);
            pathLength += nextPathPos.distManhattan(pathPos);
        }
        var packet = new RenderStackData(worldPosition, new ItemStack(moved, movedCount), codedPath, level.getGameTime(), pathLength);
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, ChunkPos.containing(worldPosition), packet);

        // used to be called on if target was empty, but check has been skipped now due to the fact that we need don't check individual slots manually anymore with new
        // transfer api
        var arrivalTime = level.getGameTime() + (int) calculatePathLength(path.getB());
        blockedUntil.putIfAbsent(to, arrivalTime);

    }

    public static double calculatePathLength(int pathBlocksCount) {
        return Math.pow(pathBlocksCount * 32, 0.6);
    }

    // return pair is optimized path and total path length
    private static Tuple<ArrayList<BlockPos>, Integer> calculatePath(BlockPos startPos, BlockPos from, BlockPos to, Set<BlockPos> network, Level level) {

        if (network.isEmpty() || !network.contains(startPos)) {
            Oritech.LOGGER.warn("tried to calculate invalid item pipe from: {} to {} with network size: {}", startPos, to, network.size());
            return null;
        }

        var length = 1;

        var path = new LinkedList<BlockPos>();
        path.add(startPos);

        var visited = new HashSet<BlockPos>();

        var watch = new StopWatch();
        watch.start();

        for (int i = 0; i < network.size() * 3; i++) {

            var currentPos = path.peekLast();

            if (currentPos == null || currentPos.distManhattan(to) == 1) { // target reached (or invalid)
                break;
            }

            visited.add(currentPos);

            var currentPosState = level.getBlockState(currentPos);
            if (!(currentPosState.getBlock() instanceof AbstractPipeBlock pipeBlock)) break;

            // collect potential edges in graph, ordered by basic cost heuristic (manhattan dist to target)
            var openEdges = getNeighbors(currentPos).stream()
                    .filter(network::contains)
                    .filter(candidate -> !visited.contains(candidate))
                    .filter(candidate -> pipeBlock.isConnectingInDirection(currentPosState, getDirectionFromOffset(currentPos, candidate), currentPos, level, false))
                    .sorted(Comparator.comparingInt(a -> a.distManhattan(to)))
                    .toArray(BlockPos[]::new);

            if (openEdges.length == 0) {    // dead end, go back
                path.pollLast();
            } else {
                path.add(openEdges[0]);
                length++;
            }

        }

        path.addFirst(from);
        path.add(to);

        // compact path (by removing straight segments)
        var result = optimizePath(path);

        watch.stop();

        Oritech.LOGGER.debug("pathsize: {} success: {} time ms: {}", result.size(), path.size() > 2, watch.getNanoTime() / 1_000_000f);
        return new Tuple<>(result, path.size());
    }

    private static ArrayList<BlockPos> optimizePath(LinkedList<BlockPos> path) {
        var result = new ArrayList<BlockPos>();
        if (path.isEmpty()) {
            return result;
        }

        var iterator = path.iterator();
        var first = iterator.next();
        result.add(first);

        if (!iterator.hasNext()) {
            return result;
        }

        var current = iterator.next();
        var currentDirection = current.subtract(first);

        while (iterator.hasNext()) {
            var next = iterator.next();
            var nextDirection = next.subtract(current);

            if (!nextDirection.equals(currentDirection)) {
                result.add(current);
                currentDirection = nextDirection;
            }

            current = next;
        }

        result.add(current);
        return result;
    }

    // returns all neighboring positions except up
    private static List<BlockPos> getNeighbors(BlockPos pos) {
        return Arrays.asList(pos.below(), pos.above(), pos.north(), pos.east(), pos.south(), pos.west());
    }

    private static Direction getDirectionFromOffset(BlockPos self, BlockPos target) {
        var offset = target.subtract(self);
        return Direction.getApproximateNearest(offset.getX(), offset.getY(), offset.getZ());
    }

    public static void receiveVisualItemsPacket(RenderStackData message, IPayloadContext context) {
        var level = context.player().level();
        var blockEntity = level.getBlockEntity(message.self, BlockEntitiesContent.ITEM_PIPE_ENTITY.get());
        if (blockEntity.isPresent()) {
            var pipeEntity = blockEntity.get();
            // use local time for moved item to avoid rendering issues caused by lag
            pipeEntity.activeStacks.add(new RenderStackData(pipeEntity.worldPosition, message.rendered, message.path, level.getGameTime(), message.pathLength));
        }
    }

    @Override
    public void setChanged() {
        if (this.level != null)
            level.blockEntityChanged(worldPosition);
    }

    public record RenderStackData(BlockPos self, ItemStack rendered, List<BlockPos> path, Long startedAt,
                                  int pathLength) implements CustomPacketPayload {

        public static final Type<RenderStackData> PIPE_ITEMS_ID = new Type<>(Oritech.id("pipe_items"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PIPE_ITEMS_ID;
        }
    }
}
