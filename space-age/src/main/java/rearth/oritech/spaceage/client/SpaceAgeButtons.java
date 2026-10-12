package rearth.oritech.spaceage.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;

import java.util.function.Consumer;

/**
 * Match text contrast to Oritech's light, dark, and pressed surfaces.
 */
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

    static ButtonWidget close(int x, int y, Consumer<ButtonWidget> onPress) {

        var button = darkPanel(x, y, 18, 16, Component.literal("×"), onPress);
        button.withTooltip(Component.translatable("screen.oritech_space_age.close"));
        return button;
    }

    static ButtonWidget collapse(int x, int y, boolean collapsed, Consumer<ButtonWidget> onPress) {

        // Draw familiar window controls directly, independent of the font's available glyphs.
        var button = new ButtonWidget(x, y, 16, 16, Component.empty(), onPress) {

            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

                super.renderContent(graphics, mouseX, mouseY, delta);
                var iconX = getX() + 4;
                var iconY = getY() + 4;
                var color = LabelWidget.DARK_TEXT;
                if (collapsed) {
                    graphics.fill(iconX, iconY, iconX + 8, iconY + 2, color);
                    graphics.fill(iconX, iconY + 7, iconX + 8, iconY + 8, color);
                    graphics.fill(iconX, iconY, iconX + 1, iconY + 8, color);
                    graphics.fill(iconX + 7, iconY, iconX + 8, iconY + 8, color);
                } else graphics.fill(iconX, iconY + 7, iconX + 8, iconY + 9, color);
            }
        };
        button.withTooltip(Component.translatable(collapsed
                ? "screen.oritech_space_age.mission.expand" : "screen.oritech_space_age.mission.collapse"));
        return colors(button);
    }

    private static ButtonWidget colors(ButtonWidget button) {

        return button.withDisabledTextColor(0xFFBBBBBB).withPressedTextColor(LabelWidget.BRIGHT_TEXT);
    }
}
