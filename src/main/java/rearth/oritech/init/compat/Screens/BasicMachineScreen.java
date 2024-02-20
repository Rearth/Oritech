package rearth.oritech.init.compat.Screens;

import io.wispforest.owo.compat.rei.ReiUIAdapter;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import rearth.oritech.init.compat.OritechDisplay;
import rearth.oritech.init.recipes.OritechRecipeType;

import java.util.List;

public class BasicMachineScreen implements DisplayCategory<Display> {
    
    protected final OritechRecipeType recipeType;
    protected final ItemConvertible icon;
    
    public BasicMachineScreen(OritechRecipeType recipeType, ItemConvertible icon) {
        this.recipeType = recipeType;
        this.icon = icon;
    }
    
    @Override
    public List<Widget> setupDisplay(Display display, Rectangle bounds) {
        var adapter = new ReiUIAdapter<>(bounds, Containers::verticalFlow);
        var root = adapter.rootComponent();
        
        root.horizontalAlignment(HorizontalAlignment.CENTER)
          .surface(Surface.PANEL)
          .padding(Insets.of(4));
        
        fillDisplay(root, (OritechDisplay) display, adapter);
        
        adapter.prepare();
        return List.of(adapter);
    }
    
    public void fillDisplay(FlowLayout root, OritechDisplay display, ReiUIAdapter<FlowLayout> adapter) {
        
        List<EntryIngredient> inputEntries = display.getInputEntries();
        for (int i = 0; i < inputEntries.size(); i++) {
            var entry = inputEntries.get(i);
            root.child(adapter.wrap(Widgets.createSlot(new Point(0, 0)).entry(entry.get(0))).positioning(Positioning.absolute(50 + i * 19, 11)));
        }
        
        List<EntryIngredient> outputEntries = display.getOutputEntries();
        for (int i = 0; i < outputEntries.size(); i++) {
            var entry = outputEntries.get(i);
            root.child(adapter.wrap(Widgets.createSlot(new Point(0, 0)).entry(entry.get(0))).positioning(Positioning.absolute(50 + i * 19, 40)));
        }
    }
    
    public static void addInputSlot(FlowLayout root, OritechDisplay display, ReiUIAdapter<FlowLayout> adapter, int x, int y, int slot) {
        root.child(adapter.wrap(
          Widgets.createSlot(new Point(0, 0)).markInput().entry(display.getInputEntries().get(slot).get(0))
        ).positioning(Positioning.absolute(x, y)));
    }
    
    public static void addOutputSlot(FlowLayout root, OritechDisplay display, ReiUIAdapter<FlowLayout> adapter, int x, int y, int slot) {
        root.child(adapter.wrap(
          Widgets.createSlot(new Point(0, 0)).markOutput().entry(display.getOutputEntries().get(slot).get(0))
        ).positioning(Positioning.absolute(x, y)));
    }
    
    public static BaseComponent getOutputSlotNoBackground(OritechDisplay display, ReiUIAdapter<FlowLayout> adapter, int slot) {
        return getOutputSlotNoBackground(adapter, display.getOutputEntries().get(slot).get(0));
    }
    
    public static BaseComponent getOutputSlotNoBackground(ReiUIAdapter<FlowLayout> adapter, EntryStack<?> stack) {
        return adapter.wrap(
          Widgets.createSlot(new Point(0, 0)).entry(stack).disableBackground()
        );
    }
    
    @Override
    public CategoryIdentifier<? extends Display> getCategoryIdentifier() {
        return CategoryIdentifier.of(recipeType.getIdentifier());
    }
    
    @Override
    public Text getTitle() {
        return Text.of(recipeType.getIdentifier());
    }
    
    @Override
    public Renderer getIcon() {
        return EntryStacks.of(icon);
    }
    
}
