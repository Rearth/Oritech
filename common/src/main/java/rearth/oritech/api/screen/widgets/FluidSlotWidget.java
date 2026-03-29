package rearth.oritech.api.screen.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix4f;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ColorHelper;

import java.util.List;

/**
 * A self-contained fluid tank display widget. Renders:
 * 1. Fluid sprite background (tinted, tiled)
 * 2. Fill-level overlay (gray box covering the empty portion)
 * 3. Foreground tank frame texture
 * 4. Tooltip with fluid name and amount
 * <p>
 * Replaces the old FluidDisplay inner class pattern from BasicMachineScreen.
 */
public class FluidSlotWidget extends UIComponent {
    
    private static final ResourceLocation GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components.png");
    private static final int OVERLAY_COLOR = ColorHelper.argb(0.31f, 0.31f, 0.31f);
    
    private final FluidApi.SingleSlotStorage storage;
    
    // Cached state for smooth animation
    private float displayedFill;
    private Fluid lastFluid;
    private TextureAtlasSprite fluidSprite;
    private int fluidColor;
    
    public FluidSlotWidget(int x, int y, int width, int height, FluidApi.SingleSlotStorage storage) {
        super(x, y, width, height);
        this.storage = storage;
        this.displayedFill = getFill();
        updateFluidSprite();
    }
    
    public FluidApi.SingleSlotStorage getStorage() {
        return storage;
    }
    
    @Override
    public void tick() {
        // Check if fluid type changed
        var currentFluid = storage.getStack().getFluid();
        if (lastFluid == null || !lastFluid.equals(currentFluid)) {
            updateFluidSprite();
        }
        
        // Smooth fill animation (lerp)
        float targetFill = getFill();
        displayedFill += (targetFill - displayedFill) * 0.15f;
        
        // Update tooltip
        updateTooltip();
    }
    
    private float getFill() {
        if (storage.getCapacity() <= 0) return 0f;
        return (float) storage.getStack().getAmount() / storage.getCapacity();
    }
    
    private void updateFluidSprite() {
        var stack = storage.getStack();
        lastFluid = stack.getFluid();
        fluidSprite = FluidStackHooks.getStillTexture(stack);
        int rawColor = FluidStackHooks.getColor(stack);
        // Force opaque
        fluidColor = ColorHelper.makeOpaque(rawColor);
    }
    
    private void updateTooltip() {
        var stack = storage.getStack();
        Component tooltipText;
        if (stack.getAmount() > 0) {
            long displayAmount = stack.getAmount() * 1000 / FluidStackHooks.bucketAmount();
            tooltipText = Component.translatable("tooltip.oritech.fluid_content",
                displayAmount, FluidStackHooks.getName(stack).getString());
        } else {
            tooltipText = Component.translatable("tooltip.oritech.fluid_empty");
        }
        setTooltip(List.of(tooltipText));
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();
        
        // 1. Fluid sprite background (tiled, tinted)
        if (fluidSprite != null && displayedFill > 0.001f) {
            renderFluidSprite(graphics, cx, cy, cw, ch);
        }
        
        // 2. Empty portion overlay (gray box covering unfilled area from top)
        float emptyFraction = 1f - displayedFill;
        int overlayHeight = (int) (ch * emptyFraction * 0.98f);
        if (overlayHeight > 0) {
            graphics.fill(cx, cy, cx + cw, cy + overlayHeight, OVERLAY_COLOR);
        }
        
        // 3. Foreground tank frame
        graphics.blit(GUI_COMPONENTS, cx, cy, cw, ch, 48, 0, 14, 50, 98, 96);
    }
    
    private void renderFluidSprite(GuiGraphics graphics, int x, int y, int w, int h) {
        if (fluidSprite == null) return;
        
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
