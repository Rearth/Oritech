package rearth.oritech.api.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.ServiceLoader;

public class EnergyApi {
    
    public static BlockEnergyApi BLOCK = ServiceLoader.load(BlockEnergyApi.class)
                                           .findFirst()
                                           .orElseThrow(() -> new IllegalStateException("Failed to load block energy api service."));
    
    public static ItemEnergyApi ITEM = ServiceLoader.load(ItemEnergyApi.class)
                                         .findFirst()
                                         .orElseThrow(() -> new IllegalStateException("Failed to load item energy api service."));
    
    public static Long transfer(EnergyStorage from, EnergyStorage to, long amount, boolean simulate) {
        var extracted = from.extract(amount, true);
        var inserted = to.insert(extracted, simulate);
        extracted = from.extract(inserted, simulate);
        
        if (extracted > 0 && !simulate) {
            from.update();
            to.update();
        }
        
        return extracted;
    }
    
    public interface BlockProvider {
        
        EnergyStorage getEnergyStorage(Direction direction);
        
    }
    
    public interface ItemProvider {
        
        EnergyStorage getEnergyStorage(ItemStack stack);
        
    }
    
    public abstract static class EnergyStorage {
        
        public boolean supportsInsertion() {
            return true;
        }
        
        public abstract long insert(long maxAmount, boolean simulate);
        
        public boolean supportsExtraction() {
            return true;
        }
        
        public abstract long extract(long maxAmount, boolean simulate);
        
        public abstract void setAmount(long amount);
        
        public abstract long getAmount();
        
        public abstract long getCapacity();
        
        public abstract void update();
    }
    
}
