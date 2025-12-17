package rearth.oritech;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.renderers.MachineGantryRenderer;
import rearth.oritech.client.ui.AugmentSelectionScreen;
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.util.Helpers;
import software.bernie.geckolib.event.GeoRenderEvent;


public final class OritechClient {
    
    public static final KeyMapping AUGMENT_SELECTOR = new KeyMapping("key.oritech.augment_screen", GLFW.GLFW_KEY_G, "key.categories.misc");
    
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
            
            if (PlayerAugments.allAugments.isEmpty() && client.level != null)
                PlayerAugments.loadAllAugments(client.level.getRecipeManager());
            
            if (AUGMENT_SELECTOR.consumeClick() && activeScreen == null) {
                activeScreen = new AugmentSelectionScreen();
                client.setScreen(activeScreen);
            } else if (activeScreen != null && !InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), AUGMENT_SELECTOR.key.getValue())) {
                activeScreen.onClose();
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
            if (client.player != null && client.player.getMainHandItem().getItem() instanceof PortableLaserItem && laserActive) {
                NetworkManager.sendToServer(new PortableLaserItem.LaserPlayerUsePacket());
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
    public static boolean handleMouseClicked(Minecraft  client, int button, int action, int mods) {
        if (client.player != null && client.player.getMainHandItem().getItem() instanceof PortableLaserItem && button == 0 && client.screen == null) {
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