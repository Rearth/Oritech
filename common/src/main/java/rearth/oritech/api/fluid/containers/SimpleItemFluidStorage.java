package rearth.oritech.api.fluid.containers;

import dev.architectury.fluid.FluidStack;
import rearth.oritech.api.fluid.FluidApi;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public class SimpleItemFluidStorage extends SimpleFluidStorage {
    
    private final ItemStack itemStack;
    public Consumer<ItemStack> contextCallback;
    
    public SimpleItemFluidStorage(Long capacity, ItemStack itemStack) {
        super(capacity);
        this.itemStack = itemStack;
        this.setStack(itemStack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty()));
        
        if (!this.getStack().isEmpty())
            itemStack.set(DataComponents.MAX_STACK_SIZE, 1);
    }
    
    @Override
    public void update() {
        super.update();
        
        if (this.getStack().isEmpty()) {
            itemStack.remove(FluidApi.ITEM.getFluidComponent());
            itemStack.set(DataComponents.MAX_STACK_SIZE, 64);
            return;
        }
        
        itemStack.set(FluidApi.ITEM.getFluidComponent(), this.getStack());
        itemStack.set(DataComponents.MAX_STACK_SIZE, 1);
        
        if (contextCallback != null) contextCallback.accept(itemStack);
    }
    
    public SimpleItemFluidStorage withCallback(Consumer<ItemStack> contextCallback) {
        this.contextCallback = contextCallback;
        return this;
    }
}
