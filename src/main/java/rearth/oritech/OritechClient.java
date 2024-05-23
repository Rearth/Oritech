package rearth.oritech;

import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.item.tools.util.Helpers;

public class OritechClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        Oritech.LOGGER.info("Oritech client initialization");

        ModRenderers.registerRenderers();
        FieldRegistrationHandler.register(ModScreens.class, Oritech.MOD_ID, false);
        ModScreens.assignScreens();
        
        
        ClientTickEvents.START_CLIENT_TICK.register(Helpers::onClientTickEvent);
    }
}
