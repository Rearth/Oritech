package rearth.oritech.api.energy.containers;

import rearth.oritech.api.energy.EnergyApi;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

public class SimpleEnergyItemStorage extends SimpleEnergyStorage {
    
    private final ItemStack stack;
    public Consumer<ItemStack> contextCallback;
    
    public SimpleEnergyItemStorage(long maxInsert, long maxExtract, long capacity, ItemStack stack) {
        super(maxInsert, maxExtract, capacity);
        this.stack = stack;
        this.setAmount(stack.getOrDefault(EnergyApi.ITEM.getEnergyComponent(), 0L));
    }
    
    @Override
    public void update() {
        super.update();
        stack.set(EnergyApi.ITEM.getEnergyComponent(), getAmount());
        
        if (contextCallback != null) contextCallback.accept(stack);
    }
    
    public SimpleEnergyItemStorage withCallback(Consumer<ItemStack> contextCallback) {
        this.contextCallback = contextCallback;
        return this;
    }
}
