package rearth.oritech.util;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface FluidProvider {
    
    Storage<FluidVariant> getFluidStorage(Direction direction);
    
    @Nullable
    default SingleVariantStorage<FluidVariant> getForDirectFluidAccess() { return null;}
    
}
