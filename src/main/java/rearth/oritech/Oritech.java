package rearth.oritech;

import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rearth.oritech.block.blocks.pipes.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.ItemPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.*;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.init.world.FeatureContent;
import rearth.oritech.item.tools.harvesting.PromethiumAxeItem;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import rearth.oritech.network.NetworkContent;

public class Oritech implements ModInitializer {
    
    public static final String MOD_ID = "oritech";
    public static final Logger LOGGER = LoggerFactory.getLogger("oritech");
    public static final OritechConfig CONFIG = OritechConfig.createAndLoad();
    
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
    
    @Override
    public void onInitialize() {
        
        LOGGER.info("Begin Oritech initialization");
        
        Oritech.LOGGER.debug("registering fluids");
        FieldRegistrationHandler.register(FluidContent.class, MOD_ID, false);
        Oritech.LOGGER.debug("registering items");
        FieldRegistrationHandler.register(ItemContent.class, MOD_ID, false);
        Oritech.LOGGER.debug("registering blocks");
        FieldRegistrationHandler.register(BlockContent.class, MOD_ID, false);
        Oritech.LOGGER.debug("registering block entities");
        FieldRegistrationHandler.register(BlockEntitiesContent.class, MOD_ID, false);
        Oritech.LOGGER.debug("registering screen handlers");
        FieldRegistrationHandler.register(ModScreens.class, Oritech.MOD_ID, false);
        Oritech.LOGGER.debug("registering sounds");
        FieldRegistrationHandler.register(SoundContent.class, Oritech.MOD_ID, false);
        Oritech.LOGGER.debug("registering others...");
        FieldRegistrationHandler.register(ToolsContent.class, MOD_ID, false);
        ToolsContent.registerEventHandlers();
        ItemGroups.registerItemGroup();
        RecipeContent.initialize();
        NetworkContent.registerChannels();
        ParticleContent.registerParticles();
        FeatureContent.initialize();
        
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        PlayerBlockBreakEvents.BEFORE.register(PromethiumPickaxeItem::preMine);
        ServerTickEvents.START_WORLD_TICK.register(PromethiumAxeItem::onTick);
    }
    
    private void onServerStarted(MinecraftServer minecraftServer) {
        minecraftServer.getWorlds().forEach(world -> {
            if (world.isClient) return;
            
            var regKey = world.getRegistryKey().getValue();
            
            var dataId = "energy_" + regKey.getNamespace() + "_" + regKey.getPath();
            var result = world.getPersistentStateManager().getOrCreate(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, dataId);
            EnergyPipeBlock.ENERGY_PIPE_DATA.put(regKey, result);
            
            var fluidDataId = "fluid_" + regKey.getNamespace() + "_" + regKey.getPath();
            var fluidResult = world.getPersistentStateManager().getOrCreate(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, fluidDataId);
            FluidPipeBlock.FLUID_PIPE_DATA.put(regKey, fluidResult);
            
            var itemDataId = "item_" + regKey.getNamespace() + "_" + regKey.getPath();
            var itemResult = world.getPersistentStateManager().getOrCreate(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, itemDataId);
            ItemPipeBlock.ITEM_PIPE_DATA.put(regKey, itemResult);
        });
    }
}