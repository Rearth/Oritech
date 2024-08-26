package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.PositionedRectangle;
import io.wispforest.owo.ui.core.Positioning;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public class CatalystScreen extends BasicMachineScreen<CatalystScreenHandler> {
    
    private LabelComponent costLabel;
    private LabelComponent stabilizationLabel;
    
    public CatalystScreen(CatalystScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    public boolean showExtensionPanel() {
        return false;
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        costLabel = Components.label(Text.literal("Cost: " + 0));
        stabilizationLabel = Components.label(Text.literal("Stable"));
        
        overlay.child(costLabel.positioning(Positioning.absolute(90, 35)));
        overlay.child(stabilizationLabel.positioning(Positioning.absolute(90, 50)));
        
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        
        var cost = handler.catalyst.getDisplayedCost();
        costLabel.text(Text.literal("Cost: " + cost).formatted(Formatting.BLACK));
        
        if (cost == 0) {
            costLabel.zIndex(-5);
        } else {
            costLabel.zIndex(1);
        }
        
        var result = getStablizationTitle();
        stabilizationLabel.text(Text.literal(result).formatted(Formatting.BLACK));
        
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
                result = "Stabilized";
            } else if (free > 0) {
                result = "Semi-Stable";
            } else {
                result = "Unstable";
            }
        } else {
            if (free > 5) {
                result = "Stable";
            } else if (free > 0) {
                result = "Semi-Stable";
            } else {
                result = "Unstable";
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
        return Text.literal(amount + " / " + max + " Souls");
    }
}
