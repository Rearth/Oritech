package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphics;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ColorHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A modal overlay that renders content centered on screen, with a translucent background.
 * Can be dismissed by clicking outside the content area with a custom onDismiss runnable.
 */
public class OverlayWidget extends UIComponent {
    
    private final List<UIComponent> children = new ArrayList<>();
    private Runnable onDismiss;
    private int bgColor = ColorHelper.argb(0f, 0f, 0f, 0.5f);
    private boolean consumeOutsideClicks = true;
    private boolean consumeOutsideScroll = true;
    
    public OverlayWidget(int screenWidth, int screenHeight) {
        super(0, 0, screenWidth, screenHeight);
        this.zIndex = 9000;
    }
    
    public void addChild(UIComponent child) {
        children.add(child);
    }
    
    public List<UIComponent> getChildren() {
        return children;
    }
    
    public OverlayWidget withDismissHandler(Runnable onDismiss) {
        this.onDismiss = onDismiss;
        return this;
    }
    
    public OverlayWidget withBackgroundColor(int argb) {
        this.bgColor = argb;
        return this;
    }

    public OverlayWidget withConsumeOutsideClicks(boolean consumeOutsideClicks) {
        this.consumeOutsideClicks = consumeOutsideClicks;
        return this;
    }

    public OverlayWidget withConsumeOutsideScroll(boolean consumeOutsideScroll) {
        this.consumeOutsideScroll = consumeOutsideScroll;
        return this;
    }
    
    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        // Dispatch to children first
        for (var child : children) {
            if (child.isVisible() && child.isMouseOver(mouseX, mouseY) && child.handleClick(mouseX, mouseY, button))
                return true;
        }
        // Check if click was inside any child content area
        for (var child : children) {
            if (child.isVisible() && child.isMouseOver(mouseX, mouseY))
                return true;
        }
        // Clicked outside content — dismiss
        if (onDismiss != null) onDismiss.run();
        return consumeOutsideClicks;
    }
    
    @Override
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        for (var child : children) {
            if (child.isVisible() && child.handleMouseScroll(mouseX, mouseY, scrollDelta))
                return true;
        }
        return consumeOutsideScroll;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Translucent background
        graphics.fill(x, y, x + width, y + height, bgColor);
        
        var sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIComponent::getZIndex));
        for (var child : sorted) {
            if (child.isVisible())
                child.render(graphics, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public void tick() {
        for (var child : children)
            child.tick();
    }
    
    public UIComponent getTopmostHovered(double mouseX, double mouseY) {
        UIComponent result = null;
        for (var child : children) {
            if (child.isVisible() && child.isMouseOver(mouseX, mouseY) && child.hasTooltip()) {
                if (result == null || child.getZIndex() > result.getZIndex())
                    result = child;
            }
        }
        return result;
    }
}
