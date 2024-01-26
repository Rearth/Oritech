package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
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
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.InventoryInputMode;

public class PulverizerScreen extends BaseOwoHandledScreen<FlowLayout, PulverizerScreenHandler> {
    
    
    private static final Identifier BACKGROUND = new Identifier(Oritech.MOD_ID, "textures/gui/modular/gui_base.png");
    private static final Identifier ITEM_SLOT = new Identifier(Oritech.MOD_ID, "textures/gui/modular/itemslot.png");
    private TextureComponent progress_indicator;
    private BoxComponent energy_indicator;
    private Component energy_tooltip;
    private ButtonComponent cycleInputButton;
    
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
          Containers.horizontalFlow(Sizing.fixed(176 + 250), Sizing.fixed(166 + 40))
            .child(Containers.horizontalFlow(Sizing.content(), Sizing.content())
                     .child(buildButtons())
                     .surface(Surface.PANEL)
                     .positioning(Positioning.absolute(176 + 117, 30)))
            .positioning(Positioning.relative(50, 50))
            .zIndex(-1)
        ).child(
          buildOverlay().positioning(Positioning.relative(50, 50))
        ).child(
          Components.texture(BACKGROUND, 0, 0, 176, 166, 176, 166)
        );
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        updateEnergyBar();
        updateProgressBar();
        updateSettingsButtons();
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
    
    public void updateSettingsButtons() {
        
        var activeMode = handler.screenData.getInventoryInputMode();
        var modeName = activeMode.name().toLowerCase();
        
        cycleInputButton.setMessage(Text.translatable("button.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName)));
        cycleInputButton.tooltip(Text.translatable("tooltip.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName)));
    }
    
    private Component buildButtons() {
        
        var container = Containers.verticalFlow(Sizing.content(), Sizing.content());
        container.surface(Surface.PANEL_INSET);
        container.horizontalAlignment(HorizontalAlignment.CENTER);
        container.child(Components.label(Text.literal("Options")));
        
        container.padding(Insets.of(3));
        container.margins(Insets.of(7));
        
        cycleInputButton = Components.button(Text.literal("Match Recipe"),
          button -> {
            NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.InventoryInputModeSelectorPacket(handler.blockPos));
        });
        cycleInputButton.horizontalSizing(Sizing.fixed(75));
        
        container.child(cycleInputButton);
        
        updateSettingsButtons();
        
        return container;
    }
    
    private FlowLayout buildOverlay() {
        
        var overlay = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(166));
        
        for (var slot : handler.screenData.getGuiSlots()) {
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
