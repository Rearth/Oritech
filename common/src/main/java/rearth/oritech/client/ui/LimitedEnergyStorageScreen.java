package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SliderWidget;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import rearth.oritech.util.TooltipHelper;

import java.util.TreeSet;

public class LimitedEnergyStorageScreen extends EnergyStorageScreen<LimitedEnergyStorageScreenHandler> {
    
    public LimitedEnergyStorageScreen(LimitedEnergyStorageScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        if (this.menu.blockEntity instanceof UnstableContainerBlockEntity) return;
        
        this.backgroundTexture = OritechMachineScreen.BACKGROUND_TALL;
        this.height = 186;
        this.setPanelSize(176, 186);
    }
    
    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();
        
        if (this.menu.blockEntity instanceof UnstableContainerBlockEntity) return;
        
        var storage = (ExpandableEnergyStorageBlockEntity) menu.blockEntity;
        var initialAmount = (int) (storage.rfOutputOverride > 0 ? storage.rfOutputOverride : storage.energyStorage.maxExtract);
        var max = (int) storage.energyStorage.maxExtract;
        
        var slider = new SliderWidget(32, 83, 137, SliderWidget.Orientation.HORIZONTAL,
          SliderWidget.ValueLabelPosition.END, Component.translatable("label.oritech.energy_storage.limit"), 1, max, initialAmount, this::onSliderDragged)
                       .withTextColor(LabelWidget.DARK_TEXT)
                       .withLogarithmicScale(true)
                       .withSnapValues(createNiceRateSteps(max))
                       .withValueFormatter(value -> Component.translatable("tooltip.oritech.rf_rate", TooltipHelper.getEnergyText(value)));
        
        addComponent(slider);
    }
    
    private void onSliderDragged(SliderWidget slider, int value) {
        NetworkManager.sendToServer(new ExpandableEnergyStorageBlockEntity.StorageLimitPacket(menu.blockPos, value));
    }
    
    // basically generates multiples of 2 and 5 that can be selected from
    private static int[] createNiceRateSteps(int maxValue) {
        var values = new TreeSet<Integer>();
        values.add(0);
        values.add(maxValue);
        
        for (long value = 1; value <= maxValue; value <<= 1) {
            values.add((int) value);
        }
        
        for (long scale = 1; scale <= maxValue; scale *= 10) {
            for (var factor : new int[]{1, 2, 5}) {
                var candidate = factor * scale;
                if (candidate <= maxValue) {
                    values.add((int) candidate);
                }
            }
        }
        
        return values.stream().mapToInt(Integer::intValue).toArray();
    }
}
