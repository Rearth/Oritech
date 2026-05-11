package rearth.oritech.api.screen.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;

import java.util.function.Consumer;

/**
 * UIComponent-based clickable button with nine-patch styling.
 * Replaces the vanilla AbstractButton-based OritechButton.
 */
public class ButtonWidget extends UIComponent {
    
    public static final int DEFAULT_TEXT_COLOR = 0xFF404040;
    public static final int DISABLED_COLOR = 0xFF787878;
    
    private final Consumer<ButtonWidget> onPress;
    private Component label;
    private OritechSurface normalSurface = OritechSurface.PANEL;
    private OritechSurface hoverSurface = OritechSurface.PANEL_HOVER;
    private OritechSurface pressedSurface = OritechSurface.PANEL_PRESSED;
    private OritechSurface disabledSurface = OritechSurface.PANEL_DARK;
    private Insets surfacePadding = Insets.NONE;
    private int textColor = DEFAULT_TEXT_COLOR;
    private boolean textShadow;
    private boolean active = true;
    private boolean hovered;
    private boolean pressed;
    
    public ButtonWidget(int x, int y, int width, int height, Component label, Consumer<ButtonWidget> onPress) {
        super(x, y, width, height);
        this.label = label;
        this.onPress = onPress;
    }
    
    public static ButtonWidget panel(int x, int y, int width, int height, Component label, Consumer<ButtonWidget> onPress) {
        return new ButtonWidget(x, y, width, height, label, onPress);
    }
    
    public static ButtonWidget darkPanel(int x, int y, int width, int height, Component label, Consumer<ButtonWidget> onPress) {
        var button = new ButtonWidget(x, y, width, height, label, onPress);
        button.normalSurface = OritechSurface.PANEL_DARK;
        button.hoverSurface = OritechSurface.PANEL_DARK_HOVER;
        button.disabledSurface = OritechSurface.PANEL_DARK;
        return button;
    }
    
    public ButtonWidget withTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }
    
    public ButtonWidget withTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }
    
    public ButtonWidget withSurfacePadding(Insets padding) {
        this.surfacePadding = padding;
        return this;
    }
    
    public void setLabel(Component label) { this.label = label; }
    public Component getLabel() { return label; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isActive() { return active; }
    
    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!active || button != 0) return false;
        pressed = true;
        onPress.accept(this);
        return true;
    }
    
    @Override
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (button == 0 && pressed) {
            pressed = false;
            return true;
        }
        return false;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        hovered = isMouseOver(mouseX, mouseY);
        if (pressed && !hovered) pressed = false;
        
        OritechSurface activeSurface;
        if (!active) activeSurface = disabledSurface;
        else if (pressed) activeSurface = pressedSurface;
        else if (hovered) activeSurface = hoverSurface;
        else activeSurface = normalSurface;
        
        activeSurface.render(graphics,
            x - surfacePadding.left(),
            y - surfacePadding.top(),
            width + surfacePadding.horizontal(),
            height + surfacePadding.vertical());
        
        var font = Minecraft.getInstance().font;
        int textY = y + (height - 8) / 2 + ((hovered && active) ? 1 : 0);
        int textX = x + (width - font.width(label)) / 2;
        graphics.drawString(font, label, textX, textY, active ? textColor : DISABLED_COLOR, textShadow);
    }
}
