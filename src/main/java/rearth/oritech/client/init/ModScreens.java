package rearth.oritech.client.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import rearth.oritech.client.ui.BasicMachineScreen;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.client.ui.UpgradableMachineScreen;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;

public class ModScreens implements AutoRegistryContainer<ScreenHandlerType<?>> {

    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> PULVERIZER_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<BasicMachineScreenHandler> GRINDER_SCREEN = new ExtendedScreenHandlerType<>(BasicMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> ASSEMBLER_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);

    public static void assignScreens() {
        HandledScreens.register(PULVERIZER_SCREEN, UpgradableMachineScreen::new);
        HandledScreens.register(GRINDER_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        HandledScreens.register(ASSEMBLER_SCREEN, UpgradableMachineScreen::new);
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
