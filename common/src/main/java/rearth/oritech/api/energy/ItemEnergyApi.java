package rearth.oritech.api.energy;

import rearth.oritech.util.StackContext;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;

public interface ItemEnergyApi {
    
    void registerForItem(Supplier<Item> itemSupplier);
    
    EnergyApi.EnergyStorage find(StackContext stack);
    
    DataComponentType<Long> getEnergyComponent();
    
}
