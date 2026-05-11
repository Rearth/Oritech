package rearth.oritech.api.screen;

public record Insets(int top, int right, int bottom, int left) {
    
    public static final Insets NONE = new Insets(0, 0, 0, 0);
    
    public static Insets of(int all) {
        return new Insets(all, all, all, all);
    }
    
    public static Insets of(int vertical, int horizontal) {
        return new Insets(vertical, horizontal, vertical, horizontal);
    }
    
    public static Insets of(int top, int right, int bottom, int left) {
        return new Insets(top, right, bottom, left);
    }
    
    public int horizontal() {
        return left + right;
    }
    
    public int vertical() {
        return top + bottom;
    }
}
