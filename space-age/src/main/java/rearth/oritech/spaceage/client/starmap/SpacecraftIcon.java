package rearth.oritech.spaceage.client.starmap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.rocket.SpacecraftModel;

public final class SpacecraftIcon {

    private static final int CANVAS = 24;
    private static final int MARKER_SIZE = 18;
    private static final BlockWidget ORIGINAL = view(SpacecraftModel.ORIGINAL, -72, 0, .46f);
    private static final BlockWidget ALTERNATE = view(SpacecraftModel.ALTERNATE, 18, 90, .8f);
    private static final BlockWidget ORIGINAL_PREVIEW = view(SpacecraftModel.ORIGINAL, -48, 26, .55f);
    private static final BlockWidget ALTERNATE_PREVIEW = view(SpacecraftModel.ALTERNATE, 26, 55, 1.02f);

    private SpacecraftIcon() {
    }

    public static BlockState block(SpacecraftModel model) {

        return (model == SpacecraftModel.ALTERNATE ? SpaceAgeBlocks.SPACECRAFT_ICON_ALT : SpaceAgeBlocks.SPACECRAFT_ICON)
                .get().defaultBlockState();
    }

    public static void draw(GuiGraphicsExtractor graphics, double x, double y, Pose craft, double verticalScale) {

        // Map icons start facing up. Rotate them to match the heading on the displayed plane.
        var angle = Math.atan2(Math.sin(craft.heading) * verticalScale, Math.cos(craft.heading)) + Math.PI / 2;
        draw(graphics, x, y, craft.model == SpacecraftModel.ALTERNATE ? ALTERNATE : ORIGINAL, MARKER_SIZE, (float) angle);
    }

    public static void drawPreview(GuiGraphicsExtractor graphics, double x, double y, SpacecraftModel model, int size) {

        // The alternate model is built upright. Bank its preview so both craft point diagonally up.
        if (model == SpacecraftModel.ALTERNATE) draw(graphics, x, y, ALTERNATE_PREVIEW, size, .50f);
        else draw(graphics, x, y, ORIGINAL_PREVIEW, size, 0);
    }

    private static BlockWidget view(SpacecraftModel model, float pitch, float yaw, float scale) {

        return new BlockWidget(-CANVAS / 2, -CANVAS / 2, CANVAS, block(model))
                .withRotation(pitch, yaw).withScale(scale).withCachedRendering();
    }

    private static void draw(GuiGraphicsExtractor graphics, double x, double y, BlockWidget view, int size, float angle) {

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y).rotate(angle).scale((float) size / CANVAS);
        view.render(graphics, 0, 0, 0);
        pose.popMatrix();
    }

    public record Pose(SpacecraftModel model, double heading) {
    }
}
