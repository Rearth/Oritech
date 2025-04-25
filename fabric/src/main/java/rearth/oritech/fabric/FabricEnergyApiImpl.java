package rearth.oritech.fabric;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.util.StackContext;
import rearth.oritech.api.energy.BlockEnergyApi;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.ItemEnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyItemStorage;
import team.reborn.energy.api.EnergyStorage;

import java.util.function.Supplier;

public class FabricEnergyApiImpl implements BlockEnergyApi, ItemEnergyApi {
    
    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((entity, direction) ->
                                                     ContainerStorageWrapper.of(((EnergyApi.BlockProvider) entity).getEnergyStorage(direction)), typeSupplier.get());
    }
    
    @Override
    public void registerForItem(Supplier<net.minecraft.item.Item> itemSupplier) {
        team.reborn.energy.api.EnergyStorage.ITEM.registerForItems((stack, context) ->
                                              ContainerStorageWrapper.of(((EnergyApi.ItemProvider) stack.getItem()).getEnergyStorage(stack), context, stack), itemSupplier.get());
    }
    
    @Override
    public ComponentType<Long> getEnergyComponent() {
        return team.reborn.energy.api.EnergyStorage.ENERGY_COMPONENT;
    }
    
    @Override
    public EnergyApi.EnergyStorage find(StackContext stack) {
        var context = ContainerItemContext.ofSingleSlot(new ItemStackStorage(stack));
        var candidate = team.reborn.energy.api.EnergyStorage.ITEM.find(stack.getValue(), context);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper && wrapper.container instanceof SimpleEnergyItemStorage itemStorage)
            return itemStorage.withCallback(ignored -> stack.sync());
        return new FabricStorageWrapper(candidate, stack);
    }
    
    @Override
    public EnergyApi.EnergyStorage find(World world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        var candidate = team.reborn.energy.api.EnergyStorage.SIDED.find(world, pos, state, entity, direction);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper) return wrapper.container;
        return new FabricStorageWrapper(candidate, null);
    }
    
    @Override
    public EnergyApi.EnergyStorage find(World world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }
    
    // this is used to interact with energy storages from other mods
    public static class FabricStorageWrapper extends EnergyApi.EnergyStorage {
        
        public final EnergyStorage storage;
        public final @Nullable StackContext context;
        
        public FabricStorageWrapper(EnergyStorage storage, @Nullable StackContext context) {
            this.storage = storage;
            this.context = context;
        }
        
        @Override
        public long insert(long maxAmount, boolean simulate) {
            try (var transaction = Transaction.openOuter()) {
                var inserted = storage.insert(maxAmount, transaction);
                if (!simulate)
                    transaction.commit();
                return inserted;
            }
        }
        
        @Override
        public long extract(long maxAmount, boolean simulate) {
            try (var transaction = Transaction.openOuter()) {
                var extracted = storage.extract(maxAmount, transaction);
                if (!simulate)
                    transaction.commit();
                return extracted;
            }
        }
        
        @Override
        public long getAmount() {
            return storage.getAmount();
        }
        
        @Override
        public long getCapacity() {
            return storage.getCapacity();
        }
        
        @Override
        public void setAmount(long amount) {
        }
        
        @Override
        public void update() {
            if (context != null)
                context.sync();
        }
    }
    
    // this is used by other mods to interact with the oritech energy containers (machines/items)
    public static class ContainerStorageWrapper extends SnapshotParticipant<Long> implements EnergyStorage {
        
        public final EnergyApi.EnergyStorage container;
        @Nullable
        public final ContainerItemContext context;
        @Nullable
        public final ItemStack stack;
        
        public static ContainerStorageWrapper of(@Nullable EnergyApi.EnergyStorage container) {
            if (container == null) return null;
            return new ContainerStorageWrapper(container);
        }
        
        public static ContainerStorageWrapper of(@Nullable EnergyApi.EnergyStorage container, @Nullable ContainerItemContext context, @Nullable ItemStack stack) {
            if (container == null) return null;
            return new ContainerStorageWrapper(container, context, stack);
        }
        
        public ContainerStorageWrapper(EnergyApi.EnergyStorage container) {
            this.container = container;
            this.context = null;
            this.stack = null;
        }
        
        public ContainerStorageWrapper(EnergyApi.EnergyStorage container, @Nullable ContainerItemContext context, @Nullable ItemStack stack) {
            this.container = container;
            this.context = context;
            this.stack = stack;
        }
        
        @Override
        public boolean supportsInsertion() {
            return container.supportsInsertion();
        }
        
        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            updateSnapshots(transaction);
            transaction.addCloseCallback((transactionContext, result) -> {
                if (result.wasCommitted()) {
                    container.update();
                }
            });
            
            var inserted = container.insert(maxAmount, false);
            
            // no idea what this does, but it does seem to fix it
            if (context != null) {
                stack.set(EnergyApi.ITEM.getEnergyComponent(), container.getAmount());
                context.exchange(ItemVariant.of(stack), 1, transaction);
            }
            
            
            return inserted;
        }
        
        @Override
        public boolean supportsExtraction() {
            return container.supportsInsertion();
        }
        
        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            updateSnapshots(transaction);
            transaction.addCloseCallback((context, result) -> {
                if (result.wasCommitted()) {
                    container.update();
                }
            });
            return container.extract(maxAmount, false);
        }
        
        @Override
        public long getAmount() {
            return container.getAmount();
        }
        
        @Override
        public long getCapacity() {
            return container.getCapacity();
        }
        
        @Override
        protected Long createSnapshot() {
            return getAmount();
        }
        
        @Override
        protected void readSnapshot(Long snapshot) {
            container.setAmount(snapshot);
        }
    }
    
}
