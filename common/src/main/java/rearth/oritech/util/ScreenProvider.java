package rearth.oritech.util;

import rearth.oritech.Oritech;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public interface ScreenProvider {
    
    default List<Tuple<Component, Component>> getExtraExtensionLabels() {return List.of();}
    
    record GuiSlot (int index, int x, int y, boolean output) {
        public GuiSlot (int index, int x, int y) {
            this(index, x, y, false);
        }
    }

    record BarConfiguration(int x, int y, int width, int height) {}

    record ArrowConfiguration(ResourceLocation empty, ResourceLocation full, int x, int y, int width, int height, boolean horizontal) {}

    List<GuiSlot> getGuiSlots();

    default boolean showEnergy() {
        return true;
    }
    
    float getDisplayedEnergyUsage();
    
    default float getDisplayedEnergyTransfer() {
        return getDisplayedEnergyUsage() * 10;
    }

    float getProgress();
    
    InventoryInputMode getInventoryInputMode();
    Container getDisplayedInventory();
    
    MenuType<?> getScreenHandlerType();
    
    default boolean inputOptionsEnabled() {return true;}
    
    default Property<Direction> getBlockFacingProperty() {
        return BlockStateProperties.HORIZONTAL_FACING;
    }
    
    default boolean showProgress() {
        return true;
    }
    
    default boolean showArmor() {return false;}
    
    default boolean showExpansionPanel() {return true;}
    
    default boolean hasRedstoneControlAvailable() {return false;}
    
    default int receivedRedstoneSignal() {return 0;}
    
    default String currentRedstoneEffect() {return "";}

    default BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(7, 24, 15, 54);
    }
    default BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(147, 6, 21, 74);
    }
    
    default Optional<String> getWikiLink() { return Optional.empty(); } // optional override. Expected format is "interaction/enderic_laser". Defaults to the item link defined in the pages markdown.
    
    default ArrowConfiguration getIndicatorConfiguration() {
        return new ArrowConfiguration(
                Oritech.id("textures/gui/modular/arrow_empty.png"),
                Oritech.id("textures/gui/modular/arrow_full.png"),
                80, 35, 29, 16, true);
    }

}
