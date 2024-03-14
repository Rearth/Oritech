package rearth.oritech.block.entity.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.ItemPipeBlock;
import rearth.oritech.block.blocks.pipes.ItemPipeConnectionBlock;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.*;
import java.util.stream.Collectors;

public class ItemPipeInterfaceEntity extends GenericPipeInterfaceEntity {
    
    private final HashMap<BlockPos, BlockApiCache<Storage<FluidVariant>, Direction>> lookupCache = new HashMap<>();
    
    public ItemPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ITEM_PIPE_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        if (world.isClient || !state.get(ItemPipeConnectionBlock.EXTRACT)) return;
        
        // find first itemstack from connected invs (that can be extracted)
        // try to move it to one of the destinations
        
        var maxTransferAmount = 8;
        
        var data = ItemPipeBlock.ITEM_PIPE_DATA;
        
        var sources = data.machineInterfaces.getOrDefault(pos, new HashSet<>());
        var stackToMove = ItemStack.EMPTY;
        Storage<ItemVariant> moveFromInventory = null;
        
        try (var mainTx = Transaction.openOuter()) {
            for (var sourcePos : sources) {
                var offset = pos.subtract(sourcePos);
                var direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ());
                var inventory = ItemStorage.SIDED.find(world, sourcePos, direction);
                if (inventory == null || !inventory.supportsExtraction()) continue;
                
                var firstStack = getFromStorage(inventory, maxTransferAmount, mainTx);
                
                if (!firstStack.isEmpty()) {
                    stackToMove = firstStack;
                    moveFromInventory = inventory;
                    break;
                }
                
            }
            
            mainTx.abort();
        }
        
        if (stackToMove.isEmpty()) return;
        
        System.out.println("found stack to move: " + stackToMove);
        
        var targets = findNetworkTargets(pos, data);
        
        var itemStorages = targets.stream()
                             .map(target -> ItemStorage.SIDED.find(world, target.getLeft(), target.getRight()))
                             .filter(obj -> Objects.nonNull(obj) && obj.supportsInsertion())
                             .collect(Collectors.toList());
        
        Collections.shuffle(itemStorages);
        
        var moveCount = stackToMove.getCount();
        var moved = 0L;
        
        try (var tx = Transaction.openOuter()) {
            for (var targetStorage : itemStorages) {
                var inserted = targetStorage.insert(ItemVariant.of(stackToMove), moveCount, tx);
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
