package rearth.oritech;

import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ClientModInitializer;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.init.ModScreens;

public class OritechClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        Oritech.LOGGER.info("Oritech client initialization");

        ModRenderers.registerRenderers();
        FieldRegistrationHandler.register(ModScreens.class, Oritech.MOD_ID, false);
        ModScreens.assignScreens();

    }
}
