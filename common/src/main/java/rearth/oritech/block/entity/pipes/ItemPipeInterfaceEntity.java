package rearth.oritech.block.entity.pipes;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.apache.commons.lang3.time.StopWatch;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeConnectionBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.api.item.ItemApi;

import java.util.*;
import java.util.stream.IntStream;

public class ItemPipeInterfaceEntity extends ExtractablePipeInterfaceEntity {
    
    private static final int TRANSFER_AMOUNT = Oritech.CONFIG.itemPipeTransferAmount();
    private static final int TRANSFER_PERIOD = Oritech.CONFIG.itemPipeIntervalDuration();
    
    private List<Pair<ItemApi.InventoryStorage, BlockPos>> filteredTargetItemStorages;
    
    // item path cache (invalidated on network update)
    private final HashMap<BlockPos, Pair<ArrayList<BlockPos>, Integer>> cachedTransferPaths = new HashMap<>();
    private final boolean renderItems;
    
    private static final HashMap<BlockPos, Long> blockedUntil = new HashMap<>();   // used to fake item movement in transparent pipes
    
    // client only
    public Set<RenderStackData> activeStacks = new HashSet<>();
    
    public ItemPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ITEM_PIPE_ENTITY, pos, state);
        this.renderItems = state.getBlock().equals(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION);
        
    }
    
    @SuppressWarnings("DataFlowIssue")
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        var block = (ExtractablePipeConnectionBlock) state.getBlock();
        if (world.isClient || !block.isExtractable(state))
            return;
        
        // boosted pipe works every tick, otherwise only every N tick
        if ((world.getTime() + this.pos.asLong()) % TRANSFER_PERIOD != 0 && !isBoostAvailable())
            return;
        
        // find first itemstack from connected invs (that can be extracted)
        // try to move it to one of the destinations
        
        var data = ItemPipeBlock.ITEM_PIPE_DATA.getOrDefault(world.getRegistryKey().getValue(), new PipeNetworkData());
        
        var sources = data.machineInterfaces.getOrDefault(pos, new HashSet<>());
        var stackToMove = ItemStack.EMPTY;
        ItemApi.InventoryStorage moveFromInventory = null;
        BlockPos takenFrom = null;
        var moveCapacity = isBoostAvailable() ? 64 : TRANSFER_AMOUNT;
        
        for (var sourcePos : sources) {
            
            var blockedTimer = blockedUntil.getOrDefault(sourcePos, 0L);
            if (world.getTime() < blockedTimer) continue;
            
            if (blockedTimer > 0)   // if timer has expired but was set
                blockedUntil.remove(sourcePos);
            
            var offset = pos.subtract(sourcePos);
            var direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ());
            if (!block.isSideExtractable(state, direction.getOpposite())) continue;
            var inventory = ItemApi.BLOCK.find(world, sourcePos, direction);
            if (inventory == null || !inventory.supportsExtraction()) continue;
            
            var firstStack = getFirstStack(inventory, moveCapacity);
            
            if (!firstStack.isEmpty()) {
                stackToMove = firstStack;
                moveFromInventory = inventory;
                takenFrom = sourcePos;
                break;
            }
            
        }
        
        if (stackToMove.isEmpty()) return;
        
        var targets = findNetworkTargets(pos, data);
        if (targets == null) {
            System.err.println("Yeah your pipe network likely is too long. At: " + this.getPos());
            return;
        }
        
        var netHash = targets.hashCode();
        
        if (netHash != filteredTargetsNetHash || filteredTargetItemStorages == null) {
            filteredTargetItemStorages = targets.stream()
                                           .filter(target -> {
                                               var direction = target.getRight();
                                               var pipePos = target.getLeft().add(direction.getVector());
                                               var pipeState = world.getBlockState(pipePos);
                                               if (!(pipeState.getBlock() instanceof ItemPipeConnectionBlock itemBlock))
                                                   return true;   // edge case, this should never happen
                                               var extracting = itemBlock.isSideExtractable(pipeState, direction.getOpposite());
                                               return !extracting;
                                           })
                                           .map(target -> new Pair<>(ItemApi.BLOCK.find(world, target.getLeft(), target.getRight()), target.getLeft()))
                                           .filter(obj -> Objects.nonNull(obj.getLeft()) && obj.getLeft().supportsInsertion())
                                           .sorted(Comparator.comparingInt(a -> a.getRight().getManhattanDistance(pos)))
                                           .toList();
            
            filteredTargetsNetHash = netHash;
            cachedTransferPaths.clear();
        }
        
        var toMove = stackToMove.getCount();
        var moved = 0;
        
        for (var storagePair : filteredTargetItemStorages) {
            if (storagePair.getLeft().equals(moveFromInventory)) continue;    // skip when targeting same machine
            
            var targetStorage = storagePair.getLeft();
            var wasEmptyStorage = IntStream.range(0, targetStorage.getSlotCount()).allMatch(slot -> targetStorage.getStackInSlot(slot).isEmpty());
            
            
            var inserted = targetStorage.insert(stackToMove, false);
            toMove -= inserted;
            moved += inserted;
            
            if (inserted > 0) {
                onItemMoved(this.pos, takenFrom, storagePair.getRight(), data.pipeNetworks.getOrDefault(data.pipeNetworkLinks.getOrDefault(this.pos, 0), new HashSet<>()), world, stackToMove.getItem(), inserted, wasEmptyStorage);
            }
            
            if (toMove <= 0) break;  // target has been found for all items
        }
        var extracted = moveFromInventory.extract(stackToMove.copyWithCount(moved), false);
        
        if (extracted != moved) {
            Oritech.LOGGER.warn("Invalid state while transferring inventory. Caused at position {}", pos);
        }
        
        if (moveCapacity > TRANSFER_AMOUNT) onBoostUsed();
        
    }
    
    private void onItemMoved(BlockPos startPos, BlockPos from, BlockPos to, Set<BlockPos> network, World world, Item moved, int movedCount, boolean wasEmpty) {
        if (!renderItems) return;
        var path = cachedTransferPaths.computeIfAbsent(to, ignored -> calculatePath(startPos, from, to, network, world));
        if (path == null) return;
        
        var codedPath = path.getLeft().stream().map(BlockPos::asLong).toList();
        var packet = new NetworkContent.ItemPipeVisualTransferPacket(startPos, codedPath, new ItemStack(moved, movedCount));
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(packet);
        
        if (wasEmpty) {
            var arrivalTime = world.getTime() + (int) calculatePathLength(path.getRight());
            blockedUntil.putIfAbsent(to, arrivalTime);
        }
        
    }
    
    public static double calculatePathLength(int pathBlocksCount) {
        return Math.pow(pathBlocksCount * 16, 0.6);
    }
    
    // return pair is optimized path and total path length
    private static Pair<ArrayList<BlockPos>, Integer> calculatePath(BlockPos startPos, BlockPos from, BlockPos to, Set<BlockPos> network, World world) {
        
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
            
            if (currentPos == null || currentPos.getManhattanDistance(to) == 1) { // target reached (or invalid)
                break;
            }
            
            visited.add(currentPos);
            
            var currentPosState = world.getBlockState(currentPos);
            if (!(currentPosState.getBlock() instanceof AbstractPipeBlock pipeBlock)) break;
            
            // collect potential edges in graph, ordered by basic cost heuristic (manhattan dist to target)
            var openEdges = getNeighbors(currentPos).stream()
                              .filter(network::contains)
                              .filter(candidate -> !visited.contains(candidate))
                              .filter(candidate -> pipeBlock.isConnectingInDirection(currentPosState, getDirectionFromOffset(currentPos, candidate), currentPos, world, false))
                              .sorted(Comparator.comparingInt(a -> a.getManhattanDistance(to)))
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
        return new Pair<>(result, path.size());
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
        return Arrays.asList(pos.down(), pos.up(), pos.north(), pos.east(), pos.south(), pos.west());
    }
    
    private static Direction getDirectionFromOffset(BlockPos self, BlockPos target) {
        var offset = target.subtract(self);
        return Direction.fromVector(offset.getX(), offset.getY(), offset.getZ());
    }
    
    public void handleVisualTransferPacket(NetworkContent.ItemPipeVisualTransferPacket packet) {
        var path = packet.codedStops().stream().map(BlockPos::fromLong).toList();
        var pathLength = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            var pathPos = path.get(i);
            var nextPathPos = path.get(i + 1);
            pathLength += nextPathPos.getManhattanDistance(pathPos);
        }
        var data = new RenderStackData(packet.moved(), path, world.getTime(), pathLength);
        activeStacks.add(data);
    }
    
    @Override
    public void markDirty() {
        if (this.world != null)
            world.markDirty(pos);
    }
    
    private static ItemStack getFirstStack(ItemApi.InventoryStorage inventory, int maxTransferAmount) {
        
        for (int i = 0; i < inventory.getSlotCount(); i++) {
            var slotStack = inventory.getStackInSlot(i);
            if (slotStack.isEmpty()) continue;
            var extracted = inventory.extractFromSlot(slotStack.copyWithCount(maxTransferAmount), i, true);
            if (extracted > 0) return slotStack.copyWithCount(extracted);
        }
        
        return ItemStack.EMPTY;
    }
    
    public record RenderStackData(ItemStack rendered, List<BlockPos> path, Long startedAt, int pathLength) {
    }
}
