package rearth.oritech.fabricgen;

import dev.architectury.fluid.FluidStack;
import net.fabricmc.api.ModInitializer;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.fabric.FabricEnergyApiImpl;
import rearth.oritech.fabric.FabricFluidApiImpl;
import rearth.oritech.fabric.FabricItemApi;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.item.ItemApi;

public final class OritechFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        
        var energyApiInstance = new FabricEnergyApiImpl();
        EnergyApi.BLOCK = energyApiInstance;
        EnergyApi.ITEM = energyApiInstance;
        
        var fluidApiInstance = new FabricFluidApiImpl();
        FluidApi.BLOCK = fluidApiInstance;
        FluidApi.ITEM = fluidApiInstance;
        
        ItemApi.BLOCK = new FabricItemApi();
        
        NetworkManager.FLUID_STACK_CODEC = FluidStack.CODEC;
        NetworkManager.FLUID_STACK_STREAM_CODEC = FluidStack.STREAM_CODEC;
        
        // Run our common setup.
        Oritech.runAllRegistries();
        Oritech.initialize();
    }
}
