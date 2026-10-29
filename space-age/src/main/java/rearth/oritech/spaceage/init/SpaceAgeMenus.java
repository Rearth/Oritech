package rearth.oritech.spaceage.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.block.beam_aperture.BeamApertureMenu;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampMenu;
import rearth.oritech.spaceage.block.mission_control.MissionControlMenu;
import rearth.oritech.spaceage.block.robot_arm.ActionCardMenu;
import rearth.oritech.spaceage.block.robot_arm.RobotMenu;
import rearth.oritech.spaceage.block.vacuum_crafter.VacuumCrafterMenu;
import rearth.oritech.spaceage.client.assembler.RocketAssemblerScreen;
import rearth.oritech.spaceage.client.beam_aperture.BeamApertureScreen;
import rearth.oritech.spaceage.client.launch_ramp.LaunchRampScreen;
import rearth.oritech.spaceage.client.mission_control.MissionControlScreen;
import rearth.oritech.spaceage.client.robot_arm.ActionCardScreen;
import rearth.oritech.spaceage.client.robot_arm.RobotScreen;
import rearth.oritech.spaceage.client.vacuum_crafter.VacuumCrafterScreen;

import java.util.function.Supplier;

public final class SpaceAgeMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, OritechSpaceAge.MOD_ID);
    public static final Supplier<MenuType<RobotMenu>> ROBOT =
            MENUS.register("robot_arm", () -> IMenuTypeExtension.create(RobotMenu::new));
    private SpaceAgeMenus() {
    }    public static final Supplier<MenuType<RocketAssemblerMenu>> ROCKET_ASSEMBLER =
            MENUS.register("rocket_assembler", () -> IMenuTypeExtension.create(RocketAssemblerMenu::new));

    public static void registerScreens(RegisterMenuScreensEvent event) {

        event.register(LAUNCH_RAMP.get(), LaunchRampScreen::new);

        event.register(ROBOT.get(), RobotScreen::new);
        event.register(ACTION_CARD.get(), ActionCardScreen::new);
        event.register(BEAM_APERTURE.get(), BeamApertureScreen::new);
        event.register(ROCKET_ASSEMBLER.get(), RocketAssemblerScreen::new);
        event.register(VACUUM_CRAFTER.get(), VacuumCrafterScreen::new);
        event.register(MISSION_CONTROL.get(), MissionControlScreen::new);
    }

    public static final Supplier<MenuType<MissionControlMenu>> MISSION_CONTROL =
            MENUS.register("mission_control", () -> IMenuTypeExtension.create(MissionControlMenu::new));



    public static final Supplier<MenuType<VacuumCrafterMenu>> VACUUM_CRAFTER =
            MENUS.register("vacuum_crafter", () -> IMenuTypeExtension.create(VacuumCrafterMenu::new));

    public static final Supplier<MenuType<BeamApertureMenu>> BEAM_APERTURE =
            MENUS.register("beam_aperture", () -> IMenuTypeExtension.create(BeamApertureMenu::new));


    public static final Supplier<MenuType<ActionCardMenu>> ACTION_CARD =
            MENUS.register("action_card", () -> IMenuTypeExtension.create(ActionCardMenu::new));

    public static final Supplier<MenuType<LaunchRampMenu>> LAUNCH_RAMP =
            MENUS.register("launch_ramp", () -> IMenuTypeExtension.create(LaunchRampMenu::new));


}
