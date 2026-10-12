package rearth.oritech.api.screen;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/** Renders a stretched nine-patch texture with one GUI render state. */
public record NinePatchRenderer(Identifier texture, int texWidth, int texHeight, int cornerWidth, int cornerHeight) {

    /** Default used by all Oritech bedrock panels: 16x16 texture, 4x4 corners. */
    public NinePatchRenderer(Identifier texture) {
        this(texture, 16, 16, 4, 4);
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        int centerW = texWidth - cornerWidth * 2;
        int centerH = texHeight - cornerHeight * 2;
        int stretchW = width - cornerWidth * 2;
        int stretchH = height - cornerHeight * 2;
        // Keep the original corner/edge order, but let the GUI sort and clip the whole panel once.
        var patches = new Patch[]{
                patch(x, y, cornerWidth, cornerHeight, 0, 0, cornerWidth, cornerHeight),
                patch(x + width - cornerWidth, y, cornerWidth, cornerHeight,
                        texWidth - cornerWidth, 0, cornerWidth, cornerHeight),
                patch(x, y + height - cornerHeight, cornerWidth, cornerHeight,
                        0, texHeight - cornerHeight, cornerWidth, cornerHeight),
                patch(x + width - cornerWidth, y + height - cornerHeight, cornerWidth, cornerHeight,
                        texWidth - cornerWidth, texHeight - cornerHeight, cornerWidth, cornerHeight),
                patch(x + cornerWidth, y, stretchW, cornerHeight, cornerWidth, 0, centerW, cornerHeight),
                patch(x + cornerWidth, y + height - cornerHeight, stretchW, cornerHeight,
                        cornerWidth, texHeight - cornerHeight, centerW, cornerHeight),
                patch(x, y + cornerHeight, cornerWidth, stretchH, 0, cornerHeight, cornerWidth, centerH),
                patch(x + width - cornerWidth, y + cornerHeight, cornerWidth, stretchH,
                        texWidth - cornerWidth, cornerHeight, cornerWidth, centerH),
                patch(x + cornerWidth, y + cornerHeight, stretchW, stretchH,
                        cornerWidth, cornerHeight, centerW, centerH)
        };
        AbstractTexture resolvedTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
        graphics.submitGuiElementRenderState(new NinePatchRenderState(
                TextureSetup.singleTexture(resolvedTexture.getTextureView(), resolvedTexture.getSampler()),
                new Matrix3x2f(graphics.pose()), patches, graphics.peekScissorStack()));
    }

    private Patch patch(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight) {
        return new Patch(x, y, x + width, y + height,
                (float) u / texWidth, (float) (u + sourceWidth) / texWidth,
                (float) v / texHeight, (float) (v + sourceHeight) / texHeight);
    }

    // Destination corners and normalized texture coordinates for one stretched piece.
    private record Patch(int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {
    }

    // Snapshot the texture, transform and clipping before the screen moves on to its next component.
    private record NinePatchRenderState(TextureSetup textureSetup, Matrix3x2f pose, Patch[] patches,
                                        @Nullable ScreenRectangle scissorArea,
                                        @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        private NinePatchRenderState(TextureSetup textureSetup, Matrix3x2f pose, Patch[] patches,
                                     @Nullable ScreenRectangle scissorArea) {
            this(textureSetup, pose, patches, scissorArea, bounds(patches, pose, scissorArea));
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            for (var patch : patches) {
                vertexConsumer.addVertexWith2DPose(pose, patch.x0, patch.y0).setUv(patch.u0, patch.v0).setColor(-1);
                vertexConsumer.addVertexWith2DPose(pose, patch.x0, patch.y1).setUv(patch.u0, patch.v1).setColor(-1);
                vertexConsumer.addVertexWith2DPose(pose, patch.x1, patch.y1).setUv(patch.u1, patch.v1).setColor(-1);
                vertexConsumer.addVertexWith2DPose(pose, patch.x1, patch.y0).setUv(patch.u1, patch.v0).setColor(-1);
            }
        }

        private static @Nullable ScreenRectangle bounds(Patch[] patches, Matrix3x2f pose,
                                                         @Nullable ScreenRectangle scissorArea) {
            int left = Integer.MAX_VALUE;
            int top = Integer.MAX_VALUE;
            int right = Integer.MIN_VALUE;
            int bottom = Integer.MIN_VALUE;
            for (var patch : patches) {
                left = Math.min(left, Math.min(patch.x0, patch.x1));
                top = Math.min(top, Math.min(patch.y0, patch.y1));
                right = Math.max(right, Math.max(patch.x0, patch.x1));
                bottom = Math.max(bottom, Math.max(patch.y0, patch.y1));
            }
            var bounds = new ScreenRectangle(left, top, right - left, bottom - top).transformMaxBounds(pose);
            return scissorArea == null ? bounds : scissorArea.intersection(bounds);
        }
    }
}
