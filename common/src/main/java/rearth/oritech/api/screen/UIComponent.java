package rearth.oritech.api.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all UI widgets. Each widget has an absolute position, size,
 * optional 9-patch surface background, padding, tooltip, and z-index.
 * <p>
 * The rendering order is: surface background → content (inside padded area).
 * Tooltip rendering is handled by the parent screen.
 */
public abstract class UIComponent {
    
    protected int x, y, width, height;
    protected Insets padding = Insets.NONE;
    protected OritechSurface surface = OritechSurface.NONE;
    protected int zIndex = 0;
    protected boolean visible = true;
    
    private List<Component> tooltip;
    
    public UIComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Renders this component: surface background first, then content.
     * Padding expands the surface outward from the content bounds.
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        
        if (!surface.isNone()) {
            surface.render(graphics,
                x - padding.left(), y - padding.top(),
                width + padding.horizontal(), height + padding.vertical());
        }
        
        renderContent(graphics, mouseX, mouseY, delta);
    }
    
    /**
     * Renders the widget's actual content inside the padded area.
     * Use {@link #contentX()}, {@link #contentY()}, {@link #contentWidth()}, {@link #contentHeight()}
     * for the available content region.
     */
    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta);
    
    /**
     * Called every screen tick (~20 times/second) for animations or data updates.
     */
    public void tick() {}
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        int px = x - padding.left();
        int py = y - padding.top();
        int pw = width + padding.horizontal();
        int ph = height + padding.vertical();
        return mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + ph;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Insets getPadding() { return padding; }
    public OritechSurface getSurface() { return surface; }
    public int getZIndex() { return zIndex; }
    public boolean isVisible() { return visible; }
    
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public void setPadding(Insets padding) { this.padding = padding; }
    public void setSurface(OritechSurface surface) { this.surface = surface; }
    public void setZIndex(int zIndex) { this.zIndex = zIndex; }
    public void setVisible(boolean visible) { this.visible = visible; }
    
    // Content area helpers (with outward padding, content = widget bounds)
    public int contentX() { return x; }
    public int contentY() { return y; }
    public int contentWidth() { return width; }
    public int contentHeight() { return height; }
    
    // Padded (total visual) area helpers
    public int paddedX() { return x - padding.left(); }
    public int paddedY() { return y - padding.top(); }
    public int paddedWidth() { return width + padding.horizontal(); }
    public int paddedHeight() { return height + padding.vertical(); }
    
    // Fluent setters
    public UIComponent withSurface(OritechSurface surface) {
        this.surface = surface;
        return this;
    }
    
    public UIComponent withPadding(Insets padding) {
        this.padding = padding;
        return this;
    }
    
    public UIComponent withZIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }
    
    public UIComponent withTooltip(Component... lines) {
        this.tooltip = splitNewlines(List.of(lines));
        return this;
    }
    
    public UIComponent withTooltip(List<Component> lines) {
        this.tooltip = splitNewlines(lines);
        return this;
    }
    
    public UIComponent addTooltipLine(Component line) {
        if (this.tooltip == null) this.tooltip = new ArrayList<>();
        this.tooltip.addAll(splitNewlines(List.of(line)));
        return this;
    }
    
    public List<Component> getTooltip() {
        return tooltip;
    }
    
    public void setTooltip(List<Component> tooltip) {
        this.tooltip = tooltip != null ? splitNewlines(tooltip) : null;
    }
    
    private static List<Component> splitNewlines(List<Component> lines) {
        var result = new ArrayList<Component>();
        for (var line : lines) {
            var str = line.getString();
            if (str.contains("\n")) {
                for (var part : str.split("\n", -1))
                    result.add(Component.literal(part));
            } else {
                result.add(line);
            }
        }
        return result;
    }
    
    public boolean hasTooltip() {
        return tooltip != null && !tooltip.isEmpty();
    }
    
    // mouse events (override in interactive widgets)
    
    public boolean handleClick(double mouseX, double mouseY, int button) { return false; }
    public boolean handleDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) { return false; }
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) { return false; }
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) { return false; }
}
