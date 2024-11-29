package rearth.oritech.util.energy;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

public class EnergyApi {
    
    public static BlockEnergyApi BLOCK;
    public static ItemEnergyApi ITEM;
    
    public static Long transfer(EnergyContainer from, EnergyContainer to, long amount, boolean simulate) {
        var extracted = from.extract(amount, true);
        var inserted = to.insert(extracted, simulate);
        extracted = from.extract(inserted, simulate);
        
        if (extracted > 0) {
            from.update();
            to.update();
        }
        
        return extracted;
    }
    
    public interface BlockProvider {
        
        EnergyContainer getStorage(Direction direction);
        
    }
    
    public interface ItemProvider {
        
        EnergyContainer getStorage(ItemStack stack);
        
    }
    
    public abstract static class EnergyContainer {
        
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
