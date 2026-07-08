package rearth.oritech.client.ui.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record LargeItemRenderState(
        ItemStack stack,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public LargeItemRenderState(
            ItemStack stack,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            Matrix3x2f pose,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                stack,
                x0,
                y0,
                x1,
                y1,
                scale,
                new Matrix3x2f(pose),
                scissorArea,
                calculateBounds(x0, y0, x1, y1, pose, scissorArea)
        );
    }

    private static @Nullable ScreenRectangle calculateBounds(
            int x0,
            int y0,
            int x1,
            int y1,
            Matrix3x2f pose,
            @Nullable ScreenRectangle scissorArea
    ) {
        var bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }
}
