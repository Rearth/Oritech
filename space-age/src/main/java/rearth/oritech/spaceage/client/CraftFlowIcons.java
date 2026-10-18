package rearth.oritech.spaceage.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import rearth.oritech.spaceage.OritechSpaceAge;

/** Uniform 32-pixel sprites, enlarged for readable flow categories. */
final class CraftFlowIcons {

    static final int SIZE = 24;
    private static final int RESOLUTION = 32;

    private CraftFlowIcons() {
    }

    static void draw(GuiGraphicsExtractor graphics, String category, int x, int y) {

        var texture = OritechSpaceAge.id("textures/gui/craft/" + category + ".png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, SIZE, SIZE,
                RESOLUTION, RESOLUTION, RESOLUTION, RESOLUTION);
    }
}
