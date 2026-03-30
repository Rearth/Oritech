package rearth.oritech.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ScreenProvider;

public class RefineryScreen extends UpgradableOritechScreen<RefineryScreenHandler> {

    private static final ScreenProvider.BarConfiguration OUTPUT_B_CONFIG = new ScreenProvider.BarConfiguration(92 + 27, 6, 21, 74);
    private static final ScreenProvider.BarConfiguration OUTPUT_C_CONFIG = new ScreenProvider.BarConfiguration(92 + 27 * 2, 6, 21, 74);

    public RefineryScreen(RefineryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        var refinery = (rearth.oritech.block.entity.processing.RefineryBlockEntity) menu.blockEntity;
        var moduleCount = refinery.getModuleCount();

        if (moduleCount < 1) {
            addComponent(new TankBlockerWidget(OUTPUT_B_CONFIG, Component.empty()));
        }
        if (moduleCount < 2) {
            addComponent(new TankBlockerWidget(OUTPUT_C_CONFIG, Component.translatable("tooltip.oritech.module_2_missing")));
        }
    }

    private static final class TankBlockerWidget extends UIComponent {

        private TankBlockerWidget(ScreenProvider.BarConfiguration config, Component tooltip) {
            super(config.x(), config.y(), config.width(), config.height());
            setSurface(OritechSurface.PANEL_DARK);
            if (!tooltip.getString().isEmpty()) {
                withTooltip(tooltip);
            }
            setZIndex(5);
        }

        @Override
        protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(x, y, x + width, y + height, 0x77000000);
        }

        @Override
        public boolean handleClick(double mouseX, double mouseY, int button) {
            return button == 0 || button == 1;
        }
    }
}
