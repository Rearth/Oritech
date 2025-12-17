package rearth.oritech;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;
import rearth.oritech.block.entity.addons.AddonBlockEntity;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.block.entity.interaction.PowerPoleEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.*;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.init.world.FeatureContent;
import rearth.oritech.item.tools.ElectricMaceItem;
import rearth.oritech.util.registry.ArchitecturyBlockRegistryContainer;
import rearth.oritech.util.registry.ArchitecturyRecipeRegistryContainer;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

public final class Oritech {
    
    public static final String MOD_ID = "oritech";
    public static final Logger LOGGER = LoggerFactory.getLogger("oritech");
    public static final OritechConfig CONFIG = OritechConfig.createAndLoad();
    
    public static final Multimap<ResourceLocation, Runnable> EVENT_MAP = initEventMap();
    
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    
    static {
        ReflectiveEndecBuilder.SHARED_INSTANCE.register(MinecraftEndecs.IDENTIFIER, ResourceLocation.class);
    }
    
    public static void initialize() {
        
        LOGGER.info("Begin Oritech initialization");
        NetworkManager.init();
        NetworkManager.registerDefaultCodecs();
        ParticleContent.registerParticles();
        FeatureContent.initialize();
        
        // for pipe data
        LifecycleEvent.SERVER_STARTED.register(Oritech::onServerStarted);
        
        // for augment data
        LifecycleEvent.SERVER_STARTED.register(server -> PlayerAugments.loadAllAugments(server.getRecipeManager()));
        
        // for particle collisions
        TickEvent.SERVER_POST.register(elem -> AcceleratorParticleLogic.onTickEnd());
        TickEvent.SERVER_POST.register(elem -> AddonBlockEntity.completeInits());
        TickEvent.SERVER_POST.register(elem -> ElectricMaceItem.processLightningEvents(elem.overworld()));
        
        ComponentContent.COMPONENTS.register();
        
        // for player augment ticks
        TickEvent.SERVER_PRE.register(event -> event.getAllLevels().forEach(world -> world.players().forEach(PlayerAugments::serverTickAugments)));
        LOGGER.info("Oritech initialization complete");
    }
    
    // fabric only
    public static void runAllRegistries() {
        
        LOGGER.info("Running Oritech registrations...");
        
        // fluids need to be first
        LOGGER.debug("Registering fluids");
        EVENT_MAP.get(Registries.FLUID.location()).forEach(Runnable::run);
        
        for (var type : EVENT_MAP.keySet()) {
            if (type.equals(Registries.FLUID.location()) || type.equals(Registries.CREATIVE_MODE_TAB.location())) continue;
            EVENT_MAP.get(type).forEach(Runnable::run);
        }
        
        LOGGER.debug("Registering item groups");
        EVENT_MAP.get(Registries.CREATIVE_MODE_TAB.location()).forEach(Runnable::run);
        LOGGER.info("Oritech registrations complete");
    }
    
    public static Multimap<ResourceLocation, Runnable> initEventMap() {
        
        Multimap<ResourceLocation, Runnable> res = ArrayListMultimap.create();
        res.put(Registries.FLUID.location(), FluidContent::registerFluids);
        res.put(Registries.BLOCK.location(), FluidContent::registerBlocks);
        res.put(Registries.ITEM.location(), FluidContent::registerItems);
        res.put(Registries.ITEM.location(), () -> ArchitecturyRegistryContainer.register(ItemContent.class, MOD_ID, false));
        res.put(Registries.BLOCK.location(), () -> ArchitecturyRegistryContainer.register(BlockContent.class, MOD_ID, false));
        res.put(Registries.ITEM.location(), ArchitecturyBlockRegistryContainer::finishItemRegister);
        res.put(Registries.BLOCK_ENTITY_TYPE.location(), () -> ArchitecturyRegistryContainer.register(BlockEntitiesContent.class, MOD_ID, false));
        res.put(Registries.SOUND_EVENT.location(), () -> ArchitecturyRegistryContainer.register(SoundContent.class, MOD_ID, false));
        res.put(Registries.ITEM.location(), () -> ArchitecturyRegistryContainer.register(ToolsContent.class, MOD_ID, false));
        res.put(Registries.FEATURE.location(), () -> ArchitecturyRegistryContainer.register(FeatureContent.class, MOD_ID, false));
        res.put(Registries.LOOT_FUNCTION_TYPE.location(), () -> ArchitecturyRegistryContainer.register(LootContent.class, MOD_ID, false));
        res.put(Registries.ENTITY_TYPE.location(), () -> ArchitecturyRegistryContainer.register(EntitiesContent.class, MOD_ID, false));
        res.put(Registries.ITEM.location(), ToolsContent::registerEventHandlers);
        res.put(Registries.MENU.location(), () -> ArchitecturyRegistryContainer.register(ModScreens.class, MOD_ID, false));
        res.put(Registries.RECIPE_TYPE.location(), () -> ArchitecturyRegistryContainer.register(RecipeContent.class, MOD_ID, false));
        res.put(Registries.CREATIVE_MODE_TAB.location(), () -> ArchitecturyRegistryContainer.register(ItemGroups.class, MOD_ID, false));
        res.put(Registries.RECIPE_SERIALIZER.location(), ArchitecturyRecipeRegistryContainer::finishSerializerRegister);
        res.put(Registries.LOOT_FUNCTION_TYPE.location(), FluidContent::registerItemsToGroups);
        res.put(ResourceLocation.fromNamespaceAndPath("neoforge", "attachment_types"), Augment::registerAttachmentTypes);   // this works just fine on fabric aswell, as they key is not really relevant there.
        
        return res;
    }
    
    private static void onServerStarted(MinecraftServer minecraftServer) {
        minecraftServer.getAllLevels().forEach(world -> {
            if (world.isClientSide) return;
            
            var regKey = world.dimension().location();
            
            var dataId = "energy_" + regKey.getNamespace() + "_" + regKey.getPath();
            var result = world.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, dataId);
            EnergyPipeBlock.ENERGY_PIPE_DATA.put(regKey, result);
            
            var fluidDataId = "fluid_" + regKey.getNamespace() + "_" + regKey.getPath();
            var fluidResult = world.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, fluidDataId);
            FluidPipeBlock.FLUID_PIPE_DATA.put(regKey, fluidResult);
            
            var itemDataId = "item_" + regKey.getNamespace() + "_" + regKey.getPath();
            var itemResult = world.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, itemDataId);
            ItemPipeBlock.ITEM_PIPE_DATA.put(regKey, itemResult);
            
            var superConductorDataId = "superconductor_" + regKey.getNamespace() + "_" + regKey.getPath();
            var superConductorResult = world.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.TYPE, superConductorDataId);
            SuperConductorBlock.SUPERCONDUCTOR_DATA.put(regKey, superConductorResult);
            
            var powerPoleId = "pole_" + regKey.getNamespace() + "_" + regKey.getPath();
            var powerPoleResult = world.getDataStorage().computeIfAbsent(PowerPoleEntity.PoleNetworkData.TYPE, powerPoleId);
            PowerPoleEntity.POLE_NETWORK_DATA.put(regKey, powerPoleResult);
        });
    }
}