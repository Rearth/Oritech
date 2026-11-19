package rearth.oritech.spaceage.client.mission_control;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.spaceage.simulation.survey.EarthScan;
import rearth.oritech.spaceage.simulation.survey.EarthScan.Report;

final class EarthSurveyMapWidget extends UIComponent {

    private final Report report;

    EarthSurveyMapWidget(int x, int y, int size, Report report) {

        super(x, y, size, size);
        this.report = report;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        var size = EarthScan.PREVIEW_SIZE;
        for (var row = 0; row < size; row++) {
            for (var column = 0; column < size; column++) {
                // Darken the corners so the preview shows the circular search boundary.
                var color = switch (report.previewAt(column, row)) {
                    case 'w' -> 0xFF4D7693;
                    case 'm' -> 0xFF9A9586;
                    default -> 0xFF6F8862;
                };
                var east = 2 * column + 1 - size;
                var south = 2 * row + 1 - size;
                if (east * east + south * south > size * size) color = 0xFF30373C;
                graphics.fill(x + column * width / size, y + row * height / size,
                        x + (column + 1) * width / size, y + (row + 1) * height / size, color);
            }
        }
        var cx = x + width / 2;
        var cy = y + height / 2;
        graphics.fill(cx - 3, cy, cx + 4, cy + 1, 0xFFE1E4DE);
        graphics.fill(cx, cy - 3, cx + 1, cy + 4, 0xFFE1E4DE);
        var hit = report.position().orElse(null);
        if (hit == null) return;
        var scan = report.scan();
        var px = Math.clamp(cx + (hit.getX() - scan.x()) * width / (2 * scan.radius()), x + 3, x + width - 4);
        var py = Math.clamp(cy + (hit.getZ() - scan.z()) * height / (2 * scan.radius()), y + 3, y + height - 4);
        graphics.fill(px - 3, py - 3, px + 4, py + 4, 0xFF24272A);
        graphics.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFFA353);
    }
}
