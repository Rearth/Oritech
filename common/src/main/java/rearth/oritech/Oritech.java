package rearth.oritech;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;
import rearth.oritech.block.entity.addons.AddonBlockEntity;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.*;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.init.world.FeatureContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.registry.ArchitecturyBlockRegistryContainer;
import rearth.oritech.util.registry.ArchitecturyRecipeRegistryContainer;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

public final class Oritech {
    
    public static final String MOD_ID = "oritech";
    public static final Logger LOGGER = LoggerFactory.getLogger("oritech");
    public static final OritechConfig CONFIG = OritechConfig.createAndLoad();
    
    public static final Multimap<Identifier, Runnable> EVENT_MAP = initEventMap();
    
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
    
    static {
        ReflectiveEndecBuilder.SHARED_INSTANCE.register(MinecraftEndecs.IDENTIFIER, Identifier.class);
    }
    
    public static void initialize() {
        
        LOGGER.info("Begin Oritech initialization");
        NetworkContent.registerChannels();  // this seems to break datagen for some reason as it claims its using client code?
        ParticleContent.registerParticles();
        FeatureContent.initialize();
        
        // for pipe data
        LifecycleEvent.SERVER_STARTED.register(Oritech::onServerStarted);
        
        // for augment data
        LifecycleEvent.SERVER_STARTED.register(server -> PlayerAugments.loadAllAugments(server.getRecipeManager()));
        
        // for particle collisions
        TickEvent.SERVER_POST.register(elem -> AcceleratorParticleLogic.onTickEnd());
        TickEvent.SERVER_POST.register(elem -> AddonBlockEntity.completeInits());
        
        // for player augment modifiers
        PlayerEvent.PLAYER_JOIN.register(PlayerAugments::refreshPlayerAugments);
        PlayerEvent.PLAYER_RESPAWN.register((player, inEnd, removalReason) -> PlayerAugments.refreshPlayerAugments(player));
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> PlayerAugments.refreshPlayerAugments(player));
        
        // for player augment ticks
        TickEvent.SERVER_PRE.register(event -> event.getWorlds().forEach(world -> world.getPlayers().forEach(PlayerAugments::serverTickAugments)));
        LOGGER.info("Oritech initialization complete");
    }
    
    // fabric only
    public static void runAllRegistries() {
        
        LOGGER.info("Running Oritech registrations...");
        
        // fluids need to be first
        LOGGER.debug("Registering fluids");
        EVENT_MAP.get(RegistryKeys.FLUID.getValue()).forEach(Runnable::run);
        
        for (var type : EVENT_MAP.keySet()) {
            if (type.equals(RegistryKeys.FLUID.getValue()) || type.equals(RegistryKeys.ITEM_GROUP.getValue())) continue;
            EVENT_MAP.get(type).forEach(Runnable::run);
        }
        
        LOGGER.debug("Registering item groups");
        EVENT_MAP.get(RegistryKeys.ITEM_GROUP.getValue()).forEach(Runnable::run);
        LOGGER.info("Oritech registrations complete");
    }
    
    public static Multimap<Identifier, Runnable> initEventMap() {
        
        Multimap<Identifier, Runnable> res = ArrayListMultimap.create();
        res.put(RegistryKeys.FLUID.getValue(), FluidContent::registerFluids);
        res.put(RegistryKeys.BLOCK.getValue(), FluidContent::registerBlocks);
        res.put(RegistryKeys.ITEM.getValue(), FluidContent::registerItems);
        res.put(RegistryKeys.ITEM.getValue(), () -> ArchitecturyRegistryContainer.register(ItemContent.class, MOD_ID, false));
        res.put(RegistryKeys.BLOCK.getValue(), () -> ArchitecturyRegistryContainer.register(BlockContent.class, MOD_ID, false));
        res.put(RegistryKeys.ITEM.getValue(), ArchitecturyBlockRegistryContainer::finishItemRegister);
        res.put(RegistryKeys.BLOCK_ENTITY_TYPE.getValue(), () -> ArchitecturyRegistryContainer.register(BlockEntitiesContent.class, MOD_ID, false));
        res.put(RegistryKeys.SOUND_EVENT.getValue(), () -> ArchitecturyRegistryContainer.register(SoundContent.class, MOD_ID, false));
        res.put(RegistryKeys.ITEM.getValue(), () -> ArchitecturyRegistryContainer.register(ToolsContent.class, MOD_ID, false));
        res.put(RegistryKeys.DATA_COMPONENT_TYPE.getValue(), ComponentContent::registerComponents);
        res.put(RegistryKeys.FEATURE.getValue(), () -> ArchitecturyRegistryContainer.register(FeatureContent.class, MOD_ID, false));
        res.put(RegistryKeys.LOOT_FUNCTION_TYPE.getValue(), () -> ArchitecturyRegistryContainer.register(LootContent.class, MOD_ID, false));
        res.put(RegistryKeys.ENTITY_TYPE.getValue(), () -> ArchitecturyRegistryContainer.register(EntitiesContent.class, MOD_ID, false));
        res.put(RegistryKeys.ITEM.getValue(), ToolsContent::registerEventHandlers);
        res.put(RegistryKeys.SCREEN_HANDLER.getValue(), () -> ArchitecturyRegistryContainer.register(ModScreens.class, MOD_ID, false));
        res.put(RegistryKeys.RECIPE_TYPE.getValue(), () -> ArchitecturyRegistryContainer.register(RecipeContent.class, MOD_ID, false));
        res.put(RegistryKeys.ITEM_GROUP.getValue(), () -> ArchitecturyRegistryContainer.register(ItemGroups.class, MOD_ID, false));
        res.put(RegistryKeys.RECIPE_SERIALIZER.getValue(), ArchitecturyRecipeRegistryContainer::finishSerializerRegister);
        res.put(RegistryKeys.LOOT_FUNCTION_TYPE.getValue(), FluidContent::registerItemsToGroups);
        res.put(Identifier.of("neoforge", "attachment_types"), Augment::registerAttachmentTypes);   // this works just fine on fabric aswell, as they key is not really relevant there.
        
        return res;
    }
    
    private static void onServerStarted(MinecraftServer minecraftServer) {
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
            
            var superConductorDataId = "superconductor_" + regKey.getNamespace() + "_" + regKey.getPath();
            var superConductorResult = world.getPersistentStateManager().getOrCreate(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, superConductorDataId);
            SuperConductorBlock.SUPERCONDUCTOR_DATA.put(regKey, superConductorResult);
        });
    }
}