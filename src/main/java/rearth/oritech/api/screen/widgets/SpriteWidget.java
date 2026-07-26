package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ColorHelper;

/**
 * Renders a tinted color fill in place of a TextureAtlasSprite.
 * (current 2D pose stack has no easy textured-quad path for atlas UV ranges).
 */
public class SpriteWidget extends UIComponent {

    private TextureAtlasSprite sprite;
    private int color = ColorHelper.WHITE;

    public SpriteWidget(int x, int y, int width, int height, TextureAtlasSprite sprite) {
        super(x, y, width, height);
        this.sprite = sprite;
    }

    public TextureAtlasSprite getSprite() {
        return sprite;
    }

    public void setSprite(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int argbColor) {
        this.color = argbColor;
    }

    public SpriteWidget withColor(int argbColor) {
        this.color = argbColor;
        return this;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(contentX(), contentY(), contentX() + contentWidth(), contentY() + contentHeight(), color);
    }
}
