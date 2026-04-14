package rearth.oritech.client.init;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import rearth.oritech.client.ui.*;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

public class ModScreens implements ArchitecturyRegistryContainer<MenuType<?>> {
    
    public static final MenuType<OritechScreenHandler> TREEFELLER_SCREEN = MenuRegistry.ofExtended((OritechScreenHandler::new));
    public static final MenuType<OritechScreenHandler> ATOMIC_FORGE_SCREEN = MenuRegistry.ofExtended((OritechScreenHandler::new));
    public static final MenuType<OritechScreenHandler> TANK_SCREEN = MenuRegistry.ofExtended((OritechScreenHandler::new));
    public static final MenuType<OritechScreenHandler> CHARGER_SCREEN = MenuRegistry.ofExtended((OritechScreenHandler::new));
    public static final MenuType<OritechScreenHandler> FUEL_PORT_SCREEN = MenuRegistry.ofExtended((OritechScreenHandler::new));
    public static final MenuType<OritechScreenHandler> AUGMENTER_INV_SCREEN = MenuRegistry.ofExtended((OritechScreenHandler::new));
    
    public static final MenuType<UpgradableOritechScreenHandler> LASER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> PULVERIZER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> GRINDER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> ASSEMBLER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> FOUNDRY_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> COOLER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<LimitedEnergyStorageScreenHandler> STORAGE_SCREEN = MenuRegistry.ofExtended((LimitedEnergyStorageScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> POWER_POLE_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> UNSTABLE_CONTAINER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> POWERED_FURNACE_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> BIO_GENERATOR_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> BASIC_GENERATOR_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> LAVA_GENERATOR_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> FUEL_GENERATOR_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<SteamEngineScreenHandler> STEAM_ENGINE_SCREEN = MenuRegistry.ofExtended((SteamEngineScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> DESTROYER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> PLACER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    public static final MenuType<UpgradableOritechScreenHandler> FERTILIZER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    
    public static final MenuType<UpgradableOritechScreenHandler> SHRINKER_SCREEN = MenuRegistry.ofExtended((UpgradableOritechScreenHandler::new));
    
    public static final MenuType<ReactorScreenHandler> REACTOR_SCREEN = MenuRegistry.ofExtended((ReactorScreenHandler::new));
    public static final MenuType<CatalystScreenHandler> CATALYST_SCREEN = MenuRegistry.ofExtended((CatalystScreenHandler::new));
    public static final MenuType<EnchanterScreenHandler> ENCHANTER_SCREEN = MenuRegistry.ofExtended((EnchanterScreenHandler::new));
    public static final MenuType<AcceleratorScreenHandler> ACCELERATOR_SCREEN = MenuRegistry.ofExtended((AcceleratorScreenHandler::new));
    public static final MenuType<ItemFilterScreenHandler> ITEM_FILTER_SCREEN = MenuRegistry.ofExtended((ItemFilterScreenHandler::new));
    public static final MenuType<PlayerModifierScreenHandler> MODIFIER_SCREEN = MenuRegistry.ofExtended((PlayerModifierScreenHandler::new));
    public static final MenuType<RedstoneAddonScreenHandler> REDSTONE_ADDON_SCREEN = MenuRegistry.ofExtended((RedstoneAddonScreenHandler::new));
    public static final MenuType<RefineryScreenHandler> REFINERY_SCREEN = MenuRegistry.ofExtended((RefineryScreenHandler::new));
    public static final MenuType<TaintedRefineryScreenHandler> TAINTED_REFINERY_SCREEN = MenuRegistry.ofExtended((TaintedRefineryScreenHandler::new));
    
    public static final MenuType<DroneScreenHandler> DRONE_SCREEN = MenuRegistry.ofExtended((DroneScreenHandler::new));
    public static final MenuType<CentrifugeScreenHandler> CENTRIFUGE_SCREEN = MenuRegistry.ofExtended((CentrifugeScreenHandler::new));
    public static final MenuType<InventoryProxyScreenHandler> INVENTORY_PROXY_SCREEN = MenuRegistry.ofExtended((InventoryProxyScreenHandler::new));
    
    public static void registerScreens() {
        MenuRegistry.registerScreenFactory(TREEFELLER_SCREEN, OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(TANK_SCREEN, TankScreen::new);
        MenuRegistry.registerScreenFactory(ATOMIC_FORGE_SCREEN, OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(CATALYST_SCREEN, CatalystScreen::new);
        MenuRegistry.registerScreenFactory(ENCHANTER_SCREEN, EnchanterScreen::new);
        MenuRegistry.registerScreenFactory(ACCELERATOR_SCREEN, AcceleratorScreen::new);
        MenuRegistry.registerScreenFactory(CHARGER_SCREEN, OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FUEL_PORT_SCREEN, OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(AUGMENTER_INV_SCREEN, OritechMachineScreen<OritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(REFINERY_SCREEN, RefineryScreen::new);
        MenuRegistry.registerScreenFactory(TAINTED_REFINERY_SCREEN, TaintedRefineryScreen::new);
        
        MenuRegistry.registerScreenFactory(PULVERIZER_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(GRINDER_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(ASSEMBLER_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FOUNDRY_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(COOLER_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(POWERED_FURNACE_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(BIO_GENERATOR_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(LAVA_GENERATOR_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FUEL_GENERATOR_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(BASIC_GENERATOR_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(STORAGE_SCREEN, LimitedEnergyStorageScreen::new);
        MenuRegistry.registerScreenFactory(POWER_POLE_SCREEN, PowerPoleScreen::new);
        MenuRegistry.registerScreenFactory(UNSTABLE_CONTAINER_SCREEN, UnstableContainerScreen::new);
        MenuRegistry.registerScreenFactory(DESTROYER_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(PLACER_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(FERTILIZER_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(LASER_SCREEN, UpgradableOritechScreen<UpgradableOritechScreenHandler>::new);
        MenuRegistry.registerScreenFactory(CENTRIFUGE_SCREEN, UpgradableOritechScreen<CentrifugeScreenHandler>::new);
        MenuRegistry.registerScreenFactory(SHRINKER_SCREEN, ShrinkerScreen::new);
        
        MenuRegistry.registerScreenFactory(INVENTORY_PROXY_SCREEN, InventoryProxyScreen::new);
        MenuRegistry.registerScreenFactory(REACTOR_SCREEN, ReactorScreen::new);
        MenuRegistry.registerScreenFactory(MODIFIER_SCREEN, PlayerModifierScreen::new);
        MenuRegistry.registerScreenFactory(ITEM_FILTER_SCREEN, ItemFilterScreen::new);
        MenuRegistry.registerScreenFactory(DRONE_SCREEN, DroneScreen::new);
        MenuRegistry.registerScreenFactory(REDSTONE_ADDON_SCREEN, RedstoneAddonScreen::new);
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
