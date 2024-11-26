package rearth.oritech.fabric;

import net.fabricmc.api.ModInitializer;
import rearth.oritech.Oritech;
import rearth.oritech.util.EnergyApi;

public final class OritechFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        
        var energyApiInstance = new FabricEnergyApiImpl();
        EnergyApi.BLOCK = energyApiInstance;
        EnergyApi.ITEM = energyApiInstance;
        
        // Run our common setup.
        Oritech.runAllRegistries();
        Oritech.initialize();
        
    }
}
