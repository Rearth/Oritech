package rearth.oritech.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import rearth.oritech.Oritech;

import java.util.List;

public interface ScreenProvider {
    
    record GuiSlot (int index, int x, int y) {}

    record BarConfiguration(int x, int y, int width, int height) {}

    record ArrowConfiguration(Identifier empty, Identifier full, int x, int y, int width, int height, boolean horizontal) {}

    List<GuiSlot> getGuiSlots();

    default boolean showEnergy() {
        return true;
    }
    
    float getDisplayedEnergyUsage();

    float getProgress();
    
    InventoryInputMode getInventoryInputMode();
    Inventory getDisplayedInventory();
    
    ScreenHandlerType<?> getScreenHandlerType();
    
    default boolean inputOptionsEnabled() {return true;}
    
    default Property<Direction> getBlockFacingProperty() {
        return Properties.HORIZONTAL_FACING;
    }
    
    default boolean showProgress() {
        return true;
    }

    default BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(7, 24, 15, 54);
    }
    default BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(147, 6, 21, 74);
    }
    
    default ArrowConfiguration getIndicatorConfiguration() {
        return new ArrowConfiguration(
                new Identifier(Oritech.MOD_ID, "textures/gui/modular/arrow_empty.png"),
                new Identifier(Oritech.MOD_ID, "textures/gui/modular/arrow_full.png"),
                80, 35, 29, 16, true);
    }

}
