package rearth.oritech.fabric;

import net.fabricmc.api.ModInitializer;
import rearth.oritech.Oritech;
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
        
        // Run our common setup.
        Oritech.runAllRegistries();
        Oritech.initialize();
        
    }
}
