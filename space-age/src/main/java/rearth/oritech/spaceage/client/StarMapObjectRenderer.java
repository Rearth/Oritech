package rearth.oritech.spaceage.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Batches the star map's flat discs and icons into one untextured GUI submission. */
final class StarMapObjectRenderer {

    private static final Identifier FOG_TEXTURE = Identifier.fromNamespaceAndPath(
            "oritech_space_age", "textures/gui/map_fog.png");

    private StarMapObjectRenderer() {
    }

    static void submit(GuiGraphicsExtractor graphics, List<Disc> discs, StarMapObjects.Viewport viewport) {
        if (discs.isEmpty()) return;
        var pose = new Matrix3x2f(graphics.pose());
        var scissor = graphics.peekScissorStack();
        var bounds = new ScreenRectangle(viewport.x(), viewport.y(), viewport.width(), viewport.height()).transformMaxBounds(pose);
        var clippedBounds = scissor == null ? bounds : scissor.intersection(bounds);
        var fog = discs.stream().filter(disc -> disc.color() != disc.edgeColor()).toList();
        var solid = discs.stream().filter(disc -> disc.color() == disc.edgeColor()).toList();
        if (!fog.isEmpty()) {
            var texture = Minecraft.getInstance().getTextureManager().getTexture(FOG_TEXTURE);
            graphics.submitGuiElementRenderState(new FogState(fog, pose,
                    TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()), scissor, clippedBounds));
        }
        if (!solid.isEmpty()) graphics.submitGuiElementRenderState(new RenderState(solid, pose, scissor, clippedBounds));
    }

    record Disc(double x, double y, double radiusX, double radiusY, int color, int edgeColor, int sides) {
        Disc(double x, double y, double radiusX, double radiusY, int color, int sides) {
            this(x, y, radiusX, radiusY, color, color, sides);
        }
    }

    // The texture contains the same linear radial alpha falloff as the old triangle fans.
    // Each puff now costs four vertices, independent of its radius and the zoom level.
    private record FogState(List<Disc> discs, Matrix3x2f pose, TextureSetup textureSetup,
                            @Nullable ScreenRectangle scissorArea,
                            @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vertices) {
            for (var disc : discs) {
                float left = (float) (disc.x() - disc.radiusX()), right = (float) (disc.x() + disc.radiusX());
                float top = (float) (disc.y() - disc.radiusY()), bottom = (float) (disc.y() + disc.radiusY());
                vertices.addVertexWith2DPose(pose, left, top).setUv(0, 0).setColor(disc.color());
                vertices.addVertexWith2DPose(pose, left, bottom).setUv(0, 1).setColor(disc.color());
                vertices.addVertexWith2DPose(pose, right, bottom).setUv(1, 1).setColor(disc.color());
                vertices.addVertexWith2DPose(pose, right, top).setUv(1, 0).setColor(disc.color());
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }
    }

    private record RenderState(List<Disc> discs, Matrix3x2f pose,
                               @Nullable ScreenRectangle scissorArea,
                               @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vertices) {
            for (var disc : discs) {
                var previousX = disc.x() + disc.radiusX();
                var previousY = disc.y();
                for (var index = 1; index <= disc.sides(); index++) {
                    var angle = -Math.PI * 2 * index / disc.sides();
                    var nextX = disc.x() + Math.cos(angle) * disc.radiusX();
                    var nextY = disc.y() + Math.sin(angle) * disc.radiusY();
                    // GUI geometry is quad based; this degenerate quad is one triangle-fan slice.
                    vertices.addVertexWith2DPose(pose, (float) disc.x(), (float) disc.y()).setColor(disc.color());
                    vertices.addVertexWith2DPose(pose, (float) previousX, (float) previousY).setColor(disc.edgeColor());
                    vertices.addVertexWith2DPose(pose, (float) nextX, (float) nextY).setColor(disc.edgeColor());
                    vertices.addVertexWith2DPose(pose, (float) disc.x(), (float) disc.y()).setColor(disc.color());
                    previousX = nextX;
                    previousY = nextY;
                }
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
}
