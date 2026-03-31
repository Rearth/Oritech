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

public class OritechReiParticleCollisionDisplay implements DisplayCategory<Display> {
    
    protected final OritechRecipeType recipeType;
    protected final ItemLike icon;
    public static final ResourceLocation PARTICLE_RECIPE_OVERLAY = Oritech.id("textures/gui/modular/particle_recipe_overlay.png");
    
    public OritechReiParticleCollisionDisplay(OritechRecipeType recipeType, ItemLike icon) {
        this.recipeType = recipeType;
        this.icon = icon;
    }
    
    @Override
    public List<Widget> setupDisplay(Display display, Rectangle bounds) {
        var widgets = new ArrayList<Widget>();
        var oDisplay = (OritechDisplay) display;
        var x = bounds.x;
        var y = bounds.y;
        
        // background
        widgets.add(Widgets.createRecipeBase(bounds));
        
        // particle collision overlay
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) ->
            graphics.blit(PARTICLE_RECIPE_OVERLAY, x + 60, y + 17, 0, 0, 36, 24, 36, 24)));
        
        // input slots
        widgets.add(Widgets.createSlot(new Point(x + 42, y + 20))
            .entries(oDisplay.getInputEntries().get(0)).markInput());
        widgets.add(Widgets.createSlot(new Point(x + 96, y + 20))
            .entries(oDisplay.getInputEntries().get(1)).markInput());
        
        // output slot
        widgets.add(Widgets.createSlot(new Point(x + 70, y + 20))
            .entries(oDisplay.getOutputEntries().get(0)).markOutput());
        
        // collision speed label
        widgets.add(Widgets.createLabel(
            new Point(x + 6, y + bounds.height - 12),
            Component.translatable("emi.title.oritech.collisionspeed", oDisplay.getEntry().value().getTime())
        ).leftAligned().color(0xFFFFFF).noShadow());
        
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
