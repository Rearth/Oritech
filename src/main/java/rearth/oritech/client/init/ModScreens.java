package rearth.oritech.client.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import rearth.oritech.client.ui.*;

public class ModScreens implements AutoRegistryContainer<ScreenHandlerType<?>> {

    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> PULVERIZER_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> GRINDER_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> ASSEMBLER_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> POWERED_FURNACE_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<BasicMachineScreenHandler> DESTROYER_SCREEN = new ExtendedScreenHandlerType<>(BasicMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<BasicMachineScreenHandler> PLACER_SCREEN = new ExtendedScreenHandlerType<>(BasicMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<InventoryProxyScreenHandler> INVENTORY_PROXY_SCREEN = new ExtendedScreenHandlerType<>(InventoryProxyScreenHandler::new);

    public static void assignScreens() {
        HandledScreens.register(PULVERIZER_SCREEN, UpgradableMachineScreen::new);
        HandledScreens.register(GRINDER_SCREEN, UpgradableMachineScreen::new);
        HandledScreens.register(ASSEMBLER_SCREEN, UpgradableMachineScreen::new);
        HandledScreens.register(POWERED_FURNACE_SCREEN, UpgradableMachineScreen::new);
        HandledScreens.register(INVENTORY_PROXY_SCREEN, InventoryProxyScreen::new);
        HandledScreens.register(DESTROYER_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        HandledScreens.register(PLACER_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
            }

    @Override
    public Registry<ScreenHandlerType<?>> getRegistry() {
        return Registries.SCREEN_HANDLER;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<ScreenHandlerType<?>> getTargetFieldType() {
        return (Class<ScreenHandlerType<?>>) (Object) ScreenHandlerType.class;
    }
}
