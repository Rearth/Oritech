package rearth.oritech.client.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import rearth.oritech.client.ui.PulverizerScreen;
import rearth.oritech.client.ui.PulverizerScreenHandler;
import rearth.oritech.init.ItemGroups;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class ModScreenHandlers implements AutoRegistryContainer<ScreenHandlerType<?>> {

    public static final ExtendedScreenHandlerType<PulverizerScreenHandler> PULVERIZER_SCREEN = new ExtendedScreenHandlerType<>(PulverizerScreenHandler::new);


    public static void assignScreens() {
        HandledScreens.register(PULVERIZER_SCREEN, PulverizerScreen::new);
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
