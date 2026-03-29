package rearth.oritech.api.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.util.ColorHelper;

import java.util.function.Supplier;

public class FluidDisplayWidget extends AbstractDataDisplayWidget {

    private static final ResourceLocation GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components.png");
    private static final int FRAME_REGION_WIDTH = 14;
    private static final int FRAME_REGION_HEIGHT = 50;
    private static final int FLUID_BACKGROUND = ColorHelper.argb(0.06f, 0.18f, 0.24f);
    private static final int OVERLAY_COLOR = ColorHelper.argb(0.31f, 0.31f, 0.31f);

    private final Supplier<FluidStack> fluidStackSupplier;
    private TextureAtlasSprite fluidSprite;
    private int fluidColor;

    public FluidDisplayWidget(int x, int y, int width, int height, DisplayDataSource dataSource, Supplier<FluidStack> fluidStackSupplier) {
        super(x, y, width, height, dataSource);
        this.fluidStackSupplier = fluidStackSupplier;
        updateFluidRenderData();
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();

        graphics.fill(cx, cy, cx + cw, cy + ch, FLUID_BACKGROUND);

        if (fluidSprite != null && getCurrentAmount() > 0) {
            renderFluidSprite(graphics, cx, cy, cw, ch);
        }

        int overlayHeight = (int) (ch * (1f - getFillRatio()) * 0.98f);
        if (overlayHeight > 0) {
            graphics.fill(cx, cy, cx + cw, cy + overlayHeight, OVERLAY_COLOR);
        }

        graphics.blit(GUI_COMPONENTS, cx, cy, cw, ch, 48, 0, FRAME_REGION_WIDTH, FRAME_REGION_HEIGHT, 98, 96);
    }

    private void updateFluidRenderData() {
        var stack = fluidStackSupplier.get();
        if (stack == null || stack.isEmpty()) {
            fluidSprite = null;
            fluidColor = ColorHelper.WHITE;
            return;
        }

        this.fluidSprite = FluidStackHooks.getStillTexture(stack);
        this.fluidColor = ColorHelper.makeOpaque(FluidStackHooks.getColor(stack));
    }

    @Override
    public void tick() {
        super.tick();
        updateFluidRenderData();
    }

    private void renderFluidSprite(GuiGraphics graphics, int x, int y, int w, int h) {
        if (fluidSprite == null) {
            return;
        }

        float r = ((fluidColor >> 16) & 0xFF) / 255f;
        float g = ((fluidColor >> 8) & 0xFF) / 255f;
        float b = (fluidColor & 0xFF) / 255f;

        RenderSystem.setShaderTexture(0, fluidSprite.atlasLocation());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        Matrix4f matrix = graphics.pose().last().pose();
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float u0 = fluidSprite.getU0();
        float v0 = fluidSprite.getV0();
        float u1 = fluidSprite.getU1();
        float v1 = fluidSprite.getV1();

        int spriteW = Math.max(fluidSprite.contents().width(), 16);
        int spriteH = Math.max(fluidSprite.contents().height(), 16);

        for (int tileY = 0; tileY < h; tileY += spriteH) {
            for (int tileX = 0; tileX < w; tileX += spriteW) {
                int drawW = Math.min(spriteW, w - tileX);
                int drawH = Math.min(spriteH, h - tileY);

                float tileU1 = u0 + (u1 - u0) * drawW / spriteW;
                float tileV1 = v0 + (v1 - v0) * drawH / spriteH;

                buffer.addVertex(matrix, x + tileX, y + tileY + drawH, 0).setUv(u0, tileV1).setColor(r, g, b, 1f);
                buffer.addVertex(matrix, x + tileX + drawW, y + tileY + drawH, 0).setUv(tileU1, tileV1).setColor(r, g, b, 1f);
                buffer.addVertex(matrix, x + tileX + drawW, y + tileY, 0).setUv(tileU1, v0).setColor(r, g, b, 1f);
                buffer.addVertex(matrix, x + tileX, y + tileY, 0).setUv(u0, v0).setColor(r, g, b, 1f);
            }
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}