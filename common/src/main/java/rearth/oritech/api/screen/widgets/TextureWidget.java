package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import rearth.oritech.api.screen.UIComponent;

/**
 * Renders a region of a texture atlas/spritesheet.
 * Supports a visible area clip rectangle for progress/energy bar animations.
 */
public class TextureWidget extends UIComponent {
    
    public final ResourceLocation texture;
    public int u, v;
    public int regionWidth, regionHeight;
    public int textureWidth, textureHeight;
    
    // Optional clip rectangle (null = no clipping, render full region)
    private int clipX, clipY, clipW, clipH;
    private boolean hasClip = false;
    
    public TextureWidget(int x, int y, int width, int height,
                         ResourceLocation texture, int u, int v,
                         int regionWidth, int regionHeight,
                         int textureWidth, int textureHeight) {
        super(x, y, width, height);
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }
    
    /**
     * Convenience for textures where regionSize == textureSize (single-image textures).
     */
    public TextureWidget(int x, int y, int width, int height,
                         ResourceLocation texture, int regionWidth, int regionHeight) {
        this(x, y, width, height, texture, 0, 0, regionWidth, regionHeight, regionWidth, regionHeight);
    }
    
    /**
     * Sets a visible area clip rectangle. Only the portion of the texture within
     * this rectangle (relative to widget position) will be rendered.
     * Used for animated progress bars and energy indicators.
     */
    public TextureWidget withVisibleArea(int clipX, int clipY, int clipW, int clipH) {
        this.clipX = clipX;
        this.clipY = clipY;
        this.clipW = clipW;
        this.clipH = clipH;
        this.hasClip = true;
        return this;
    }
    
    public void setVisibleArea(int clipX, int clipY, int clipW, int clipH) {
        this.clipX = clipX;
        this.clipY = clipY;
        this.clipW = clipW;
        this.clipH = clipH;
        this.hasClip = true;
    }
    
    public void clearVisibleArea() {
        this.hasClip = false;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();
        
        if (hasClip) {
            // Calculate the clipped source UV and destination based on clip rect
            float scaleX = (float) regionWidth / cw;
            float scaleY = (float) regionHeight / ch;
            
            int drawX = cx + clipX;
            int drawY = cy + clipY;
            int drawW = clipW;
            int drawH = clipH;
            
            int srcU = u + (int) (clipX * scaleX);
            int srcV = v + (int) (clipY * scaleY);
            int srcW = (int) (clipW * scaleX);
            int srcH = (int) (clipH * scaleY);
            
            graphics.blit(texture, drawX, drawY, drawW, drawH, srcU, srcV, srcW, srcH, textureWidth, textureHeight);
        } else {
            graphics.blit(texture, cx, cy, cw, ch, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        }
    }
}
