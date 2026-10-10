package rearth.oritech.spaceage;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import rearth.oritech.Oritech;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.init.SpaceAgeCreativeTabs;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.init.SpaceAgeItems;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.init.SpaceAgeRecipes;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.network.RocketNetworking;
import rearth.oritech.spaceage.simulation.MissionController;
import rearth.oritech.spaceage.simulation.RocketSimulationController;
import rearth.oritech.spaceage.simulation.SpaceAgeGameTests;

import java.util.List;

@Mod(OritechSpaceAge.MOD_ID)
public final class OritechSpaceAge {

    public static final String MOD_ID = "oritech_space_age";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OritechSpaceAge(IEventBus modEventBus, ModContainer modContainer) {

        LOGGER.info("Initializing Oritech: Space Age with {}", Oritech.MOD_ID);
        SpaceAgeGameTests.FUNCTIONS.register(modEventBus);
        modEventBus.addListener(SpaceAgeGameTests::register);

        NeoForge.EVENT_BUS.addListener(this::onServerTickPost);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        modEventBus.addListener(this::addNetworkHandlers);
        modEventBus.addListener(SpaceAgeDataMaps::register);

        SpaceAgeComponents.COMPONENTS.register(modEventBus);
        SpaceAgeRecipes.TYPES.register(modEventBus);
        SpaceAgeRecipes.SERIALIZERS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) ->
                event.sendRecipes(List.of(SpaceAgeRecipes.VACUUM_CRAFTING.get())));
        SpaceAgeItems.addBlockItems();
        SpaceAgeBlocks.BLOCKS.register(modEventBus);
        SpaceAgeItems.ITEMS.register(modEventBus);
        SpaceAgeBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        SpaceAgeMenus.MENUS.register(modEventBus);
        SpaceAgeCreativeTabs.TABS.register(modEventBus);
    }

    public static Identifier id(String path) {

        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private void addNetworkHandlers(RegisterPayloadHandlersEvent event) {

        RocketNetworking.register(event.registrar("1"));
        MissionNetworking.register(event.registrar("1"));
    }

    private void onServerTickPost(ServerTickEvent.Post event) {

        RocketSimulationController.tick(event.getServer());
        MissionController.tick(event.getServer());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            RocketSimulationController.syncActiveRocketsToPlayer(serverPlayer);
        }
    }

    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            RocketSimulationController.syncActiveRocketsToPlayer(serverPlayer);
        }
    }
}
