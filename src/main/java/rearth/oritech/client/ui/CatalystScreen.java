package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.PositionedRectangle;
import io.wispforest.owo.ui.core.Positioning;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;

public class CatalystScreen extends BasicMachineScreen<CatalystScreenHandler> {
    
    public static final Identifier GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components_souls.png");
    public static final Identifier BOOK_SLOT = Oritech.id("textures/gui/modular/book_slot_background.png");
    
    private LabelComponent costLabel;
    private LabelComponent stabilizationLabel;
    
    public CatalystScreen(CatalystScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    public Identifier getGuiComponents() {
        return GUI_COMPONENTS;
    }
    
    @Override
    public boolean showExtensionPanel() {
        return false;
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        costLabel = Components.label(Text.translatable("tooltip.oritech.soul_count_tooltip"));
        stabilizationLabel = Components.label(Text.translatable("tooltip.oritech.catalyst_stable"));
        
        overlay.child(costLabel.positioning(Positioning.absolute(56, 58)));
        overlay.child(stabilizationLabel.positioning(Positioning.absolute(108, 39)));
        
        var slotConfig = handler.screenData.getGuiSlots().getFirst();
        overlay.child(Components.texture(BOOK_SLOT, 0, 0, 16, 16, 16, 16).positioning(Positioning.absolute(slotConfig.x(), slotConfig.y())));
        
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        
        var cost = handler.catalyst.getDisplayedCost();
        costLabel.text(Text.translatable("tooltip.oritech.cost_tooltip").append(Text.literal(String.valueOf(cost))).formatted(Formatting.BLACK));
        
        if (cost == 0) {
            costLabel.zIndex(-5);
        } else {
            costLabel.zIndex(1);
        }
        
        var result = getStablizationTitle();
        stabilizationLabel.text(Text.translatable("tooltip.oritech.catalyst_" + result).formatted(Formatting.BLACK));
        
    }
    
    @NotNull
    private String getStablizationTitle() {
        var currentSouls = handler.catalyst.collectedSouls;
        var baseSouls = handler.catalyst.baseSoulCapacity;
        var maxSouls = handler.catalyst.maxSouls;
        var soulBonus = maxSouls - baseSouls;
        var free = maxSouls - currentSouls;
        
        var result = "Stable";
        
        if (soulBonus > 0 && currentSouls >= baseSouls) {
            if (free > 5) {
                result = "stabilized";
            } else if (free > 0) {
                result = "semi_stable";
            } else {
                result = "unstable";
            }
        } else {
            if (free > 5) {
                result = "stable";
            } else if (free > 0) {
                result = "semi_stable";
            } else {
                result = "unstable";
            }
        }
        return result;
    }
    
    @Override
    protected void updateEnergyBar() {
        
        var capacity = handler.catalyst.maxSouls;
        var amount = handler.catalyst.collectedSouls;
        
        var fillAmount = (float) amount / capacity;
        var tooltipText = getSoulTooltip(amount, capacity);
        
        energyIndicator.tooltip(tooltipText);
        energyIndicator.visibleArea(PositionedRectangle.of(0, 96 - ((int) (96 * (fillAmount))), 24, (int) (96 * fillAmount)));
    }
    
    public Text getSoulTooltip(long amount, long max) {
        return Text.literal(amount + " / " + max + " ").append(Text.translatable("tooltip.oritech.soul_count_tooltip"));
    }
}
