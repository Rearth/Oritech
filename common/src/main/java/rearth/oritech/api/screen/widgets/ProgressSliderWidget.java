package rearth.oritech.api.screen.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ColorHelper;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Bedrock-styled labeled progress bar with quarter markers and an end-cap marker.
 */
public class ProgressSliderWidget extends UIComponent {
    
    public static final int PRESET_GREEN = -12810969;
    public static final int PRESET_ORANGE = -1012726;
    public static final int PRESET_RED = ColorHelper.argb(0.83f, 0.31f, 0.29f);
    public static final int PRESET_BLUE = ColorHelper.argb(0.31f, 0.60f, 0.85f);
    public static final int PRESET_GRAY = ColorHelper.argb(0.60f, 0.62f, 0.65f);
    
    public static final int DEFAULT_TEXT_COLOR = LabelWidget.BRIGHT_TEXT;
    public static final int BAR_OUTLINE = ColorHelper.argb(0.12f, 0.12f, 0.13f);
    public static final int BAR_BACKGROUND = ColorHelper.argb(0.22f, 0.22f, 0.24f);
    public static final int BAR_MARKER = -3092012;
    public static final int BAR_END_MARKER = -526345;
    
    private static final int DEFAULT_HEIGHT = 17;
    private static final int DEFAULT_BAR_HEIGHT = 6;
    private static final int TITLE_BAR_GAP = 2;
    
    private Component title;
    private Supplier<Float> fillSupplier;
    private IntSupplier fillColorSupplier;
    private int textColor = DEFAULT_TEXT_COLOR;
    private boolean textShadow;
    private boolean pulsing;
    
    public ProgressSliderWidget(int x, int y, int width, Component title,
                                Supplier<Float> fillSupplier, IntSupplier fillColorSupplier) {
        this(x, y, width, DEFAULT_HEIGHT, title, fillSupplier, fillColorSupplier);
    }
    
    public ProgressSliderWidget(int x, int y, int width, int height, Component title,
                                Supplier<Float> fillSupplier, IntSupplier fillColorSupplier) {
        super(x, y, width, height);
        this.title = title;
        this.fillSupplier = fillSupplier;
        this.fillColorSupplier = fillColorSupplier;
    }
    
    public Component getTitle() {
        return title;
    }
    
    public void setTitle(Component title) {
        this.title = title;
    }
    
    public Supplier<Float> getFillSupplier() {
        return fillSupplier;
    }
    
    public void setFillSupplier(Supplier<Float> fillSupplier) {
        this.fillSupplier = fillSupplier;
    }
    
    public IntSupplier getFillColorSupplier() {
        return fillColorSupplier;
    }
    
    public void setFillColorSupplier(IntSupplier fillColorSupplier) {
        this.fillColorSupplier = fillColorSupplier;
    }
    
    public ProgressSliderWidget withTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }
    
    public ProgressSliderWidget withTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }
    
    public ProgressSliderWidget withPulsing(boolean pulsing) {
        this.pulsing = pulsing;
        return this;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();
        
        int titleRowHeight = font.lineHeight;
        int barHeight = 7;
        int barY = cy + Math.max(0, ch - barHeight);
        int titleY = cy + Math.max(0, (titleRowHeight - font.lineHeight) / 2);
        
        if (title != null) {
            graphics.drawString(font, title, cx, titleY, textColor, textShadow);
        }
        
        renderBar(graphics, cx, barY, cw, barHeight);
    }
    
    private void renderBar(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 1 || height <= 1) {
            return;
        }
        
        graphics.fill(x, y, x + width, y + height, BAR_OUTLINE);
        
        int innerX = x + 1;
        int innerY = y + 1;
        int innerWidth = width - 2;
        int innerHeight = height - 2;
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }
        
        graphics.fill(innerX, innerY, innerX + innerWidth, innerY + innerHeight, BAR_BACKGROUND);
        
        float fill = getFillAmount();
        if (fill > 0f) {
            int filledWidth = Mth.clamp(Math.round(innerWidth * fill), 1, innerWidth);
            int fillColor = fillColorSupplier != null ? fillColorSupplier.getAsInt() : PRESET_GREEN;
            graphics.fill(innerX, innerY, innerX + filledWidth, innerY + innerHeight, fillColor);
            
            int endMarkerX = innerX + Mth.clamp((int) Math.floor((innerWidth) * fill), 0, innerWidth - 1);
            var endMarkerColor = BAR_END_MARKER;
            if (pulsing) {
                var time = System.currentTimeMillis();
                var brightness = Math.min(1, Math.sin(time / 100d) * 0.3d + 0.95f);
                endMarkerColor = ColorHelper.argb((float) brightness - 0.02f, (float) brightness - 0.02f, (float) brightness);
            }
            graphics.fill(endMarkerX, innerY, endMarkerX + 1, innerY + innerHeight, endMarkerColor);
        }
        
        drawMarker(graphics, innerX, innerY + 1, innerWidth, innerHeight - 2, 0.25f);
        drawMarker(graphics, innerX, innerY + 1, innerWidth, innerHeight - 2, 0.5f);
        drawMarker(graphics, innerX, innerY + 1, innerWidth, innerHeight - 2, 0.75f);
    }
    
    private void drawMarker(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
        if (width <= 0 || height <= 0) {
            return;
        }
        
        int markerX = x + Mth.clamp(Math.round((width - 1) * progress), 0, width - 1);
        graphics.fill(markerX, y, markerX + 1, y + height, BAR_MARKER);
    }
    
    private float getFillAmount() {
        if (fillSupplier == null) {
            return 0f;
        }
        
        Float suppliedFill = fillSupplier.get();
        if (suppliedFill == null || suppliedFill.isNaN() || suppliedFill.isInfinite()) {
            return 0f;
        }
        
        return Mth.clamp(suppliedFill, 0f, 1f);
    }
}