package rearth.oritech.spaceage.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.client.RocketAssemblerScreen;

import java.util.function.Supplier;

public final class SpaceAgeMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, OritechSpaceAge.MOD_ID);

    public static final Supplier<MenuType<RocketAssemblerMenu>> ROCKET_ASSEMBLER =
            MENUS.register("rocket_assembler", () -> IMenuTypeExtension.create(RocketAssemblerMenu::new));

    private SpaceAgeMenus() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ROCKET_ASSEMBLER.get(), RocketAssemblerScreen::new);
    }
}
