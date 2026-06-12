package rearth.oritech;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.client.cablesurfer.ClientZiplineHandler;
import rearth.oritech.client.cablesurfer.ZiplineFxHandler;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.AugmentSelectionScreen;
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;

@Mod(value = Oritech.MOD_ID, dist = Dist.CLIENT)
public final class OritechClient {

    public static final KeyMapping.Category ORITECH_KEYS_CATEGORY = new KeyMapping.Category(Oritech.id("keys"));
    public static final KeyMapping AUGMENT_SELECTOR = new KeyMapping("key.oritech.augment_screen", GLFW.GLFW_KEY_G, ORITECH_KEYS_CATEGORY);

    public static AugmentSelectionScreen activeScreen = null;

    public static boolean laserActive = false;

    public OritechClient(IEventBus modEventBus, ModContainer container) {
        Oritech.LOGGER.info("Oritech client initialization");

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        var neoEventBus = NeoForge.EVENT_BUS;

        neoEventBus.addListener(this::onPreClientTick);
        neoEventBus.addListener(this::onMouseClicked);

        modEventBus.addListener(this::registerBindings);
        modEventBus.addListener(ModScreens::registerScreens);

        ModScreens.MENUS.register(modEventBus);
    }

    private void registerBindings(RegisterKeyMappingsEvent event) {
        event.registerCategory(ORITECH_KEYS_CATEGORY);
        event.register(AUGMENT_SELECTOR);
    }

    private void onPreClientTick(ClientTickEvent.Pre event) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (client.player == null || client.level == null) return;

        // ensure prometheum pick is animated correctly. This is to be in non-pre variant of the event, not sure if this will work
        var stack = client.player.getMainHandItem();
        if (stack.getItem() instanceof PromethiumPickaxeItem pickaxeItem) {
            pickaxeItem.onHeldTick(stack, client.player, client.level);
        }

        // laser player use event
        if (player.getMainHandItem().getItem() instanceof PortableLaserItem && laserActive) {
            ClientPacketDistributor.sendToServer(new PortableLaserItem.LaserPlayerUsePacket());
        } else {
            laserActive = false;
        }

    }

    private void onMouseClicked(InputEvent.MouseButton.Pre event) {

        var client = Minecraft.getInstance();

        if (client.player != null && client.player.getMainHandItem().getItem() instanceof PortableLaserItem && event.getButton() == 0 && client.screen == null) {
            var wasDown = event.getAction() == 1;
            laserActive = wasDown; // activate laser on mouse down
            event.setCanceled(wasDown);
        }
    }

    // old, needs to be updated/migrated as we go
    public static void initialize() {

        // used for augment UI
        ClientTickEvent.CLIENT_PRE.register(client -> {
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

            for (var augment : PlayerAugments.getAllAugments(player.registryAccess()).values()) {
                if (augment.isEnabled(player))
                    augment.refreshClient(player);
            }
        });

        ClientTickEvent.CLIENT_POST.register((client) -> {
            ClientZiplineHandler.onClientTick();
            ZiplineFxHandler.tick();
        });

        Oritech.LOGGER.info("Oritech client initialization done");
    }

    public static void registerRenderers() {

        Oritech.LOGGER.info("Registering oritech renderers");
        ModRenderers.registerRenderers();
    }
}
