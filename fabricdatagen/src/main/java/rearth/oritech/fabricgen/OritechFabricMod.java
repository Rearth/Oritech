package rearth.oritech.fabricgen;

import net.fabricmc.api.ModInitializer;
import rearth.oritech.Oritech;

public final class OritechFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        
        // Run our common setup.
        Oritech.runAllRegistries();
        Oritech.initialize();
    }
}
