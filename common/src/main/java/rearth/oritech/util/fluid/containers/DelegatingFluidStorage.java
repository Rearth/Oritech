package rearth.oritech.util.fluid.containers;

import dev.architectury.fluid.FluidStack;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.util.fluid.FluidApi;

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
    
    @Override
    public long insert(FluidStack toInsert, boolean simulate) {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().insert(toInsert, simulate);
        return 0;
    }
    
    @Override
    public long extract(FluidStack toExtract, boolean simulate) {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().extract(toExtract, simulate);
        return 0;
    }
    
    @Override
    public List<FluidStack> getContent() {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().getContent();
        return List.of();
    }
    
    public void setContent(List<FluidStack> content) {
        if (validPredicate.getAsBoolean()) {
            // extract all, then insert new stacks
            var targetStorage = backingStorage.get();
            if (targetStorage instanceof FluidApi.SingleSlotStorage singleSlotContainer && content.size() == 1) {
                singleSlotContainer.setStack(content.getFirst());
            } else if (targetStorage instanceof FluidApi.InOutSlotStorage dualSlotContainer && content.size() == 2) {
                dualSlotContainer.setInStack(content.getFirst());
                dualSlotContainer.setOutStack(content.getLast());
            } else {
                Oritech.LOGGER.error("Using invalid container / snapshot for Delegating Fluid Storage");
            }
        }
    }
    
    @Override
    public void update() {
        if (validPredicate.getAsBoolean())
            backingStorage.get().update();
    }
    
    @Override
    public long getCapacity() {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().getCapacity();
        
        return 0;
    }
    
    @Override
    public boolean supportsInsertion() {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().supportsInsertion();
        
        return false;
    }
    
    @Override
    public boolean supportsExtraction() {
        if (validPredicate.getAsBoolean())
            return backingStorage.get().supportsExtraction();
        
        return false;
    }
}
