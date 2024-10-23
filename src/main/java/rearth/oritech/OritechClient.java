package rearth.oritech;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents;
import net.minecraft.entity.EquipmentSlot;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.item.tools.armor.BaseJetpackItem;
import rearth.oritech.item.tools.util.Helpers;

public class OritechClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        Oritech.LOGGER.info("Oritech client initialization");

        ModRenderers.registerRenderers();
        ModScreens.assignScreens();
        
        
        ClientTickEvents.START_CLIENT_TICK.register(Helpers::onClientTickEvent);
        LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER.register(player -> !(player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof BaseJetpackItem));
    }
}