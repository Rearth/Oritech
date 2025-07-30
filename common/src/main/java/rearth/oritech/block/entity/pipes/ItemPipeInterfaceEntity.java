package rearth.oritech.block.entity.pipes;

import org.apache.commons.lang3.time.StopWatch;
import rearth.oritech.Oritech;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.ItemApi.InventoryStorage;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeConnectionBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import java.util.*;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ItemPipeInterfaceEntity extends ExtractablePipeInterfaceEntity {
    
    private static final int TRANSFER_AMOUNT = Oritech.CONFIG.itemPipeTransferAmount();
    private static final int TRANSFER_PERIOD = Oritech.CONFIG.itemPipeIntervalDuration();
    
    private List<Tuple<ItemApi.InventoryStorage, BlockPos>> filteredTargetItemStorages;
    
    // item path cache (invalidated on network update)
    private final HashMap<BlockPos, Tuple<ArrayList<BlockPos>, Integer>> cachedTransferPaths = new HashMap<>();
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
    public void tick(Level world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        var block = (ExtractablePipeConnectionBlock) state.getBlock();
        if (world.isClientSide || !block.isExtractable(state))
            return;
        
        // boosted pipe works every tick, otherwise only every N tick
        if ((world.getGameTime() + this.worldPosition.asLong()) % TRANSFER_PERIOD != 0 && !isBoostAvailable())
            return;
        
        // find first itemstack from connected invs (that can be extracted)
        // try to move it to one of the destinations
        
        var data = ItemPipeBlock.ITEM_PIPE_DATA.getOrDefault(world.dimension().location(), new PipeNetworkData());
        
        var sources = data.machineInterfaces.getOrDefault(pos, new HashSet<>());
        var stackToMove = ItemStack.EMPTY;
        ItemApi.InventoryStorage moveFromInventory = null;
        BlockPos takenFrom = null;
        var moveCapacity = isBoostAvailable() ? 64 : TRANSFER_AMOUNT;
        
        var hasMotor = state.getValue(ItemPipeConnectionBlock.HAS_MOTOR);
        
        for (var sourcePos : sources) {
            var blockedTimer = blockedUntil.getOrDefault(sourcePos, 0L);
            if (world.getGameTime() < blockedTimer) continue;
            
            if (blockedTimer > 0)   // if timer has expired but was set
                blockedUntil.remove(sourcePos);
            
            var offset = pos.subtract(sourcePos);
            var direction = Direction.fromDelta(offset.getX(), offset.getY(), offset.getZ());
            if (!block.isSideExtractable(state, direction.getOpposite())) continue;
            var inventory = ItemApi.BLOCK.find(world, sourcePos, direction);
            if (inventory == null || !inventory.supportsExtraction()) continue;
            
            for (int i = 0; i < inventory.getSlotCount(); i++) {
                var slotStack = inventory.getStackInSlot(i);
                if (slotStack.isEmpty()) continue;
                var canTake = inventory.extractFromSlot(slotStack.copyWithCount(moveCapacity), i, true);
                if (canTake > 0) {
                    stackToMove = slotStack.copyWithCount(canTake);
                    moveFromInventory = inventory;
                    takenFrom = sourcePos;
                } else {
                    stackToMove = ItemStack.EMPTY;
                }
                
                if (stackToMove.isEmpty()) continue;
                
                var targets = findNetworkTargets(pos, data);
                if (targets == null) {
                    System.err.println("Yeah your pipe network likely is too long. At: " + this.getBlockPos());
                    return;
                }
                
                var netHash = targets.hashCode();
                
                if (netHash != filteredTargetsNetHash || filteredTargetItemStorages == null) {
                    filteredTargetItemStorages = targets.stream()
                                                   .filter(target -> {
                                                       var targetDir = target.getB();
                                                       var pipePos = target.getA().offset(targetDir.getNormal());
                                                       var pipeState = world.getBlockState(pipePos);
                                                       if (!(pipeState.getBlock() instanceof ItemPipeConnectionBlock itemBlock))
                                                           return true;   // edge case, this should never happen
                                                       var extracting = itemBlock.isSideExtractable(pipeState, targetDir.getOpposite());
                                                       return !extracting;
                                                   })
                                                   .map(target -> new Tuple<>(ItemApi.BLOCK.find(world, target.getA(), target.getB()), target.getA()))
                                                   .filter(obj -> Objects.nonNull(obj.getA()) && obj.getA().supportsInsertion())
                                                   .sorted(Comparator.comparingInt(a -> a.getB().distManhattan(pos)))
                                                   .toList();
                    
                    filteredTargetsNetHash = netHash;
                    cachedTransferPaths.clear();
                }
                
                var toMove = stackToMove.getCount();
                var moved = 0;
                
                for (var storagePair : filteredTargetItemStorages) {
                    if (storagePair.getA().equals(moveFromInventory))
                        continue;    // skip when targeting same machine
                    
                    var targetStorage = storagePair.getA();
                    var wasEmptyStorage = IntStream.range(0, targetStorage.getSlotCount()).allMatch(slot -> targetStorage.getStackInSlot(slot).isEmpty());
                    
                    var inserted = targetStorage.insert(stackToMove, false);
                    toMove -= inserted;
                    moved += inserted;
                    
                    if (inserted > 0) {
                        onItemMoved(this.worldPosition, takenFrom, storagePair.getB(), data.pipeNetworks.getOrDefault(data.pipeNetworkLinks.getOrDefault(this.worldPosition, 0), new HashSet<>()), world, stackToMove.getItem(), inserted, wasEmptyStorage);
                    }
                    
                    if (toMove <= 0) break;  // target has been found for all items
                }
                var extracted = moveFromInventory.extract(stackToMove.copyWithCount(moved), false);
                
                if (extracted != moved) {
                    Oritech.LOGGER.warn("Invalid state while transferring inventory. Caused at position {}", pos);
                }
                
                // only move one slot content
                if (moved > 0)
                    break;
                
                // only try to move the first non-empty stack without motors
                if (!hasMotor)
                    break;
            }
        }
        
        if (moveCapacity > TRANSFER_AMOUNT) onBoostUsed();
        
    }
    
    private void onItemMoved(BlockPos startPos, BlockPos from, BlockPos to, Set<BlockPos> network, Level world, Item moved, int movedCount, boolean wasEmpty) {
        if (!renderItems) return;
        var path = cachedTransferPaths.computeIfAbsent(to, ignored -> calculatePath(startPos, from, to, network, world));
        if (path == null) return;
        
        var codedPath = path.getA();
        var pathLength = 0;
        for (int i = 0; i < codedPath.size() - 1; i++) {
            var pathPos = codedPath.get(i);
            var nextPathPos = codedPath.get(i + 1);
            pathLength += nextPathPos.distManhattan(pathPos);
        }
        var packet = new RenderStackData(worldPosition, new ItemStack(moved, movedCount), codedPath, world.getGameTime(), pathLength);
        NetworkManager.sendBlockHandle(this, packet);
        
        if (wasEmpty) {
            var arrivalTime = world.getGameTime() + (int) calculatePathLength(path.getB());
            blockedUntil.putIfAbsent(to, arrivalTime);
        }
        
    }
    
    public static double calculatePathLength(int pathBlocksCount) {
        return Math.pow(pathBlocksCount * 16, 0.6);
    }
    
    // return pair is optimized path and total path length
    private static Tuple<ArrayList<BlockPos>, Integer> calculatePath(BlockPos startPos, BlockPos from, BlockPos to, Set<BlockPos> network, Level world) {
        
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
            
            var currentPosState = world.getBlockState(currentPos);
            if (!(currentPosState.getBlock() instanceof AbstractPipeBlock pipeBlock)) break;
            
            // collect potential edges in graph, ordered by basic cost heuristic (manhattan dist to target)
            var openEdges = getNeighbors(currentPos).stream()
                              .filter(network::contains)
                              .filter(candidate -> !visited.contains(candidate))
                              .filter(candidate -> pipeBlock.isConnectingInDirection(currentPosState, getDirectionFromOffset(currentPos, candidate), currentPos, world, false))
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
        return Direction.fromDelta(offset.getX(), offset.getY(), offset.getZ());
    }
    
    public static void receiveVisualItemsPacket(RenderStackData message, Level world, RegistryAccess registryAccess) {
        var blockEntity = world.getBlockEntity(message.self, BlockEntitiesContent.ITEM_PIPE_ENTITY);
        if (blockEntity.isPresent()) {
            var pipeEntity = blockEntity.get();
            // use local time for moved item to avoid rendering issues caused by lag
            pipeEntity.activeStacks.add(new RenderStackData(pipeEntity.worldPosition, message.rendered, message.path, world.getGameTime(), message.pathLength));
        }
    }
    
    @Override
    public void setChanged() {
        if (this.level != null)
            level.blockEntityChanged(worldPosition);
    }
    
    public record RenderStackData(BlockPos self, ItemStack rendered, List<BlockPos> path, Long startedAt, int pathLength) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<RenderStackData> PIPE_ITEMS_ID = new CustomPacketPayload.Type<>(Oritech.id("pipe_items"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PIPE_ITEMS_ID;
        }
    }
}
