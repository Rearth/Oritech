package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
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

public class BasicMachineScreen<S extends BasicMachineScreenHandler> extends BaseOwoHandledScreen<FlowLayout, S> {
    
    
    public static final Identifier BACKGROUND = new Identifier(Oritech.MOD_ID, "textures/gui/modular/gui_base.png");
    public static final Identifier ITEM_SLOT = new Identifier(Oritech.MOD_ID, "textures/gui/modular/itemslot.png");
    public static final Identifier GUI_COMPONENTS = new Identifier(Oritech.MOD_ID, "textures/gui/modular/machine_gui_components.png");
    public FlowLayout root;
    private TextureComponent progress_indicator;
    private TextureComponent energy_indicator;
    private Component energy_tooltip;
    private ButtonComponent cycleInputButton;
    
    public BasicMachineScreen(S handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    public static Component getItemFrame(int x, int y) {
        return Components.texture(ITEM_SLOT, 0, 0, 18, 17, 18, 17).positioning(Positioning.absolute(x - 2, y - 2));
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        this.root = rootComponent;
        
        rootComponent
          .surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        rootComponent.child(
          Containers.horizontalFlow(Sizing.fixed(176 + 250), Sizing.fixed(166 + 40))        // span entire inner area
            .child(Containers.horizontalFlow(Sizing.content(), Sizing.content())
                     .child(buildExtensionPanel())
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
        
        var capacity = handler.energyStorage.getCapacity();
        var amount = handler.energyStorage.getAmount();
        
        var fillAmount = (float) amount / capacity;
        var tooltipText = getEnergyTooltip(amount, capacity);
        
        energy_tooltip.tooltip(tooltipText);
        energy_indicator.visibleArea(PositionedRectangle.of(0, 96 - ((int) (96 * (fillAmount))), 24, (int) (96 * fillAmount)));
    }
    
    public Text getEnergyTooltip(long amount, long max) {
        var percentage = (float) amount / max;
        var energyFill = String.format("%.1f", percentage * 100);
        var energyUsage = handler.screenData.getDisplayedEnergyUsage();
        var energyUsageText = String.format("%.1f", energyUsage);
        return Text.literal(amount + " / " + max + " RF\n" + energyFill + "% Charged\n\nMaximum Usage: " + energyUsageText + " RF/t");
    }
    
    public void updateSettingsButtons() {
        
        var activeMode = handler.screenData.getInventoryInputMode();
        var modeName = activeMode.name().toLowerCase();
        
        cycleInputButton.setMessage(Text.translatable("button.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName)));
        cycleInputButton.tooltip(Text.translatable("tooltip.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName)));
    }
    
    private Component buildExtensionPanel() {
        
        var container = Containers.verticalFlow(Sizing.content(), Sizing.content());
        container.surface(Surface.PANEL_INSET);
        container.horizontalAlignment(HorizontalAlignment.CENTER);
        
        container.padding(Insets.of(1, 4, 1, 1));
        container.margins(Insets.of(7));
        
        addExtensionComponents(container);
        updateSettingsButtons();
        
        return container;
    }
    
    public void addExtensionComponents(FlowLayout container) {
        
        cycleInputButton = Components.button(Text.literal("Match Recipe"),
          button -> {
              NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.InventoryInputModeSelectorPacket(handler.blockPos));
          });
        cycleInputButton.horizontalSizing(Sizing.fixed(73));
        cycleInputButton.margins(Insets.of(3));
        
        container.child(Components.label(Text.literal("Options")).margins(Insets.of(3, 1, 1, 1)));
        
        if (handler.screenData.inputOptionsEnabled())
            container.child(cycleInputButton);
    }
    
    private FlowLayout buildOverlay() {
        
        var overlay = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(166));
        fillOverlay(overlay);
        
        return overlay;
    }
    
    public void fillOverlay(FlowLayout overlay) {
        
        for (var slot : handler.screenData.getGuiSlots()) {
            overlay.child(getItemFrame(slot.x(), slot.y()));
        }
        
        if (handler.screenData.showEnergy()) {
            addEnergyBar(overlay);
            updateEnergyBar();
        }
        
        addProgressArrow(overlay);
        updateProgressBar();
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
        var insetSize = 1;
        
        var frame = Containers.horizontalFlow(Sizing.fixed(config.width() + insetSize * 2), Sizing.fixed(config.height() + insetSize * 2));
        frame.surface(Surface.PANEL_INSET);
        frame.padding(Insets.of(insetSize));
        frame.positioning(Positioning.absolute(config.x() - insetSize, config.y() - insetSize));
        panel.child(frame);
        
        var fillAmount = 1f; // those will be overridden on tick
        var tooltipText = Text.literal("10/50 RF");
        
        energy_tooltip = Components.texture(GUI_COMPONENTS, 24, 0, 24, 96, 48, 96);
        energy_tooltip.sizing(Sizing.fixed(config.width()), Sizing.fixed(config.height()));
        energy_tooltip.positioning(Positioning.absolute(config.x(), config.y()));
        energy_tooltip.tooltip(tooltipText);
        
        var offset = (1 - fillAmount) * config.height();
        
        energy_indicator = Components.texture(GUI_COMPONENTS, 0, 0, 24, (int) (96 * fillAmount), 48, 96);
        energy_indicator.sizing(Sizing.fixed(config.width()), Sizing.fixed((int) (config.height() * fillAmount)));
        energy_indicator.positioning(Positioning.absolute(config.x(), (int) (config.y() + offset)));
        
        panel
          .child(energy_tooltip)
          .child(energy_indicator)
        ;
    }
}
