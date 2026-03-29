package rearth.oritech.api.screen;

import net.minecraft.client.gui.GuiGraphics;
import rearth.oritech.Oritech;
import rearth.oritech.util.ColorHelper;

/**
 * Predefined surface types for Oritech UI components.
 * Supports nine-patch textures and programmatic renderers.
 */
public enum OritechSurface {
    
    NONE(null),
    PANEL(ninePatch("textures/gui/bedrock_panel.png")),
    PANEL_DARK(ninePatch("textures/gui/bedrock_panel_dark.png")),
    PANEL_INSET(OritechSurface::renderInset),
    PANEL_ORANGE(ninePatch("textures/gui/bedrock_panel_orange.png")),
    PANEL_HOVER(ninePatch("textures/gui/bedrock_panel_hover.png")),
    PANEL_DARK_HOVER(ninePatch("textures/gui/bedrock_panel_dark_hover.png")),
    PANEL_ORANGE_HOVER(ninePatch("textures/gui/bedrock_panel_orange_hover.png")),
    PANEL_PRESSED(ninePatch("textures/gui/bedrock_panel_pressed.png"));
    
    @FunctionalInterface
    public interface SurfaceRenderer {
        void render(GuiGraphics graphics, int x, int y, int width, int height);
    }
    
    private final SurfaceRenderer renderer;
    
    OritechSurface(SurfaceRenderer renderer) {
        this.renderer = renderer;
    }
    
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (renderer != null) {
            renderer.render(graphics, x, y, width, height);
        }
    }
    
    public boolean isNone() {
        return renderer == null;
    }
    
    private static SurfaceRenderer ninePatch(String path) {
        var r = new NinePatchRenderer(Oritech.id(path));
        return r::render;
    }
    
    private static void renderInset(GuiGraphics graphics, int x, int y, int w, int h) {
        int shadow = ColorHelper.argb(0f, 0f, 0f, 0.4f);
        int highlight = ColorHelper.argb(1f, 1f, 1f, 0.15f);
        int fill = ColorHelper.argb(0.22f, 0.22f, 0.24f);
        
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w, y + 1, shadow);
        graphics.fill(x, y, x + 1, y + h, shadow);
        graphics.fill(x, y + h - 1, x + w, y + h, highlight);
        graphics.fill(x + w - 1, y, x + w, y + h, highlight);
    }
}
