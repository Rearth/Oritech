package rearth.oritech;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.lwjgl.glfw.GLFW;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.client.cablesurfer.ActiveCableRenderer;
import rearth.oritech.client.cablesurfer.ClientZiplineHandler;
import rearth.oritech.client.cablesurfer.ZiplineFxHandler;
import rearth.oritech.client.init.*;
import rearth.oritech.client.renderers.BlockOutlineRenderer;
import rearth.oritech.client.renderers.OreFinderRenderer;
import rearth.oritech.client.renderers.SmallTankItemRenderer;
import rearth.oritech.client.ui.AugmentSelectionScreen;
import rearth.oritech.datagen.AdvancementGenerator;
import rearth.oritech.datagen.BlockLootGenerator;
import rearth.oritech.datagen.ModelGenerator;
import rearth.oritech.datagen.RecipeGenerator;
import rearth.oritech.datagen.tags.BlockTagGenerator;
import rearth.oritech.datagen.tags.EntityTagGenerator;
import rearth.oritech.datagen.tags.FluidTagGenerator;
import rearth.oritech.datagen.tags.ItemTagGenerator;
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;

import java.util.List;
import java.util.Set;

@Mod(value = Oritech.MOD_ID, dist = Dist.CLIENT)
public final class OritechClient {

    public static final KeyMapping.Category ORITECH_KEYS_CATEGORY = new KeyMapping.Category(Oritech.id("keys"));
    public static final KeyMapping AUGMENT_SELECTOR = new KeyMapping("key.oritech.augment_screen", GLFW.GLFW_KEY_G, ORITECH_KEYS_CATEGORY);

    public static AugmentSelectionScreen activeScreen = null;

    public static boolean laserActive = false;

    public OritechClient(IEventBus modEventBus, ModContainer container) {
        Oritech.LOGGER.info("Oritech client initialization");

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        container.registerConfig(ModConfig.Type.CLIENT, OritechClientConfig.CLIENT_SPEC);

        var neoEventBus = NeoForge.EVENT_BUS;

        neoEventBus.addListener(this::onPreClientTick);
        neoEventBus.addListener(this::onPostClientTick);
        neoEventBus.addListener(this::onMouseClicked);
        neoEventBus.addListener(this::onBlockOutlinesRendered);

        // zipline cable rendering uses the split-phase extraction/submission render flow
        neoEventBus.addListener(ActiveCableRenderer::onExtractRenderState);
        neoEventBus.addListener(ActiveCableRenderer::onSubmitGeometry);

        // ore-scanner highlight uses the same split-phase world-render flow
        neoEventBus.addListener(OreFinderRenderer::onExtractRenderState);
        neoEventBus.addListener(OreFinderRenderer::onSubmitGeometry);

        modEventBus.addListener(this::registerBindings);
        modEventBus.addListener(this::registerSpecialModelRenderers);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(ModScreens::registerScreens);
        modEventBus.addListener(ModRenderers::registerRenderers);
        modEventBus.addListener(ClientGuiRenderers::registerPipRenderers);
        modEventBus.addListener(FluidModelContent::registerFluidModels);
    }

    // client datagen
    public void gatherData(GatherDataEvent.Client event) {
        event.createProvider(ModelGenerator::new);
        event.createProvider(BlockTagGenerator::new);
        event.createProvider(ItemTagGenerator::new);
        event.createProvider(FluidTagGenerator::new);
        event.createProvider(EntityTagGenerator::new);

        var generator = event.getGenerator();
        var packOutput = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();

        // Register Recipe Generator
        generator.addProvider(true, new RecipeGenerator.Runner(packOutput, lookupProvider));

        event.createProvider((output, provider) ->
                new AdvancementProvider(output, provider, List.of(new AdvancementGenerator())));

        // no idea why this is on the client, but oh well
        event.createProvider((output, lookup) -> new LootTableProvider(
                output,
                Set.of(),
                List.of(
                        new LootTableProvider.SubProviderEntry(BlockLootGenerator::new, LootContextParamSets.BLOCK)
                ),
                lookup
        ));
    }

    private void registerBindings(RegisterKeyMappingsEvent event) {
        event.registerCategory(ORITECH_KEYS_CATEGORY);
        event.register(AUGMENT_SELECTOR);
    }

    // wires the portable tank item renderer into the 26.1 special-model item pipeline
    private void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(SmallTankItemRenderer.ID, SmallTankItemRenderer.Unbaked.MAP_CODEC);
    }

    private void onPreClientTick(ClientTickEvent.Pre event) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (client.player == null || client.level == null) return;

        // ensure prometheum pick is animated correctly
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

        // used for augment UI
        if (AUGMENT_SELECTOR.consumeClick() && activeScreen == null) {
            activeScreen = new AugmentSelectionScreen();
            client.setScreen(activeScreen);
        } else if (activeScreen != null && !InputConstants.isKeyDown(client.getWindow(), AUGMENT_SELECTOR.getKey().getValue())) {
            activeScreen.onClose();
        }

        for (var augment : PlayerAugments.getAllAugments(player.registryAccess()).values()) {
            if (augment.isEnabled((Player) player))
                augment.refreshClient(player);
        }
    }

    private void onPostClientTick(ClientTickEvent.Post event) {
        ClientZiplineHandler.onClientTick();
        ZiplineFxHandler.tick();
    }

    private void onMouseClicked(InputEvent.MouseButton.Pre event) {

        var client = Minecraft.getInstance();

        if (client.player != null && client.player.getMainHandItem().getItem() instanceof PortableLaserItem && event.getButton() == 0 && client.screen == null) {
            var wasDown = event.getAction() == 1;
            laserActive = wasDown; // activate laser on mouse down
            event.setCanceled(wasDown);
        }
    }

    private void onBlockOutlinesRendered(ExtractBlockOutlineRenderStateEvent event) {
        BlockOutlineRenderer.onOutlineExtract(event);
    }
}
