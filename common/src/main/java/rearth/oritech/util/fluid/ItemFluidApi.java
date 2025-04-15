package rearth.oritech.util.fluid;

import dev.architectury.fluid.FluidStack;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.StackContext;

import java.util.function.Supplier;

public interface ItemFluidApi {
    
    void registerForItem(Supplier<Item> itemSupplier);
    
    FluidApi.FluidStorage find(StackContext stack);
    
    default ComponentType<FluidStack> getFluidComponent() {
        return ComponentContent.STORED_FLUID.get();
    }
}
