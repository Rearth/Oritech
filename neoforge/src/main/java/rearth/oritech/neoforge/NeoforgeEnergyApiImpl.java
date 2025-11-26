package rearth.oritech.neoforge;

import com.google.auto.service.AutoService;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import dev.technici4n.grandpower.impl.NonLongWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.item.BlockItemApi;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.StackContext;
import rearth.oritech.api.energy.BlockEnergyApi;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.ItemEnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyItemStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@AutoService({BlockEnergyApi.class, ItemEnergyApi.class})
public class NeoforgeEnergyApiImpl implements BlockEnergyApi, ItemEnergyApi {
    
    private final List<Supplier<BlockEntityType<?>>> registeredBlockEntities = new ArrayList<>();
    private final List<Supplier<net.minecraft.world.item.Item>> registeredItems = new ArrayList<>();
    
    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        registeredBlockEntities.add(typeSupplier);
    }
    
    @Override
    public void registerForItem(Supplier<net.minecraft.world.item.Item> itemSupplier) {
        registeredItems.add(itemSupplier);
    }
    
    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return OritechModNeoForge.EventHandler.NEO_ENERGY_COMPONENT.get();
    }
    
    public void registerEvent(RegisterCapabilitiesEvent event) {
        for (var supplied : registeredBlockEntities) {
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, supplied.get(), (entity, direction) -> ContainerStorageWrapper.of(((EnergyApi.BlockProvider) entity).getEnergyStorage(direction)));
        }
        
        for (var supplied : registeredItems) {
            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ignored) -> ContainerStorageWrapper.of(((EnergyApi.ItemProvider) stack.getItem()).getEnergyStorage(stack)), supplied.get());
        }
    }
    
    @Override
    public EnergyApi.EnergyStorage find(StackContext stack) {
        if (stack.getValue().getCount() > 1) return null;
        var candidate = stack.getValue().getCapability(ILongEnergyStorage.ITEM);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper && wrapper.container instanceof SimpleEnergyItemStorage itemStorage) return itemStorage.withCallback(ignored -> stack.sync());
        return new NeoforgeStorageWrapper(candidate);
    }
    
    @Override
    public EnergyApi.EnergyStorage find(Level world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        var candidate = world.getCapability(ILongEnergyStorage.BLOCK, pos, state, entity, direction);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper) return wrapper.container;
        if (candidate instanceof NonLongWrapper outerWrapper && outerWrapper.storage() instanceof ContainerStorageWrapper wrapper) return wrapper.container;
        return new NeoforgeStorageWrapper(candidate);
    }
    
    @Override
    public EnergyApi.EnergyStorage find(Level world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }
    
    // this is used to interact with energy storages from other mods
    public static class NeoforgeStorageWrapper extends EnergyApi.EnergyStorage {
        
        public final ILongEnergyStorage storage;
        
        public NeoforgeStorageWrapper(ILongEnergyStorage storage) {
            this.storage = storage;
        }
        
        @Override
        public long insert(long maxAmount, boolean simulate) {
            return storage.receive(maxAmount, simulate);
        }
        
        @Override
        public long extract(long maxAmount, boolean simulate) {
            return storage.extract(maxAmount, simulate);
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
    public static class ContainerStorageWrapper implements ILongEnergyStorage {
        
        public final EnergyApi.EnergyStorage container;
        
        public static ContainerStorageWrapper of(EnergyApi.EnergyStorage container) {
            if (container == null) return null;
            return new ContainerStorageWrapper(container);
        }
        
        public ContainerStorageWrapper(EnergyApi.EnergyStorage container) {
            this.container = container;
        }
        
        @Override
        public long receive(long amount, boolean simulate) {
            long inserted = container.insert(amount, simulate);
            if (!simulate) container.update();
            return inserted;
        }
        
        @Override
        public long extract(long amount, boolean simulate) {
            long inserted = container.extract(amount, simulate);
            if (!simulate) container.update();
            return inserted;
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
        public boolean canExtract() {
            return container.supportsExtraction();
        }
        
        @Override
        public boolean canReceive() {
            return container.supportsInsertion();
        }
    }
    
}
