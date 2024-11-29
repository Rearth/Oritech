package rearth.oritech.neoforge;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.energy.BlockEnergyApi;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.ItemEnergyApi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NeoforgeEnergyApiImpl implements BlockEnergyApi, ItemEnergyApi {
    
    private final List<Supplier<BlockEntityType<?>>> registeredBlockEntities = new ArrayList<>();
    private final List<Supplier<net.minecraft.item.Item>> registeredItems = new ArrayList<>();
    
    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        registeredBlockEntities.add(typeSupplier);
    }
    
    @Override
    public void registerForItem(Supplier<net.minecraft.item.Item> itemSupplier) {
        registeredItems.add(itemSupplier);
    }
    
    @Override
    public ComponentType<Long> getEnergyComponent() {
        return ComponentContent.NEO_ENERGY_COMPONENT.get();
    }
    
    public void registerEvent(RegisterCapabilitiesEvent event) {
        for (var supplied : registeredBlockEntities) {
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, supplied.get(), (entity, direction) -> ContainerStorageWrapper.of(((EnergyApi.BlockProvider) entity).getStorage(direction)));
        }
        
        for (var supplied : registeredItems) {
            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ignored) -> ContainerStorageWrapper.of(((EnergyApi.ItemProvider) stack.getItem()).getStorage(stack)), supplied.get());
        }
    }
    
    @Override
    public EnergyApi.EnergyContainer find(ItemStack stack, ContainerItemContext context) {
        var candidate = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper) return wrapper.container;
        return new NeoforgeStorageWrapper(candidate);
    }
    
    @Override
    public EnergyApi.EnergyContainer find(World world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        var candidate = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, state, entity, direction);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper) return wrapper.container;
        return new NeoforgeStorageWrapper(candidate);
    }
    
    @Override
    public EnergyApi.EnergyContainer find(World world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }
    
    // this is used to interact with energy storages from other mods
    public static class NeoforgeStorageWrapper extends EnergyApi.EnergyContainer {
        
        public final IEnergyStorage storage;
        
        public NeoforgeStorageWrapper(IEnergyStorage storage) {
            this.storage = storage;
        }
        
        @Override
        public long insert(long maxAmount, boolean simulate) {
            return storage.receiveEnergy((int) maxAmount, simulate);
        }
        
        @Override
        public long extract(long maxAmount, boolean simulate) {
            return storage.extractEnergy((int) maxAmount, simulate);
        }
        
        @Override
        public long getAmount() {
            return storage.getEnergyStored();
        }
        
        @Override
        public long getCapacity() {
            return storage.getMaxEnergyStored();
        }
        
        @Override
        public void setAmount(long amount) {
        }
        
        @Override
        public void update() {
        }
    }
    
    // this is used by other mods to interact with the oritech energy containers (machines/items)
    public static class ContainerStorageWrapper implements IEnergyStorage {
        
        public final EnergyApi.EnergyContainer container;
        
        public static ContainerStorageWrapper of(EnergyApi.EnergyContainer container) {
            if (container == null) return null;
            return new ContainerStorageWrapper(container);
        }
        
        public ContainerStorageWrapper(EnergyApi.EnergyContainer container) {
            this.container = container;
        }
        
        @Override
        public int receiveEnergy(int i, boolean bl) {
            long inserted = container.insert(i, bl);
            if (!bl) container.update();
            return (int) inserted;
        }
        
        @Override
        public int extractEnergy(int i, boolean bl) {
            long extracted = container.extract(i, bl);
            if (!bl) container.update();
            return (int) extracted;
        }
        
        @Override
        public int getEnergyStored() {
            return (int) container.getAmount();
        }
        
        @Override
        public int getMaxEnergyStored() {
            return (int) container.getCapacity();
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
