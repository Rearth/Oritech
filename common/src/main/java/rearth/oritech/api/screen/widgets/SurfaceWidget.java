package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphics;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;

/**
 * A component that only renders its surface background.
 * Used for panel backgrounds, inset frames, and decorative elements.
 */
public class SurfaceWidget extends UIComponent {
    
    public SurfaceWidget(int x, int y, int width, int height, OritechSurface surface) {
        super(x, y, width, height);
        this.setSurface(surface);
    }
    
    public SurfaceWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Surface rendered by UIComponent.render()
    }
}
