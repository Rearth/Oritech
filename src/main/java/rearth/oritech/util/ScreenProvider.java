package rearth.oritech.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
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

    default BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(7, 24, 15, 54);
    }
    default BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(146, 6, 21, 74);
    }
    
    default ArrowConfiguration getIndicatorConfiguration() {
        return new ArrowConfiguration(
                new Identifier(Oritech.MOD_ID, "textures/gui/modular/arrow_empty.png"),
                new Identifier(Oritech.MOD_ID, "textures/gui/modular/arrow_full.png"),
                84, 30, 7, 26, false);
    }

}
