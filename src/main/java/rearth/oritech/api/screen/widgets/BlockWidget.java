package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.client.ui.render.BlockPreviewRenderState;

import java.util.List;

/**
 * Renders a BlockState as a 3D isometric preview in the UI via a custom
 * {@link net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState}.
 */
public class BlockWidget extends UIComponent {
    
    private BlockState state;
    private float rotationY = 225f; // isometric-ish default
    private float rotationX = 30f;
    private float mouseRotationSpeed = 0f;
    private float currentMouseRotation = 0f;
    
    public BlockWidget(int x, int y, int size, BlockState state) {
        super(x, y, size, size);
        this.state = state;
    }
    
    public BlockState getState() { return state; }
    public void setState(BlockState state) { this.state = state; }
    
    public BlockWidget withRotation(float rotationX, float rotationY) {
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        return this;
    }
    
    public BlockWidget withMouseRotation(float speed) {
        this.mouseRotationSpeed = speed;
        return this;
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
        float scale = cw * 0.625f;
        
        var entries = List.of(new BlockPreviewRenderState.Entry(state, null, Vec3i.ZERO));
        var renderState = new BlockPreviewRenderState(
            entries,
            rotationX,
            rotationY + currentMouseRotation,
            0f, 0f, 0f,
            delta,
            cx, cy, cx + cw, cy + ch,
            scale,
            null
        );
        graphics.submitPictureInPictureRenderState(renderState);
    }
}
