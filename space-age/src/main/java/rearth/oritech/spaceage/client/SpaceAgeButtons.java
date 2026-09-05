package rearth.oritech.spaceage.client;

import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;

import java.util.function.Consumer;

/** Match text contrast to Oritech's light, dark, and pressed surfaces. */
final class SpaceAgeButtons {
    private SpaceAgeButtons() {
    }

    static ButtonWidget panel(int x, int y, int width, int height, Component label, Consumer<ButtonWidget> onPress) {
        return colors(ButtonWidget.panel(x, y, width, height, label, onPress));
    }

    static ButtonWidget darkPanel(int x, int y, int width, int height, Component label, Consumer<ButtonWidget> onPress) {
        return colors(ButtonWidget.darkPanel(x, y, width, height, label, onPress))
                .withTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
    }

    static ButtonWidget orangePanel(int x, int y, int width, int height, Component label, Consumer<ButtonWidget> onPress) {
        return colors(ButtonWidget.orangePanel(x, y, width, height, label, onPress));
    }

    private static ButtonWidget colors(ButtonWidget button) {
        return button.withDisabledTextColor(0xFFBBBBBB).withPressedTextColor(LabelWidget.BRIGHT_TEXT);
    }
}
