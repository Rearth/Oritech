package rearth.oritech.api.energy;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import rearth.oritech.util.StackContext;

import java.util.function.Supplier;

public interface ItemEnergyApi {
    
    void registerForItem(Supplier<Item> itemSupplier);
    
    EnergyApi.EnergyStorage find(StackContext stack);
    
    DataComponentType<Long> getEnergyComponent();
    
}
