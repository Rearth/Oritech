package rearth.oritech.api.fluid.containers;

import dev.architectury.fluid.FluidStack;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.FluidApi.FluidStorage;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DelegatingFluidStorage extends FluidApi.FluidStorage {
    
    protected final Supplier<FluidApi.FluidStorage> backingStorage;
    protected final BooleanSupplier validPredicate;
    
    public DelegatingFluidStorage(Supplier<FluidApi.FluidStorage> backingStorage, @Nullable BooleanSupplier validPredicate) {
        this.backingStorage = backingStorage;
        this.validPredicate = validPredicate == null ? () -> true : validPredicate;
    }
    
    public DelegatingFluidStorage(FluidApi.FluidStorage backingStorage, @Nullable BooleanSupplier validPredicate) {
        this(() -> backingStorage, validPredicate);
    }
    
    private boolean canUseBackend() {
        return validPredicate.getAsBoolean() && backingStorage.get() != null;
    }
    
    @Override
    public long insert(FluidStack toInsert, boolean simulate) {
        if (canUseBackend())
            return backingStorage.get().insert(toInsert, simulate);
        return 0;
    }
    
    @Override
    public long extract(FluidStack toExtract, boolean simulate) {
        if (canUseBackend())
            return backingStorage.get().extract(toExtract, simulate);
        return 0;
    }
    
    @Override
    public List<FluidStack> getContent() {
        if (canUseBackend())
            return backingStorage.get().getContent();
        return List.of();
    }
    
    public void setContent(List<FluidStack> content) {
        if (canUseBackend()) {
            // extract all, then insert new stacks
            var targetStorage = backingStorage.get();
            if (targetStorage instanceof FluidApi.SingleSlotStorage singleSlotContainer && content.size() == 1) {
                singleSlotContainer.setStack(content.getFirst());
            } else if (targetStorage instanceof FluidApi.MultiSlotStorage multiSlotContainer && content.size() == multiSlotContainer.getSlotCount()) {
                for (int i = 0; i < multiSlotContainer.getSlotCount(); i++) {
                    multiSlotContainer.setStack(i, content.get(i));
                }
            } else {
                Oritech.LOGGER.error("Using invalid container / snapshot for Delegating Fluid Storage");
            }
        }
    }
    
    @Override
    public void update() {
        if (canUseBackend())
            backingStorage.get().update();
    }
    
    @Override
    public long getCapacity() {
        if (canUseBackend())
            return backingStorage.get().getCapacity();
        
        return 0;
    }
    
    @Override
    public boolean supportsInsertion() {
        if (canUseBackend())
            return backingStorage.get().supportsInsertion();
        
        return false;
    }
    
    @Override
    public boolean supportsExtraction() {
        if (canUseBackend())
            return backingStorage.get().supportsExtraction();
        
        return false;
    }
    
    @Nullable
    public FluidApi.FluidStorage getBackend() {
        if (canUseBackend()) return backingStorage.get();
        return null;
    }
}
