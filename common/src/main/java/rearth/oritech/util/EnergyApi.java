package rearth.oritech.util;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

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
    
    public interface BlockEnergyApi {
        
        void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier);
        
        EnergyContainer find(World world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction);
        
        EnergyContainer find(World world, BlockPos pos, @Nullable Direction direction);
        
        interface EnergyProvider {
            
            EnergyContainer getStorage(Direction direction);
            
        }
    }
    
    public interface ItemEnergyApi {
        void registerForItem(Supplier<Item> itemSupplier);
        
        EnergyContainer find(ItemStack stack, ContainerItemContext context);
        
        ComponentType<Long> getEnergyComponent();
        
        interface EnergyProvider {
            
            EnergyContainer getStorage(ItemStack stack);
            
        }
        
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
