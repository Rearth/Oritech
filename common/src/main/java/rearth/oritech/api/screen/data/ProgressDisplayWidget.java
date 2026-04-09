package rearth.oritech.api.screen.data;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import rearth.oritech.Oritech;

public class ProgressDisplayWidget extends AbstractDataDisplayWidget {

    private static final ResourceLocation EMPTY_ARROW = Oritech.id("textures/gui/modular/arrow_empty.png");
    private static final ResourceLocation FULL_ARROW = Oritech.id("textures/gui/modular/arrow_full.png");
    private static final int REGION_WIDTH = 29;
    private static final int REGION_HEIGHT = 16;

    public ProgressDisplayWidget(DisplayDataSource dataSource) {
        super(dataSource);
    }
    
    @Override
    protected boolean applySmoothing() {
        return false;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();

        graphics.blit(EMPTY_ARROW, cx, cy, cw, ch, 0, 0, REGION_WIDTH, REGION_HEIGHT, REGION_WIDTH, REGION_HEIGHT);

        float fillRatio = getFillRatio();
        int filledWidth = (int) (cw * fillRatio);
        if (filledWidth <= 0) {
            return;
        }

        int srcWidth = (int) (REGION_WIDTH * fillRatio);
        graphics.blit(FULL_ARROW, cx, cy, filledWidth, ch, 0, 0, srcWidth, REGION_HEIGHT, REGION_WIDTH, REGION_HEIGHT);
    }
}