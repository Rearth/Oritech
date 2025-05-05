package rearth.oritech.fabric;

import com.google.common.collect.Streams;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.fabric.FluidStackHooksFabric;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.util.StackContext;
import rearth.oritech.api.fluid.BlockFluidApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.ItemFluidApi;
import rearth.oritech.api.fluid.containers.DelegatingFluidStorage;
import rearth.oritech.api.fluid.containers.SimpleItemFluidStorage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class FabricFluidApiImpl implements BlockFluidApi, ItemFluidApi {
    
    @SuppressWarnings("IfCanBeSwitch")
    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity(
          (entity, direction) -> {
              
              var storage = ((FluidApi.BlockProvider) entity).getFluidStorage(direction);
              
              if (storage == null) return null;
              
              if (storage instanceof FluidApi.InOutSlotStorage inOutContainer) {
                  return InOutFluidContainerStorageWrapper.of(inOutContainer);
              } else if (storage instanceof FluidApi.SingleSlotStorage singleContainer) {
                  return SingleSlotContainerStorageWrapper.of(singleContainer);
              } else if (storage instanceof DelegatingFluidStorage delegatedStorage) {
                  return DelegatedContainerStorageWrapper.of(delegatedStorage);
              }
              
              Oritech.LOGGER.error("Error during fluid provider registration, unable to register a fluid container");
              Oritech.LOGGER.error("Erroring container type is: {}", typeSupplier.get());
              
              return null;
          }, typeSupplier.get());
    }
    
    @Override
    public void registerForItem(Supplier<Item> itemSupplier) {
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.registerForItems(
          (stack, context) -> SingleSlotContainerStorageWrapper.of(((FluidApi.ItemProvider) stack.getItem()).getFluidStorage(stack)),
          itemSupplier.get()
        );
    }
    
    @Override
    public FluidApi.FluidStorage find(World world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        var candidate = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(world, pos, state, entity, direction);
        
        return switch (candidate) {
            case null -> null;
            case SingleSlotContainerStorageWrapper wrapper -> wrapper.container;
            case InOutFluidContainerStorageWrapper wrapper -> wrapper.container.getStorageForDirection(direction);
            case DelegatedContainerStorageWrapper wrapper -> wrapper.storage;
            default -> new FabricStorageWrapper(candidate, null);
        };
    }
    
    @Override
    public FluidApi.FluidStorage find(World world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }
    
    @Override
    public FluidApi.FluidStorage find(StackContext stack) {
        var context = ContainerItemContext.ofSingleSlot(new ItemStackStorage(stack));
        var candidate = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.find(stack.getValue(), context);
        if (candidate == null) return null;
        if (candidate instanceof SingleSlotContainerStorageWrapper wrapper && wrapper.container instanceof SimpleItemFluidStorage itemContainer) return itemContainer.withCallback(ignored -> stack.sync());
        return new FabricStorageWrapper(candidate, stack);
    }
    
    // this is used to interact with fluid storages from other mods
    public static class FabricStorageWrapper extends FluidApi.FluidStorage {
        
        public final Storage<FluidVariant> storage;
        public final @Nullable StackContext context;
        
        public FabricStorageWrapper(Storage<FluidVariant> storage, @Nullable StackContext context) {
            this.storage = storage;
            this.context = context;
        }
        
        @Override
        public long insert(FluidStack in, boolean simulate) {
            try (var transaction = Transaction.openOuter()) {
                var inserted = storage.insert(FluidStackHooksFabric.toFabric(in), in.getAmount(), transaction);
                if (!simulate)
                    transaction.commit();
                return inserted;
            }
        }
        
        @Override
        public long extract(FluidStack out, boolean simulate) {
            try (var transaction = Transaction.openOuter()) {
                var extracted = storage.extract(FluidStackHooksFabric.toFabric(out), out.getAmount(), transaction);
                if (!simulate)
                    transaction.commit();
                return extracted;
            }
        }
        
        @Override
        public List<FluidStack> getContent() {
            return Streams
                     .stream(storage.iterator())
                     .map(FluidStackHooksFabric::fromFabric)
                     .toList();
        }
        
        @Override
        public void update() {
            if (context != null)
                context.sync();
        }
        
        @Override
        public long getCapacity() {
            Oritech.LOGGER.warn("tried to access capacity of external container");
            return 0L;  // this should not be used I guess?
        }
    }
    
    // this is used by other mods to interact with the oritech single slot fluid containers (machines/items)
    public static class SingleSlotContainerStorageWrapper extends SnapshotParticipant<FluidStack> implements Storage<FluidVariant> {
        
        public final FluidApi.SingleSlotStorage container;
        private Set<StorageView<FluidVariant>> contentView;
        
        public static SingleSlotContainerStorageWrapper of(@Nullable FluidApi.SingleSlotStorage container) {
            if (container == null) return null;
            return new SingleSlotContainerStorageWrapper(container);
        }
        
        public SingleSlotContainerStorageWrapper(FluidApi.SingleSlotStorage container) {
            this.container = container;
        }
        
        @Override
        public boolean supportsInsertion() {
            return container.supportsInsertion();
        }
        
        @Override
        public long insert(FluidVariant fluidVariant, long amount, TransactionContext transaction) {
            updateSnapshots(transaction);
            transaction.addCloseCallback((transactionContext, result) -> {
                if (result.wasCommitted()) {
                    container.update();
                }
            });
            return container.insert(FluidStackHooksFabric.fromFabric(fluidVariant, amount), false);
        }
        
        @Override
        public boolean supportsExtraction() {
            return container.supportsExtraction();
        }
        
        @Override
        public long extract(FluidVariant fluidVariant, long amount, TransactionContext transaction) {
            updateSnapshots(transaction);
            transaction.addCloseCallback((transactionContext, result) -> {
                if (result.wasCommitted()) {
                    container.update();
                }
            });
            return container.extract(FluidStackHooksFabric.fromFabric(fluidVariant, amount), false);
        }
        
        @Override
        public @NotNull Iterator<StorageView<FluidVariant>> iterator() {
            
            if (contentView != null) return contentView.iterator();
            
            contentView = Collections.singleton(new StorageView<FluidVariant>() {
                @Override
                public long extract(FluidVariant fluidVariant, long amount, TransactionContext transaction) {
                    return SingleSlotContainerStorageWrapper.this.extract(fluidVariant, amount, transaction);
                }
                
                @Override
                public boolean isResourceBlank() {
                    return container.getStack().getFluid().equals(Fluids.EMPTY);
                }
                
                @Override
                public FluidVariant getResource() {
                    return FluidStackHooksFabric.toFabric(container.getStack());
                }
                
                @Override
                public long getAmount() {
                    return container.getStack().getAmount();
                }
                
                @Override
                public long getCapacity() {
                    return container.getCapacity();
                }
            }.getUnderlyingView());
            
            return contentView.iterator();
        }
        
        @Override
        protected FluidStack createSnapshot() {
            return container.getStack().copy();
        }
        
        @Override
        protected void readSnapshot(FluidStack fluidVariantResourceAmount) {
            container.setStack(fluidVariantResourceAmount);
        }
    }
    
    // this is used by other mods to interact with the oritech in/out fluid containers
    public static class InOutFluidContainerStorageWrapper extends SnapshotParticipant<Pair<FluidStack, FluidStack>> implements Storage<FluidVariant> {
        
        public final FluidApi.InOutSlotStorage container;
        private List<@NotNull StorageView<FluidVariant>> storageViews;
        
        public static InOutFluidContainerStorageWrapper of(FluidApi.InOutSlotStorage container) {
            if (container == null) return null;
            return new InOutFluidContainerStorageWrapper(container);
        }
        
        private InOutFluidContainerStorageWrapper(FluidApi.InOutSlotStorage container) {
            this.container = container;
        }
        
        @Override
        public boolean supportsInsertion() {
            return container.supportsInsertion();
        }
        
        @Override
        public long insert(FluidVariant fluidVariant, long amount, TransactionContext transaction) {
            updateSnapshots(transaction);
            transaction.addCloseCallback((transactionContext, result) -> {
                if (result.wasCommitted()) {
                    container.update();
                }
            });
            return container.insert(FluidStackHooksFabric.fromFabric(fluidVariant, amount), false);
        }
        
        @Override
        public boolean supportsExtraction() {
            return container.supportsExtraction();
        }
        
        @Override
        public long extract(FluidVariant fluidVariant, long amount, TransactionContext transaction) {
            updateSnapshots(transaction);
            transaction.addCloseCallback((transactionContext, result) -> {
                if (result.wasCommitted()) {
                    container.update();
                }
            });
            return container.extract(FluidStackHooksFabric.fromFabric(fluidVariant, amount), false);
        }
        
        @Override
        public @NotNull Iterator<StorageView<FluidVariant>> iterator() {
            
            if (storageViews != null) return storageViews.iterator();
            
            storageViews = List.of(new StorageView<FluidVariant>() {
                // in storage
                @Override
                public long extract(FluidVariant fluidVariant, long l, TransactionContext transactionContext) {
                    return 0;
                }
                
                @Override
                public boolean isResourceBlank() {
                    return container.getInStack().getFluid().equals(Fluids.EMPTY);
                }
                
                @Override
                public FluidVariant getResource() {
                    return FluidStackHooksFabric.toFabric(container.getInStack());
                }
                
                @Override
                public long getAmount() {
                    return container.getInStack().getAmount();
                }
                
                @Override
                public long getCapacity() {
                    return container.getCapacity();
                }
            }.getUnderlyingView(), new StorageView<FluidVariant>() {
                // out storage
                @Override
                public long extract(FluidVariant fluidVariant, long l, TransactionContext transactionContext) {
                    return InOutFluidContainerStorageWrapper.this.extract(fluidVariant, l, transactionContext);
                }
                
                @Override
                public boolean isResourceBlank() {
                    return container.getOutStack().getFluid().equals(Fluids.EMPTY);
                }
                
                @Override
                public FluidVariant getResource() {
                    return FluidStackHooksFabric.toFabric(container.getOutStack());
                }
                
                @Override
                public long getAmount() {
                    return container.getOutStack().getAmount();
                }
                
                @Override
                public long getCapacity() {
                    return container.getCapacity();
                }
            }.getUnderlyingView());
            
            return storageViews.iterator();
        }
        
        @Override
        protected Pair<FluidStack, FluidStack> createSnapshot() {
            return new Pair<>(container.getInStack().copy(), container.getOutStack().copy());
        }
        
        @Override
        protected void readSnapshot(Pair<FluidStack, FluidStack> snapshot) {
            container.setInStack(snapshot.getLeft());
            container.setOutStack(snapshot.getRight());
        }
    }
    
    public static class DelegatedContainerStorageWrapper extends SnapshotParticipant<List<FluidStack>> implements Storage<FluidVariant> {
        
        private final DelegatingFluidStorage storage;
        
        public static DelegatedContainerStorageWrapper of(DelegatingFluidStorage storage) {
            if (storage == null) return null;
            return new DelegatedContainerStorageWrapper(storage);
        }
        
        private DelegatedContainerStorageWrapper(DelegatingFluidStorage storage) {
            this.storage = storage;
        }
        
        @Override
        public boolean supportsInsertion() {
            return storage.supportsInsertion();
        }
        
        @Override
        public long insert(FluidVariant fluidVariant, long l, TransactionContext transaction) {
            updateSnapshots(transaction);
            transaction.addCloseCallback((transactionContext, result) -> {
                if (result.wasCommitted()) {
                    storage.update();
                }
            });
            return storage.insert(FluidStackHooksFabric.fromFabric(fluidVariant, l), false);
        }
        
        @Override
        public boolean supportsExtraction() {
            return storage.supportsExtraction();
        }
        
        @Override
        public long extract(FluidVariant fluidVariant, long l, TransactionContext transaction) {
            updateSnapshots(transaction);
            transaction.addCloseCallback((transactionContext, result) -> {
                if (result.wasCommitted()) {
                    storage.update();
                }
            });
            return storage.extract(FluidStackHooksFabric.fromFabric(fluidVariant, l), false);
        }
        
        @Override
        public @NotNull Iterator<StorageView<FluidVariant>> iterator() {
            return storage.getContent().stream().map(stack -> new StorageView<FluidVariant>() {
                
                @Override
                public long extract(FluidVariant fluidVariant, long l, TransactionContext transactionContext) {
                    return DelegatedContainerStorageWrapper.this.extract(fluidVariant, l, transactionContext);
                }
                
                @Override
                public boolean isResourceBlank() {
                    return stack.isEmpty();
                }
                
                @Override
                public FluidVariant getResource() {
                    return FluidStackHooksFabric.toFabric(stack);
                }
                
                @Override
                public long getAmount() {
                    return stack.getAmount();
                }
                
                @Override
                public long getCapacity() {
                    return storage.getCapacity();
                }
            }.getUnderlyingView()).iterator();
        }
        
        @Override
        protected List<FluidStack> createSnapshot() {
            return storage.getContent().stream().map(FluidStack::copy).toList();
        }
        
        @Override
        protected void readSnapshot(List<FluidStack> fluidStacks) {
            storage.setContent(fluidStacks);
        }
    }
    
}
