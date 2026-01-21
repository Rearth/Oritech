package rearth.oritech.init.compat.rei.Screens;

import io.wispforest.owo.compat.rei.ReiUIAdapter;
import io.wispforest.owo.ui.component.Components;
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
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import rearth.oritech.Oritech;
import rearth.oritech.init.compat.rei.OritechDisplay;
import rearth.oritech.init.recipes.OritechRecipeType;

import java.util.List;

public class OritechReiLaserDisplay implements DisplayCategory<Display> {
    
    protected final OritechRecipeType recipeType;
    protected final ItemLike icon;
    public static final ResourceLocation LASER_RECIPE_OVERLAY = Oritech.id("textures/gui/modular/laser_recipe_background.png");
    
    public OritechReiLaserDisplay(OritechRecipeType recipeType, ItemLike icon) {
        this.recipeType = recipeType;
        this.icon = icon;
    }
    
    @Override
    public int getDisplayHeight() {
        return 80;
    }
    
    @Override
    public int getDisplayWidth(Display display) {
        return 170;
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
        
        var particleBackground = Components.texture(LASER_RECIPE_OVERLAY, 0, 0, 300, 300, 300, 300);
        
        root.child(
          adapter.wrap(Widgets.createSlot(new Point(0, 0)).entries(display.getInputEntries().get(0)).markInput()).positioning(Positioning.absolute(80, 15)));
        
        root.child(
          adapter.wrap(Widgets.createSlot(new Point(0, 0)).entries(display.getOutputEntries().get(0)).markOutput()).positioning(Positioning.absolute(135, 15)));
        
        root.child(adapter.wrap(Widgets.createArrow(new Point(0, 0))).positioning(Positioning.absolute(105, 15)));
        root.child(particleBackground.sizing(Sizing.fixed(80)).positioning(Positioning.absolute(5, 5)));
        
    }
    
    @Override
    public CategoryIdentifier<? extends Display> getCategoryIdentifier() {
        return CategoryIdentifier.of(recipeType.getIdentifier());
    }
    
    @Override
    public Component getTitle() {
        return Component.translatable("rei.process." + recipeType.getIdentifier());
    }
    
    @Override
    public Renderer getIcon() {
        return EntryStacks.of(icon);
    }
    
}
