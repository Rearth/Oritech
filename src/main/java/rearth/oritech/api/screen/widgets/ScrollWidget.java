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
 * Supports wheel scrolling, draggable scrollbars and optional drag-to-pan.
 */
public class ScrollWidget extends UIComponent {

    private static final int SCROLLBAR_TRACK = ColorHelper.argb(0.2f, 0.25f, 0.3f, 0.35f);
    private static final int SCROLLBAR_THUMB = 0xC0808890;
    private static final int SCROLLBAR_ACTIVE = 0xFFB8C0C8;
    private static final int FADE_SIZE = 6;

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
    private boolean draggingScrollBar;
    private boolean draggingHorizontalBar;
    private double thumbGrabOffset;
    private UIComponent interactionChild;
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
        setScrollPosition(scrollX, scrollY);
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
        if (draggingScrollBar) return true;

        double adjX = mouseX - (x + innerMargin) + renderedX;
        double adjY = mouseY - (y + innerMargin) + renderedY;
        if (isInViewport(mouseX, mouseY)) {
            var sorted = new ArrayList<>(children);
            sorted.sort(Comparator.comparingInt(UIComponent::getZIndex).reversed());
            for (var child : sorted) {
                if (child.isVisible() && child.isMouseOver(adjX, adjY) && child.handleMouseScroll(adjX, adjY, scrollDelta))
                    return true;
            }
        }

        int viewW = width - innerMargin * 2;
        int viewH = height - innerMargin * 2;

