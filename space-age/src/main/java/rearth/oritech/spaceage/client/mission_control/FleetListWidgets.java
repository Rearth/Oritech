package rearth.oritech.spaceage.client.mission_control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.UIComponent;

/**
 * Flat list furniture, kept separate from mission actions and category persistence.
 */
final class FleetListWidgets {

    private static final int TEXT = 0xFFE2E8ED;
    private static final int MUTED = 0xFF9EAAB5;

    private FleetListWidgets() {
    }

    static UIComponent section(int x, int y, int width, Component title, int count, boolean collapsed,
                               Runnable toggle, Runnable actions) {

        return new UIComponent(x, y, width, 24) {

            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

                var hovered = isMouseOver(mouseX, mouseY);
                graphics.fill(x, y, x + width, y + height, hovered ? 0xFF515B65 : 0xFF424B54);
                chevron(graphics, x + 6, y + 9, collapsed, MUTED);
                var font = Minecraft.getInstance().font;
                var countText = Integer.toString(count);
                var countWidth = font.width(countText) + 10;
                var countX = x + width - 23 - countWidth;
                var name = font.plainSubstrByWidth(title.getString(), Math.max(1, countX - x - 24));
                graphics.text(font, name, x + 19, y + 8, TEXT, false);
                graphics.fill(countX, y + 5, countX + countWidth, y + 19, 0xFF343C44);
                graphics.text(font, countText, countX + 5, y + 8, MUTED, false);
                if (actions != null && hovered)
                    graphics.text(font, "...", x + width - 17, y + 8,
                            mouseX >= x + width - 21 ? 0xFFFFA5A5 : MUTED, false);
            }

            @Override
            public boolean handleClick(double mouseX, double mouseY, int button) {

                if (button != 0) return false;
                if (actions != null && mouseX >= x + width - 21) actions.run();
                else toggle.run();
                return true;
            }
        }.withTooltip(title, Component.translatable(collapsed
                        ? "screen.oritech_space_age.mission.expand" : "screen.oritech_space_age.mission.collapse"),
                Component.translatable("screen.oritech_space_age.fleet.actions_hint"));
    }

    static UIComponent row(int x, int y, int width, int height, Runnable toggle, Runnable context) {

        return new UIComponent(x, y, width, height) {

            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

                graphics.fill(x, y, x + width, y + height, isMouseOver(mouseX, mouseY) ? 0xFF414950 : 0xFF363C42);
                graphics.fill(x, y + height - 1, x + width, y + height, 0xFF4A525A);
            }

            @Override
            public boolean handleClick(double mouseX, double mouseY, int button) {

                if (button == 0) toggle.run();
                else if (button == 1) context.run();
                else return false;
                return true;
            }
        };
    }

    static UIComponent guide(int x, int y, int height) {

        return new UIComponent(x, y, 1, height) {

            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

                graphics.fill(x, y, x + 1, y + height, 0xFF56616C);
            }
        };
    }

    private static void chevron(GuiGraphicsExtractor graphics, int x, int y, boolean collapsed, int color) {

        for (var i = 0; i < 3; i++) {
            if (collapsed) graphics.fill(x + i, y + i, x + i + 1, y + 5 - i, color);
            else graphics.fill(x + i, y + i, x + 5 - i, y + i + 1, color);
        }
    }
}
