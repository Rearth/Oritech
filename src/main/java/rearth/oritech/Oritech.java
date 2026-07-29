package rearth.oritech;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.behavior.EndericLaserBlockBehavior;
import rearth.oritech.block.behavior.EndericLaserEntityBehavior;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.blocks.processing.RefineryBlock;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.interaction.EnergyTransmissionPoleEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.*;
import rearth.oritech.init.datamap.DataMapContent;
import rearth.oritech.init.datapack.AugmentContent;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.init.world.FeatureContent;
import rearth.oritech.item.tools.ElectricMaceItem;
import rearth.oritech.item.tools.armor.ExoArmorItem;
import rearth.oritech.item.tools.harvesting.PromethiumAxeItem;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import rearth.oritech.item.tools.util.ArmorEventHandler;
import rearth.oritech.util.LaserMachinePlayer;
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
        neoEventBus.addListener(this::onPlayerMinedEvent);
        neoEventBus.addListener(this::onPlayerDamaged);
        neoEventBus.addListener(this::onEquipmentChanged);
        neoEventBus.addListener(LaserMachinePlayer::collectDrops);
        neoEventBus.addListener(LaserMachinePlayer::suppressExperienceDrops);

        // registration events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(AugmentContent::registerDataPackRegistries);
        modEventBus.addListener(DataMapContent::registerDataMapTypes);
        modEventBus.addListener(BlockEntitiesContent::registerBlockEntityCapabilities);
        modEventBus.addListener(ToolsContent::registerItemCapabilities);
        modEventBus.addListener(ToolsContent::modifyDefaultComponents);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, OritechConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.STARTUP, OritechStartupConfig.STARTUP_SPEC);

        // codecs for reflective builders
        NetworkManager.loadDefaultCodecs();

        // registrations
        ItemContent.ITEMS.register(modEventBus);
        BlockContent.BLOCKS.register(modEventBus);
        BlockContent.BLOCK_ITEMS.register(modEventBus);
        BlockEntitiesContent.BLOCK_ENTITY_TYPES.register(modEventBus);
        ToolsContent.EQUIPMENT.register(modEventBus);

        ItemGroups.TABS.register(modEventBus);
        SoundContent.SOUND_EVENTS.register(modEventBus);
        LootContent.LOOT_FUNCTIONS.register(modEventBus);
        EntitiesContent.ENTITY_TYPES.register(modEventBus);
        ComponentContent.COMPONENTS.register(modEventBus);
        FeatureContent.FEATURES.register(modEventBus);
        ModScreens.MENUS.register(modEventBus);
        AttachmentContent.ATTACHMENT_TYPES.register(modEventBus);
        RecipeContent.RECIPE_SERIALIZERS.register(modEventBus);
        RecipeContent.RECIPE_TYPES.register(modEventBus);

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

        event.enqueueWork(() -> {

            EndericLaserBlockBehavior.registerDefaults();
            EndericLaserEntityBehavior.registerDefaults();
        });


    }

    private void onServerTickPost(ServerTickEvent.Post event) {
        AcceleratorParticleLogic.onTickEnd();
        RefineryBlock.updateTaintEvents();
    }

    private void onLevelTickPos(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        ElectricMaceItem.processLightningEvents(serverLevel);
        PromethiumAxeItem.onTick(serverLevel);
    }

    private void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerZiplineHandler.onPlayerTick(serverPlayer);
            PlayerAugments.serverTickAugments(serverPlayer);
        }
    }

    private void onPlayerDamaged(LivingIncomingDamageEvent event) {
        if (ExoArmorItem.CancelFallDamage(event.getSource(), event.getContainer(), event.getEntity()))
            event.setCanceled(true);
    }

    private void onPlayerMinedEvent(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer && event.getLevel() instanceof ServerLevel serverLevel) {
            PromethiumPickaxeItem.preMine(serverLevel, event.getPos(), event.getState(), serverPlayer);
        }
    }

    private void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        ArmorEventHandler.processEvent(event.getEntity(), event.getSlot(), event.getFrom(), event.getTo());
    }

    private void onServerStarted(ServerStartedEvent event) {
        // load pipe data to memory
        event.getServer().getAllLevels().forEach(this::loadLevelPipeData);
    }


    private void addNetworkHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        NetworkManager.initClientBound(registrar);
        NetworkManager.initServerBound(registrar);
    }

    private void loadLevelPipeData(ServerLevel level) {
        var dimId = level.dimension().identifier();
        var result = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.ENERGY_TYPE);
        EnergyPipeBlock.ENERGY_PIPE_DATA.put(dimId, result);

        var fluidResult = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.FLUID_TYPE);
        FluidPipeBlock.FLUID_PIPE_DATA.put(dimId, fluidResult);

        var itemResult = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.ITEM_TYPE);
        ItemPipeBlock.ITEM_PIPE_DATA.put(dimId, itemResult);

        var superConductorResult = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.SUPERCONDUCTOR_TYPE);
        SuperConductorBlock.SUPERCONDUCTOR_DATA.put(dimId, superConductorResult);

        var energyTransmissionPoleResult = level.getDataStorage().computeIfAbsent(EnergyTransmissionPoleEntity.PoleNetworkData.TYPE);
        EnergyTransmissionPoleEntity.POLE_NETWORK_DATA.put(dimId, energyTransmissionPoleResult);
    }
}