        int delta = (int) (scrollDelta * scrollSpeed);
        if (horizontalScroll && (shiftHeld || !verticalScroll)) {
            scrollX = Mth.clamp(scrollX - delta, 0, Math.max(0, contentTotalWidth - viewW));
        } else if (verticalScroll) {
            scrollY = Mth.clamp(scrollY - delta, 0, Math.max(0, contentTotalHeight - viewH));
        } else {
            return false;
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
        interactionChild = null;
        if (button == 0 && (isOverScrollBar(mouseX, mouseY, false) || isOverScrollBar(mouseX, mouseY, true))) {
            draggingHorizontalBar = isOverScrollBar(mouseX, mouseY, true);
            double position = draggingHorizontalBar ? mouseX : mouseY;
            int start = thumbStart(draggingHorizontalBar);
            int size = thumbSize(draggingHorizontalBar);
            boolean overThumb = position >= start && position < start + size;
            thumbGrabOffset = overThumb ? position - start : size / 2.0;
            draggingScrollBar = true;
            // Stop wheel easing where the thumb is currently drawn.
            setScrollPosition(renderedX, renderedY);
            if (!overThumb) dragScrollBar(position);
            return true;
        }
        if (!isInViewport(mouseX, mouseY)) return true;

        double adjX = mouseX - (x + innerMargin) + renderedX;
        double adjY = mouseY - (y + innerMargin) + renderedY;

        var sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIComponent::getZIndex).reversed());

        for (var child : sorted) {
            if (child.isVisible() && child.isMouseOver(adjX, adjY) && child.handleClick(adjX, adjY, button)) {
                interactionChild = child;
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
        if (draggingScrollBar && button == 0) {
            dragScrollBar(draggingHorizontalBar ? mouseX : mouseY);
            return true;
        }
        if (interactionChild != null && interactionChild.isVisible()
                && interactionChild.handleDrag(mouseX - x - innerMargin + renderedX, mouseY - y - innerMargin + renderedY, deltaX, deltaY, button))
            return true;
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

    @Override
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (draggingScrollBar && button == 0) {
            draggingScrollBar = false;
            return true;
        }
        var child = interactionChild;
        interactionChild = null;
        return child != null && child.handleMouseRelease(mouseX - x - innerMargin + renderedX, mouseY - y - innerMargin + renderedY, button);
    }

    public float getScrollX() {
        return scrollX;
    }

    public float getScrollY() {
        return scrollY;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        scrollX = Mth.clamp(scrollX, 0, maxScroll(true));
        scrollY = Mth.clamp(scrollY, 0, maxScroll(false));
        renderedX = Mth.clamp(Mth.lerp(0.12f, renderedX, scrollX), 0, maxScroll(true));
        renderedY = Mth.clamp(Mth.lerp(0.12f, renderedY, scrollY), 0, maxScroll(false));
        if (Math.abs(renderedX - scrollX) < 0.5f) renderedX = scrollX;
        if (Math.abs(renderedY - scrollY) < 0.5f) renderedY = scrollY;

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
        renderEdgeFades(graphics, cx, cy, cw, ch);
        graphics.disableScissor();

        // Scrollbar indicator
        if (verticalScroll && contentTotalHeight > ch) {
            renderScrollBar(graphics, mouseX, mouseY, false);
        }
        if (horizontalScroll && contentTotalWidth > cw) {
            renderScrollBar(graphics, mouseX, mouseY, true);
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

    private boolean isInViewport(double mouseX, double mouseY) {
        return mouseX >= x + innerMargin && mouseX < x + width - innerMargin
                && mouseY >= y + innerMargin && mouseY < y + height - innerMargin;
    }

    private int trackSize(boolean horizontal) {
        return Math.max(0, (horizontal ? width : height) - innerMargin * 2);
    }

    private int maxScroll(boolean horizontal) {
        return Math.max(0, (horizontal ? contentTotalWidth : contentTotalHeight) - trackSize(horizontal));
    }

    private int thumbSize(boolean horizontal) {
        int track = trackSize(horizontal);
        int content = horizontal ? contentTotalWidth : contentTotalHeight;
        return Math.min(track, Math.max(8, (int) (track * (track / (float) Math.max(1, content)))));
    }

    private int thumbStart(boolean horizontal) {
        float scroll = horizontal ? renderedX : renderedY;
        return (horizontal ? x : y) + innerMargin
                + Math.round((trackSize(horizontal) - thumbSize(horizontal)) * scroll / Math.max(1, maxScroll(horizontal)));
    }

    private boolean isOverScrollBar(double mouseX, double mouseY, boolean horizontal) {
        if (!(horizontal ? horizontalScroll : verticalScroll) || maxScroll(horizontal) == 0) return false;
        // Use the existing outer margin as the hit area, preserving every caller's content width.
        if (horizontal)
            return mouseX >= x + innerMargin && mouseX < x + width - innerMargin
                    && mouseY >= y + height - innerMargin && mouseY < y + height;
        return mouseY >= y + innerMargin && mouseY < y + height - innerMargin
                && mouseX >= x + width - innerMargin && mouseX < x + width;
    }

    private void dragScrollBar(double position) {
        boolean horizontal = draggingHorizontalBar;
        int travel = trackSize(horizontal) - thumbSize(horizontal);
        if (travel <= 0) return;
        double progress = Mth.clamp((position - (horizontal ? x : y) - innerMargin - thumbGrabOffset) / travel, 0.0, 1.0);
        int offset = (int) Math.round(progress * maxScroll(horizontal));
        if (horizontal) {
            scrollX = offset;
            renderedX = offset;
        } else {
            scrollY = offset;
            renderedY = offset;
        }
    }

    private void renderScrollBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean horizontal) {
        int barX = horizontal ? x + innerMargin : x + width - innerMargin;
        int barY = horizontal ? y + height - innerMargin : y + innerMargin;
        int barW = horizontal ? trackSize(true) : 2;
        int barH = horizontal ? 2 : trackSize(false);
        int start = thumbStart(horizontal);
        int size = thumbSize(horizontal);
        double position = horizontal ? mouseX : mouseY;
        boolean hovered = isOverScrollBar(mouseX, mouseY, horizontal) && position >= start && position < start + size;
        int color = hovered || (draggingScrollBar && draggingHorizontalBar == horizontal) ? SCROLLBAR_ACTIVE : SCROLLBAR_THUMB;
        graphics.fill(barX, barY, barX + barW, barY + barH, SCROLLBAR_TRACK);
        if (horizontal)
            graphics.fill(start, barY, start + size, barY + barH, color);
        else
            graphics.fill(barX, start, barX + barW, start + size, color);
    }

    private void renderEdgeFades(GuiGraphicsExtractor graphics, int cx, int cy, int cw, int ch) {
        if (verticalScroll) {
            int top = Math.min(FADE_SIZE, (int) Math.ceil(renderedY));
            int bottom = Math.min(FADE_SIZE, (int) Math.ceil(maxScroll(false) - renderedY));
            if (top > 0)
                graphics.fillGradient(cx, cy, cx + cw, cy + top, fadeColor(top), 0);
            if (bottom > 0)
                graphics.fillGradient(cx, cy + ch - bottom, cx + cw, cy + ch, 0, fadeColor(bottom));
        }
        if (horizontalScroll) {
            int left = Math.min(FADE_SIZE, (int) Math.ceil(renderedX));
            int right = Math.min(FADE_SIZE, (int) Math.ceil(maxScroll(true) - renderedX));
            // GuiGraphicsExtractor only offers vertical gradients; use one-pixel strips sideways.
            for (int i = 0; i < left; i++)
                graphics.fill(cx + i, cy, cx + i + 1, cy + ch, fadeColor(left - i));
            for (int i = 0; i < right; i++)
                graphics.fill(cx + cw - i - 1, cy, cx + cw - i, cy + ch, fadeColor(right - i));
        }
    }

    private static int fadeColor(int size) {
        return Math.min(0x90, size * 24) << 24;
    }

    @Override
    public void tick() {
        for (var child : children)
            child.tick();
    }

    public UIComponent getTopmostHovered(double mouseX, double mouseY) {
        if (!isInViewport(mouseX, mouseY)) return null;
        UIComponent result = null;
        double adjX = mouseX - (x + innerMargin) + renderedX;
        double adjY = mouseY - (y + innerMargin) + renderedY;
        for (var child : children) {
            if (child.isVisible() && child.isMouseOver(adjX, adjY) && child.hasTooltip()) {
                if (result == null || child.getZIndex() > result.getZIndex())
                    result = child;
            }
        }
        return result;
    }
}
