package rearth.oritech.api.item.containers;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.item.ItemApi;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public class DelegatingInventoryStorage implements ItemApi.InventoryStorage {
    
    protected final Supplier<ItemApi.InventoryStorage> backingStorage;
    protected final BooleanSupplier validPredicate;
    
    public DelegatingInventoryStorage(Supplier<ItemApi.InventoryStorage> backingStorage, @Nullable BooleanSupplier validPredicate) {
        this.backingStorage = backingStorage;
        this.validPredicate = validPredicate == null ? () -> true : validPredicate;
    }
    
    public DelegatingInventoryStorage(ItemApi.InventoryStorage backingStorage, @Nullable BooleanSupplier validPredicate) {
        this(() -> backingStorage, validPredicate);
    }
    
    @Override
    public void update() {
        if (validPredicate.getAsBoolean())
            backingStorage.get().update();
    }
    
    @Override
    public boolean supportsInsertion() {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().supportsInsertion();
        
        return false;
    }
    
    @Override
    public int insert(ItemStack inserted, boolean simulate) {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().insert(inserted, simulate);
        return 0;
    }
    
    @Override
    public int insertToSlot(ItemStack inserted, int slot, boolean simulate) {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().insertToSlot(inserted, slot, simulate);
        return 0;
    }
    
    @Override
    public boolean supportsExtraction() {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().supportsExtraction();
        
        return false;
    }
    
    @Override
    public int extract(ItemStack extracted, boolean simulate) {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().extract(extracted, simulate);
        return 0;
    }
    
    @Override
    public int extractFromSlot(ItemStack extracted, int slot, boolean simulate) {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().extractFromSlot(extracted, slot, simulate);
        
        return 0;
    }
    
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (validPredicate.getAsBoolean())
            backingStorage.get().setStackInSlot(slot, stack);
    }
    
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().getStackInSlot(slot);
        
        return ItemStack.EMPTY;
    }
    
    @Override
    public int getSlotCount() {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().getSlotCount();
        return 0;
    }
    
    @Override
    public int getSlotLimit(int slot) {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().getSlotLimit(slot);
        return 0;
    }
}
