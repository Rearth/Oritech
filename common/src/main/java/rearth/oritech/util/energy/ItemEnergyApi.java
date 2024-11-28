package rearth.oritech.util.energy;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;

import java.util.function.Supplier;

public interface ItemEnergyApi {
    
    void registerForItem(Supplier<net.minecraft.item.Item> itemSupplier);
    
    EnergyApi.EnergyContainer find(ItemStack stack, ContainerItemContext context);
    
    ComponentType<Long> getEnergyComponent();
    
}
