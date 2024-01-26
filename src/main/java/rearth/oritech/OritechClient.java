package rearth.oritech;

import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ClientModInitializer;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.init.ModScreenHandlers;

public class OritechClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        Oritech.LOGGER.info("Oritech client initialization");

        ModRenderers.registerRenderers();
        FieldRegistrationHandler.register(ModScreenHandlers.class, Oritech.MOD_ID, false);
        ModScreenHandlers.assignScreens();

    }
}
