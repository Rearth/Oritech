package rearth.oritech.api.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

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
public record NinePatchRenderer(Identifier texture, int texWidth, int texHeight, int cornerWidth, int cornerHeight) {
    
    /**
     * Default used by all Oritech bedrock panels: 16x16 texture, 4x4 corners.
     */
    public NinePatchRenderer(Identifier texture) {
        this(texture, 16, 16, 4, 4);
    }
    
    public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int cw = cornerWidth;
        int ch = cornerHeight;
        int centerW = texWidth - cw * 2;
        int centerH = texHeight - ch * 2;
        int stretchW = width - cw * 2;
        int stretchH = height - ch * 2;
        
        // Top-left corner
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, cw, ch, cw, ch, texWidth, texHeight);
        // Top-right corner
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - cw, y, texWidth - cw, 0, cw, ch, cw, ch, texWidth, texHeight);
        // Bottom-left corner
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + height - ch, 0, texHeight - ch, cw, ch, cw, ch, texWidth, texHeight);
        // Bottom-right corner
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - cw, y + height - ch, texWidth - cw, texHeight - ch, cw, ch, cw, ch, texWidth, texHeight);
        
        // Top edge (stretched)
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + cw, y, cw, 0, stretchW, ch, centerW, ch, texWidth, texHeight);
        // Bottom edge (stretched)
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + cw, y + height - ch, cw, texHeight - ch, stretchW, ch, centerW, ch, texWidth, texHeight);
        // Left edge (stretched)
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + ch, 0, ch, cw, stretchH, cw, centerH, texWidth, texHeight);
        // Right edge (stretched)
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - cw, y + ch, texWidth - cw, ch, cw, stretchH, cw, centerH, texWidth, texHeight);
        
        // Center (stretched)
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + cw, y + ch, cw, ch, stretchW, stretchH, centerW, centerH, texWidth, texHeight);
    }
}
