package rearth.oritech.client.init;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import rearth.oritech.client.ui.*;
import rearth.oritech.util.registry.OritechDeferredRegistry;

public class ModScreens {
    
    public static final OritechDeferredRegistry<MenuType<?>> MENUS = OritechDeferredRegistry.create(Registries.MENU);
    
    public static final RegistrySupplier<MenuType<OritechScreenHandler>> TREEFELLER_SCREEN = MENUS.register("treefeller_screen", () -> MenuRegistry.ofExtended((OritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<OritechScreenHandler>> ATOMIC_FORGE_SCREEN = MENUS.register("atomic_forge_screen", () -> MenuRegistry.ofExtended((OritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<OritechScreenHandler>> TANK_SCREEN = MENUS.register("tank_screen", () -> MenuRegistry.ofExtended((OritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<OritechScreenHandler>> CHARGER_SCREEN = MENUS.register("charger_screen", () -> MenuRegistry.ofExtended((OritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<OritechScreenHandler>> FUEL_PORT_SCREEN = MENUS.register("fuel_port_screen", () -> MenuRegistry.ofExtended((OritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<OritechScreenHandler>> AUGMENTER_INV_SCREEN = MENUS.register("augmenter_inv_screen", () -> MenuRegistry.ofExtended((OritechScreenHandler::new)));
    
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> LASER_SCREEN = MENUS.register("laser_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> PULVERIZER_SCREEN = MENUS.register("pulverizer_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> GRINDER_SCREEN = MENUS.register("grinder_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> ASSEMBLER_SCREEN = MENUS.register("assembler_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> FOUNDRY_SCREEN = MENUS.register("foundry_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> COOLER_SCREEN = MENUS.register("cooler_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<LimitedEnergyStorageScreenHandler>> STORAGE_SCREEN = MENUS.register("storage_screen", () -> MenuRegistry.ofExtended((LimitedEnergyStorageScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> POWER_POLE_SCREEN = MENUS.register("power_pole_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> UNSTABLE_CONTAINER_SCREEN = MENUS.register("unstable_container_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> POWERED_FURNACE_SCREEN = MENUS.register("powered_furnace_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> BIO_GENERATOR_SCREEN = MENUS.register("bio_generator_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> BASIC_GENERATOR_SCREEN = MENUS.register("basic_generator_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> LAVA_GENERATOR_SCREEN = MENUS.register("lava_generator_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> FUEL_GENERATOR_SCREEN = MENUS.register("fuel_generator_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<SteamEngineScreenHandler>> STEAM_ENGINE_SCREEN = MENUS.register("steam_engine_screen", () -> MenuRegistry.ofExtended((SteamEngineScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> DESTROYER_SCREEN = MENUS.register("destroyer_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> PLACER_SCREEN = MENUS.register("placer_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> FERTILIZER_SCREEN = MENUS.register("fertilizer_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    
    public static final RegistrySupplier<MenuType<UpgradableOritechScreenHandler>> SHRINKER_SCREEN = MENUS.register("shrinker_screen", () -> MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new)));
    
    public static final RegistrySupplier<MenuType<ReactorScreenHandler>> REACTOR_SCREEN = MENUS.register("reactor_screen", () -> MenuRegistry.ofExtended((ReactorScreenHandler::new)));
    public static final RegistrySupplier<MenuType<CatalystScreenHandler>> CATALYST_SCREEN = MENUS.register("catalyst_screen", () -> MenuRegistry.ofExtended((CatalystScreenHandler::new)));
    public static final RegistrySupplier<MenuType<EnchanterScreenHandler>> ENCHANTER_SCREEN = MENUS.register("enchanter_screen", () -> MenuRegistry.ofExtended((EnchanterScreenHandler::new)));
    public static final RegistrySupplier<MenuType<AcceleratorScreenHandler>> ACCELERATOR_SCREEN = MENUS.register("accelerator_screen", () -> MenuRegistry.ofExtended((AcceleratorScreenHandler::new)));
    public static final RegistrySupplier<MenuType<ItemFilterScreenHandler>> ITEM_FILTER_SCREEN = MENUS.register("item_filter_screen", () -> MenuRegistry.ofExtended((ItemFilterScreenHandler::new)));
    public static final RegistrySupplier<MenuType<PlayerModifierScreenHandler>> MODIFIER_SCREEN = MENUS.register("modifier_screen", () -> MenuRegistry.ofExtended((PlayerModifierScreenHandler::new)));
    public static final RegistrySupplier<MenuType<RedstoneAddonScreenHandler>> REDSTONE_ADDON_SCREEN = MENUS.register("redstone_addon_screen", () -> MenuRegistry.ofExtended((RedstoneAddonScreenHandler::new)));
    public static final RegistrySupplier<MenuType<RefineryScreenHandler>> REFINERY_SCREEN = MENUS.register("refinery_screen", () -> MenuRegistry.ofExtended((RefineryScreenHandler::new)));
    public static final RegistrySupplier<MenuType<TaintedRefineryScreenHandler>> TAINTED_REFINERY_SCREEN = MENUS.register("tainted_refinery_screen", () -> MenuRegistry.ofExtended((TaintedRefineryScreenHandler::new)));
    
    public static final RegistrySupplier<MenuType<DroneScreenHandler>> DRONE_SCREEN = MENUS.register("drone_screen", () -> MenuRegistry.ofExtended((DroneScreenHandler::new)));
    public static final RegistrySupplier<MenuType<CentrifugeScreenHandler>> CENTRIFUGE_SCREEN = MENUS.register("centrifuge_screen", () -> MenuRegistry.ofExtended((CentrifugeScreenHandler::new)));
    public static final RegistrySupplier<MenuType<InventoryProxyScreenHandler>> INVENTORY_PROXY_SCREEN = MENUS.register("inventory_proxy_screen", () -> MenuRegistry.ofExtended((InventoryProxyScreenHandler::new)));
    
    public static void registerScreens() {
        MenuRegistry.registerScreenFactory(TREEFELLER_SCREEN.get(), OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(TANK_SCREEN.get(), TankScreen::new);
        MenuRegistry.registerScreenFactory(ATOMIC_FORGE_SCREEN.get(), OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(CATALYST_SCREEN.get(), CatalystScreen::new);
        MenuRegistry.registerScreenFactory(ENCHANTER_SCREEN.get(), EnchanterScreen::new);
        MenuRegistry.registerScreenFactory(ACCELERATOR_SCREEN.get(), AcceleratorScreen::new);
        MenuRegistry.registerScreenFactory(CHARGER_SCREEN.get(), OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FUEL_PORT_SCREEN.get(), OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(AUGMENTER_INV_SCREEN.get(), OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(REFINERY_SCREEN.get(), RefineryScreen::new);
        MenuRegistry.registerScreenFactory(TAINTED_REFINERY_SCREEN.get(), TaintedRefineryScreen::new);
        
        MenuRegistry.registerScreenFactory(PULVERIZER_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(GRINDER_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(ASSEMBLER_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FOUNDRY_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(COOLER_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(POWERED_FURNACE_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(BIO_GENERATOR_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(LAVA_GENERATOR_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FUEL_GENERATOR_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(BASIC_GENERATOR_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(STORAGE_SCREEN.get(), LimitedEnergyStorageScreen::new);
        MenuRegistry.registerScreenFactory(POWER_POLE_SCREEN.get(), PowerPoleScreen::new);
        MenuRegistry.registerScreenFactory(UNSTABLE_CONTAINER_SCREEN.get(), UnstableContainerScreen::new);
        MenuRegistry.registerScreenFactory(DESTROYER_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(PLACER_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FERTILIZER_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(LASER_SCREEN.get(), UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(CENTRIFUGE_SCREEN.get(), UpgradableOritechScreen<CentrifugeScreenHandler>::new);
        MenuRegistry.registerScreenFactory(SHRINKER_SCREEN.get(), ShrinkerScreen::new);
        
        MenuRegistry.registerScreenFactory(INVENTORY_PROXY_SCREEN.get(), InventoryProxyScreen::new);
        MenuRegistry.registerScreenFactory(REACTOR_SCREEN.get(), ReactorScreen::new);
        MenuRegistry.registerScreenFactory(MODIFIER_SCREEN.get(), PlayerModifierScreen::new);
        MenuRegistry.registerScreenFactory(ITEM_FILTER_SCREEN.get(), ItemFilterScreen::new);
        MenuRegistry.registerScreenFactory(DRONE_SCREEN.get(), DroneScreen::new);
        MenuRegistry.registerScreenFactory(REDSTONE_ADDON_SCREEN.get(), RedstoneAddonScreen::new);
        MenuRegistry.registerScreenFactory(STEAM_ENGINE_SCREEN.get(), SteamEngineScreen::new);
    }

    public static void register() {
        MENUS.register();
    }
}
