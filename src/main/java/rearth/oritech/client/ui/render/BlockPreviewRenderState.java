package rearth.oritech.client.ui.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2f;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Render state for a 3D isometric block preview composed of one or more blocks (plus optional block entities).
 */
public record BlockPreviewRenderState(
        List<Entry> blocks,
        float rotationX,
        float rotationY,
        float centerX,
        float centerY,
        float centerZ,
        float partialTick,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public BlockPreviewRenderState(List<Entry> blocks, float rotationX, float rotationY,
                                   float centerX, float centerY, float centerZ,
                                   float partialTick,
                                   int x0, int y0, int x1, int y1, float scale,
                                   Matrix3x2f pose,
                                   @Nullable ScreenRectangle scissorArea) {
        this(blocks, rotationX, rotationY, centerX, centerY, centerZ, partialTick,
                x0, y0, x1, y1, scale, new Matrix3x2f(pose), scissorArea,
                calculateBounds(x0, y0, x1, y1, pose, scissorArea));
    }

    private static @Nullable ScreenRectangle calculateBounds(
            int x0, int y0, int x1, int y1,
            Matrix3x2f pose,
            @Nullable ScreenRectangle scissorArea
    ) {
        var bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }

    public record Entry(BlockState state, @Nullable BlockEntity entity, Vec3i offset) {
    }
}
