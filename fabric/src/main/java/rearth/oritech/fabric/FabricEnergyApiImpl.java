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
import rearth.oritech.util.energy.BlockEnergyApi;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.ItemEnergyApi;
import team.reborn.energy.api.EnergyStorage;

import java.util.function.Supplier;

public class FabricEnergyApiImpl implements BlockEnergyApi, ItemEnergyApi {
    
    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        EnergyStorage.SIDED.registerForBlockEntity((entity, direction) -> ContainerStorageWrapper.of(((EnergyApi.BlockProvider) entity).getStorage(direction)), typeSupplier.get());
    }
    
    @Override
    public void registerForItem(Supplier<net.minecraft.item.Item> itemSupplier) {
        EnergyStorage.ITEM.registerForItems((stack, context) -> ContainerStorageWrapper.of(((EnergyApi.ItemProvider) stack.getItem()).getStorage(stack), context, stack), itemSupplier.get());
    }
    
    @Override
    public ComponentType<Long> getEnergyComponent() {
        return EnergyStorage.ENERGY_COMPONENT;
    }
    
    @Override
    public EnergyApi.EnergyContainer find(ItemStack stack, ContainerItemContext context) {
        var candidate = EnergyStorage.ITEM.find(stack, context);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper) return wrapper.container;
        return new FabricStorageWrapper(candidate);
    }
    
    @Override
    public EnergyApi.EnergyContainer find(World world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        var candidate = EnergyStorage.SIDED.find(world, pos, state, entity, direction);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper) return wrapper.container;
        return new FabricStorageWrapper(candidate);
    }
    
    @Override
    public EnergyApi.EnergyContainer find(World world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }
    
    // this is used to interact with energy storages from other mods
    public static class FabricStorageWrapper extends EnergyApi.EnergyContainer {
        
        public final EnergyStorage storage;
        
        public FabricStorageWrapper(EnergyStorage storage) {
            this.storage = storage;
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
        }
    }
    
    // this is used by other mods to interact with the oritech energy containers (machines/items)
    public static class ContainerStorageWrapper extends SnapshotParticipant<Long> implements EnergyStorage {
        
        public final EnergyApi.EnergyContainer container;
        @Nullable
        public final ContainerItemContext context;
        @Nullable
        public final ItemStack stack;
        
        public static ContainerStorageWrapper of(@Nullable EnergyApi.EnergyContainer container) {
            if (container == null) return null;
            return new ContainerStorageWrapper(container);
        }
        
        public static ContainerStorageWrapper of(@Nullable EnergyApi.EnergyContainer container, @Nullable ContainerItemContext context, @Nullable ItemStack stack) {
            if (container == null) return null;
            return new ContainerStorageWrapper(container, context, stack);
        }
        
        public ContainerStorageWrapper(EnergyApi.EnergyContainer container) {
            this.container = container;
            this.context = null;
            this.stack = null;
        }
        
        public ContainerStorageWrapper(EnergyApi.EnergyContainer container, @Nullable ContainerItemContext context, @Nullable ItemStack stack) {
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
                    if (this.context != null)
                        updateStackToContext();
                }
            });
            return container.insert(maxAmount, false);
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
                    if (this.context != null)
                        updateStackToContext();
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
        
        // this is required for non-oritech machines trying to interact with oritech energy items
        // inspired by https://github.com/Sinytra/ConnectorExtras/blob/master/energy-bridge/src/main/java/dev/su5ed/sinytra/connectorextras/energybridge/ForgeEnergyStorageHandler.java#L54
        private void updateStackToContext() {
            if (stack == null || context == null) return;
            var thread = new Thread(() -> { // needs to be a separate thread because on main a transaction is in process of closing
                try (var t = Transaction.openOuter()) {
                    //Update the item components
                    context.extract(context.getItemVariant(), 1, t);
                    context.insert(ItemVariant.of(stack), 1, t);
                    t.commit();
                } catch (Exception ignored) {
                }  // these exceptions sometimes happen during world load
            });
            thread.start();
        }
    }
    
}
