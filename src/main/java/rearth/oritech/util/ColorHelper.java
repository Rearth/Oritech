package rearth.oritech.util;

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
    
    public static final int WHITE = 0xFFFFFFFF;
    
    private ColorHelper() {}
}
