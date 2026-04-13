package rearth.oritech.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.OverlayWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.util.ColorHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class OritechWidgetScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    
    public static final int SEPARATOR_COLOR = ColorHelper.argb(0.8f, 0.8f, 0.8f);
    
    protected final List<UIComponent> components = new ArrayList<>();
    protected ResourceLocation backgroundTexture;
    private UIComponent interactionTarget;
    
    protected OritechWidgetScreen(T handler, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        this(handler, inventory, title, imageWidth, imageHeight, null);
    }
    
    protected OritechWidgetScreen(T handler, Inventory inventory, Component title, int imageWidth, int imageHeight, ResourceLocation backgroundTexture) {
        super(handler, inventory, title);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.backgroundTexture = backgroundTexture;
    }
    
    @Override
    protected void init() {
        super.init();
        rebuildComponents();
    }
    
    protected abstract void buildComponents();
    
    protected void rebuildComponents() {
        clearWidgets();
        components.clear();
        interactionTarget = null;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        if (shouldCreateTitle())
            addTitle();
        
        buildComponents();
    }
    
    protected void setPanelSize(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var sorted = new ArrayList<>(components);
        sorted.sort(Comparator.comparingInt(UIComponent::getZIndex));
        
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        
        var backgroundDrawn = false;
        var lastZ = Integer.MIN_VALUE;
        
        for (var component : sorted) {
            if (!component.isVisible() || component instanceof OverlayWidget) continue;
            if (!backgroundDrawn && backgroundTexture != null && lastZ < 0 && component.getZIndex() >= 0) {
                graphics.blit(backgroundTexture, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                backgroundDrawn = true;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos, topPos, 0);
            component.render(graphics, relX, relY, partialTick);
            graphics.pose().popPose();
            lastZ = component.getZIndex();
        }
        
        if (!backgroundDrawn && backgroundTexture != null) {
            graphics.blit(backgroundTexture, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        }
    }
    
    protected void renderOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        
        for (var component : components) {
            if (component instanceof OverlayWidget && component.isVisible()) {
                graphics.pose().pushPose();
                graphics.pose().translate(leftPos, topPos, 400);
                component.render(graphics, relX, relY, partialTick);
                graphics.pose().popPose();
            }
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderOverlays(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderComponentTooltips(graphics, mouseX, mouseY);
    }
    
    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (hasActiveOverlay()) return false;
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }
    
    protected boolean hasActiveOverlay() {
        for (var c : components) {
            if (c instanceof OverlayWidget && c.isVisible()) return true;
        }
        return false;
    }
    
    private void renderComponentTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        
        // abort early for overlay widgets
        for (var c : components) {
            if (c instanceof OverlayWidget overlay && c.isVisible()) {
                var hovered = overlay.getTopmostHovered(relX, relY);
                if (hovered != null) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 220);   // item stack are 150, item stack tooltips are 200
                    graphics.renderComponentTooltip(Minecraft.getInstance().font, hovered.getTooltip(), mouseX, mouseY);
                    graphics.pose().popPose();
                }
                return;
            }
        }
        
        // if not aborted due to overlay, process scroll widgets
        for (var c : components) {
            if (c instanceof ScrollWidget scrollWidget && c.isVisible() && c.isMouseOver(mouseX, mouseY)) {
                var hovered = scrollWidget.getTopmostHovered(relX, relY);
                if (hovered != null)
                    graphics.renderComponentTooltip(Minecraft.getInstance().font, hovered.getTooltip(), mouseX, mouseY);
                return;
            }
        }
        
        UIComponent topHovered = null;
        for (var c : components) {
            if (c.isVisible() && c.isMouseOver(relX, relY) && c.hasTooltip()) {
                if (topHovered == null || c.getZIndex() > topHovered.getZIndex())
                    topHovered = c;
            }
        }
        
        if (topHovered != null && !topHovered.getTooltip().isEmpty() && !topHovered.getTooltip().stream().allMatch(elem -> elem.getString().isBlank()))
            graphics.renderComponentTooltip(Minecraft.getInstance().font, topHovered.getTooltip(), mouseX, mouseY);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        interactionTarget = null;
        
        var sorted = new ArrayList<>(components);
        sorted.sort(Comparator.comparingInt(UIComponent::getZIndex).reversed());
        
        for (var c : sorted) {
            if (c.isVisible() && (c.isMouseOver(relX, relY) || c instanceof OverlayWidget) && c.handleClick(relX, relY, button)) {
                interactionTarget = c;
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        
        if (interactionTarget != null && interactionTarget.isVisible() && interactionTarget.handleDrag(relX, relY, dragX, dragY, button)) {
            return true;
        }
        
        var sorted = new ArrayList<>(components);
        sorted.sort(Comparator.comparingInt(UIComponent::getZIndex).reversed());
        
        for (var c : sorted) {
            if (c.isVisible() && (c == interactionTarget || c.isMouseOver(relX, relY) || c instanceof OverlayWidget) && c.handleDrag(relX, relY, dragX, dragY, button)) {
                interactionTarget = c;
                return true;
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        
        for (var c : components) {
            if (c.isVisible()) c.handleMouseRelease(relX, relY, button);
        }
        
        interactionTarget = null;
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        
        for (var c : components) {
            if (c.isVisible() && c.handleMouseScroll(relX, relY, scrollY))
                return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    protected void addComponent(UIComponent component) {
        components.add(component);
    }
    
    protected void removeComponent(UIComponent component) {
        components.remove(component);
    }
    
    protected ResourceLocation getBackgroundTexture() {
        return backgroundTexture;
    }
    
    public List<Rect2i> getExclusionZones() {
        return List.of();
    }
    
    public boolean shouldCreateTitle() {
        return true;
    }
    
    public abstract BlockState getTitleState();
    
    public ItemStack getTitleIcon() {
        return new ItemStack(getTitleState().getBlock());
    }
    
    public int getTitleY() {
        return -27;
    }
    
    protected void addTitle() {
        var blockTitle = getTitleState().getBlock().getName();
        var icon = getTitleIcon();
        
        var iconWidget = new ItemWidget(0, 0, 28, icon);
        iconWidget.withSurface(OritechSurface.PANEL);
        iconWidget.withPadding(Insets.of(0, 2, 3, 2));
        iconWidget.withShowOverlay(false);
        iconWidget.withTooltipFromStack(false);
        iconWidget.withZIndex(50);
        
        var textWidth = Minecraft.getInstance().font.width(blockTitle);
        var labelWidget = new LabelWidget(0, 0, textWidth + 10, 14, blockTitle);
        labelWidget.withSurface(OritechSurface.PANEL);
        labelWidget.withPadding(Insets.of(5, 0, 1, 10));
        labelWidget.withZIndex(50);
        labelWidget.withDarkColor();
        
        int combinedWidth = iconWidget.getWidth() + labelWidget.getWidth() + 2;
        int titleX = (imageWidth - combinedWidth) * 65 / 100;
        if (blockTitle.getString().length() > 15)
            titleX = imageWidth - combinedWidth;
        int titleY = getTitleY();
        
        iconWidget.setPosition(titleX, titleY);
        labelWidget.setPosition(titleX + iconWidget.getWidth() + iconWidget.getPadding().right() + 6, titleY + 9);
        
        addComponent(labelWidget);
        addComponent(iconWidget);
    }
}