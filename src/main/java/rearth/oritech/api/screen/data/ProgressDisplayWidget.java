package rearth.oritech.api.screen.data;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import rearth.oritech.Oritech;

public class ProgressDisplayWidget extends AbstractDataDisplayWidget {

    private static final Identifier EMPTY_ARROW = Oritech.id("textures/gui/modular/arrow_empty.png");
    private static final Identifier FULL_ARROW = Oritech.id("textures/gui/modular/arrow_full.png");
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
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();

        graphics.blit(RenderPipelines.GUI_TEXTURED, EMPTY_ARROW, cx, cy, 0, 0, cw, ch, REGION_WIDTH, REGION_HEIGHT, REGION_WIDTH, REGION_HEIGHT);

        float fillRatio = getFillRatio();
        int filledWidth = (int) (cw * fillRatio);
        if (filledWidth <= 0) {
            return;
        }

        int srcWidth = (int) (REGION_WIDTH * fillRatio);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FULL_ARROW, cx, cy, 0, 0, filledWidth, ch, srcWidth, REGION_HEIGHT, REGION_WIDTH, REGION_HEIGHT);
    }
}
