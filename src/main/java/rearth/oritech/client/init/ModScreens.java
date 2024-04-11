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
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> FOUNDRY_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<CentrifugeScreenHandler> CENTRIFUGE_SCREEN = new ExtendedScreenHandlerType<>(CentrifugeScreenHandler::new);
    public static final ExtendedScreenHandlerType<BasicMachineScreenHandler> ATOMIC_FORGE_SCREEN = new ExtendedScreenHandlerType<>(BasicMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> STORAGE_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> POWERED_FURNACE_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> BIO_GENERATOR_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> BASIC_GENERATOR_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> LAVA_GENERATOR_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> DESTROYER_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> PLACER_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<UpgradableMachineScreenHandler> FERTILIZER_SCREEN = new ExtendedScreenHandlerType<>(UpgradableMachineScreenHandler::new);
    public static final ExtendedScreenHandlerType<InventoryProxyScreenHandler> INVENTORY_PROXY_SCREEN = new ExtendedScreenHandlerType<>(InventoryProxyScreenHandler::new);
    public static final ExtendedScreenHandlerType<ItemFilterScreenHandler> ITEM_FILTER_SCREEN = new ExtendedScreenHandlerType<>(ItemFilterScreenHandler::new);
    
    public static void assignScreens() {
        HandledScreens.register(PULVERIZER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(GRINDER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(ASSEMBLER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(FOUNDRY_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(CENTRIFUGE_SCREEN, CentrifugeScreen::new);
        HandledScreens.register(POWERED_FURNACE_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(BIO_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(LAVA_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(BASIC_GENERATOR_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(ATOMIC_FORGE_SCREEN, BasicMachineScreen<BasicMachineScreenHandler>::new);
        HandledScreens.register(STORAGE_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(INVENTORY_PROXY_SCREEN, InventoryProxyScreen::new);
        HandledScreens.register(ITEM_FILTER_SCREEN, ItemFilterScreen::new);
        HandledScreens.register(DESTROYER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(PLACER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
        HandledScreens.register(FERTILIZER_SCREEN, UpgradableMachineScreen<UpgradableMachineScreenHandler>::new);
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
