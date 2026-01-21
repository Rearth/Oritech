package rearth.oritech.init.compat.rei.Screens;

import dev.architectury.hooks.fluid.FluidStackHooks;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.client.ui.BasicMachineScreen;
import rearth.oritech.init.compat.rei.OritechDisplay;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.StringJoiner;

import static rearth.oritech.client.ui.BasicMachineScreen.GUI_COMPONENTS;


public class OritechReiDisplay implements DisplayCategory<Display> {
    
    protected final OritechRecipeType recipeType;
    private final Boolean isGenerator;
    private final List<ScreenProvider.GuiSlot> slots;
    private final InventorySlotAssignment slotOffsets;
    protected final ItemLike icon;
    private final ScreenProvider.ArrowConfiguration indicatorConfig;
    
    public OritechReiDisplay(OritechRecipeType recipeType, Class<? extends MachineBlockEntity> screenProviderSource, ItemLike icon) {
        
        var blockState = Blocks.STONE.defaultBlockState();
        if (icon instanceof Block blockItem)
            blockState = blockItem.defaultBlockState();
        var finalBlockState = blockState;
        
        this.recipeType = recipeType;
        try {
            var screenProvider = screenProviderSource.getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(new BlockPos(0, 0, 0), finalBlockState);
            this.isGenerator = screenProvider instanceof UpgradableGeneratorBlockEntity;
            this.slots = screenProvider.getGuiSlots();
            this.slotOffsets = screenProvider.getSlotAssignments();
            this.indicatorConfig = screenProvider.getIndicatorConfiguration();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        this.icon = icon;
    }
    
    public OritechReiDisplay(OritechRecipeType recipeType, ItemLike icon, boolean isGenerator, List<ScreenProvider.GuiSlot> slots, InventorySlotAssignment assignments) {
        
        this.recipeType = recipeType;
        this.icon = icon;
        this.isGenerator = isGenerator;
        this.slots = slots;
        this.slotOffsets = assignments;
        this.indicatorConfig = new ScreenProvider.ArrowConfiguration(
          Oritech.id("textures/gui/modular/arrow_empty.png"),
          Oritech.id("textures/gui/modular/arrow_full.png"),
          80, 35, 29, 16, true);
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
    
    @Override
    public int getDisplayHeight() {
        return 74;
    }
    
    public void fillDisplay(FlowLayout root, OritechDisplay display, ReiUIAdapter<FlowLayout> adapter) {
        
        var offsetX = 23;
        var offsetY = 17;
        
        // inputs
        var inputEntries = display.getInputEntries();
        for (int i = 0; i < inputEntries.size(); i++) {
            var entry = inputEntries.get(i);
            if (entry.isEmpty()) continue;
            var pos = slots.get(slotOffsets.inputStart() + i);
            var usedY = Math.clamp(2, pos.y() - offsetY, getDisplayHeight() - 18 - 4);
            root.child(
              adapter.wrap(Widgets.createSlot(new Point(0, 0)).entries(entry).markInput())
                .positioning(Positioning.absolute(pos.x() - offsetX, usedY)));
        }
        
        // arrow
        if (isGenerator) {
            root.child(adapter.wrap(Widgets.createBurningFire(new Point(0, 0))).positioning(Positioning.absolute(indicatorConfig.x() - offsetX, indicatorConfig.y() -  offsetY)));
        } else {
            root.child(adapter.wrap(Widgets.createArrow(new Point(0, 0))).positioning(Positioning.absolute(indicatorConfig.x() - offsetX, indicatorConfig.y() -  offsetY)));
        }
        
        // outputs
        var outputEntries = display.getOutputEntries();
        for (int i = 0; i < outputEntries.size(); i++) {
            var entry = outputEntries.get(i);
            if (entry.isEmpty()) continue;
            var pos = slots.get(slotOffsets.outputStart() + i);
            root.child(
              adapter.wrap(Widgets.createSlot(new Point(0, 0)).entry(entry.get(0)).markOutput())
                .positioning(Positioning.absolute(pos.x() - offsetX, pos.y() - offsetY)));
        }
        
        // data
        var duration = String.format("%.0f", display.getEntry().value().getTime() / 20f);
        root.child(
          Components.label(Component.translatable("rei.title.oritech.cookingtime", duration, display.getEntry().value().getTime())).lineHeight(7)
            .positioning(Positioning.relative(90, 100))
        );
        
        // fluids
        if (display.entry.value().getFluidInput() != null && display.entry.value().getFluidInput().amount() > 0) {
            var fluidInput = display.entry.value().getFluidInput();
            
            root.child(rearth.oritech.client.ui.BasicMachineScreen.createFluidRenderer(fluidInput.getFluidStacks().getFirst(), new ScreenProvider.BarConfiguration(4, 5, 16, 50)));
            
            
            var text = fluidInput.amount() > 0
                ? Component.translatable("tooltip.oritech.fluid_content", fluidInput.amount(), fluidInput.name())
                : Component.translatable("tooltip.oritech.fluid_empty");

            if (fluidInput.hasTag()) {
                var joiner = new StringJoiner(", ", "\n", "");
                joiner.setEmptyValue("");
                for (var fluidStack : fluidInput.getFluidStacks()) {
                    joiner.add(fluidStack.getName().getString());
                }
                var fluidsText = MutableComponent.create(Component.nullToEmpty(Component.nullToEmpty(joiner.toString()).getString(40)).getContents()).withColor(BasicMachineScreen.GRAY_TEXT_COLOR);
                text.append(fluidsText);
            }

            var foreGround = Components.texture(GUI_COMPONENTS, 48, 0, 14, 50, 98, 96);
            foreGround.sizing(Sizing.fixed(18), Sizing.fixed(52));
            foreGround.positioning(Positioning.absolute(3, 4));
            foreGround.tooltip(text);
            root.child(foreGround);
        }
        
        if (!display.entry.value().getFluidOutputs().isEmpty()) {
            var tankCount = 0;
            var tankStartX = display.entry.value().getFluidOutputs().size() > 1 ? 80 : 120;
            for (var fluidResult : display.entry.value().getFluidOutputs()) {
                if (fluidResult.isEmpty()) continue;
                
                var amount = fluidResult.getAmount();
                root.child(rearth.oritech.client.ui.BasicMachineScreen.createFluidRenderer(fluidResult, new ScreenProvider.BarConfiguration(tankStartX + tankCount * 20 + 1, 5, 16, 50)));
                
                var text = amount > 0
                             ? Component.translatable("tooltip.oritech.fluid_content", amount, FluidStackHooks.getName(fluidResult).getString())
                             : Component.translatable("tooltip.oritech.fluid_empty");
                var foreGround = Components.texture(GUI_COMPONENTS, 48, 0, 14, 50, 98, 96);
                foreGround.sizing(Sizing.fixed(18), Sizing.fixed(52));
                foreGround.positioning(Positioning.absolute(tankStartX + tankCount * 20, 4));
                foreGround.tooltip(text);
                root.child(foreGround);
                
                tankCount++;
            }
        }
        
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
