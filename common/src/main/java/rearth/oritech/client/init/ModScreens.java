package rearth.oritech.client.init;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import rearth.oritech.client.ui.*;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

public class ModScreens implements ArchitecturyRegistryContainer<MenuType<?>> {
    
    public static final MenuType<BasicMachineScreenHandler> TREEFELLER_SCREEN = MenuRegistry.ofExtended((BasicMachineScreenHandler::new));
    public static final MenuType<BasicMachineScreenHandler> ATOMIC_FORGE_SCREEN = MenuRegistry.ofExtended((BasicMachineScreenHandler::new));
    public static final MenuType<BasicMachineScreenHandler> TANK_SCREEN = MenuRegistry.ofExtended((BasicMachineScreenHandler::new));
    public static final MenuType<BasicMachineScreenHandler> CHARGER_SCREEN = MenuRegistry.ofExtended((BasicMachineScreenHandler::new));
    public static final MenuType<BasicMachineScreenHandler> FUEL_PORT_SCREEN = MenuRegistry.ofExtended((BasicMachineScreenHandler::new));
    public static final MenuType<BasicMachineScreenHandler> AUGMENTER_INV_SCREEN = MenuRegistry.ofExtended((BasicMachineScreenHandler::new));
    
    public static final MenuType<UpgradableMachineScreenHandler> LASER_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> PULVERIZER_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> GRINDER_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> ASSEMBLER_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> FOUNDRY_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> COOLER_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> STORAGE_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> POWERED_FURNACE_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> BIO_GENERATOR_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> BASIC_GENERATOR_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> LAVA_GENERATOR_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> FUEL_GENERATOR_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> STEAM_ENGINE_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> DESTROYER_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> PLACER_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    public static final MenuType<UpgradableMachineScreenHandler> FERTILIZER_SCREEN = MenuRegistry.ofExtended((UpgradableMachineScreenHandler::new));
    
    public static final MenuType<ReactorScreenHandler> REACTOR_SCREEN = MenuRegistry.ofExtended((ReactorScreenHandler::new));
    public static final MenuType<CatalystScreenHandler> CATALYST_SCREEN = MenuRegistry.ofExtended((CatalystScreenHandler::new));
    public static final MenuType<EnchanterScreenHandler> ENCHANTER_SCREEN = MenuRegistry.ofExtended((EnchanterScreenHandler::new));
    public static final MenuType<AcceleratorScreenHandler> ACCELERATOR_SCREEN = MenuRegistry.ofExtended((AcceleratorScreenHandler::new));
    public static final MenuType<ItemFilterScreenHandler> ITEM_FILTER_SCREEN = MenuRegistry.ofExtended((ItemFilterScreenHandler::new));
    public static final MenuType<PlayerModifierScreenHandler> MODIFIER_SCREEN = MenuRegistry.ofExtended((PlayerModifierScreenHandler::new));
    public static final MenuType<RedstoneAddonScreenHandler> REDSTONE_ADDON_SCREEN = MenuRegistry.ofExtended((RedstoneAddonScreenHandler::new));
    public static final MenuType<RefineryScreenHandler> REFINERY_SCREEN = MenuRegistry.ofExtended((RefineryScreenHandler::new));
    
    public static final MenuType<DroneScreenHandler> DRONE_SCREEN = MenuRegistry.ofExtended((DroneScreenHandler::new));
    public static final MenuType<CentrifugeScreenHandler> CENTRIFUGE_SCREEN = MenuRegistry.ofExtended((CentrifugeScreenHandler::new));
    public static final MenuType<InventoryProxyScreenHandler> INVENTORY_PROXY_SCREEN = MenuRegistry.ofExtended((InventoryProxyScreenHandler::new));
    
    public static void registerScreens() {
        MenuRegistry.registerScreenFactory(TREEFELLER_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(TANK_SCREEN, TankScreen::new);
        MenuRegistry.registerScreenFactory(ATOMIC_FORGE_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(CATALYST_SCREEN, CatalystScreen::new);
        MenuRegistry.registerScreenFactory(ENCHANTER_SCREEN, EnchanterScreen::new);
        MenuRegistry.registerScreenFactory(ACCELERATOR_SCREEN, AcceleratorScreen::new);
        MenuRegistry.registerScreenFactory(CHARGER_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FUEL_PORT_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(AUGMENTER_INV_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(REFINERY_SCREEN, RefineryScreen::new);
        
        MenuRegistry.registerScreenFactory(PULVERIZER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(GRINDER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(ASSEMBLER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FOUNDRY_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(COOLER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(POWERED_FURNACE_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(BIO_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(LAVA_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FUEL_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(BASIC_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(STORAGE_SCREEN, EnergyStorageScreen::new);
        MenuRegistry.registerScreenFactory(DESTROYER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(PLACER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FERTILIZER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        MenuRegistry.registerScreenFactory(LASER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        
        MenuRegistry.registerScreenFactory(INVENTORY_PROXY_SCREEN, InventoryProxyScreen::new);
        MenuRegistry.registerScreenFactory(REACTOR_SCREEN, ReactorScreen::new);
        MenuRegistry.registerScreenFactory(MODIFIER_SCREEN, PlayerModifierScreen::new);
        MenuRegistry.registerScreenFactory(ITEM_FILTER_SCREEN, ItemFilterScreen::new);
        MenuRegistry.registerScreenFactory(DRONE_SCREEN, DroneScreen::new);
        MenuRegistry.registerScreenFactory(REDSTONE_ADDON_SCREEN, RedstoneAddonScreen::new);
        MenuRegistry.registerScreenFactory(CENTRIFUGE_SCREEN, CentrifugeScreen::new);
        MenuRegistry.registerScreenFactory(STEAM_ENGINE_SCREEN, SteamEngineScreen::new);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Class<MenuType<?>> getTargetFieldType() {
        return (Class<MenuType<?>>) (Object) MenuType.class;
    }
    
    @Override
    public ResourceKey<Registry<MenuType<?>>> getRegistryType() {
        return Registries.MENU;
    }
}
