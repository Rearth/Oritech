package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector2f;
import org.joml.Vector3f;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.client.ui.render.BlockPreviewRenderState;

import java.util.List;

/**
 * Renders a BlockState as a 3D isometric preview in the UI via a custom
 * {@link net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState}.
 */
public class BlockWidget extends UIComponent {

    private BlockState state;
    private boolean cacheTexture;
    private float modelScale = 1f;
    private int renderPadding;
    private float rotationY = 225f; // isometric-ish default
    private float rotationX = 30f;
    private float mouseRotationSpeed = 0f;
    private float currentMouseRotation = 0f;

    public BlockWidget(int x, int y, int size, BlockState state) {
        super(x, y, size, size);
        this.state = state;
    }

    /** Opt in only for static block models without animated textures. */
    public BlockWidget withCachedRendering() {
        cacheTexture = true;
        return this;
    }

    public BlockState getState() {
        return state;
    }

    public void setState(BlockState state) {
        this.state = state;
    }

    public BlockWidget withRotation(float rotationX, float rotationY) {
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        return this;
    }

    public BlockWidget withScale(float scale) {
        this.modelScale = scale;
        return this;
    }

    /** Expands the preview texture without changing the model's scale or center. */
    public BlockWidget withRenderPadding(int pixels) {
        this.renderPadding = pixels;
        return this;
    }

    public BlockWidget withMouseRotation(float speed) {
        this.mouseRotationSpeed = speed;
        return this;
    }

    /** Projects a model-space point into widget-width units relative to the icon center. */
    public Vector2f projectModelPoint(float x, float y, float z) {
        var point = BlockPreviewRenderState.modelRotation(rotationX, rotationY + currentMouseRotation)
                .transformPosition(x - .5f, y - .5f, z - .5f, new Vector3f());
        return new Vector2f(point.x, point.y).mul(.625f * modelScale);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (state == null || state.getRenderShape() == RenderShape.INVISIBLE) return;

        if (mouseRotationSpeed > 0) {
            currentMouseRotation += mouseRotationSpeed * delta;
        }

        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();
        float scale = cw * 0.625f * modelScale;

        var entries = List.of(new BlockPreviewRenderState.Entry(state, null, Vec3i.ZERO));
        var renderState = new BlockPreviewRenderState(
                entries,
                rotationX,
                rotationY + currentMouseRotation,
                0f, 0f, 0f,
                cacheTexture ? 0 : delta, cacheTexture,
                cx - renderPadding, cy - renderPadding, cx + cw + renderPadding, cy + ch + renderPadding,
                scale,
                graphics.pose(),
                graphics.peekScissorStack()
        );
        graphics.submitPictureInPictureRenderState(renderState);
    }
}
