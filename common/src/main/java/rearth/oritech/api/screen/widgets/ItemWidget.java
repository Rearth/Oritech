package rearth.oritech.api.screen.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.api.screen.UIComponent;

import java.util.List;

/**
 * Renders an ItemStack with optional count overlay and tooltip derived from the stack.
 */
public class ItemWidget extends UIComponent {
    
    private ItemStack stack;
    private boolean showOverlay = true;
    private boolean tooltipFromStack = true;
    
    public ItemWidget(int x, int y, ItemStack stack) {
        super(x, y, 16, 16);
        this.stack = stack;
    }
    
    public ItemWidget(int x, int y, int size, ItemStack stack) {
        super(x, y, size, size);
        this.stack = stack;
    }
    
    public ItemStack getStack() { return stack; }
    public void setStack(ItemStack stack) { this.stack = stack; }
    
    public ItemWidget withShowOverlay(boolean show) {
        this.showOverlay = show;
        return this;
    }
    
    public ItemWidget withTooltipFromStack(boolean fromStack) {
        this.tooltipFromStack = fromStack;
        return this;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (stack == null || stack.isEmpty()) return;
        
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();
        
        int targetSize = Math.min(cw, ch);
        if (targetSize != 16 && targetSize > 0) {
            float scale = targetSize / 16f;
            graphics.pose().pushPose();
            graphics.pose().translate(cx, cy, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.renderItem(stack, 0, 0);
            if (showOverlay)
                graphics.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
            graphics.pose().popPose();
        } else {
            graphics.renderItem(stack, cx, cy);
            if (showOverlay)
                graphics.renderItemDecorations(Minecraft.getInstance().font, stack, cx, cy);
        }
    }
    
    @Override
    public boolean hasTooltip() {
        boolean hasStackTooltip = tooltipFromStack && stack != null && !stack.isEmpty();
        return hasStackTooltip || super.hasTooltip();
    }
    
    @Override
    public List<Component> getTooltip() {
        if (tooltipFromStack && stack != null && !stack.isEmpty()) {
            return Screen.getTooltipFromItem(
                Minecraft.getInstance(), stack);
        }
        return super.getTooltip();
    }
}
