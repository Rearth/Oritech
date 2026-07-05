package rearth.oritech.util;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Locale;

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

    static Identifier getTextureForColor(Identifier base, ColorVariant color) {
        if (color.equals(ColorVariant.ORANGE)) return base;

        var colorFileSuffix = color.toString().toLowerCase(Locale.ROOT);
        return Identifier.fromNamespaceAndPath(base.getNamespace(), base.getPath().replace("models", "models/colored").replace(".png", "_" + colorFileSuffix + ".png"));
    }

    default void serializeColor(ValueOutput output) {
        output.putShort("color", (short) this.getCurrentColor().ordinal());
    }

    default void deserializeColor(ValueInput input) {
        var loaded = input.getShortOr("color", Short.MAX_VALUE);
        if (loaded != Short.MAX_VALUE)
            this.assignColor(ColorVariant.values()[loaded]);
    }
}
