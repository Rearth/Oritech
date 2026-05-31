package rearth.oritech.api.screen.data;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import rearth.oritech.Oritech;

public class EnergyDisplayWidget extends AbstractDataDisplayWidget {

    private static final Identifier GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components.png");
    private static final int REGION_WIDTH = 24;
    private static final int REGION_HEIGHT = 96;

    public EnergyDisplayWidget(DisplayDataSource.EnergyDataSource source) {
        super(source);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();

        // background
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_COMPONENTS, cx, cy, 24, 0, cw, ch, REGION_WIDTH, REGION_HEIGHT, 98, 96);

        float fillRatio = getFillRatio();
        int filledHeight = (int) (ch * fillRatio);
        if (filledHeight <= 0) {
            return;
        }

        int drawY = cy + ch - filledHeight;
        int srcY = REGION_HEIGHT - (int) (REGION_HEIGHT * fillRatio);
        int srcHeight = REGION_HEIGHT - srcY;

        // foreground
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_COMPONENTS, cx, drawY, 0, srcY, cw, filledHeight, REGION_WIDTH, srcHeight, 98, 96);
    }
}
