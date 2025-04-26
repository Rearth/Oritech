package rearth.oritech;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.AugmentSelectionScreen;
import rearth.oritech.item.tools.util.Helpers;

public final class OritechClient {
    
    public static final KeyBinding AUGMENT_SELECTOR = new KeyBinding("key.oritech.augment_screen", GLFW.GLFW_KEY_G, "key.categories.misc");
    
    public static AugmentSelectionScreen activeScreen = null;
    
    public static void initialize() {
        
        Oritech.LOGGER.info("Oritech client initialization");
        ModScreens.registerScreens();
        
        KeyMappingRegistry.register(AUGMENT_SELECTOR);
        
        // used mainly for prometheum pick
        ClientTickEvent.CLIENT_PRE.register(Helpers::onClientTickEvent);
        
        // used for augment UI
        ClientTickEvent.CLIENT_PRE.register(client -> {
            if (AUGMENT_SELECTOR.wasPressed() && activeScreen == null) {
                activeScreen = new AugmentSelectionScreen();
                client.setScreen(activeScreen);
            } else if (activeScreen != null && !InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), AUGMENT_SELECTOR.boundKey.getCode())) {
                activeScreen.close();
            }
        });
        
        ClientTickEvent.CLIENT_PRE.register(client -> {
            if (client.player != null)
                PlayerAugments.clientTickAugments(client.player);
        });
        
        Oritech.LOGGER.info("Oritech client initialization done");
    }
    
    public static void registerRenderers() {
        
        Oritech.LOGGER.info("Registering oritech renderers");
        ModRenderers.registerRenderers();
    }
}