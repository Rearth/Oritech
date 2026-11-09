package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.OverlayWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.api.screen.widgets.ToggleWidget;
import rearth.oritech.api.transfer.energy.EnergyHistory;

import java.util.function.Supplier;

/**
 * Modal graph shared by storage and pole screens; the supplier follows the client's current network history.
 */
final class EnergyHistoryPopup extends OverlayWidget {

    EnergyHistoryPopup(int screenWidth, int screenHeight, int originX, int originY,
                       Supplier<EnergyHistory> history, boolean network, Runnable close) {
        super(screenWidth, screenHeight);
        // Screen widgets use container-relative coordinates. Offset the overlay to cover the full window.
        setPosition(originX, originY);
        withDismissHandler(close);
        // Leave room around the popup at larger GUI scales instead of clipping a fixed-size panel.
        int panelWidth = Math.min(380, screenWidth - 16);
        int panelHeight = Math.min(256, screenHeight - 16);
        int px = originX + (screenWidth - panelWidth) / 2;
        int py = originY + (screenHeight - panelHeight) / 2;

        addChild(new SurfaceWidget(px, py, panelWidth, panelHeight).withSurface(OritechSurface.PANEL));

        var graph = new EnergyHistoryWidget(px + 8, py + 29, panelWidth - 16, panelHeight - 62, history);
        // Use the same textured toggle as the storage screen, with unshadowed dark text on the bright panel.
        var logarithmic = ToggleWidget.of(0, py + 5,
                        Component.translatable("title.oritech.energy.history.logarithmic"), false,
                        (toggle, selected) -> graph.setLogarithmicScale(selected))
                .withTextColor(LabelWidget.DARK_TEXT)
                .withTextShadow(false);
        logarithmic.setPosition(px + panelWidth - 8 - logarithmic.getWidth(), py + 5);

        addChild(new LabelWidget(px + 8, py + 8, panelWidth - logarithmic.getWidth() - 24, 10,
                Component.translatable(network ? "title.oritech.energy.history.network" : "title.oritech.energy.history.title"))
                .withDarkColor());
        addChild(logarithmic.withTooltip(Component.translatable("title.oritech.energy.history.logarithmic.tooltip")));
        addChild(graph);

        int buttonY = py + panelHeight - 25;

        addChild(ButtonWidget.darkPanel(px + 8, buttonY, 40, 16,
                        Component.translatable("title.oritech.energy.history.minute"), button -> graph.setWindowSeconds(60))
                .withTextColor(LabelWidget.BRIGHT_TEXT));
        addChild(ButtonWidget.darkPanel(px + 52, buttonY, 40, 16,
                        Component.translatable("title.oritech.energy.history.five_minutes"), button -> graph.setWindowSeconds(300))
                .withTextColor(LabelWidget.BRIGHT_TEXT));
        addChild(ButtonWidget.darkPanel(px + 96, buttonY, 44, 16,
                        Component.translatable("title.oritech.energy.history.thirty_minutes"), button -> graph.setWindowSeconds(1800))
                .withTextColor(LabelWidget.BRIGHT_TEXT));
        addChild(ButtonWidget.darkPanel(px + panelWidth - 74, buttonY, 66, 16,
                Component.translatable("title.oritech.energy.history.pause"), button -> {
                    graph.togglePaused();
                    button.setLabel(Component.translatable(graph.isPaused()
                            ? "title.oritech.energy.history.resume" : "title.oritech.energy.history.pause"));
                }).withTextColor(LabelWidget.BRIGHT_TEXT));
    }
}
