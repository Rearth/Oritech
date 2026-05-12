package rearth.oritech;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.resource.NeoForgeReloadListeners;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.blocks.processing.RefineryBlock;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;
import rearth.oritech.block.entity.addons.AddonBlockEntity;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.interaction.PowerPoleEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.*;
import rearth.oritech.init.world.FeatureContent;
import rearth.oritech.item.tools.ElectricMaceItem;
import rearth.oritech.util.ServerZiplineHandler;

// todos: compostables


@Mod(Oritech.MOD_ID)
public final class Oritech {
    
    public static final String MOD_ID = "oritech";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
    
    public Oritech(IEventBus modEventBus, ModContainer modContainer) {
        
        LOGGER.info("Hello from Oritech!");
        
        // runtime events
        var neoEventBus = NeoForge.EVENT_BUS;
        neoEventBus.addListener(this::onServerStarted);
        neoEventBus.addListener(this::onServerTickPost);
        neoEventBus.addListener(this::onLevelTickPos);
        neoEventBus.addListener(this::onPlayerTickPost);
        neoEventBus.addListener(this::addServerReloadListeners);
        
        // registration events
        modEventBus.addListener(this::commonSetup);
        
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, OritechConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.STARTUP, OritechStartupConfig.STARTUP_SPEC);
        
        // codecs for reflective builders
        NetworkManager.loadDefaultCodecs();
        
        // registrations
        ItemContent.ITEMS.register(modEventBus);
        BlockContent.BLOCKS.register(modEventBus);
        BlockContent.BLOCK_ITEMS.register(modEventBus);
        ToolsContent.EQUIPMENT.register(modEventBus);
        
        ItemGroups.TABS.register(modEventBus);
        SoundContent.SOUND_EVENTS.register(modEventBus);
        LootContent.LOOT_FUNCTIONS.register(modEventBus);
        EntitiesContent.ENTITY_TYPES.register(modEventBus);
        ComponentContent.COMPONENTS.register(modEventBus);
        FeatureContent.FEATURES.register(modEventBus);
        
        FluidContent.FLUID_TYPES.register(modEventBus);
        FluidContent.FLUIDS.register(modEventBus);
        FluidContent.FLUID_BLOCKS.register(modEventBus);
        FluidContent.BUCKET_ITEMS.register(modEventBus);
        
        // register networking
        modEventBus.addListener(this::addNetworkHandlers);
        
        // post processing / extra registrations
        BlockContent.AddBlockItems();
        
        LOGGER.info("All events registered to the event busses");
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        
    }
    
    private void onServerTickPost(ServerTickEvent.Post event) {
        AcceleratorParticleLogic.onTickEnd();
        AddonBlockEntity.completeInits();
        RefineryBlock.updateTaintEvents();
    }
    
    private void onLevelTickPos(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;
        ElectricMaceItem.processLightningEvents(event.getLevel());
    }
    
    private void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerZiplineHandler.onPlayerTick(serverPlayer);
            PlayerAugments.serverTickAugments(serverPlayer);
        }
    }
    
    private void onServerStarted(ServerStartedEvent event) {
        
        // load augments from recipes
        PlayerAugments.loadAllAugments(event.getServer().getRecipeManager());
        
        // load pipe data to memory
        event.getServer().getAllLevels().forEach(this::loadLevelPipeData);
    }
    
    private void addServerReloadListeners(AddServerReloadListenersEvent event) {
        
        var id = id("augment_recipe_watcher");
        
        // refresh augments when recipes are reloaded
        event.addListener(id, new SimplePreparableReloadListener<Void>() {
            @Override
            protected @NotNull Void prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                return null;
            }
            
            @Override
            protected void apply(@NotNull Void ignored, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                PlayerAugments.loadAllAugments(event.getServerResources().getRecipeManager());
            }
        });
        event.addDependency(NeoForgeReloadListeners.RECIPE_PRIORITIES, id);
    }
    
    private void addNetworkHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        NetworkManager.initClientBound(registrar);
        NetworkManager.initServerBound(registrar);
    }
    
    private void loadLevelPipeData(ServerLevel level) {
        var dimId = level.dimension().identifier();
        var dataId = "energy_" + dimId + "_" + dimId.getPath();
        var result = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, dataId);
        EnergyPipeBlock.ENERGY_PIPE_DATA.put(dimId, result);
        
        var fluidDataId = "fluid_" + dimId.getNamespace() + "_" + dimId.getPath();
        var fluidResult = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, fluidDataId);
        FluidPipeBlock.FLUID_PIPE_DATA.put(dimId, fluidResult);
        
        var itemDataId = "item_" + dimId.getNamespace() + "_" + dimId.getPath();
        var itemResult = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, itemDataId);
        ItemPipeBlock.ITEM_PIPE_DATA.put(dimId, itemResult);
        
        var superConductorDataId = "superconductor_" + dimId.getNamespace() + "_" + dimId.getPath();
        var superConductorResult = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, superConductorDataId);
        SuperConductorBlock.SUPERCONDUCTOR_DATA.put(dimId, superConductorResult);
        
        var powerPoleId = "pole_" + dimId.getNamespace() + "_" + dimId.getPath();
        var powerPoleResult = level.getDataStorage().computeIfAbsent(PowerPoleEntity.PoleNetworkData.TYPE, powerPoleId);
        PowerPoleEntity.POLE_NETWORK_DATA.put(dimId, powerPoleResult);
    }
}
