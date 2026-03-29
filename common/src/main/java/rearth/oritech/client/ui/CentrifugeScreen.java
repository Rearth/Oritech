package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.screen.widgets.FluidSlotWidget;
import rearth.oritech.util.ScreenProvider;

public class CentrifugeScreen extends UpgradableOritechScreen<CentrifugeScreenHandler> {
    private static final ScreenProvider.BarConfiguration INPUT_CONFIG = new ScreenProvider.BarConfiguration(28, 6, 21, 74);

    private final FluidSlotWidget inputFluidDisplay;

    public CentrifugeScreen(CentrifugeScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.inputFluidDisplay = handler.inputTank != null ? createFluidDisplay(handler.inputTank, INPUT_CONFIG) : null;
    }

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        if (inputFluidDisplay != null) {
            addComponent(inputFluidDisplay);
        }
    }
}
