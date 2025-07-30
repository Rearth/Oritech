package rearth.oritech.fabric;

import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.util.StackContext;

public class ItemStackStorage extends SingleStackStorage {
    
    private final StackContext stack;
    
    public ItemStackStorage(StackContext stack) {
        this.stack = stack;
    }
    
    @Override
    protected ItemStack getStack() {
        return stack.getValue();
    }
    
    @Override
    protected void setStack(ItemStack stack) {
        this.stack.setValue(stack);
    }
}
