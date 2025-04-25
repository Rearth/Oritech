package rearth.oritech.api.energy;

import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import rearth.oritech.util.StackContext;

import java.util.function.Supplier;

public interface ItemEnergyApi {
    
    void registerForItem(Supplier<Item> itemSupplier);
    
    EnergyApi.EnergyStorage find(StackContext stack);
    
    ComponentType<Long> getEnergyComponent();
    
}
