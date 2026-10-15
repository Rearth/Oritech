package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.api.screen.widgets.ToggleWidget;

import java.util.Locale;

/**
 * Draws the fixed map controls, compact flight summary, and collapsible path legend.
 */
final class StarMapOverlay {

    private static final int COLLAPSED_LEGEND_WIDTH = 112;
    private static final int COLLAPSED_LEGEND_HEIGHT = 18;
    private static final String[] LEGEND_KEYS = {"accelerate", "redirect", "coast", "brake", "separation", "command_coverage"};
    private static final int[] LEGEND_COLORS = {0xFFFF8A20, 0xFF8FDB68, 0xFF66B9D5, 0xFFB68CFF, 0xFFFFD45C, 0xFF5AD6EB};

    private StarMapOverlay() {
    }

    static void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                       Component selectedTarget, Component selectedBranch,
                       RocketFlightPathCalculator.CraftPath selectedPath, boolean legendExpanded, boolean showSummary,
                       boolean showCommandCoverage) {

        var font = Minecraft.getInstance().font;
        graphics.fill(x + 6, y + 6, x + width - 6, y + 21, 0xDD080D18);
        graphics.text(font, Component.translatable("screen.oritech_space_age.star_system"), x + 10, y + 9, 0xFFCAD8E5, true);
        if (selectedTarget != null) graphics.text(font, selectedTarget, x + 82, y + 9, 0xFFF6C65B, false);

        if (showSummary) {
            var statsRight = x + width - 9;
            var statsX = Math.max(x + 195, statsRight - 470);
            OritechSurface.PANEL_DARK.render(graphics, statsX, y - 30, statsRight - statsX, 20);
            var deltaV = selectedPath == null ? "–" : String.format(Locale.ROOT, "%.0f", selectedPath.remainingDeltaV());
            var status = selectedPath == null ? Component.literal("–") : Component.translatable(
                    "screen.oritech_space_age.terminal." + selectedPath.terminalState().name().toLowerCase(Locale.ROOT));
            var duration = selectedPath == null ? "–"
                    : String.format(Locale.ROOT, "%.2f", selectedPath.durationSeconds() / 1_200d);
            if (selectedPath != null && selectedPath.failure().filter(failure -> failure.reason().equals("time_limit")).isPresent())
                duration = "~" + String.format(Locale.ROOT, "%.2f", (selectedPath.durationSeconds() + selectedPath.failure().orElseThrow().actual()) / 1_200d);
            var summary = Component.translatable("screen.oritech_space_age.flight_stats_compact",
                    selectedBranch, duration, deltaV, status);
            graphics.text(font, summary, statsX + 8, y - 24,
                    selectedPath != null && selectedPath.terminalState().isFailure() ? 0xFFFF9999 : 0xFFCAD8E5, false);
        }

        var layout = legendLayout(width, legendExpanded);
        var legendWidth = layout.width();
        var legendHeight = layout.height();
        var legendX = x + width - legendWidth - 9;
        var legendY = y + height - legendHeight - 8;
        OritechSurface.PANEL_DARK.render(graphics, legendX, legendY, legendWidth, legendHeight);
        graphics.text(font, Component.translatable("screen.oritech_space_age.path_legend"),
                legendX + 7, legendY + 5, 0xFFF2F6FA, true);
        graphics.text(font, Component.literal(legendExpanded ? "−" : "+"),
                legendX + legendWidth - 14, legendY + 5, 0xFFF2F6FA, true);
        if (!legendExpanded) return;
        for (int index = 0; index < LEGEND_KEYS.length; index++) {
            if (index == 5 && !showCommandCoverage) continue;
            var entryX = legendX + 8 + index % layout.columns() * layout.cellWidth();
            var entryY = legendY + 18 + index / layout.columns() * 12;
            if (index == 5) dashedEntry(graphics, entryX, entryY, LEGEND_COLORS[index],
                    "screen.oritech_space_age.path." + LEGEND_KEYS[index]);
            else entry(graphics, entryX, entryY, LEGEND_COLORS[index], LEGEND_KEYS[index]);
        }
    }

    static boolean isOverLegend(double mouseX, double mouseY, int x, int y, int width, int height,
                                boolean expanded) {

        var layout = legendLayout(width, expanded);
        var legendWidth = layout.width();
        var legendHeight = layout.height();
        var legendX = x + width - legendWidth - 9;
        var legendY = y + height - legendHeight - 8;
        return mouseX >= legendX && mouseX < legendX + legendWidth
                && mouseY >= legendY && mouseY < legendY + legendHeight;
    }

    static void positionOptions(ToggleWidget beams, ToggleWidget sun, int x, int y, int width, int height) {

        var layout = legendLayout(width, true);
        var left = x + width - layout.width() - 1;
        var bottom = y + height - 8;
        beams.setPosition(left, bottom - 40);
        sun.setPosition(left, bottom - 20);
        beams.setWidth(layout.width() - 16);
        sun.setWidth(layout.width() - 16);
    }

    private static LegendLayout legendLayout(int mapWidth, boolean expanded) {

        if (!expanded) return new LegendLayout(COLLAPSED_LEGEND_WIDTH, COLLAPSED_LEGEND_HEIGHT, 1, 0);
        var cellWidth = 0;
        var font = Minecraft.getInstance().font;
        for (var key : LEGEND_KEYS)
            cellWidth = Math.max(cellWidth, 26 + font.width(Component.translatable("screen.oritech_space_age.path." + key)));
        var columns = mapWidth - 18 >= 16 + 2 * cellWidth ? 2 : 1;
        return new LegendLayout(16 + columns * cellWidth, 64 + (LEGEND_KEYS.length / columns) * 12, columns, cellWidth);
    }

    private record LegendLayout(int width, int height, int columns, int cellWidth) {
    }

    private static void entry(GuiGraphicsExtractor graphics, int x, int y, int color, String key) {

        graphics.fill(x, y + 3, x + 13, y + 5, color);
        graphics.text(Minecraft.getInstance().font, Component.translatable("screen.oritech_space_age.path." + key),
                x + 18, y, 0xFFCAD8E5, false);
    }

    private static void dashedEntry(GuiGraphicsExtractor graphics, int x, int y, int color, String key) {

        for (int part = 0; part < 3; part++) graphics.fill(x + part * 5, y + 3, x + part * 5 + 3, y + 5, color);
        graphics.text(Minecraft.getInstance().font, Component.translatable(key), x + 18, y, 0xFFCAD8E5, false);
    }
}
