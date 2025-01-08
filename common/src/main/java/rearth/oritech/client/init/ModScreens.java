package rearth.oritech.client.init;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.Oritech;
import rearth.oritech.client.ui.*;
import rearth.oritech.util.ArchitecturyRegistryContainer;
import rearth.oritech.util.MachineAddonController;

import java.lang.reflect.InvocationTargetException;

import static rearth.oritech.util.MachineAddonController.ADDON_UI_ENDEC;

public class ModScreens implements ArchitecturyRegistryContainer<ScreenHandlerType<?>> {
    
    
    public static final ExtendedScreenHandlerType<BasicMachineScreenHandler, BasicData> ATOMIC_FORGE_SCREEN = basicHandler();
    public static final ExtendedScreenHandlerType<BasicMachineScreenHandler, BasicData> TANK_SCREEN = basicHandler();
    public static final ExtendedScreenHandlerType<BasicMachineScreenHandler, BasicData> TREEFELLER_SCREEN = basicHandler();
    public static final ExtendedScreenHandlerType<BasicMachineScreenHandler, BasicData> CHARGER_SCREEN = basicHandler();
    public static final ExtendedScreenHandlerType<BasicMachineScreenHandler, BasicData> FUEL_PORT_SCREEN = basicHandler();
    
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> PULVERIZER_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> GRINDER_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> ASSEMBLER_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> FOUNDRY_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> COOLER_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> STORAGE_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> POWERED_FURNACE_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> BIO_GENERATOR_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> BASIC_GENERATOR_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> LAVA_GENERATOR_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> FUEL_GENERATOR_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> DESTROYER_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> PLACER_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> FERTILIZER_SCREEN = upgradeHandler();
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> LASER_SCREEN = upgradeHandler();
    
