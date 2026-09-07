package rearth.oritech.api.screen.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
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

    private static final int SCROLLBAR_TRACK = ColorHelper.argb(0.2f, 0.25f, 0.3f, 0.35f);
    private static final int SCROLLBAR_THUMB = ColorHelper.argb(0.2f, 0.25f, 0.3f, 0.9f);

    private final List<UIComponent> children = new ArrayList<>();
    private int scrollX = 0;
    private int scrollY = 0;
    private float renderedX = 0;    // basically the smoothed versions of scrollX/Y
    private float renderedY = 0;
    private int contentTotalWidth = 0;
    private int contentTotalHeight = 0;
    private boolean verticalScroll = true;
    private boolean horizontalScroll = false;
    private boolean dragScrolling = false;
    // Opt in only when children describe their drawing area with accurate bounds.
    private boolean renderCulling = false;
    // Small effects outside a child's surface should still appear near a viewport edge.
    private int renderCullOverflow = 0;
    private int scrollSpeed = 10;
    private final int innerMargin = 4;
    // Reuse the sorting buffer instead of copying every child on every frame.
    private final List<UIComponent> renderCandidates = new ArrayList<>();

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

    public ScrollWidget withDragScrolling(boolean enabled) {
        this.dragScrolling = enabled;
        return this;
    }

    /**
     * Skips children outside the viewport before sorting and rendering them.
     * The overflow allowance keeps effects that extend slightly past a component's bounds visible at an edge.
     */
    public ScrollWidget withRenderCulling(int overflow) {
        this.renderCulling = true;
        this.renderCullOverflow = Math.max(0, overflow);
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

    /** Restores a previously saved scroll position without replaying the smoothing animation. */
    public void setScrollPosition(float x, float y) {
        int viewW = width - innerMargin * 2;
        int viewH = height - innerMargin * 2;
        scrollX = Mth.clamp(Math.round(x), 0, Math.max(0, contentTotalWidth - viewW));
        scrollY = Mth.clamp(Math.round(y), 0, Math.max(0, contentTotalHeight - viewH));
        renderedX = scrollX;
        renderedY = scrollY;
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
        return handleMouseScroll(mouseX, mouseY, scrollDelta, Minecraft.getInstance().hasShiftDown());
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        double adjX = mouseX - (x + innerMargin) + scrollX;
        double adjY = mouseY - (y + innerMargin) + scrollY;

        var sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIComponent::getZIndex).reversed());

        for (var child : sorted) {
            if (child.isVisible() && child.isMouseOver(adjX, adjY) && child.handleClick(adjX, adjY, button)) {
                return true;
            }
        }

        for (var child : sorted) {
            if (child.isVisible() && child.isMouseOver(adjX, adjY)) {
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean handleDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (!dragScrolling || button != 0) return false;

        int viewW = width - innerMargin * 2;
        int viewH = height - innerMargin * 2;

        if (horizontalScroll) {
            scrollX = Mth.clamp((int) Math.round(scrollX - deltaX), 0, Math.max(0, contentTotalWidth - viewW));
        }

        if (verticalScroll) {
            scrollY = Mth.clamp((int) Math.round(scrollY - deltaY), 0, Math.max(0, contentTotalHeight - viewH));
        }

        return horizontalScroll || verticalScroll;
    }

    public float getScrollX() {
        return scrollX;
    }

    public float getScrollY() {
        return scrollY;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        renderedX = Mth.lerp(0.12f, renderedX, scrollX);
        renderedY = Mth.lerp(0.12f, renderedY, scrollY);

        // Inner viewport with margin inside the surface
        int cx = x + innerMargin;
        int cy = y + innerMargin;
        int cw = width - innerMargin * 2;
        int ch = height - innerMargin * 2;

        // enableScissor already applies the current GUI pose transform. Passing pose-adjusted
        // coordinates double-offsets nested scroll widgets, which clips their children out.
        var pose = graphics.pose();
        graphics.enableScissor(cx, cy, cx + cw, cy + ch);

        // Translate so children at (0,0) render at the content origin
        pose.pushMatrix();
        pose.translate(cx - renderedX, cy - renderedY);

        // Mouse coords relative to content origin + scroll offset
        float childMouseX = mouseX - cx + renderedX;
        float childMouseY = mouseY - cy + renderedY;

        renderCandidates.clear();
        for (var child : children) {
            if (child.isVisible() && (!renderCulling || isChildInViewport(child, renderedX, renderedY, cw, ch))) {
                renderCandidates.add(child);
            }
        }
        renderCandidates.sort(Comparator.comparingInt(UIComponent::getZIndex));

        for (var child : renderCandidates) {
            child.render(graphics, (int) childMouseX, (int) childMouseY, delta);
        }

        pose.popMatrix();
        graphics.disableScissor();

        // Scrollbar indicator
        if (verticalScroll && contentTotalHeight > ch) {
            renderVerticalScrollbar(graphics, cx + cw, cy, 2, ch, renderedY, contentTotalHeight, ch);
        }
        if (horizontalScroll && contentTotalWidth > cw) {
            renderHorizontalScrollbar(graphics, cx, cy + ch, cw, 2, renderedX, contentTotalWidth, cw);
        }
    }

    /**
     * Uses padded bounds because a component surface may extend past its content area.
     * Subclasses can widen this for components that draw beyond their declared bounds.
     */
    protected boolean isChildInViewport(UIComponent child, float viewportX, float viewportY, int viewportWidth, int viewportHeight) {
        int left = child.paddedX();
        int top = child.paddedY();
        int right = left + child.paddedWidth();
        int bottom = top + child.paddedHeight();
        return right > viewportX - renderCullOverflow && left < viewportX + viewportWidth + renderCullOverflow
                && bottom > viewportY - renderCullOverflow && top < viewportY + viewportHeight + renderCullOverflow;
    }

    private void renderVerticalScrollbar(GuiGraphicsExtractor graphics, int barX, int barY, int barW, int trackH,
                                         float scroll, int totalContent, int viewportH) {
        float thumbRatio = (float) viewportH / totalContent;
        int thumbH = Math.max(8, (int) (trackH * thumbRatio));
        float scrollRatio = scroll / (totalContent - viewportH);
        int thumbY = barY + (int) ((trackH - thumbH) * scrollRatio);

        graphics.fill(barX, barY, barX + barW, barY + trackH, SCROLLBAR_TRACK);
        graphics.fill(barX, thumbY, barX + barW, thumbY + thumbH, SCROLLBAR_THUMB);
    }

    private void renderHorizontalScrollbar(GuiGraphicsExtractor graphics, int barX, int barY, int trackW, int barH,
                                           float scroll, int totalContent, int viewportW) {
        float thumbRatio = (float) viewportW / totalContent;
        int thumbW = Math.max(8, (int) (trackW * thumbRatio));
        float scrollRatio = scroll / (totalContent - viewportW);
        int thumbX = barX + (int) ((trackW - thumbW) * scrollRatio);

        graphics.fill(barX, barY, barX + trackW, barY + barH, SCROLLBAR_TRACK);
        graphics.fill(thumbX, barY, thumbX + thumbW, barY + barH, SCROLLBAR_THUMB);
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
