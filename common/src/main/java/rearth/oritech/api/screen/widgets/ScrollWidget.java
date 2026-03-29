package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ColorHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A scrollable container that clips its children to a viewport.
 * Supports vertical scrolling with mouse wheel; horizontal with shift+wheel.
 */
public class ScrollWidget extends UIComponent {
    
    private static final int SCROLLBAR_TRACK = ColorHelper.argb(0.6f, 0.65f, 0.7f, 0.25f);
    private static final int SCROLLBAR_THUMB = ColorHelper.argb(0.6f, 0.65f, 0.7f, 0.5f);
    
    private final List<UIComponent> children = new ArrayList<>();
    private int scrollX = 0;
    private int scrollY = 0;
    private int contentTotalWidth = 0;
    private int contentTotalHeight = 0;
    private boolean verticalScroll = true;
    private boolean horizontalScroll = false;
    private int scrollSpeed = 10;
    private int innerMargin = 4;
    
    public ScrollWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.surface = OritechSurface.PANEL;
    }
    
    public ScrollWidget withVerticalScroll(boolean enabled) {
        this.verticalScroll = enabled;
        return this;
    }
    
    public ScrollWidget withHorizontalScroll(boolean enabled) {
        this.horizontalScroll = enabled;
        return this;
    }
    
    public ScrollWidget withScrollSpeed(int speed) {
        this.scrollSpeed = speed;
        return this;
    }
    
    public void addChild(UIComponent child) {
        children.add(child);
    }
    
    public List<UIComponent> getChildren() {
        return children;
    }
    
    // Set total content dimensions (used for scroll bounds calculation)
    public void setContentDimensions(int totalWidth, int totalHeight) {
        this.contentTotalWidth = totalWidth;
        this.contentTotalHeight = totalHeight;
    }
    
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta, boolean shiftHeld) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        
        int viewW = width - innerMargin * 2;
        int viewH = height - innerMargin * 2;
        
        int delta = (int) (scrollDelta * scrollSpeed);
        if (horizontalScroll && (shiftHeld || !verticalScroll)) {
            scrollX = Mth.clamp(scrollX - delta, 0, Math.max(0, contentTotalWidth - viewW));
        } else if (verticalScroll) {
            scrollY = Mth.clamp(scrollY - delta, 0, Math.max(0, contentTotalHeight - viewH));
        }
        return true;
    }
    
    @Override
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        return handleMouseScroll(mouseX, mouseY, scrollDelta, net.minecraft.client.gui.screens.Screen.hasShiftDown());
    }
    
    public int getScrollX() { return scrollX; }
    public int getScrollY() { return scrollY; }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Inner viewport with margin inside the surface
        int cx = x + innerMargin;
        int cy = y + innerMargin;
        int cw = width - innerMargin * 2;
        int ch = height - innerMargin * 2;
        
        // enableScissor uses GUI coords, not transformed coords — offset by pose translation
        Matrix4f matrix = graphics.pose().last().pose();
        int offsetX = (int) matrix.m30();
        int offsetY = (int) matrix.m31();
        graphics.enableScissor(cx + offsetX, cy + offsetY, cx + cw + offsetX, cy + ch + offsetY);
        
        var sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIComponent::getZIndex));
        
        // Translate so children at (0,0) render at the content origin
        graphics.pose().pushPose();
        graphics.pose().translate(cx - scrollX, cy - scrollY, 0);
        
        // Mouse coords relative to content origin + scroll offset
        int childMouseX = mouseX - cx + scrollX;
        int childMouseY = mouseY - cy + scrollY;
        
        for (var child : sorted) {
            if (child.isVisible())
                child.render(graphics, childMouseX, childMouseY, delta);
        }
        
        graphics.pose().popPose();
        graphics.disableScissor();
        
        // Scrollbar indicator
        if (verticalScroll && contentTotalHeight > ch) {
            renderScrollbar(graphics, cx + cw, cy, 2, ch, scrollY, contentTotalHeight, ch);
        }
    }
    
    private void renderScrollbar(GuiGraphics graphics, int barX, int barY, int barW, int trackH, int scroll, int totalContent, int viewportH) {
        float thumbRatio = (float) viewportH / totalContent;
        int thumbH = Math.max(8, (int) (trackH * thumbRatio));
        float scrollRatio = (float) scroll / (totalContent - viewportH);
        int thumbY = barY + (int) ((trackH - thumbH) * scrollRatio);
        
        graphics.fill(barX, barY, barX + barW, barY + trackH, SCROLLBAR_TRACK);
        graphics.fill(barX, thumbY, barX + barW, thumbY + thumbH, SCROLLBAR_THUMB);
    }
    
    @Override
    public void tick() {
        for (var child : children)
            child.tick();
    }
    
    public UIComponent getTopmostHovered(double mouseX, double mouseY) {
        UIComponent result = null;
        double adjX = mouseX - (x + innerMargin) + scrollX;
        double adjY = mouseY - (y + innerMargin) + scrollY;
        for (var child : children) {
            if (child.isVisible() && child.isMouseOver(adjX, adjY) && child.hasTooltip()) {
                if (result == null || child.getZIndex() > result.getZIndex())
                    result = child;
            }
        }
        return result;
    }
}
