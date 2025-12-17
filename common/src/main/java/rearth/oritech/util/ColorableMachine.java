package rearth.oritech.util;

import net.minecraft.nbt.CompoundTag;

public interface ColorableMachine {
    
    enum ColorVariant {
        ORANGE,
        DIAMOND,
        CAMO,
        FLUXITE,
        WHITE,
        INDUSTRIAL,
        NETHERITE,
        REDSTONE,
        SCULK
    }
    
    default boolean supportRecoloring() {
        return true;
    }
    
    ColorVariant getCurrentColor();
    
    void assignColor(ColorVariant color);
    
    default ColorVariant getDefaultColor() {
        return ColorVariant.ORANGE;
    }
    
    default void addColorToNbt(CompoundTag tag) {
        tag.putShort("color", (short) this.getCurrentColor().ordinal());
    }
    
    default void loadColorFromNbt(CompoundTag tag) {
        if (tag.contains("color")) {
            var color = tag.getShort("color");
            this.assignColor(ColorVariant.values()[color]);
        }
    }
}