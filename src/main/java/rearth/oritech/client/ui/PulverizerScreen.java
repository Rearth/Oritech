package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;

public class PulverizerScreen extends BaseOwoHandledScreen<FlowLayout, PulverizerScreenHandler> {


    private static final Identifier BACKGROUND = new Identifier(Oritech.MOD_ID, "textures/gui/modular/gui_base.png");
    private static final Identifier ITEM_SLOT = new Identifier(Oritech.MOD_ID, "textures/gui/modular/itemslot.png");
    private TextureComponent progress_indicator;
    private BoxComponent energy_indicator;
    private Component energy_tooltip;

    public PulverizerScreen(PulverizerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    private static Component getItemFrame(int x, int y) {
        return Components.texture(ITEM_SLOT, 0, 0, 18, 17, 18, 17).positioning(Positioning.absolute(x - 2, y - 2));
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        rootComponent.child(
                Components.texture(BACKGROUND, 0, 0, 176, 166, 176, 166)
        ).child(buildOverlay().positioning(Positioning.relative(50, 50)));
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        updateEnergyBar();
        updateProgressBar();
    }

    private void updateProgressBar() {
        var config = handler.screenData.getIndicatorConfiguration();
        var progress = handler.screenData.getProgress();

        if (config.horizontal()) {
            progress_indicator.visibleArea(PositionedRectangle.of(0, 0, (int) (config.width() * progress), config.height()));
        } else {
            progress_indicator.visibleArea(PositionedRectangle.of(0, 0, config.width(), (int) (config.height() * progress)));
        }
    }

    private void updateEnergyBar() {

        var config = handler.screenData.getEnergyConfiguration();
        var capacity = handler.energyStorage.getCapacity();
        var amount = handler.energyStorage.getAmount();

        var fillAmount = (float) amount / capacity;
        var tooltipText = getEnergyTooltip(amount, capacity);
        energy_tooltip.tooltip(tooltipText);
        energy_indicator.verticalSizing(Sizing.fixed((int) ((config.height() - 2) * fillAmount)));
    }

    public Text getEnergyTooltip(long amount, long max) {
        var percentage = (float) amount / max;
        var energyFill = String.format("%.1f", percentage * 100);
        return Text.literal(amount + " / " + max + " RF\n" + energyFill + "% Charged");
    }

    private FlowLayout buildOverlay() {

        var overlay = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(166));

        for (var slot : handler.screenData.getActiveSlots()) {
            overlay.child(getItemFrame(slot.x(), slot.y()));
        }

        if (handler.screenData.showEnergy()) {
            addEnergyBar(overlay);
            updateEnergyBar();
        }

        addProgressArrow(overlay);
        updateProgressBar();

        return overlay;
    }

    private void addProgressArrow(FlowLayout panel) {

        var config = handler.screenData.getIndicatorConfiguration();

        var empty = Components.texture(config.empty(), 0, 0, config.width(), config.height(), config.width(), config.height());
        progress_indicator = Components.texture(config.full(), 0, 0, config.width(), config.height(), config.width(), config.height());

        panel
                .child(empty.positioning(Positioning.absolute(config.x(), config.y())))
                .child(progress_indicator.positioning(Positioning.absolute(config.x(), config.y())));
    }

    private void addEnergyBar(FlowLayout panel) {

        var config = handler.screenData.getEnergyConfiguration();

        var fillAmount = 0.1; // those will be overridden on tick
        var tooltipText = Text.literal("10/50 RF");

        var energy_empty = Components.box(Sizing.fixed(config.width()), Sizing.fixed(config.height())).color(Color.BLACK).fill(true);
        energy_indicator = Components.box(Sizing.fixed(config.width() - 2), Sizing.fixed((int) ((config.height() - 2) * fillAmount))).color(Color.RED).fill(true);
        energy_tooltip = Components.box(Sizing.fixed(config.width()), Sizing.fixed(config.height())).tooltip(tooltipText);

        panel
                .child(energy_empty.positioning(Positioning.absolute(config.x(), config.y())))
                .child(energy_indicator.positioning(Positioning.absolute(config.x() + 1, config.y() + 1)))
                .child(energy_tooltip.positioning(Positioning.absolute(config.x(), config.y())));
    }
}
