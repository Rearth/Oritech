package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import rearth.oritech.block.fluid.FuelFluid;
import rearth.oritech.block.fluid.OilFluid;
import rearth.oritech.block.fluid.SteamFluid;

public class FluidContent implements AutoRegistryContainer<Fluid> {
    
    public static final Fluid STILL_OIL = new OilFluid.Still();
    public static final Fluid FLOWING_OIL = new OilFluid.Flowing();
    public static final Fluid STILL_FUEL = new FuelFluid.Still();
    public static final Fluid FLOWING_FUEL = new FuelFluid.Flowing();
    public static final Fluid STILL_STEAM = new SteamFluid.Still();
    public static final Fluid FLOWING_STEAM = new SteamFluid.Flowing();
    
    @Override
    public Registry<Fluid> getRegistry() {
        return Registries.FLUID;
    }
    
    @Override
    public Class<Fluid> getTargetFieldType() {
        return Fluid.class;
    }
}
