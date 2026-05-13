package rearth.oritech.api.transfer.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DelegatingInventoryStorage implements ResourceHandler<ItemResource> {
    
    protected final Supplier<ItemStacksResourceHandler> backingStorage;
    protected final BooleanSupplier validPredicate;
    
    public DelegatingInventoryStorage(Supplier<ItemStacksResourceHandler> backingStorage, @Nullable BooleanSupplier validPredicate) {
        this.backingStorage = backingStorage;
        this.validPredicate = validPredicate == null ? () -> true : validPredicate;
    }
    
    public DelegatingInventoryStorage(ItemStacksResourceHandler backingStorage, @Nullable BooleanSupplier validPredicate) {
        this(() -> backingStorage, validPredicate);
    }
    
    private boolean canUseBackend() {
        return validPredicate.getAsBoolean() && backingStorage.get() != null;
    }
    
    @Override
    public void update() {
        if (canUseBackend())
            backingStorage.get().update();
    }
    
    @Override
    public boolean supportsInsertion() {
        if (canUseBackend())
            return backingStorage.get().supportsInsertion();
        
        return false;
    }
    
    @Override
    public int insert(ItemStack inserted, boolean simulate) {
        if (canUseBackend())
            return backingStorage.get().insert(inserted, simulate);
        return 0;
    }
    
    @Override
    public int insertToSlot(ItemStack inserted, int slot, boolean simulate) {
        if (canUseBackend())
            return backingStorage.get().insertToSlot(inserted, slot, simulate);
        return 0;
    }
    
    @Override
    public boolean supportsExtraction() {
        if (canUseBackend())
            return backingStorage.get().supportsExtraction();
        
        return false;
    }
    
    @Override
    public int extract(ItemStack extracted, boolean simulate) {
        if (canUseBackend())
            return backingStorage.get().extract(extracted, simulate);
        return 0;
    }
    
    @Override
    public int extractFromSlot(ItemStack extracted, int slot, boolean simulate) {
        if (canUseBackend())
            return backingStorage.get().extractFromSlot(extracted, slot, simulate);
        
        return 0;
    }
    
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (canUseBackend())
            backingStorage.get().setStackInSlot(slot, stack);
    }
    
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (canUseBackend())
            return backingStorage.get().getStackInSlot(slot);
        
        return ItemStack.EMPTY;
    }
    
    @Override
    public int getSlotCount() {
        if (canUseBackend())
            return backingStorage.get().getSlotCount();
        return 0;
    }
    
    @Override
    public int getSlotLimit(int slot) {
        if (canUseBackend())
            return backingStorage.get().getSlotLimit(slot);
        return 0;
    }
}
