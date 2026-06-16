package rearth.oritech.client.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;
import rearth.oritech.client.ui.*;

import java.util.function.Supplier;

public class ModScreens {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Oritech.MOD_ID);

    public static final Supplier<MenuType<OritechScreenHandler>> TREEFELLER_SCREEN = MENUS.register("treefeller_screen", () -> IMenuTypeExtension.create(OritechScreenHandler::new));

    public static final Supplier<MenuType<OritechScreenHandler>> ATOMIC_FORGE_SCREEN = MENUS.register("atomic_forge_screen", () -> IMenuTypeExtension.create(OritechScreenHandler::new));
    public static final Supplier<MenuType<OritechScreenHandler>> TANK_SCREEN = MENUS.register("tank_screen", () -> IMenuTypeExtension.create(OritechScreenHandler::new));
    public static final Supplier<MenuType<OritechScreenHandler>> CHARGER_SCREEN = MENUS.register("charger_screen", () -> IMenuTypeExtension.create(OritechScreenHandler::new));
    public static final Supplier<MenuType<OritechScreenHandler>> FUEL_PORT_SCREEN = MENUS.register("fuel_port_screen", () -> IMenuTypeExtension.create(OritechScreenHandler::new));
    public static final Supplier<MenuType<OritechScreenHandler>> AUGMENTER_INV_SCREEN = MENUS.register("augmenter_inv_screen", () -> IMenuTypeExtension.create(OritechScreenHandler::new));

    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> LASER_SCREEN = MENUS.register("laser_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> PULVERIZER_SCREEN = MENUS.register("pulverizer_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> GRINDER_SCREEN = MENUS.register("grinder_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> ASSEMBLER_SCREEN = MENUS.register("assembler_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> FOUNDRY_SCREEN = MENUS.register("foundry_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> COOLER_SCREEN = MENUS.register("cooler_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<LimitedEnergyStorageScreenHandler>> STORAGE_SCREEN = MENUS.register("storage_screen", () -> IMenuTypeExtension.create(LimitedEnergyStorageScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> POWER_POLE_SCREEN = MENUS.register("power_pole_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> UNSTABLE_CONTAINER_SCREEN = MENUS.register("unstable_container_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> POWERED_FURNACE_SCREEN = MENUS.register("powered_furnace_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> BIO_GENERATOR_SCREEN = MENUS.register("bio_generator_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> BASIC_GENERATOR_SCREEN = MENUS.register("basic_generator_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> LAVA_GENERATOR_SCREEN = MENUS.register("lava_generator_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> FUEL_GENERATOR_SCREEN = MENUS.register("fuel_generator_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<SteamEngineScreenHandler>> STEAM_ENGINE_SCREEN = MENUS.register("steam_engine_screen", () -> IMenuTypeExtension.create(SteamEngineScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> DESTROYER_SCREEN = MENUS.register("destroyer_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> PLACER_SCREEN = MENUS.register("placer_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));
    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> FERTILIZER_SCREEN = MENUS.register("fertilizer_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));

    public static final Supplier<MenuType<UpgradableOritechScreenHandler>> SHRINKER_SCREEN = MENUS.register("shrinker_screen", () -> IMenuTypeExtension.create(UpgradableOritechScreenHandler::new));

    public static final Supplier<MenuType<ReactorScreenHandler>> REACTOR_SCREEN = MENUS.register("reactor_screen", () -> IMenuTypeExtension.create(ReactorScreenHandler::new));
    public static final Supplier<MenuType<CatalystScreenHandler>> CATALYST_SCREEN = MENUS.register("catalyst_screen", () -> IMenuTypeExtension.create(CatalystScreenHandler::new));
    public static final Supplier<MenuType<EnchanterScreenHandler>> ENCHANTER_SCREEN = MENUS.register("enchanter_screen", () -> IMenuTypeExtension.create(EnchanterScreenHandler::new));
    public static final Supplier<MenuType<AcceleratorScreenHandler>> ACCELERATOR_SCREEN = MENUS.register("accelerator_screen", () -> IMenuTypeExtension.create(AcceleratorScreenHandler::new));
    public static final Supplier<MenuType<ItemFilterScreenHandler>> ITEM_FILTER_SCREEN = MENUS.register("item_filter_screen", () -> IMenuTypeExtension.create(ItemFilterScreenHandler::new));
    public static final Supplier<MenuType<PlayerModifierScreenHandler>> MODIFIER_SCREEN = MENUS.register("modifier_screen", () -> IMenuTypeExtension.create(PlayerModifierScreenHandler::new));
    public static final Supplier<MenuType<RedstoneAddonScreenHandler>> REDSTONE_ADDON_SCREEN = MENUS.register("redstone_addon_screen", () -> IMenuTypeExtension.create(RedstoneAddonScreenHandler::new));
    public static final Supplier<MenuType<RefineryScreenHandler>> REFINERY_SCREEN = MENUS.register("refinery_screen", () -> IMenuTypeExtension.create(RefineryScreenHandler::new));
    public static final Supplier<MenuType<TaintedRefineryScreenHandler>> TAINTED_REFINERY_SCREEN = MENUS.register("tainted_refinery_screen", () -> IMenuTypeExtension.create(TaintedRefineryScreenHandler::new));

    public static final Supplier<MenuType<DroneScreenHandler>> DRONE_SCREEN = MENUS.register("drone_screen", () -> IMenuTypeExtension.create(DroneScreenHandler::new));
    public static final Supplier<MenuType<CentrifugeScreenHandler>> CENTRIFUGE_SCREEN = MENUS.register("centrifuge_screen", () -> IMenuTypeExtension.create(CentrifugeScreenHandler::new));
    public static final Supplier<MenuType<InventoryProxyScreenHandler>> INVENTORY_PROXY_SCREEN = MENUS.register("inventory_proxy_screen", () -> IMenuTypeExtension.create(InventoryProxyScreenHandler::new));

    public static void registerScreens(RegisterMenuScreensEvent event) {

        // explicit types are needed here somehow because otherwise the gradle build fails. No idea why.
        event.<OritechScreenHandler, OritechMachineScreen<OritechScreenHandler>>register(TREEFELLER_SCREEN.get(), OritechMachineScreen::new);
        event.register(TANK_SCREEN.get(), TankScreen::new);
        event.<OritechScreenHandler, OritechMachineScreen<OritechScreenHandler>>register(ATOMIC_FORGE_SCREEN.get(), OritechMachineScreen::new);
        event.register(CATALYST_SCREEN.get(), CatalystScreen::new);
        event.register(ENCHANTER_SCREEN.get(), EnchanterScreen::new);
        event.register(ACCELERATOR_SCREEN.get(), AcceleratorScreen::new);
        event.<OritechScreenHandler, OritechMachineScreen<OritechScreenHandler>>register(CHARGER_SCREEN.get(), OritechMachineScreen::new);
        event.<OritechScreenHandler, OritechMachineScreen<OritechScreenHandler>>register(FUEL_PORT_SCREEN.get(), OritechMachineScreen::new);
        event.<OritechScreenHandler, OritechMachineScreen<OritechScreenHandler>>register(AUGMENTER_INV_SCREEN.get(), OritechMachineScreen::new);
        event.register(REFINERY_SCREEN.get(), RefineryScreen::new);
        event.register(TAINTED_REFINERY_SCREEN.get(), TaintedRefineryScreen::new);

        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(PULVERIZER_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(GRINDER_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(ASSEMBLER_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(FOUNDRY_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(COOLER_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(POWERED_FURNACE_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(BIO_GENERATOR_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(LAVA_GENERATOR_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(FUEL_GENERATOR_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(BASIC_GENERATOR_SCREEN.get(), UpgradableOritechScreen::new);
        event.register(STORAGE_SCREEN.get(), LimitedEnergyStorageScreen::new);
        event.register(POWER_POLE_SCREEN.get(), PowerPoleScreen::new);
        event.register(UNSTABLE_CONTAINER_SCREEN.get(), UnstableContainerScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(DESTROYER_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(PLACER_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(FERTILIZER_SCREEN.get(), UpgradableOritechScreen::new);
        event.<UpgradableOritechScreenHandler, UpgradableOritechScreen<UpgradableOritechScreenHandler>>register(LASER_SCREEN.get(), UpgradableOritechScreen::new);
        event.<CentrifugeScreenHandler, UpgradableOritechScreen<CentrifugeScreenHandler>>register(CENTRIFUGE_SCREEN.get(), UpgradableOritechScreen::new);
        event.register(SHRINKER_SCREEN.get(), ShrinkerScreen::new);

        event.register(INVENTORY_PROXY_SCREEN.get(), InventoryProxyScreen::new);
        event.register(REACTOR_SCREEN.get(), ReactorScreen::new);
        event.register(MODIFIER_SCREEN.get(), PlayerModifierScreen::new);
        event.register(ITEM_FILTER_SCREEN.get(), ItemFilterScreen::new);
        event.register(DRONE_SCREEN.get(), DroneScreen::new);
        event.register(REDSTONE_ADDON_SCREEN.get(), RedstoneAddonScreen::new);
        event.register(STEAM_ENGINE_SCREEN.get(), SteamEngineScreen::new);
    }
}
