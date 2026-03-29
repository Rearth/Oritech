package rearth.oritech.api.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a 9-patch (9-slice) texture at any size by stretching/tiling the center
 * while keeping corners and edges at their original size.
 * <p>
 * The texture is divided into a 3x3 grid:
 * <pre>
 * [TL] [T ] [TR]
 * [L ] [C ] [R ]
 * [BL] [B ] [BR]
 * </pre>
 * Corners stay fixed-size, edges stretch in one axis, center stretches in both.
 */
public record NinePatchRenderer(ResourceLocation texture, int texWidth, int texHeight, int cornerWidth, int cornerHeight) {
    
    /**
     * Default used by all Oritech bedrock panels: 16x16 texture, 4x4 corners.
     */
    public NinePatchRenderer(ResourceLocation texture) {
        this(texture, 16, 16, 4, 4);
    }
    
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        int cw = cornerWidth;
        int ch = cornerHeight;
        int centerW = texWidth - cw * 2;
        int centerH = texHeight - ch * 2;
        int stretchW = width - cw * 2;
        int stretchH = height - ch * 2;
        
        // Top-left corner
        graphics.blit(texture, x, y, cw, ch, 0, 0, cw, ch, texWidth, texHeight);
        // Top-right corner
        graphics.blit(texture, x + width - cw, y, cw, ch, texWidth - cw, 0, cw, ch, texWidth, texHeight);
        // Bottom-left corner
        graphics.blit(texture, x, y + height - ch, cw, ch, 0, texHeight - ch, cw, ch, texWidth, texHeight);
        // Bottom-right corner
        graphics.blit(texture, x + width - cw, y + height - ch, cw, ch, texWidth - cw, texHeight - ch, cw, ch, texWidth, texHeight);
        
        // Top edge (stretched)
        graphics.blit(texture, x + cw, y, stretchW, ch, cw, 0, centerW, ch, texWidth, texHeight);
        // Bottom edge (stretched)
        graphics.blit(texture, x + cw, y + height - ch, stretchW, ch, cw, texHeight - ch, centerW, ch, texWidth, texHeight);
        // Left edge (stretched)
        graphics.blit(texture, x, y + ch, cw, stretchH, 0, ch, cw, centerH, texWidth, texHeight);
        // Right edge (stretched)
        graphics.blit(texture, x + width - cw, y + ch, cw, stretchH, texWidth - cw, ch, cw, centerH, texWidth, texHeight);
        
        // Center (stretched)
        graphics.blit(texture, x + cw, y + ch, stretchW, stretchH, cw, ch, centerW, centerH, texWidth, texHeight);
    }
}
