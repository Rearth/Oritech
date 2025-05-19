package rearth.oritech;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
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
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.util.Helpers;
import rearth.oritech.network.NetworkContent;

public final class OritechClient {
    
    public static final KeyBinding AUGMENT_SELECTOR = new KeyBinding("key.oritech.augment_screen", GLFW.GLFW_KEY_G, "key.categories.misc");
    
    public static AugmentSelectionScreen activeScreen = null;
    
    public static boolean laserActive = false;
    
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
            var player = client.player;
            if (player == null) return;
            
            for (var augment : PlayerAugments.allAugments.values()) {
                if (augment.isEnabled(player))
                    augment.refreshClient(player);
            }
        });
        
        // send mining laser use events to server
        ClientTickEvent.CLIENT_PRE.register(client -> {
            if (client.player != null && client.player.getMainHandStack().getItem() instanceof PortableLaserItem && laserActive) {
                NetworkContent.MACHINE_CHANNEL.clientHandle().send(new NetworkContent.LaserPlayerUsePacket());
            } else {
                laserActive = false;
            }
        });
        
        // interrupt left mouse for portable lasers (only seems to work on fabric)
        ClientRawInputEvent.MOUSE_CLICKED_PRE.register((client, button, action, mods) ->
                                                         handleMouseClicked(client, button, action, mods) ? EventResult.interruptTrue() : EventResult.pass());
        
        Oritech.LOGGER.info("Oritech client initialization done");
    }
    
    // returns true if the event is cancelled
    public static boolean handleMouseClicked(MinecraftClient  client, int button, int action, int mods) {
        if (client.player != null && client.player.getMainHandStack().getItem() instanceof PortableLaserItem && button == 0 && client.currentScreen == null) {
            laserActive = action == 1; // activate laser on mouse down
            return action == 1;
        }
        return false;
    }
    
    public static void registerRenderers() {
        
        Oritech.LOGGER.info("Registering oritech renderers");
        ModRenderers.registerRenderers();
    }
}