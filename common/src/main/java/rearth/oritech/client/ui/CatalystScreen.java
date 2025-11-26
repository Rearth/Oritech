package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.PositionedRectangle;
import io.wispforest.owo.ui.core.Positioning;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.util.ScreenProvider.GuiSlot;

public class CatalystScreen extends BasicMachineScreen<CatalystScreenHandler> {
    
    public static final ResourceLocation GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components_souls.png");
    public static final ResourceLocation BOOK_SLOT = Oritech.id("textures/gui/modular/book_slot_background.png");
    
    private LabelComponent costLabel;
    private LabelComponent stabilizationLabel;
    
    public CatalystScreen(CatalystScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    public ResourceLocation getGuiComponents() {
        return GUI_COMPONENTS;
    }
    
    @Override
    public boolean showExtensionPanel() {
        return false;
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        costLabel = Components.label(Component.translatable("message.oritech.catalyst.cost", 0));
        stabilizationLabel = Components.label(Component.translatable("title.oritech.catalyst.stable"));
        overlay.child(costLabel.positioning(Positioning.absolute(56, 58)));
        overlay.child(stabilizationLabel.positioning(Positioning.absolute(108, 39)));
        
        var slotConfig = menu.screenData.getGuiSlots().getFirst();
        overlay.child(Components.texture(BOOK_SLOT, 0, 0, 16, 16, 16, 16).positioning(Positioning.absolute(slotConfig.x(), slotConfig.y())));
        
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        var cost = menu.catalyst.getDisplayedCost();
        costLabel.text(Component.translatable("message.oritech.catalyst.cost", cost).withStyle(ChatFormatting.BLACK));
        
        if (cost == 0) {
            costLabel.zIndex(-5);
        } else {
            costLabel.zIndex(1);
        }
        
        var result = getStabilizationTitle();
        stabilizationLabel.text(result.withStyle(ChatFormatting.BLACK));
        
    }
    
    @NotNull
    private MutableComponent getStabilizationTitle() {
        var currentSouls = menu.catalyst.collectedSouls;
        var baseSouls = menu.catalyst.baseSoulCapacity;
        var maxSouls = menu.catalyst.maxSouls;
        var soulBonus = maxSouls - baseSouls;
        var free = maxSouls - currentSouls;
        
        var result = Component.translatable("title.oritech.catalyst.stable");
        
        if (soulBonus > 0 && currentSouls >= baseSouls) {
            if (free > 5) {
                result = Component.translatable("title.oritech.catalyst.stabilized");;
            } else if (free > 0) {
                result = Component.translatable("title.oritech.catalyst.semi_stable");;
            } else {
                result = Component.translatable("title.oritech.catalyst.unstable");;
            }
        } else {
            if (free > 5) {
                result = Component.translatable("title.oritech.catalyst.stable");;
            } else if (free > 0) {
                result = Component.translatable("title.oritech.catalyst.semi_stable");;
            } else {
                result = Component.translatable("title.oritech.catalyst.stable");;
            }
        }
        return result;
    }
    
    @Override
    protected void updateEnergyBar() {
        
        var capacity = menu.catalyst.maxSouls;
        var amount = menu.catalyst.collectedSouls;
        
        var fillAmount = (float) amount / capacity;
        var tooltipText = getSoulTooltip(amount, capacity);
        
        energyIndicator.tooltip(tooltipText);
        energyIndicator.visibleArea(PositionedRectangle.of(0, 96 - ((int) (96 * (fillAmount))), 24, (int) (96 * fillAmount)));
    }
    
    public Component getSoulTooltip(long amount, long max) {
        return Component.translatable("tooltip.oritech.spawner.collected_souls", amount, max);
    }
}
