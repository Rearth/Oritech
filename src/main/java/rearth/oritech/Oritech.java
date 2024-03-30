package rearth.oritech;

import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rearth.oritech.block.blocks.pipes.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.ItemPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.*;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.init.world.FeatureContent;
import rearth.oritech.network.NetworkContent;

public class Oritech implements ModInitializer {
    public static final String MOD_ID = "oritech";
    public static final Logger LOGGER = LoggerFactory.getLogger("oritech");
    
    @Override
    public void onInitialize() {
        
        LOGGER.info("Hello Fabric world!");
        
        System.out.println("registering fluids");
        FieldRegistrationHandler.register(FluidContent.class, MOD_ID, false);
        System.out.println("registering items");
        FieldRegistrationHandler.register(ItemContent.class, MOD_ID, false);
        System.out.println("registering blocks");
        FieldRegistrationHandler.register(BlockContent.class, MOD_ID, false);
        System.out.println("registering block entities");
        FieldRegistrationHandler.register(BlockEntitiesContent.class, MOD_ID, false);
        System.out.println("registering others....");
        ItemGroups.registerItemGroup();
        RecipeContent.initialize();
        NetworkContent.registerChannels();
        ParticleContent.registerParticles();
        FeatureContent.initialize();
        
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
    }
    
    private void onServerStarted(MinecraftServer minecraftServer) {
        minecraftServer.getWorlds().forEach(world -> {
            if (world.isClient) return;
            
            var regKey = world.getRegistryKey().getValue();
            
            var dataId = "energy_" + regKey.getNamespace() + "_" + regKey.getPath();
            var result = world.getPersistentStateManager().getOrCreate(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, dataId);
            if (result.hashCode() != 0) {
                System.out.println("setting data: " + result);
                EnergyPipeBlock.ENERGY_PIPE_DATA.put(regKey, result);
            }
            
            var fluidDataId = "fluid_" + regKey.getNamespace() + "_" + regKey.getPath();
            var fluidResult = world.getPersistentStateManager().getOrCreate(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, fluidDataId);
            if (fluidResult.hashCode() != 0) {
                FluidPipeBlock.FLUID_PIPE_DATA.put(regKey, fluidResult);
            }
            
            var itemDataId = "item_" + regKey.getNamespace() + "_" + regKey.getPath();
            var itemResult = world.getPersistentStateManager().getOrCreate(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, itemDataId);
            if (itemResult.hashCode() != 0) {
                ItemPipeBlock.ITEM_PIPE_DATA.put(regKey, itemResult);
            }
        });
    }
}