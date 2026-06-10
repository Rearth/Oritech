package rearth.oritech.util;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.fluids.FluidStack;

public final class ColorHelper {

    public static int argb(float r, float g, float b) {
        return argb(r, g, b, 1f);
    }

    public static int argb(float r, float g, float b, float a) {
        return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static int makeOpaque(int argbColor) {
        return argbColor | 0xFF000000;
    }

    /**
     * Resolves the tint color (ARGB) of the given fluid using the data-driven fluid models introduced in NeoForge 26.1.
     * This replaces the removed {@code IClientFluidTypeExtensions#getTintColor}. Client-side only.
     *
     * @return the fluid's ARGB tint color, or {@link #WHITE} if the fluid has no tint source.
     */
    public static int getFluidTint(FluidStack stack) {
        var tintSource = Minecraft.getInstance().getModelManager()
                .getFluidStateModelSet()
                .get(stack.getFluid().defaultFluidState())
                .fluidTintSource();
        return tintSource != null ? tintSource.colorAsStack(stack) : WHITE;
    }

    public static final int WHITE = 0xFFFFFFFF;

    private ColorHelper() {
    }
}
