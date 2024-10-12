package rearth.oritech.init.compat.rei.Screens;

import io.wispforest.owo.compat.rei.ReiUIAdapter;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Surface;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.compat.rei.OritechDisplay;
import rearth.oritech.init.recipes.OritechRecipeType;

import java.util.List;

public class OritechReiParticleCollisionDisplay implements DisplayCategory<Display> {
    
    protected final OritechRecipeType recipeType;
    protected final ItemConvertible icon;
    public static final Identifier PARTICLE_RECIPE_OVERLAY = Oritech.id("textures/gui/modular/particle_recipe_overlay.png");
    
    public OritechReiParticleCollisionDisplay(OritechRecipeType recipeType, ItemConvertible icon) {
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
        
        var particleBackground = Components.texture(PARTICLE_RECIPE_OVERLAY, 0, 0, 36, 24, 36, 24);
        root.child(particleBackground.positioning(Positioning.absolute(60, 17)));
        
        root.child(
          adapter.wrap(Widgets.createSlot(new Point(0, 0)).entries(display.getInputEntries().get(0)).markInput()).positioning(Positioning.absolute(42, 20)));
        root.child(
          adapter.wrap(Widgets.createSlot(new Point(0, 0)).entries(display.getInputEntries().get(1)).markInput()).positioning(Positioning.absolute(96, 20)));
        
        root.child(
          adapter.wrap(Widgets.createSlot(new Point(0, 0)).entries(display.getOutputEntries().get(0)).markOutput()).positioning(Positioning.absolute(70, 20)));
        
        // data
        root.child(
          Components.label(Text.translatable("emi.title.oritech.collisionspeed", display.getEntry().value().getTime())).lineHeight(7)
            .positioning(Positioning.relative(0, 97))
        );
        
    }
    
    @Override
    public CategoryIdentifier<? extends Display> getCategoryIdentifier() {
        return CategoryIdentifier.of(recipeType.getIdentifier());
    }
    
    @Override
    public Text getTitle() {
        return Text.translatable("rei.process." + recipeType.getIdentifier());
    }
    
    @Override
    public Renderer getIcon() {
        return EntryStacks.of(icon);
    }
    
}
