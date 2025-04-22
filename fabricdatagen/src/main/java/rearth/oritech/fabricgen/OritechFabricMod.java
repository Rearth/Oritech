package rearth.oritech.fabricgen;

import net.fabricmc.api.ModInitializer;
import rearth.oritech.Oritech;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.fluid.FluidApi;
import rearth.oritech.util.item.ItemApi;

public final class OritechFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        
        // Run our common setup.
        Oritech.runAllRegistries();
        Oritech.initialize();
    }
}
