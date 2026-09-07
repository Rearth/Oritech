package rearth.oritech.spaceage.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;

import java.util.Locale;

/** Draws the fixed map controls, flight summary, and path legend. */
final class StarMapOverlay {

    private StarMapOverlay() {
    }

    static void render(GuiGraphicsExtractor graphics, int x, int y, int width, Component selectedTarget,
                       RocketFlightPathCalculator.FlightPath flightPath,
                       RocketFlightPathCalculator.CraftPath selectedPath) {
        var font = Minecraft.getInstance().font;
        graphics.fill(x + 6, y + 6, x + width - 6, y + 21, 0xDD080D18);
        graphics.text(font, Component.translatable("screen.oritech_space_age.star_system"), x + 10, y + 9, 0xFFCAD8E5, true);
        if (selectedTarget != null) graphics.text(font, selectedTarget, x + 82, y + 9, 0xFFF6C65B, false);
        graphics.text(font, Component.translatable("screen.oritech_space_age.map_controls"), x + width - 160, y + 9, 0xFFA7BACB, false);

        OritechSurface.PANEL_DARK.render(graphics, x + 9, y + 27, 176, 58);
        graphics.text(font, Component.translatable("screen.oritech_space_age.flight_stats").withStyle(ChatFormatting.BOLD),
                x + 16, y + 33, 0xFFF2F6FA, false);
        graphics.text(font, Component.translatable("screen.oritech_space_age.last_command_days",
                        String.format(Locale.ROOT, "%.2f", flightPath.lastCommandSeconds() / 1_200d)),
                x + 16, y + 45, 0xFFCAD8E5, false);
        if (selectedPath != null) {
            graphics.text(font, Component.translatable("screen.oritech_space_age.remaining_delta_v",
                            String.format(Locale.ROOT, "%.0f", selectedPath.remainingDeltaV())),
                    x + 16, y + 57, 0xFFCAD8E5, false);
            graphics.text(font, Component.translatable("screen.oritech_space_age.plan_status",
                            Component.translatable("screen.oritech_space_age.terminal."
                                    + selectedPath.terminalState().name().toLowerCase(Locale.ROOT))),
                    x + 16, y + 69, selectedPath.terminalState().isFailure() ? 0xFFFF7777 : 0xFFCAD8E5, false);
        }

        int panelX = x + width - 181;
        int panelY = y + 27;
        OritechSurface.PANEL_DARK.render(graphics, panelX, panelY, 172, 76);
        graphics.text(font, Component.translatable("screen.oritech_space_age.path_legend").withStyle(ChatFormatting.BOLD),
                panelX + 7, panelY + 6, 0xFFF2F6FA, false);
        entry(graphics, panelX + 8, panelY + 20, 0xFFFF8A20, "accelerate");
        entry(graphics, panelX + 8, panelY + 31, 0xFF8FDB68, "redirect");
        entry(graphics, panelX + 8, panelY + 42, 0xFF66B9D5, "coast");
        entry(graphics, panelX + 8, panelY + 53, 0xFFB68CFF, "brake");
        entry(graphics, panelX + 8, panelY + 64, 0xFFFFD45C, "separation");
    }

    private static void entry(GuiGraphicsExtractor graphics, int x, int y, int color, String key) {
        graphics.fill(x, y + 3, x + 13, y + 5, color);
        graphics.text(Minecraft.getInstance().font, Component.translatable("screen.oritech_space_age.path." + key),
                x + 18, y, 0xFFCAD8E5, false);
    }
}
