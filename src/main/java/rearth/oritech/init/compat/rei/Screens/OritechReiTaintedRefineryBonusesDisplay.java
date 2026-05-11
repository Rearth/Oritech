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

public class OritechReiTaintedRefineryBonusesDisplay implements DisplayCategory<Display> {
    
    private final CategoryIdentifier<?> categoryId;
    private final ItemLike icon;
    
    public OritechReiTaintedRefineryBonusesDisplay(CategoryIdentifier<?> categoryId, ItemLike icon) {
        this.categoryId = categoryId;
        this.icon = icon;
    }
    
    @Override
    public int getDisplayHeight() {
        return 90;
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
        var bonusType = infoDisplay.getBonusType();
        
        // background
        widgets.add(Widgets.createRecipeBase(bounds));
        
        // title
        widgets.add(Widgets.createLabel(
            new Point(x + 6, y + 6),
            Component.translatable("emi.title.oritech.tainted_bonus." + bonusType)
        ).leftAligned().color(0xFFFFFF).noShadow());
        
        // block grid (8 columns)
        var cols = 8;
        var entries = infoDisplay.getInputEntries();
        for (int i = 0; i < entries.size(); i++) {
            var col = i % cols;
            var row = i / cols;
            widgets.add(Widgets.createSlot(new Point(x + 6 + col * 18, y + 18 + row * 18))
                .entries(entries.get(i)).markInput());
        }
        
        // description text at bottom
        var rows = (entries.size() + cols - 1) / cols;
        widgets.add(Widgets.createLabel(
            new Point(x + 6, y + 20 + rows * 18),
            Component.translatable("emi.description.oritech.tainted_bonus." + bonusType)
        ).leftAligned().color(0xFFFFFF).noShadow());
        
        return widgets;
    }
    
    @Override
    public CategoryIdentifier<? extends Display> getCategoryIdentifier() {
        return categoryId;
    }
    
    @Override
    public Component getTitle() {
        return Component.translatable("emi.category.oritech.tainted_refinery_bonuses");
    }
    
    @Override
    public Renderer getIcon() {
        return EntryStacks.of(icon);
    }
}
