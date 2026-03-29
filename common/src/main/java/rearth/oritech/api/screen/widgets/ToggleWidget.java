package rearth.oritech.api.screen.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ColorHelper;

import java.util.function.BiConsumer;

/**
 * UIComponent-based toggle switch with texture and label.
 * Replaces the vanilla AbstractButton-based OritechToggleButton.
 */
public class ToggleWidget extends UIComponent {
    
    private static final ResourceLocation TOGGLE_ON = Oritech.id("textures/gui/modular/toggle_on.png");
    private static final ResourceLocation TOGGLE_ON_HOVER = Oritech.id("textures/gui/modular/toggle_on_hover.png");
    private static final ResourceLocation TOGGLE_OFF = Oritech.id("textures/gui/modular/toggle_off.png");
    private static final ResourceLocation TOGGLE_OFF_HOVER = Oritech.id("textures/gui/modular/toggle_off_hover.png");
    
    private static final int TOGGLE_WIDTH = 30;
    private static final int TOGGLE_HEIGHT = 16;
    private static final int LABEL_GAP = 4;
    private static final int DEFAULT_TEXT_COLOR = ColorHelper.argb(0.2f, 0.2f, 0.3f);
    private static final int DISABLED_TEXT_COLOR = ColorHelper.argb(0.47f, 0.47f, 0.47f);
    
    private final BiConsumer<ToggleWidget, Boolean> onToggle;
    private Component label;
    private boolean value;
    private boolean active = true;
    private int textColor = DEFAULT_TEXT_COLOR;
    private boolean textShadow;
    
    public ToggleWidget(int x, int y, int width, Component label, boolean value, BiConsumer<ToggleWidget, Boolean> onToggle) {
        super(x, y, width, TOGGLE_HEIGHT);
        this.label = label;
        this.value = value;
        this.onToggle = onToggle;
    }
    
    public static ToggleWidget of(int x, int y, Component label, boolean value, BiConsumer<ToggleWidget, Boolean> onToggle) {
        var font = Minecraft.getInstance().font;
        int w = TOGGLE_WIDTH + LABEL_GAP + font.width(label);
        return new ToggleWidget(x, y, w, label, value, onToggle);
    }
    
    public ToggleWidget withTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }
    
    public ToggleWidget withTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }
    
    public boolean getValue() { return value; }
    public void setValue(boolean value) { this.value = value; }
    public void setLabel(Component label) { this.label = label; }
    public void setActive(boolean active) { this.active = active; }
    
    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!active || button != 0) return false;
        value = !value;
        onToggle.accept(this, value);
        return true;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        
        ResourceLocation texture;
        if (value) texture = hovered ? TOGGLE_ON_HOVER : TOGGLE_ON;
        else texture = hovered ? TOGGLE_OFF_HOVER : TOGGLE_OFF;
        
        graphics.blit(texture, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT,
            0, 0, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_WIDTH, TOGGLE_HEIGHT);
        
        var font = Minecraft.getInstance().font;
        int textX = x + TOGGLE_WIDTH + LABEL_GAP;
        int textY = y + (height - 8) / 2;
        graphics.drawString(font, label, textX, textY, active ? textColor : DISABLED_TEXT_COLOR, textShadow);
    }
}
