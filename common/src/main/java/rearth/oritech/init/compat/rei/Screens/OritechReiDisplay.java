package rearth.oritech.init.compat.rei.Screens;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.client.ui.OritechMachineScreen;
import rearth.oritech.init.compat.rei.OritechDisplay;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class OritechReiDisplay implements DisplayCategory<Display> {
    
    protected final OritechRecipeType recipeType;
    private final boolean isGenerator;
    private final List<ScreenProvider.GuiSlot> slots;
    private final InventorySlotAssignment slotOffsets;
    protected final ItemLike icon;
    private final ScreenProvider.ArrowConfiguration indicatorConfig;
    
    private static final int OFFSET_X = 23;
    private static final int OFFSET_Y = 15;
    
    public OritechReiDisplay(OritechRecipeType recipeType, Class<? extends MachineBlockEntity> screenProviderSource, ItemLike icon) {
        var blockState = (icon instanceof Block block) ? block.defaultBlockState() : Blocks.STONE.defaultBlockState();
        
        this.recipeType = recipeType;
        this.icon = icon;
        
        try {
            var screenProvider = screenProviderSource.getDeclaredConstructor(BlockPos.class, BlockState.class)
                .newInstance(new BlockPos(0, 0, 0), blockState);
            this.isGenerator = screenProvider instanceof UpgradableGeneratorBlockEntity;
            this.slots = screenProvider.getGuiSlots();
            this.slotOffsets = screenProvider.getSlotAssignments();
            this.indicatorConfig = screenProvider.getIndicatorConfiguration();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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
        var widgets = new ArrayList<Widget>();
        var oDisplay = (OritechDisplay) display;
        var recipe = oDisplay.getEntry().value();
        var x = bounds.x;
        var y = bounds.y;
        
        // background
        widgets.add(Widgets.createRecipeBase(bounds));
        
        // input slots
        var inputEntries = display.getInputEntries();
        for (int i = 0; i < inputEntries.size(); i++) {
            var entry = inputEntries.get(i);
            if (entry.isEmpty()) continue;
            var pos = slots.get(slotOffsets.inputStart() + i);
            var usedY = Math.clamp(pos.y() - OFFSET_Y, 2, getDisplayHeight() - 18 - 4);
            widgets.add(Widgets.createSlot(new Point(x + pos.x() - OFFSET_X, y + usedY + 3)).entries(entry).markInput());
        }
        
        // arrow / fire indicator
        var indicatorPoint = new Point(x + indicatorConfig.x() - OFFSET_X, y + indicatorConfig.y() + 3 - OFFSET_Y);
        if (isGenerator) {
            widgets.add(Widgets.createBurningFire(indicatorPoint));
        } else {
            widgets.add(Widgets.createArrow(indicatorPoint));
        }
        
        // output slots
        var outputEntries = display.getOutputEntries();
        for (int i = 0; i < outputEntries.size(); i++) {
            var entry = outputEntries.get(i);
            if (entry.isEmpty()) continue;
            var pos = slots.get(slotOffsets.outputStart() + i);
            widgets.add(Widgets.createSlot(new Point(x + pos.x() - OFFSET_X, y + pos.y() + 3 - OFFSET_Y)).entry(entry.get(0)).markOutput());
        }
        
        // cooking time label
        var duration = String.format("%.0f", recipe.getTime() / 20f);
        widgets.add(Widgets.createLabel(
            new Point(x + (int) (bounds.width * 0.45), y + bounds.height - 12),
            Component.translatable("rei.title.oritech.cookingtime", duration, recipe.getTime())
        ).color(0xFFFFFF).noShadow());
        
        // fluid input
        if (recipe.getFluidInput() != null && recipe.getFluidInput().amount() > 0) {
            var fluidInput = recipe.getFluidInput();
            var fluidEntries = EntryIngredient.builder();
            for (var fluidStack : fluidInput.getFluidStacks()) {
                fluidEntries.add(EntryStacks.of(fluidStack.getFluid(), fluidStack.getAmount() / 81));
            }
            widgets.add(Widgets.createSlot(new Rectangle(x + 4, y + 5 + 7, 12, 48)).entries(fluidEntries.build()).markInput());
            
            // tank frame
            widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) ->
                graphics.blit(OritechMachineScreen.GUI_COMPONENTS, x + 3, y + 4 + 7, 48, 0, 14, 50, 98, 96)));
        }
        
        // fluid outputs
        if (!recipe.getFluidOutputs().isEmpty()) {
            var tankCount = 0;
            var tankStartX = recipe.getFluidOutputs().size() > 1 ? 80 : 120;
            for (var fluidResult : recipe.getFluidOutputs()) {
                if (fluidResult.isEmpty()) continue;
                
                var tankX = x + tankStartX + tankCount * 20;
                widgets.add(Widgets.createSlot(new Rectangle(tankX + 1, y + 5 + 7, 12, 48))
                    .entry(EntryStacks.of(fluidResult.getFluid(), fluidResult.getAmount() / 81)).markOutput());
                
                // tank frame
                final int finalTankX = tankX;
                widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) ->
                    graphics.blit(OritechMachineScreen.GUI_COMPONENTS, finalTankX, y + 4 + 7, 48, 0, 14, 50, 98, 96)));
                
                tankCount++;
            }
        }
        
        return widgets;
    }
    
    @Override
    public int getDisplayHeight() {
        return 78;
    }
    
//    @Override
//    public int getDisplayWidth(Display display) {
//        return DisplayCategory.super.getDisplayWidth(display);
//    return 170;
//    }
    
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
