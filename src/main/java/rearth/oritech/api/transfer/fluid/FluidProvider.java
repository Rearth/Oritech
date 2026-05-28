package rearth.oritech.api.transfer.fluid;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;

public interface FluidProvider {
    
    ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction);
    
}
