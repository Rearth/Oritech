package rearth.oritech.item.other;

import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import rearth.oritech.init.ComponentContent;

public class PortableTankFluidHandler extends ItemAccessFluidHandler {

    public PortableTankFluidHandler(ItemAccess itemAccess, int capacity) {
        super(itemAccess, ComponentContent.STORED_FLUID.get(), capacity);
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        if (newAmount == 0) {
            return accessResource
                    .without(component)
                    .with(DataComponents.MAX_STACK_SIZE, accessResource.getItem().getDefaultMaxStackSize());
        }

        return accessResource
                .with(component, SimpleFluidContent.copyOf(newResource.toStack(newAmount)))
                .with(DataComponents.MAX_STACK_SIZE, 1);
    }
}
