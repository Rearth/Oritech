package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphics;
import rearth.oritech.api.screen.UIComponent;

/**
 * Renders a filled or outlined colored rectangle.
 */
public class BoxWidget extends UIComponent {
    
    private int color;
    private boolean filled;
    
    public BoxWidget(int x, int y, int width, int height, int color, boolean filled) {
        super(x, y, width, height);
        this.color = color;
        this.filled = filled;
    }
    
    public static BoxWidget filled(int x, int y, int width, int height, int argbColor) {
        return new BoxWidget(x, y, width, height, argbColor, true);
    }
    
    public static BoxWidget outline(int x, int y, int width, int height, int argbColor) {
        return new BoxWidget(x, y, width, height, argbColor, false);
    }
    
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    
    public boolean isFilled() { return filled; }
    public void setFilled(boolean filled) { this.filled = filled; }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();
        
        if (filled) {
            graphics.fill(cx, cy, cx + cw, cy + ch, color);
        } else {
            // Top
            graphics.fill(cx, cy, cx + cw, cy + 1, color);
            // Bottom
            graphics.fill(cx, cy + ch - 1, cx + cw, cy + ch, color);
            // Left
            graphics.fill(cx, cy, cx + 1, cy + ch, color);
            // Right
            graphics.fill(cx + cw - 1, cy, cx + cw, cy + ch, color);
        }
    }
}
