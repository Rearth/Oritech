package rearth.oritech.api.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ColorHelper;

/**
 * Renders a TextureAtlasSprite (e.g. fluid still texture) tiled to fill the content area,
 * tinted with a color. Used as the fluid background layer in fluid slots.
 */
public class SpriteWidget extends UIComponent {
    
    private TextureAtlasSprite sprite;
    private int color = ColorHelper.WHITE;
    
    public SpriteWidget(int x, int y, int width, int height, TextureAtlasSprite sprite) {
        super(x, y, width, height);
        this.sprite = sprite;
    }
    
    public TextureAtlasSprite getSprite() { return sprite; }
    public void setSprite(TextureAtlasSprite sprite) { this.sprite = sprite; }
    
    public int getColor() { return color; }
    public void setColor(int argbColor) { this.color = argbColor; }
    
    public SpriteWidget withColor(int argbColor) {
        this.color = argbColor;
        return this;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (sprite == null) return;
        
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();
        
        drawTiledSprite(graphics, cx, cy, cw, ch, sprite, r, g, b, a);
    }
    
    /**
     * Draws a sprite tiled to fill the given rectangle.
     */
    private void drawTiledSprite(GuiGraphics graphics, int x, int y, int width, int height,
                                 TextureAtlasSprite sprite, float r, float g, float b, float a) {
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        
        Matrix4f matrix = graphics.pose().last().pose();
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();
        
        int spriteW = Math.max(sprite.contents().width(), 16);
        int spriteH = Math.max(sprite.contents().height(), 16);
        
        for (int tileY = 0; tileY < height; tileY += spriteH) {
            for (int tileX = 0; tileX < width; tileX += spriteW) {
                int drawW = Math.min(spriteW, width - tileX);
                int drawH = Math.min(spriteH, height - tileY);
                
                float tileU1 = u0 + (u1 - u0) * drawW / spriteW;
                float tileV1 = v0 + (v1 - v0) * drawH / spriteH;
                
                buffer.addVertex(matrix, x + tileX, y + tileY + drawH, 0).setUv(u0, tileV1).setColor(r, g, b, a);
                buffer.addVertex(matrix, x + tileX + drawW, y + tileY + drawH, 0).setUv(tileU1, tileV1).setColor(r, g, b, a);
                buffer.addVertex(matrix, x + tileX + drawW, y + tileY, 0).setUv(tileU1, v0).setColor(r, g, b, a);
                buffer.addVertex(matrix, x + tileX, y + tileY, 0).setUv(u0, v0).setColor(r, g, b, a);
            }
        }
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
