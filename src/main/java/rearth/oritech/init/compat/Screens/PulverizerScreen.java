package rearth.oritech.init.compat.Screens;

import io.wispforest.owo.compat.rei.ReiUIAdapter;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import rearth.oritech.init.compat.OritechDisplay;
import rearth.oritech.init.recipes.OritechRecipeType;

public class PulverizerScreen extends BasicMachineScreen {
    
    private static final int HEIGHT_BASE = 15;
    
    public PulverizerScreen(OritechRecipeType recipeType, ItemConvertible icon) {
        super(recipeType, icon);
    }
    
    @Override
    public void fillDisplay(FlowLayout root, OritechDisplay display, ReiUIAdapter<FlowLayout> adapter) {
    
        // input
        addInputSlot(root, display, adapter, 20, HEIGHT_BASE, 0);
        
        // middle arrow
        root.child(adapter.wrap(Widgets.createArrow(new Point(0, 0))).positioning(Positioning.absolute(40, HEIGHT_BASE)));
        
        // result background
        var resultBackground = Containers.horizontalFlow(Sizing.content(2), Sizing.content(2));
        resultBackground.surface(Surface.PANEL_INSET);
        resultBackground.verticalAlignment(VerticalAlignment.CENTER);
        
        // output main
        var mainOutput = getOutputSlotNoBackground(display, adapter, 0).margins(Insets.of(2));
        
        // output secondary
        var secondOutputItem = display.getOutputEntries().size() == 2 ? display.getOutputEntries().get(1).get(0) : EntryStack.empty();
        var secondOutput = getOutputSlotNoBackground(adapter, secondOutputItem);
        
        resultBackground.child(mainOutput);
        resultBackground.child(secondOutput);
        
        root.child(
          resultBackground.positioning(Positioning.absolute(70, HEIGHT_BASE - 4))
        );
        
        // data
        var energyUsage = display.getEntry().value().getEnergyPerTick();
        var duration = String.format("%.0f", display.getEntry().value().getTime() / 20f);
        root.child(
          Components.label(Text.of(duration + "s, " + energyUsage + " RF/t")).lineHeight(7)
            .positioning(Positioning.absolute(72, HEIGHT_BASE + 28))
        );
    
    }
}