    public static final ExtendedScreenHandlerType<CatalystScreenHandler, BasicData> CATALYST_SCREEN = new ExtendedScreenHandlerType<>(new BasicFactory<>(CatalystScreenHandler.class), BasicData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<EnchanterScreenHandler, BasicData> ENCHANTER_SCREEN = new ExtendedScreenHandlerType<>(new BasicFactory<>(EnchanterScreenHandler.class), BasicData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<AcceleratorScreenHandler, BasicData> ACCELERATOR_SCREEN = new ExtendedScreenHandlerType<>(new BasicFactory<>(AcceleratorScreenHandler.class), BasicData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<DroneScreenHandler, UpgradableData> DRONE_SCREEN = new ExtendedScreenHandlerType<>(new UpgradeFactory<>(DroneScreenHandler.class), UpgradableData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<RedstoneAddonScreenHandler, BasicData> REDSTONE_ADDON_SCREEN = new ExtendedScreenHandlerType<>(new BasicFactory<>(RedstoneAddonScreenHandler.class), BasicData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<CentrifugeScreenHandler, UpgradableData> CENTRIFUGE_SCREEN = new ExtendedScreenHandlerType<>(new UpgradeFactory<>(CentrifugeScreenHandler.class), UpgradableData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<SteamEngineScreenHandler, UpgradableData> STEAM_ENGINE_SCREEN = new ExtendedScreenHandlerType<>(new UpgradeFactory<>(SteamEngineScreenHandler.class), UpgradableData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<ItemFilterScreenHandler, BasicData> ITEM_FILTER_SCREEN = new ExtendedScreenHandlerType<>(new BasicFactory<>(ItemFilterScreenHandler.class), BasicData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<InventoryProxyScreenHandler, InventoryProxyScreenHandler.InvProxyData> INVENTORY_PROXY_SCREEN = new ExtendedScreenHandlerType<>(new InventoryProxyScreenHandler.HandlerFactory(), InventoryProxyScreenHandler.InvProxyData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<ReactorScreenHandler, BasicData> REACTOR_SCREEN = new ExtendedScreenHandlerType<>(new ReactorScreenHandler.HandlerFactory(), BasicData.PACKET_CODEC);
    public static final ExtendedScreenHandlerType<PlayerModifierScreenHandler, BasicData> MODIFIER_SCREEN = new ExtendedScreenHandlerType<>(new PlayerModifierScreenHandler.HandlerFactory(), BasicData.PACKET_CODEC);
    
    public static void assignScreens() {
        HandledScreens.register(TANK_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        HandledScreens.register(TREEFELLER_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        HandledScreens.register(ATOMIC_FORGE_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        HandledScreens.register(CATALYST_SCREEN, CatalystScreen::new);
        HandledScreens.register(ENCHANTER_SCREEN, EnchanterScreen::new);
        HandledScreens.register(ACCELERATOR_SCREEN, AcceleratorScreen::new);
        HandledScreens.register(CHARGER_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        HandledScreens.register(FUEL_PORT_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        
        HandledScreens.register(PULVERIZER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(GRINDER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(ASSEMBLER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(FOUNDRY_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(COOLER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(POWERED_FURNACE_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(BIO_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(LAVA_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(FUEL_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(BASIC_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(STORAGE_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(DESTROYER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(PLACER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(FERTILIZER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(LASER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        
        HandledScreens.register(INVENTORY_PROXY_SCREEN, InventoryProxyScreen::new);
        HandledScreens.register(REACTOR_SCREEN, ReactorScreen::new);
        HandledScreens.register(MODIFIER_SCREEN, PlayerModifierScreen::new);
        HandledScreens.register(ITEM_FILTER_SCREEN, ItemFilterScreen::new);
        HandledScreens.register(DRONE_SCREEN, DroneScreen::new);
        HandledScreens.register(REDSTONE_ADDON_SCREEN, RedstoneAddonScreen::new);
        HandledScreens.register(CENTRIFUGE_SCREEN, CentrifugeScreen::new);
        HandledScreens.register(STEAM_ENGINE_SCREEN, SteamEngineScreen::new);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Class<ScreenHandlerType<?>> getTargetFieldType() {
        return (Class<ScreenHandlerType<?>>) (Object) ScreenHandlerType.class;
    }
    
    private static ExtendedScreenHandlerType<BasicMachineScreenHandler, BasicData> basicHandler() {
        return new ExtendedScreenHandlerType<>(new BasicFactory<>(), BasicData.PACKET_CODEC);
    }
    
    private static ExtendedScreenHandlerType<UpgradableMachineScreenHandler, UpgradableData> upgradeHandler() {
        return new ExtendedScreenHandlerType<>(new UpgradeFactory<>(), UpgradableData.PACKET_CODEC);
    }
    
    @Override
    public RegistryKey<Registry<ScreenHandlerType<?>>> getRegistryType() {
        return RegistryKeys.SCREEN_HANDLER;
    }
    
    public record BasicData(BlockPos pos) {
        public static final Endec<BasicData> PACKET_ENDEC = StructEndecBuilder.of(MinecraftEndecs.BLOCK_POS.fieldOf("pos", BasicData::pos), BasicData::new);
        public static final PacketCodec<RegistryByteBuf, BasicData> PACKET_CODEC = CodecUtils.toPacketCodec(PACKET_ENDEC);
    }
    
    public record UpgradableData(BlockPos pos, MachineAddonController.AddonUiData addonUiData, float coreQuality) {
        public static final Endec<UpgradableData> PACKET_ENDEC = StructEndecBuilder.of(
          MinecraftEndecs.BLOCK_POS.fieldOf("pos", UpgradableData::pos),
          ADDON_UI_ENDEC.fieldOf("addonUiData", UpgradableData::addonUiData),
          Endec.FLOAT.fieldOf("coreQuality", UpgradableData::coreQuality),
          UpgradableData::new);
        public static final PacketCodec<RegistryByteBuf, UpgradableData> PACKET_CODEC = CodecUtils.toPacketCodec(PACKET_ENDEC);
    }
    
    private static class BasicFactory<T extends ScreenHandler> implements ExtendedScreenHandlerType.ExtendedFactory<T, BasicData> {
        
        private final Class<T> target;
        
        private BasicFactory(Class<T> target) {
            this.target = target;
        }
        
        private BasicFactory() {
            this.target = (Class<T>) BasicMachineScreenHandler.class;
        }
        
        @Override
        public T create(int syncId, PlayerInventory inventory, BasicData data) {
            try {
                return target.getDeclaredConstructor(int.class, PlayerInventory.class, BlockEntity.class).newInstance(syncId, inventory, inventory.player.getWorld().getBlockEntity(data.pos()));
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                Oritech.LOGGER.error("Yeah something went very very wrong registering basic mod screen for Oritech");
                e.printStackTrace();
                return (T) new BasicMachineScreenHandler(1, inventory, new BasicData(BlockPos.ORIGIN));
            }
        }
    }
    
    private static class UpgradeFactory<T extends ScreenHandler> implements ExtendedScreenHandlerType.ExtendedFactory<T, UpgradableData> {
        
        private final Class<T> target;
        
        private UpgradeFactory(Class<T> target) {
            this.target = target;
        }
        
        private UpgradeFactory() {
            this.target = (Class<T>) UpgradableMachineScreenHandler.class;
        }
        
        @Override
        public T create(int syncId, PlayerInventory inventory, UpgradableData data) {
            
            try {
                return target.getDeclaredConstructor(int.class, PlayerInventory.class, BlockEntity.class, MachineAddonController.AddonUiData.class, float.class).newInstance(syncId, inventory, inventory.player.getWorld().getBlockEntity(data.pos()), data.addonUiData, data.coreQuality);
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                Oritech.LOGGER.error("Yeah something went very very wrong registering upgradable mod screen for Oritech");
                e.printStackTrace();
                return (T) new BasicMachineScreenHandler(1, inventory, new BasicData(BlockPos.ORIGIN));
            }
        }
    }
}
