package rearth.oritech.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.TextureWidget;

public class CatalystScreen extends OritechMachineScreen<CatalystScreenHandler> {
    
    public static final ResourceLocation GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components_souls.png");
    public static final ResourceLocation BOOK_SLOT = Oritech.id("textures/gui/modular/book_slot_background.png");
    
    private LabelWidget costLabel;
    private LabelWidget stabilizationLabel;
    
    public CatalystScreen(CatalystScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    public boolean showExtensionPanel() {
        return false;
    }
    
    @Override
    protected void addExtraComponents() {
        costLabel = new LabelWidget(56, 58, 40, 10, Component.translatable("message.oritech.catalyst.cost", 0));
        costLabel.withDarkColor();
        costLabel.setVisible(false);

        stabilizationLabel = new LabelWidget(101, 39, 60, 10, Component.translatable("title.oritech.catalyst.stable"));
        stabilizationLabel.withDarkColor();

        var slotConfig = menu.screenData.getGuiSlots().getFirst();
        addComponent(new TextureWidget(slotConfig.x(), slotConfig.y(), 16, 16, BOOK_SLOT, 16, 16));
        addComponent(costLabel);
        addComponent(stabilizationLabel);
    }
    
    @Override
    protected void tickExtra() {
        var cost = menu.catalyst.getDisplayedCost();
        costLabel.setText(Component.translatable("message.oritech.catalyst.cost", cost).withStyle(ChatFormatting.BLACK));
        costLabel.setVisible(cost > 0);

        var result = getStabilizationTitle();
        stabilizationLabel.setText(result.withStyle(ChatFormatting.BLACK));
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
}
