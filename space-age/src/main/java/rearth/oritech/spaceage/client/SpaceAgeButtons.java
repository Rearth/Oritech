package rearth.oritech.spaceage.client;

import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Match text contrast to Oritech's light, dark, and pressed surfaces.
 */
public final class SpaceAgeButtons {

    private SpaceAgeButtons() {
    }

    public static List<ButtonWidget> missionTabs(int selected, Runnable fleet, Runnable craft, Runnable plan) {

        return tabs(selected, 76, 80, List.of("screen.oritech_space_age.mission.fleet", "screen.oritech_space_age.craft.title",
                "screen.oritech_space_age.flight_plan"), List.of(fleet, craft, plan));
    }

    public static List<ButtonWidget> assemblerTabs(int selected, Runnable rocket, Runnable plan) {

        return tabs(selected, 92, 92, List.of("screen.oritech_space_age.rocket", "screen.oritech_space_age.flight_plan"),
                List.of(rocket, plan));
    }

    /**
     * Launch ramps add their own tab in front of the reused assembler tabs.
     */
    public static List<ButtonWidget> rampTabs(int selected, Runnable ramp, Runnable rocket, Runnable plan) {

        return tabs(selected, 70, 72, List.of("screen.oritech_space_age.launch_ramp.tab", "screen.oritech_space_age.rocket",
                "screen.oritech_space_age.flight_plan"), List.of(ramp, rocket, plan));
    }

    /**
     * The selected tab is drawn pressed and disabled; the others switch screens.
     */
    private static List<ButtonWidget> tabs(int selected, int width, int spacing, List<String> keys, List<Runnable> actions) {

        var tabs = new ArrayList<ButtonWidget>();
        for (var index = 0; index < keys.size(); index++) {
            var action = actions.get(index);
            var tab = panel(9 + index * spacing, 9, width, 20, Component.translatable(keys.get(index)), ignored -> action.run());
            if (index == selected) {
                tab.withDisabledSurface(OritechSurface.PANEL_PRESSED).withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
                tab.setActive(false);
            }
            tabs.add(tab);
        }
        return tabs;
    }

    public static ButtonWidget panel(int x, int y, int width, int height, Component label, Consumer<ButtonWidget> onPress) {

        return colors(ButtonWidget.panel(x, y, width, height, label, onPress));
    }

    public static ButtonWidget darkPanel(int x, int y, int width, int height, Component label, Consumer<ButtonWidget> onPress) {

        return colors(ButtonWidget.darkPanel(x, y, width, height, label, onPress))
                .withTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
    }

    public static ButtonWidget orangePanel(int x, int y, int width, int height, Component label, Consumer<ButtonWidget> onPress) {

        return colors(ButtonWidget.orangePanel(x, y, width, height, label, onPress));
    }

    public static ButtonWidget close(int x, int y, Consumer<ButtonWidget> onPress) {

        var button = darkPanel(x, y, 18, 16, Component.literal("×"), onPress);
        button.withTooltip(Component.translatable("screen.oritech_space_age.close"));
        return button;
    }

    private static ButtonWidget colors(ButtonWidget button) {

        return button.withDisabledTextColor(0xFFBBBBBB).withPressedTextColor(LabelWidget.BRIGHT_TEXT);
    }
}
