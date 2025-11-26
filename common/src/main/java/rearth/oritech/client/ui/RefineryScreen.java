package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.util.ScreenProvider;

public class RefineryScreen extends UpgradableMachineScreen<RefineryScreenHandler> {
    
    private final FluidDisplay outADisplay;
    private final FluidDisplay outBDisplay;
    private final FluidDisplay outCDisplay;
    
    private static final ScreenProvider.BarConfiguration outAConfig = new ScreenProvider.BarConfiguration(92, 6, 21, 74);
    private static final ScreenProvider.BarConfiguration outBConfig = new ScreenProvider.BarConfiguration(92 + 27, 6, 21, 74);
    private static final ScreenProvider.BarConfiguration outCConfig = new ScreenProvider.BarConfiguration(92 + 27 * 2, 6, 21, 74);
    
    public RefineryScreen(RefineryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        
        outADisplay = initFluidDisplay(handler.outputAContainer, outAConfig);
        outBDisplay = initFluidDisplay(handler.outputBContainer, outBConfig);
        outCDisplay = initFluidDisplay(handler.outputCContainer, outCConfig);
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        addFluidDisplay(overlay, outADisplay);
        updateFluidDisplay(outADisplay);
        
        addFluidDisplay(overlay, outBDisplay);
        updateFluidDisplay(outBDisplay);
        
        addFluidDisplay(overlay, outCDisplay);
        updateFluidDisplay(outCDisplay);
        
        var moduleCount = menu.refinery.getModuleCount();
        if (moduleCount < 1) {
            var blocker = Components.button(Component.literal("\uD83D\uDEAB"), event -> {});
            blocker.positioning(Positioning.absolute(outBConfig.x(), outBConfig.y()));
            blocker.sizing(Sizing.fixed(outBConfig.width()), Sizing.fixed(outBConfig.height()));
            blocker.active(false);
            blocker.zIndex(5);
            overlay.child(blocker);
        }
        if (moduleCount < 2) {
            var blocker = Components.button(Component.literal("\uD83D\uDEAB"), event -> {});
            blocker.positioning(Positioning.absolute(outCConfig.x(), outCConfig.y()));
            blocker.sizing(Sizing.fixed(outCConfig.width()), Sizing.fixed(outCConfig.height()));
            blocker.tooltip(Component.translatable("tooltip.oritech.module_2_missing"));
            blocker.active(false);
            overlay.child(blocker);
        }
        
    }
    
    @Override
    protected void containerTick() {
        
        updateFluidDisplay(outADisplay);
        updateFluidDisplay(outBDisplay);
        updateFluidDisplay(outCDisplay);
        
        super.containerTick();
    }
}
