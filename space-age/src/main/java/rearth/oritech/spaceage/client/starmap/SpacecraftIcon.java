package rearth.oritech.spaceage.client.starmap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import rearth.oritech.spaceage.OritechSpaceAge;

public final class SpacecraftIcon {

    private SpacecraftIcon() {
    }

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width) {

        var height = Math.max(1, Math.round(width * 59f / 128));
        graphics.blit(RenderPipelines.GUI_TEXTURED, OritechSpaceAge.id("textures/gui/spacecraft.png"), x - width / 2, y - height / 2,
                0, 0, width, height, 128, 59, 128, 59);
    }
}
