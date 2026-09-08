package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;

import java.util.Locale;

/** Draws the fixed map controls, compact flight summary, and collapsible path legend. */
final class StarMapOverlay {

    private static final int COLLAPSED_LEGEND_WIDTH = 112;
    private static final int COLLAPSED_LEGEND_HEIGHT = 18;
    private static final int EXPANDED_LEGEND_WIDTH = 330;
    private static final int EXPANDED_LEGEND_HEIGHT = 44;

    private StarMapOverlay() {
    }

    static void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                       Component selectedTarget, RocketFlightPathCalculator.FlightPath flightPath,
                       RocketFlightPathCalculator.CraftPath selectedPath, boolean legendExpanded) {
        var font = Minecraft.getInstance().font;
        graphics.fill(x + 6, y + 6, x + width - 6, y + 21, 0xDD080D18);
        graphics.text(font, Component.translatable("screen.oritech_space_age.star_system"), x + 10, y + 9, 0xFFCAD8E5, true);
        if (selectedTarget != null) graphics.text(font, selectedTarget, x + 82, y + 9, 0xFFF6C65B, false);
        graphics.text(font, Component.translatable("screen.oritech_space_age.map_controls"), x + width - 160, y + 9, 0xFFA7BACB, false);

        int statsRight = x + width - 9;
        int statsX = Math.max(x + 195, statsRight - 470);
        OritechSurface.PANEL_DARK.render(graphics, statsX, y - 30, statsRight - statsX, 20);
        var deltaV = selectedPath == null ? "–" : String.format(Locale.ROOT, "%.0f", selectedPath.remainingDeltaV());
        var status = selectedPath == null ? Component.literal("–") : Component.translatable(
                "screen.oritech_space_age.terminal." + selectedPath.terminalState().name().toLowerCase(Locale.ROOT));
        var summary = Component.translatable("screen.oritech_space_age.flight_stats_compact",
                String.format(Locale.ROOT, "%.2f", flightPath.lastCommandSeconds() / 1_200d), deltaV, status);
        graphics.text(font, summary, statsX + 8, y - 24,
                selectedPath != null && selectedPath.terminalState().isFailure() ? 0xFFFF9999 : 0xFFCAD8E5, false);

        int legendWidth = legendExpanded ? EXPANDED_LEGEND_WIDTH : COLLAPSED_LEGEND_WIDTH;
        int legendHeight = legendExpanded ? EXPANDED_LEGEND_HEIGHT : COLLAPSED_LEGEND_HEIGHT;
        int legendX = x + width - legendWidth - 9;
        int legendY = y + height - legendHeight - 8;
        OritechSurface.PANEL_DARK.render(graphics, legendX, legendY, legendWidth, legendHeight);
        graphics.text(font, Component.translatable("screen.oritech_space_age.path_legend"),
                legendX + 7, legendY + 5, 0xFFF2F6FA, true);
        graphics.text(font, Component.literal(legendExpanded ? "−" : "+"),
                legendX + legendWidth - 14, legendY + 5, 0xFFF2F6FA, true);
        if (!legendExpanded) return;
        entry(graphics, legendX + 8, legendY + 18, 0xFFFF8A20, "accelerate");
        entry(graphics, legendX + 8, legendY + 29, 0xFF8FDB68, "redirect");
        entry(graphics, legendX + 118, legendY + 18, 0xFF66B9D5, "coast");
        entry(graphics, legendX + 118, legendY + 29, 0xFFB68CFF, "brake");
        entry(graphics, legendX + 218, legendY + 18, 0xFFFFD45C, "separation");
    }

    static boolean isOverLegend(double mouseX, double mouseY, int x, int y, int width, int height,
                                boolean expanded) {
        int legendWidth = expanded ? EXPANDED_LEGEND_WIDTH : COLLAPSED_LEGEND_WIDTH;
        int legendHeight = expanded ? EXPANDED_LEGEND_HEIGHT : COLLAPSED_LEGEND_HEIGHT;
        int legendX = x + width - legendWidth - 9;
        int legendY = y + height - legendHeight - 8;
        return mouseX >= legendX && mouseX < legendX + legendWidth
                && mouseY >= legendY && mouseY < legendY + legendHeight;
    }

    private static void entry(GuiGraphicsExtractor graphics, int x, int y, int color, String key) {
        graphics.fill(x, y + 3, x + 13, y + 5, color);
        graphics.text(Minecraft.getInstance().font, Component.translatable("screen.oritech_space_age.path." + key),
                x + 18, y, 0xFFCAD8E5, false);
    }
}
