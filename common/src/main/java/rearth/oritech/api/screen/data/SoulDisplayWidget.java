package rearth.oritech.api.screen.data;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import rearth.oritech.Oritech;

public class SoulDisplayWidget extends AbstractDataDisplayWidget {

    private static final ResourceLocation GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components_souls.png");
    private static final int REGION_WIDTH = 24;
    private static final int REGION_HEIGHT = 96;

    public SoulDisplayWidget(DisplayDataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();

        graphics.blit(GUI_COMPONENTS, cx, cy, cw, ch, 24, 0, REGION_WIDTH, REGION_HEIGHT, 98, 96);

        float fillRatio = getFillRatio();
        int filledHeight = (int) (ch * fillRatio);
        if (filledHeight <= 0) {
            return;
        }

        int drawY = cy + ch - filledHeight;
        int srcY = REGION_HEIGHT - (int) (REGION_HEIGHT * fillRatio);
        int srcHeight = REGION_HEIGHT - srcY;

        graphics.blit(GUI_COMPONENTS, cx, drawY, cw, filledHeight, 0, srcY, REGION_WIDTH, srcHeight, 98, 96);
    }
}