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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import rearth.oritech.Oritech;
import rearth.oritech.init.compat.rei.OritechDisplay;
import rearth.oritech.init.recipes.OritechRecipeType;

import java.util.ArrayList;
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
        var widgets = new ArrayList<Widget>();
        var oDisplay = (OritechDisplay) display;
        var x = bounds.x;
        var y = bounds.y;
        
        // background
        widgets.add(Widgets.createRecipeBase(bounds));
        
        // laser background texture
        widgets.add(Widgets.createTexturedWidget(LASER_RECIPE_OVERLAY, x + 5, y + 5, 0, 0, 80, 80, 300, 300, 300, 300));

        // input slot
        widgets.add(Widgets.createSlot(new Point(x + 80, y + 15))
            .entries(oDisplay.getInputEntries().get(0)).markInput());
        
        // arrow
        widgets.add(Widgets.createArrow(new Point(x + 105, y + 15)));
        
        // output slot
        widgets.add(Widgets.createSlot(new Point(x + 135, y + 15))
            .entries(oDisplay.getOutputEntries().get(0)).markOutput());
        
        return widgets;
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
