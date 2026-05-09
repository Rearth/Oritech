package rearth.oritech;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ItemLike;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.tuples.Pair;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.blocks.processing.RefineryBlock;
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
import rearth.oritech.util.ServerZiplineHandler;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Oritech {
    
    public static final String MOD_ID = "oritech";
    public static final Logger LOGGER = LoggerFactory.getLogger("oritech");
    
    public static final List<RegistryStep> REGISTRY_STEPS = initRegistrySteps();
    public static Set<Pair<ItemLike, Float>> COMPOSTABLES_DATA = new HashSet<>();
    
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
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
        
        TickEvent.SERVER_PRE.register(elem -> RefineryBlock.updateTaintEvents());
        
        TickEvent.PLAYER_POST.register(ServerZiplineHandler::onPlayerTick);
        
        // for player augment ticks
        TickEvent.SERVER_PRE.register(event -> event.getAllLevels().forEach(world -> world.players().forEach(PlayerAugments::serverTickAugments)));
        LOGGER.info("Oritech initialization complete");
    }
    
    // fabric only
    public static void runAllRegistries() {
        
        LOGGER.info("Running Oritech registrations...");
        REGISTRY_STEPS.forEach(RegistryStep::run);
        LOGGER.info("Oritech registrations complete");
    }

    public static void runRegistry(Identifier registryId) {
        REGISTRY_STEPS.stream()
          .filter(step -> step.registryId().equals(registryId))
          .forEach(RegistryStep::run);
    }
    
    public static List<RegistryStep> initRegistrySteps() {
        return List.of(
          registry(Registries.DATA_COMPONENT_TYPE, ComponentContent::register),
          registry(Registries.FLUID, FluidContent::registerFluids),
          registry(Registries.BLOCK, FluidContent::registerBlocks),
          registry(Registries.ITEM, FluidContent::registerItems),
          registry(Registries.ITEM, ToolsContent::register),
          registry(Registries.ITEM, ItemContent::registerItems),
          registry(Registries.BLOCK, BlockContent::registerBlocks),
          registry(Registries.BLOCK_ENTITY_TYPE, BlockEntitiesContent::register),
          registry(Registries.SOUND_EVENT, SoundContent::register),
          registry(Registries.FEATURE, FeatureContent::register),
          registry(Registries.LOOT_FUNCTION_TYPE, LootContent::register),
          registry(Registries.ENTITY_TYPE, EntitiesContent::register),
          registry(Registries.ITEM, ToolsContent::registerEventHandlers),
          registry(Registries.MENU, ModScreens::register),
          registry(Registries.RECIPE_TYPE, RecipeContent::registerTypes),
          registry(Registries.RECIPE_SERIALIZER, RecipeContent::registerSerializers),
          registry(Identifier.fromNamespaceAndPath("neoforge", "attachment_types"), () -> {
              Augment.registerAttachmentTypes();
              ServerZiplineHandler.registerAttachments();
          }),
                    registry(Registries.CREATIVE_MODE_TAB, FluidContent::registerItemsToGroups),
          registry(Registries.CREATIVE_MODE_TAB, ItemGroups::register)
        );
    }

    private static RegistryStep registry(ResourceKey<?> registryKey, Runnable action) {
        return registry(registryKey.identifier(), action);
    }

    private static RegistryStep registry(Identifier registryId, Runnable action) {
        return new RegistryStep(registryId, action);
    }

    public record RegistryStep(Identifier registryId, Runnable action) {

        public void run() {
            LOGGER.debug("Registering {}", registryId);
            action.run();
        }
    }
    
    private static void onServerStarted(MinecraftServer minecraftServer) {
        minecraftServer.getAllLevels().forEach(world -> {
            if (world.isClientSide()) return;
            
            var regKey = world.dimension().identifier();
            
            var dataId = "energy_" + regKey + "_" + regKey.getPath();
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