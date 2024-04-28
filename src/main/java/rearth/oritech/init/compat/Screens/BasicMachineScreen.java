package rearth.oritech.init.compat.Screens;

import io.wispforest.owo.compat.rei.ReiUIAdapter;
import io.wispforest.owo.ui.base.BaseComponent;
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
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.init.compat.OritechDisplay;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.ScreenProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static rearth.oritech.client.ui.BasicMachineScreen.GUI_COMPONENTS;

public class BasicMachineScreen implements DisplayCategory<Display> {
    
    protected final OritechRecipeType recipeType;
    protected final MachineBlockEntity screenProvider;
    protected final ItemConvertible icon;
    
    public BasicMachineScreen(OritechRecipeType recipeType, Class<? extends MachineBlockEntity> screenProviderSource, ItemConvertible icon) {
        this.recipeType = recipeType;
        try {
            this.screenProvider = screenProviderSource.getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(new BlockPos(0, 0, 0), Blocks.AIR.getDefaultState());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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
        
        var slots = screenProvider.getGuiSlots();
        var slotOffsets = screenProvider.getSlots();
        var offsetX = 23;
        var offsetY = 17;
        
        // inputs
        var inputEntries = display.getInputEntries();
        for (int i = 0; i < inputEntries.size(); i++) {
            var entry = inputEntries.get(i);
            var pos = slots.get(slotOffsets.inputStart() + i);
            root.child(adapter.wrap(Widgets.createSlot(new Point(0, 0)).entry(entry.get(0))).positioning(Positioning.absolute(pos.x() - offsetX, pos.y() - offsetY)));
        }
        
        // arrow
        root.child(adapter.wrap(Widgets.createArrow(new Point(0, 0))).positioning(Positioning.absolute(80 - offsetX, 35 - offsetY)));
        
        // outputs
        var outputEntries = display.getOutputEntries();
        for (int i = 0; i < outputEntries.size(); i++) {
            var entry = outputEntries.get(i);
            var pos = slots.get(slotOffsets.outputStart() + i);
            root.child(adapter.wrap(Widgets.createSlot(new Point(0, 0)).entry(entry.get(0))).positioning(Positioning.absolute(pos.x() - offsetX, pos.y() - offsetY)));
        }
        
        // data
        var duration = String.format("%.0f", display.getEntry().value().getTime() / 20f);
        root.child(
          Components.label(Text.of(duration + "s")).lineHeight(7)
            .positioning(Positioning.relative(30, 97))
        );
        
        // fluids
        if (display.entry.value().getFluidInput() != null) {
            var fluid = display.entry.value().getFluidInput().variant();
            var amount = display.entry.value().getFluidInput().amount();
            
            root.child(rearth.oritech.client.ui.BasicMachineScreen.createFluidRenderer(fluid, 81000, new ScreenProvider.BarConfiguration(4, 5, 16, 50)));
            
            
            var text = Text.literal(amount * 1000 / FluidConstants.BUCKET + " mB " + FluidVariantAttributes.getName(fluid).getString()).formatted(Formatting.DARK_AQUA);
            var foreGround = Components.texture(GUI_COMPONENTS, 48, 0, 14, 50, 98, 96);
            foreGround.sizing(Sizing.fixed(18), Sizing.fixed(52));
            foreGround.positioning(Positioning.absolute(3, 4));
            foreGround.tooltip(text);
            root.child(foreGround);
        }
        
        if (display.entry.value().getFluidOutput() != null) {
            var fluid = display.entry.value().getFluidOutput().variant();
            var amount = display.entry.value().getFluidOutput().amount();
            
            root.child(rearth.oritech.client.ui.BasicMachineScreen.createFluidRenderer(fluid, 81000, new ScreenProvider.BarConfiguration(123, 5, 16, 50)));
            
            var text = Text.literal(amount * 1000 / FluidConstants.BUCKET + " mB " + FluidVariantAttributes.getName(fluid).getString()).formatted(Formatting.DARK_AQUA);
            var foreGround = Components.texture(GUI_COMPONENTS, 48, 0, 14, 50, 98, 96);
            foreGround.sizing(Sizing.fixed(18), Sizing.fixed(52));
            foreGround.positioning(Positioning.absolute(122, 4));
            foreGround.tooltip(text);
            root.child(foreGround);
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
