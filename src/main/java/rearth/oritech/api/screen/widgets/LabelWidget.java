package rearth.oritech.api.screen.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ColorHelper;

/**
 * Renders a text label. Supports color, horizontal alignment, and text shadow.
 */
public class LabelWidget extends UIComponent {
    
    public static int BRIGHT_TEXT = ColorHelper.argb(0.95f, 0.97f, 0.99f);
    public static int DARK_TEXT = ColorHelper.argb(0.25f, 0.25f, 0.25f);
    
    public enum Alignment { LEFT, CENTER, RIGHT }
    
    private Component text;
    private int color = BRIGHT_TEXT;
    private boolean shadow = false;
    private Alignment alignment = Alignment.LEFT;
    private boolean wrap = false;
    
    public LabelWidget(int x, int y, int width, int height, Component text) {
        super(x, y, width, height);
        this.text = text;
    }
    
    public static int getTextWidth(Component text) {
        var font = Minecraft.getInstance().font;
        return font.width(text);
    }
    
    /**
     * Auto-height label: height defaults to standard font line height (9px).
     */
    public LabelWidget(int x, int y, int width, Component text) {
        this(x, y, width, 9, text);
    }
    
    public Component getText() { return text; }
    
    public void setText(Component text) { this.text = text; }
    
    public LabelWidget withColor(int color) {
        this.color = color;
        return this;
    }
    public LabelWidget withBrightColor() {
        this.color = BRIGHT_TEXT;
        return this;
    }
    public LabelWidget withDarkColor() {
        this.color = DARK_TEXT;
        return this;
    }
    
    public LabelWidget withShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }
    
    public LabelWidget withAlignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }
    
    public LabelWidget withWrap(boolean wrap) {
        this.wrap = wrap;
        return this;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        
        if (wrap) {
            var lines = font.split(text, cw);
            for (int i = 0; i < lines.size(); i++) {
                var line = lines.get(i);
                int textX = switch (alignment) {
                    case LEFT -> cx;
                    case CENTER -> cx + (cw - font.width(line)) / 2;
                    case RIGHT -> cx + cw - font.width(line);
                };
                graphics.drawString(font, line, textX, cy + i * font.lineHeight, color, shadow);
            }
        } else {
            int textX = switch (alignment) {
                case LEFT -> cx;
                case CENTER -> cx + (cw - font.width(text)) / 2;
                case RIGHT -> cx + cw - font.width(text);
            };
            graphics.drawString(font, text, textX, cy, color, shadow);
        }
    }
}
