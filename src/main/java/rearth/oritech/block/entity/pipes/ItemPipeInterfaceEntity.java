package rearth.oritech.block.entity.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.ItemPipeBlock;
import rearth.oritech.block.blocks.pipes.ItemPipeConnectionBlock;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.*;

public class ItemPipeInterfaceEntity extends GenericPipeInterfaceEntity {
    
    private static final int TRANSFER_AMOUNT = Oritech.CONFIG.itemPipeTransferAmount();
    private static final int TRANSFER_PERIOD = Oritech.CONFIG.itemPipeIntervalDuration();
    
    private final HashMap<BlockPos, BlockApiCache<Storage<ItemVariant>, Direction>> lookupCache = new HashMap<>();
    private List<Pair<Storage<ItemVariant>, BlockPos>> filteredTargetItemStorages;
    private int filteredTargetsNetHash;
    
    public ItemPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ITEM_PIPE_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        if (world.isClient || world.getTime() % TRANSFER_PERIOD != 0 || !state.get(ItemPipeConnectionBlock.EXTRACT))
            return;
        
        // find first itemstack from connected invs (that can be extracted)
        // try to move it to one of the destinations
        
        var data = ItemPipeBlock.ITEM_PIPE_DATA.getOrDefault(world.getRegistryKey().getValue(), new PipeNetworkData());
        
        var sources = data.machineInterfaces.getOrDefault(pos, new HashSet<>());
        var stackToMove = ItemStack.EMPTY;
        Storage<ItemVariant> moveFromInventory = null;
        
        try (var mainTx = Transaction.openOuter()) {
            for (var sourcePos : sources) {
                var offset = pos.subtract(sourcePos);
                var direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ());
                var inventory = findFromCache(world, sourcePos, direction);
                if (inventory == null || !inventory.supportsExtraction()) continue;
                
                var firstStack = getFromStorage(inventory, TRANSFER_AMOUNT, mainTx);
                
                if (!firstStack.isEmpty()) {
                    stackToMove = firstStack;
                    moveFromInventory = inventory;
                    break;
                }
                
            }
            
            mainTx.abort();
        }
        
        if (stackToMove.isEmpty()) return;
        
        var targets = findNetworkTargets(pos, data);
        if (targets == null) return;
        
        var netHash = targets.hashCode();
        
        if (netHash != filteredTargetsNetHash) {
            filteredTargetItemStorages = targets.stream()
                                           .filter(target -> {
                                               var pipePos = target.getLeft().add(target.getRight().getVector());
                                               var pipeState = world.getBlockState(pipePos);
                                               if (!(pipeState.getBlock() instanceof ItemPipeConnectionBlock))
                                                   return true;   // edge case, this should never happen
                                               var extracting = pipeState.get(ItemPipeConnectionBlock.EXTRACT);
                                               return !extracting;
                                           })
                                           .map(target -> new Pair<>(findFromCache(world, target.getLeft(), target.getRight()), target.getLeft()))
                                           .filter(obj -> Objects.nonNull(obj.getLeft()) && obj.getLeft().supportsInsertion() && obj.getRight().getManhattanDistance(pos) > 1)
                                           .sorted(Comparator.comparingInt(a -> a.getRight().getManhattanDistance(pos)))
                                           .toList();
            
            filteredTargetsNetHash = netHash;
        }
        
        var moveCount = stackToMove.getCount();
        var moved = 0L;
        
        try (var tx = Transaction.openOuter()) {
            for (var targetStorage : filteredTargetItemStorages) {
                var inserted = targetStorage.getLeft().insert(ItemVariant.of(stackToMove), moveCount, tx);
                moveCount -= (int) inserted;
                moved += inserted;
                
                if (moveCount <= 0) break;
            }
            
            var extracted = moveFromInventory.extract(ItemVariant.of(stackToMove), moved, tx);
            
            if (extracted != moved) {
                Oritech.LOGGER.warn("Invalid state while transferring inventory. Caused at position " + pos);
                tx.abort();
            } else {
                tx.commit();
            }
            
        }
        
    }
    
    @Override
    public void markDirty() {
        if (this.world != null)
            world.markDirty(pos);
    }
    
    private Storage<ItemVariant> findFromCache(World world, BlockPos pos, Direction direction) {
        var cacheRes = lookupCache.computeIfAbsent(pos, elem -> BlockApiCache.create(ItemStorage.SIDED, (ServerWorld) world, pos));
        return cacheRes.find(direction);
    }
    
    private static ItemStack getFromStorage(Storage<ItemVariant> inventory, int maxTransferAmount, Transaction mainTx) {
        for (Iterator<StorageView<ItemVariant>> it = inventory.nonEmptyIterator(); it.hasNext(); ) {
            var stack = it.next();
            var type = stack.getResource();
            var extractedAmount = inventory.extract(type, maxTransferAmount, mainTx);
            if (extractedAmount > 0) {
                return type.toStack((int) extractedAmount);
            }
        }
        
        return ItemStack.EMPTY;
    }
}
