package rearth.oritech.init.compat.rei.Screens;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import rearth.oritech.init.compat.rei.TaintedRefineryInfoDisplay;

import java.util.ArrayList;
import java.util.List;

public class OritechReiTaintedRefineryCreationDisplay implements DisplayCategory<Display> {
    
    private final CategoryIdentifier<?> categoryId;
    private final ItemLike icon;
    
    public OritechReiTaintedRefineryCreationDisplay(CategoryIdentifier<?> categoryId, ItemLike icon) {
        this.categoryId = categoryId;
        this.icon = icon;
    }
    
    @Override
    public int getDisplayHeight() {
        return 82;
    }
    
    @Override
    public int getDisplayWidth(Display display) {
        return 170;
    }
    
    @Override
    public List<Widget> setupDisplay(Display display, Rectangle bounds) {
        var widgets = new ArrayList<Widget>();
        var infoDisplay = (TaintedRefineryInfoDisplay) display;
        var x = bounds.x;
        var y = bounds.y;
        
        // background
        widgets.add(Widgets.createRecipeBase(bounds));
        
        // input: refinery
        widgets.add(Widgets.createSlot(new Point(x + 10, y + 10))
            .entries(infoDisplay.getInputEntries().get(0)).markInput());
        
        // input: catalyst
        widgets.add(Widgets.createSlot(new Point(x + 10, y + 35))
            .entries(infoDisplay.getInputEntries().get(1)).markInput());
        
        // arrow
        widgets.add(Widgets.createArrow(new Point(x + 50, y + 22)));
        
        // output: tainted refinery
        widgets.add(Widgets.createSlot(new Point(x + 90, y + 22))
            .entries(infoDisplay.getOutputEntries().get(0)).markOutput());
        
        // hint text
        widgets.add(Widgets.createLabel(
            new Point(x + 6, y + bounds.height - 24),
            Component.translatable("emi.title.oritech.tainted_creation_hint")
        ).leftAligned().color(0xFFFFFF).noShadow());
        
        // hint text
        widgets.add(Widgets.createLabel(
            new Point(x + 6, y + bounds.height - 12),
            Component.translatable("emi.title.oritech.tainted_creation_hint2")
        ).leftAligned().color(0xFFFFFF).noShadow());
        
        return widgets;
    }
    
    @Override
    public CategoryIdentifier<? extends Display> getCategoryIdentifier() {
        return categoryId;
    }
    
    @Override
    public Component getTitle() {
        return Component.translatable("emi.category.oritech.tainted_refinery_creation");
    }
    
    @Override
    public Renderer getIcon() {
        return EntryStacks.of(icon);
    }
}
