package rearth.oritech;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import org.lwjgl.glfw.GLFW;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.client.renderers.BlockOutlineRenderer;
import rearth.oritech.client.ui.AugmentSelectionScreen;
import rearth.oritech.item.tools.armor.BaseJetpackItem;
import rearth.oritech.item.tools.util.Helpers;

public final class OritechClient {
    
    public static final KeyBinding AUGMENT_SELECTOR = new KeyBinding("key.oritech.augment_screen", GLFW.GLFW_KEY_G, "key.categories.misc");
    
    public static AugmentSelectionScreen activeScreen = null;
    
    public static void initialize() {
        
        Oritech.LOGGER.info("Oritech client initialization");
        ModScreens.assignScreens();
        
        KeyBindingHelper.registerKeyBinding(AUGMENT_SELECTOR);
        
        // used mainly for prometheum pick
        ClientTickEvents.START_CLIENT_TICK.register(Helpers::onClientTickEvent);
        
        // used for augment UI
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (AUGMENT_SELECTOR.wasPressed() && activeScreen == null) {
                System.out.println("opening screen");
                activeScreen = new AugmentSelectionScreen();
                client.setScreen(activeScreen);
            } else if (activeScreen != null && !InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), AUGMENT_SELECTOR.boundKey.getCode())) {
                System.out.println("closing screen! " + AUGMENT_SELECTOR.wasPressed());
                activeScreen.close();
            }
        });
        
        // used for elytra jetpack cape rendering
        LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER.register(player -> !(player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof BaseJetpackItem));
        
        // used for area outline rendering
        WorldRenderEvents.BLOCK_OUTLINE.register(OritechClient::renderBlockOutline);
        
        WorldRenderEvents.AFTER_ENTITIES.register(OreFinderRenderer::doRender);
        
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null)
                PlayerAugments.clientTickAugments(client.player);
        });
    }
    
    public static boolean renderBlockOutline(WorldRenderContext worldRenderContext, WorldRenderContext.BlockOutlineContext blockOutlineContext) {
        BlockOutlineRenderer.render(worldRenderContext.world(), worldRenderContext.camera(), worldRenderContext.tickCounter(), worldRenderContext.matrixStack(), worldRenderContext.consumers(), worldRenderContext.gameRenderer(), worldRenderContext.projectionMatrix(), worldRenderContext.lightmapTextureManager(), worldRenderContext.worldRenderer());
        return true;
    }
    
    public static void registerRenderers() {
        
        Oritech.LOGGER.info("Registering oritech renderers");
        ModRenderers.registerRenderers();
    }
}