package rearth.oritech.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.item.BlockItemApi;
import rearth.oritech.api.item.ItemApi;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NeoforgeItemApiImpl implements BlockItemApi {
    
    private final List<Supplier<BlockEntityType<?>>> registeredBlockEntities = new ArrayList<>();
    
    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        registeredBlockEntities.add(typeSupplier);
    }
    
    public void registerEvent(RegisterCapabilitiesEvent event) {
        for (var supplied : registeredBlockEntities) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, supplied.get(), (entity, direction) -> ContainerStorageWrapper.of(((ItemApi.BlockProvider) entity).getInventoryStorage(direction)));
        }
    }
    
    @Override
    public ItemApi.InventoryStorage find(Level world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        
        var candidate = world.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, entity, direction);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper) return wrapper.container;
        return new NeoforgeStoragerWrapper(candidate);
    }
    
    @Override
    public ItemApi.InventoryStorage find(Level world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }
    
    // used to interact with storages from other mods. Oritech really only uses the insert/extract methods, not the insertToSlot/extractFromSlot variants.
    public static class NeoforgeStoragerWrapper implements ItemApi.InventoryStorage {
        
        private final IItemHandler container;
        
        public NeoforgeStoragerWrapper(IItemHandler candidate) {
            this.container = candidate;
        }
        
        @Override
        public int insert(ItemStack inserted, boolean simulate) {
            return inserted.getCount() - ItemHandlerHelper.insertItem(container, inserted, simulate).getCount();
        }
        
        @Override
        public int insertToSlot(ItemStack inserted, int slot, boolean simulate) {
            return inserted.getCount() - container.insertItem(slot, inserted, simulate).getCount();
        }
        
        @Override
        public int extract(ItemStack extracted, boolean simulate) {
            var total = 0;
            for (int i = 0; i < container.getSlots(); i++) {
                var available = container.getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(available, extracted)) {
                    total += container.extractItem(i, extracted.getCount() - total, simulate).getCount();
                }
            }
            
            return total;
        }
        
        @Override
        public int extractFromSlot(ItemStack extracted, int slot, boolean simulate) {
            return container.extractItem(slot, extracted.getCount(), simulate).getCount();
        }
        
        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (container instanceof IItemHandlerModifiable handler) {
                handler.setStackInSlot(slot, stack);
            } else {
                Oritech.LOGGER.error("Unable to set stack in slot: {}, stack is: {}", slot, stack);
                Oritech.LOGGER.error("This should never happen");
            }
        }
        
        @Override
        public ItemStack getStackInSlot(int slot) {
            return container.getStackInSlot(slot);
        }
        
        @Override
        public int getSlotCount() {
            return container.getSlots();
        }
        
        @Override
        public int getSlotLimit(int slot) {
            return container.getSlotLimit(slot);
        }
        
        @Override
        public void update() {
            // nothing to do
        }
    }
    
    // used by other mods to interact with oritech block / storages
    public static class ContainerStorageWrapper implements IItemHandlerModifiable {
        
        public final ItemApi.InventoryStorage container;
        
        public static ContainerStorageWrapper of(ItemApi.InventoryStorage storage) {
            if (storage == null) return null;
            return new ContainerStorageWrapper(storage);
        }
        
        public ContainerStorageWrapper(ItemApi.InventoryStorage container) {
            this.container = container;
        }
        
        @Override
        public int getSlots() {
            return container.getSlotCount();
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return container.getStackInSlot(slot);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack arg, boolean simulate) {
            var inserted = container.insertToSlot(arg, slot, simulate);
            
            if (inserted > 0 && !simulate) {
                container.update();
            }
            
            // need to return the remainder here
            return arg.copyWithCount(arg.getCount() - inserted);
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            var takenStack = container.getStackInSlot(slot).copyWithCount(amount);
            var extracted = container.extractFromSlot(takenStack, slot, simulate);
            
            if (extracted > 0 && !simulate) {
                container.update();
            }
            
            return takenStack.copyWithCount(extracted);
        }
        
        @Override
        public int getSlotLimit(int i) {
            return container.getSlotLimit(i);
        }
        
        @Override
        public boolean isItemValid(int i, @NotNull ItemStack arg) {
            return container.insertToSlot(arg, i, true) > 0;
        }
        
        @Override
        public void setStackInSlot(int i, @NotNull ItemStack arg) {
            container.setStackInSlot(i, arg);
            container.update();
        }
    }
    
}
