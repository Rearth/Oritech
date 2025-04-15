package rearth.oritech.util.fluid.containers;

import dev.architectury.fluid.FluidStack;
import net.minecraft.item.ItemStack;
import rearth.oritech.util.fluid.FluidApi;

import java.util.function.Consumer;

public class SimpleItemFluidStorage extends SimpleFluidStorage {
    
    private final ItemStack itemStack;
    public Consumer<ItemStack> contextCallback;
    
    public SimpleItemFluidStorage(Long capacity, ItemStack itemStack) {
        super(capacity);
        this.itemStack = itemStack;
        this.setStack(itemStack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty()));
    }
    
    @Override
    public void update() {
        super.update();
        
        if (this.getStack().isEmpty()) {
            itemStack.remove(FluidApi.ITEM.getFluidComponent());
            return;
        }
        
        itemStack.set(FluidApi.ITEM.getFluidComponent(), this.getStack());
        
        if (contextCallback != null) contextCallback.accept(itemStack);
    }
    
    public SimpleItemFluidStorage withCallback(Consumer<ItemStack> contextCallback) {
        this.contextCallback = contextCallback;
        return this;
    }
}
