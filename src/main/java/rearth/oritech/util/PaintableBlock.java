package rearth.oritech.util;

import net.minecraft.world.level.block.state.properties.EnumProperty;
import rearth.oritech.util.ColorableMachine.ColorVariant;

public interface PaintableBlock {
    EnumProperty<ColorVariant> COLOR = EnumProperty.create("color", ColorVariant.class);
}
