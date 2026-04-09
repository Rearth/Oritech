package rearth.oritech.api.screen.widgets;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import rearth.oritech.api.screen.UIComponent;

/**
 * Renders a BlockState as a 3D isometric preview in the UI.
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
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (state == null || state.getRenderShape() == RenderShape.INVISIBLE) return;
        
        if (mouseRotationSpeed > 0) {
            currentMouseRotation += mouseRotationSpeed * delta;
        }
        
        var client = Minecraft.getInstance();
        var pose = graphics.pose();
        
        pose.pushPose();
        
        int cx = contentX() + contentWidth() / 2;
        int cy = contentY() + contentHeight() / 2;
        float scale = contentWidth() * 0.625f; // 40/64 ratio matching existing code
        
        pose.translate(cx, cy, 150);
        pose.scale(scale, -scale, scale);
        
        appleRotation(pose);
        
        pose.translate(-0.5f, -0.5f, -0.5f);
        
        RenderSystem.runAsFancy(() -> {
            var bufferSource = client.renderBuffers().bufferSource();
            client.getBlockRenderer().renderSingleBlock(
                state, pose, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
            );
            RenderSystem.setShaderLights(new Vector3f(-1.5f, -.5f, 0), new Vector3f(0, -1, 0));
            bufferSource.endBatch();
            Lighting.setupFor3DItems();
        });

        pose.popPose();
    }
    
    public void appleRotation(PoseStack pose) {
        pose.mulPose(Axis.XP.rotationDegrees(rotationX));
        pose.mulPose(Axis.YP.rotationDegrees(rotationY + currentMouseRotation));
    }
}
