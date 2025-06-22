package rearth.oritech.api.fluid;

import dev.architectury.fluid.FluidStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.fluid.containers.DelegatingFluidStorage;

import java.util.ArrayList;
import java.util.List;

public class FluidApi {
    
    public static long transferFirst(FluidStorage from, FluidStorage to, long max, boolean simulate) {
        if (from.getContent().isEmpty()) return 0L;
        
        var kind = from.getContent().getFirst();
        if (kind.isEmpty()) return 0L;
        return transfer(from, to, kind.copyWithAmount(max), simulate);
    }
    
    public static long transferLast(FluidStorage from, FluidStorage to, long max, boolean simulate) {
        if (from.getContent().isEmpty()) return 0L;
        
        var kind = from.getContent().getLast();
        if (kind.isEmpty()) return 0L;
        return transfer(from, to, kind.copyWithAmount(max), simulate);
    }
    
    public static long transferLastIncludingInputs(FluidStorage from, FluidStorage to, long max, boolean simulate) {
        if (from.getContent().isEmpty()) return 0L;
        
        if (from instanceof DelegatingFluidStorage delegatingFluidStorage && delegatingFluidStorage.getBackend() != null)
            from = delegatingFluidStorage.getBackend();
        
        if (from instanceof MultiSlotStorage multiSlotStorage) {
            for (int i = multiSlotStorage.getSlotCount() - 1; i >= 0; i--) {
                var storage = multiSlotStorage.getStorageForSlot(i);
                if (!storage.getContent().isEmpty()) {
                    var moved = transferLast(storage, to, max, simulate);
                    if (moved > 0) return moved;
                }
            }
        }
        
        return transferLast(from, to, max, simulate);
        
    }
    
    public static long transfer(FluidStorage from, FluidStorage to, FluidStack toMove, boolean simulate) {
        var extracted = from.extract(toMove, true);
        var inserted = to.insert(toMove.copyWithAmount(extracted), simulate);
        extracted = from.extract(toMove.copyWithAmount(inserted), simulate);
        
        if (extracted > 0 && !simulate) {
            from.update();
            to.update();
        }
        
        return extracted;
    }
    
    public static BlockFluidApi BLOCK;
    public static ItemFluidApi ITEM;
    
    public static abstract class FluidStorage {
        
        public boolean supportsInsertion() {
            return true;
        }
        
        public abstract long insert(FluidStack toInsert, boolean simulate);
        
        public boolean supportsExtraction() {
            return true;
        }
        
        public abstract long extract(FluidStack toExtract, boolean simulate);
        
        public abstract List<FluidStack> getContent();
        
        public abstract void update();
        
        public abstract long getCapacity();
        
    }
    
    public interface ItemProvider {
        SingleSlotStorage getFluidStorage(ItemStack stack);
    }
    
    public interface BlockProvider {
        
        FluidStorage getFluidStorage(@Nullable Direction direction);
        
    }
    
    // used for things like tanks, etc.
    public static abstract class SingleSlotStorage extends FluidStorage {
        
        public abstract void setStack(FluidStack stack);
        public abstract FluidStack getStack();
        
    }
    
    public static abstract class MultiSlotStorage extends FluidStorage {
        
        public abstract FluidStorage getStorageForDirection(Direction direction);
        
        public abstract void setStack(int slot, FluidStack stack);
        public abstract FluidStack getStack(int slot);
        public abstract int getSlotCount();
        public abstract FluidStorage getStorageForSlot(int slot);
        
    }
    
}
