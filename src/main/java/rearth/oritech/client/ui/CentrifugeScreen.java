package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.text.Text;
import rearth.oritech.block.entity.machines.processing.CentrifugeBlockEntity;
import rearth.oritech.client.renderers.LaserArmModel;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

public class CentrifugeScreen extends UpgradableMachineScreen<CentrifugeScreenHandler> {
    private ColoredSpriteComponent inFluidBackground;
    private float inLastFluidFill;
    private BoxComponent inFluidFillStatusOverlay;
    
    private static final ScreenProvider.BarConfiguration inputConfig = new ScreenProvider.BarConfiguration(28, 6, 21, 74);
    
    public CentrifugeScreen(CentrifugeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected void updateFluidBar() {
        
        if (!((CentrifugeBlockEntity) handler.blockEntity).hasFluidAddon) return;
        
        super.updateFluidBar();
        
        var container = handler.inputTank;
        var fill = 1 - ((float) container.getAmount() / container.getCapacity());

        var targetFill = LaserArmModel.lerp(inLastFluidFill, fill, 0.15f);
        inLastFluidFill = targetFill;

        inFluidFillStatusOverlay.verticalSizing(Sizing.fixed((int) (inputConfig.height() * targetFill * 0.98f)));

        var tooltipText = List.of(Text.of(FluidVariantRendering.getTooltip(container.getResource()).get(0)), Text.of((container.getAmount() * 1000 / FluidConstants.BUCKET) + " mb"));
        inFluidBackground.tooltip(tooltipText);
    }
    
    @Override
    protected void addFluidBar(FlowLayout panel) {
        
        if (!((CentrifugeBlockEntity) handler.blockEntity).hasFluidAddon) return;
        
        super.addFluidBar(panel);
        
        var storage = handler.inputTank;
        inFluidBackground = null;
        var config = inputConfig;
        inLastFluidFill = 1 - ((float) storage.getAmount() / storage.getCapacity());


        for (var it = storage.nonEmptyIterator(); it.hasNext(); ) {

            var fluid = it.next();
            inFluidBackground = BasicMachineScreen.createFluidRenderer(fluid.getResource(), fluid.getAmount(), config);
            break;
        }

        if (inFluidBackground == null) {
            inFluidBackground = createFluidRenderer(FluidVariant.of(Fluids.EMPTY), 0L, config);
        }

        inFluidFillStatusOverlay = Components.box(Sizing.fixed(config.width()), Sizing.fixed((int) (config.height() * inLastFluidFill)));
        inFluidFillStatusOverlay.color(new Color(77.6f / 255f, 77.6f / 255f, 77.6f / 255f));
        inFluidFillStatusOverlay.fill(true);
        inFluidFillStatusOverlay.positioning(Positioning.absolute(config.x(), config.y()));


        var foreGround = Components.texture(GUI_COMPONENTS, 48, 0, 14, 50, 98, 96);
        foreGround.sizing(Sizing.fixed(config.width()), Sizing.fixed(config.height()));
        foreGround.positioning(Positioning.absolute(config.x(), config.y()));

        panel.child(inFluidBackground);
        panel.child(inFluidFillStatusOverlay);
        panel.child(foreGround);
        
    }
}
