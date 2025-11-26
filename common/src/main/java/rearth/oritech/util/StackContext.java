package rearth.oritech.util;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

public class StackContext {
    
    private ItemStack value;
    private final Consumer<ItemStack> updater;
    
    public StackContext(ItemStack value, Consumer<ItemStack> updater) {
        this.value = value;
        this.updater = updater;
    }
    
    public ItemStack getValue() {
        return value;
    }
    
    // this is used to apply component changes (e.g. changes inside the itemstack, such as adding or removing components), or to
    // set entirely new itemstacks. New itemstacks need to be synced back to their contains via sync()
    public void setValue(ItemStack value) {
        this.value = value;
    }
    
    // syncs the current itemstack pointer to the context
    public void sync() {
        updater.accept(value);
    }
}
