package rearth.oritech.client.ui;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import rearth.oracle.Oracle;
import rearth.oracle.OracleClient;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.api.screen.data.EnergyDisplayWidget;
import rearth.oritech.api.screen.data.FluidDisplayWidget;
import rearth.oritech.api.screen.data.ProgressDisplayWidget;
import rearth.oritech.api.screen.data.SoulDisplayWidget;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.util.ColorHelper;
import rearth.oritech.util.InventoryInputMode;

import java.util.*;

/**
 * Base screen for all Oritech machine GUIs.
 * Manages a flat list of {@link UIComponent} widgets with absolute positioning.
 */
public class OritechScreen<T extends OritechScreenHandler> extends AbstractContainerScreen<T> {
    
    public static final ResourceLocation BACKGROUND = Oritech.id("textures/gui/modular/gui_base.png");
    public static final ResourceLocation GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components.png");
    
    public static final int TEXT_COLOR = ColorHelper.argb(0.2f, 0.2f, 0.3f);
    public static final int SEPARATOR_COLOR = ColorHelper.argb(0.8f, 0.8f, 0.8f);
    public static final int DISABLED_TEXT_COLOR = ColorHelper.argb(0.47f, 0.47f, 0.47f);
    
    protected final List<UIComponent> components = new ArrayList<>();
    
    protected ButtonWidget cycleInputButton;
    
    protected Rect2i extensionBounds;
    protected Rect2i extensionInsetBounds;
    protected Rect2i equipmentBounds;
    
    public OritechScreen(T handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }
    
    // init
    @Override
    protected void init() {
        super.init();
        clearWidgets();
        components.clear();
        cycleInputButton = null;
        extensionBounds = null;
        extensionInsetBounds = null;
        equipmentBounds = null;
        
        buildComponents();
    }
    
    protected void buildComponents() {
        addTitle();
        addItemSlots();
        addDataSlots();
        
        if (showExtensionPanel())
            buildExtensionPanel();
        
        if (menu.armorSlots != null)
            buildEquipmentPanel();
        
        buildOracleButton();
        addExtraComponents();
    }
    
    private void addItemSlots() {
        for (var slot : menu.screenData.getGuiSlots())
            addComponent(new ItemSlotWidget(slot.x(), slot.y()));
    }
    
    private void addDataSlots() {
        for (var source : menu.getDataDisplays()) {
            if (source instanceof DisplayDataSource.EnergyDataSource energySource) {
                addComponent(new EnergyDisplayWidget(energySource).withSurface(OritechSurface.PANEL_INSET).withPadding(Insets.of(1)));
            } else if (source instanceof DisplayDataSource.FluidDataSource fluidSource) {
                addComponent(new FluidDisplayWidget(fluidSource));
            } else if (source instanceof DisplayDataSource.SoulDataSource soulSource) {
                addComponent(new SoulDisplayWidget(soulSource));
            } else if (source instanceof DisplayDataSource.ProgressDataSource progressSource) {
                addComponent(new ProgressDisplayWidget(progressSource));
            }
        }
    }
    
    // subclass hooks
    protected void addExtraComponents() {}
    protected void tickExtra() {}
    
    protected void addExtensionContent(List<UIComponent> content) {
        content.add(new LabelWidget(0, 0, 60, 10, Component.translatable("title.oritech.details")).withAlignment(LabelWidget.Alignment.CENTER));
        
        var inputSlots = menu.screenData.getGuiSlots().stream().filter(s -> !s.output()).count();
        if (menu.screenData.inputOptionsEnabled() && inputSlots > 1) {
            cycleInputButton = ButtonWidget.panel(0, 0, 63, 14,
                Component.translatable("button.oritech.input_mode_fill_matching_recipe").withColor(TEXT_COLOR),
                btn -> NetworkManager.sendToServer(new MachineBlockEntity.InventoryInputModeSelectorPacket(menu.blockPos)))
                .withTextColor(TEXT_COLOR);
        }
        
        for (var label : menu.screenData.getExtraExtensionLabels()) {
            var lbl = new LabelWidget(0, 0, 60, 10, label.getA());
            lbl.withTooltip(label.getB());
            content.add(lbl);
        }
        
        if (menu.showRedstoneAddon()) {
            content.add(BoxWidget.filled(0, 0, 63, 1, SEPARATOR_COLOR));
            content.add(new LabelWidget(0, 0, 60, 10,
                Component.translatable("text.oritech.redstone_power", menu.screenData.receivedRedstoneSignal())));
            
            if (!menu.screenData.currentRedstoneEffect().isEmpty()) {
                var effectLabel = new LabelWidget(0, 0, 60, 10,
                    Component.translatable(menu.screenData.currentRedstoneEffect()));
                effectLabel.withTooltip(
                    Component.translatable(menu.screenData.currentRedstoneEffect() + ".tooltip"));
                content.add(effectLabel);
            }
        }
    }
    
    // title
    protected void addTitle() {
        var blockTitle = menu.machineBlock.getBlock().getName();
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
        int titleY = -27;
        
        iconWidget.setPosition(titleX, titleY);
        labelWidget.setPosition(titleX + iconWidget.getWidth() + iconWidget.getPadding().right() + 6, titleY + 9);
        
        addComponent(labelWidget);
        addComponent(iconWidget);
    }
    
    public ItemStack getTitleIcon() {
        return new ItemStack(menu.blockEntity.getBlockState().getBlock());
    }
    
    
    // extension panel
    public boolean showExtensionPanel() {
        return menu.screenData.showExpansionPanel();
    }
    
    private void buildExtensionPanel() {
        var content = new ArrayList<UIComponent>();
        addExtensionContent(content);
        if (content.isEmpty()) return;
        
        int panelX = imageWidth - 10;
        int panelY = 8;
        int panelW = 85;
        int outerPad = 7;
        int innerPad = 4;
        int spacing = 3;
        
        int cx = panelX + outerPad + innerPad + 3;
        int cy = panelY + outerPad + innerPad;
        
        for (int i = 0; i < content.size(); i++) {
            var component = content.get(i);
            component.setPosition(component.getX() + cx, component.getY() + cy);
            addComponent(component);
            cy += component.getHeight() + spacing;
            
            if (i == 0 && cycleInputButton != null) {
                cycleInputButton.setPosition(cx, cy);
                addComponent(cycleInputButton);
                cy += cycleInputButton.getHeight() + spacing;
            }
        }
        
        int contentH = Math.max(0, cy - (panelY + outerPad + innerPad) - spacing);
        int insetH = contentH + innerPad * 2;
        int panelH = insetH + outerPad * 2;
        
        extensionBounds = new Rect2i(panelX, panelY, panelW, panelH);
        extensionInsetBounds = new Rect2i(panelX + outerPad, panelY + outerPad, panelW - outerPad * 2, insetH);
        
        // Panel background components
        var panelBg = new SurfaceWidget(panelX, panelY, panelW, panelH);
        panelBg.withSurface(OritechSurface.PANEL);
        panelBg.withZIndex(-10);
        addComponent(panelBg);
        
        var insetBg = new SurfaceWidget(panelX + outerPad, panelY + outerPad, panelW - outerPad * 2, insetH);
        insetBg.withSurface(OritechSurface.PANEL_INSET);
        insetBg.withZIndex(-9);
        addComponent(insetBg);
        
        updateSettingsButtons();
    }
    
    protected void updateSettingsButtons() {
        if (cycleInputButton == null) return;
        
        var activeMode = menu.screenData.getInventoryInputMode();
        var modeName = activeMode.name().toLowerCase(Locale.ROOT);
        
        if (activeMode.equals(InventoryInputMode.SIDED) && menu.blockEntity instanceof MachineBlockEntity machineBlock) {
            var tooltip = Component.translatable("tooltip.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName));
            var assignment = machineBlock.getSlotAssignments();
            for (var direction : Direction.values()) {
                var key = "tooltip.oritech.mode_sided_slot_number";
                if (direction.equals(Direction.DOWN)) key = "tooltip.oritech.mode_sided_bottom";
                if (direction.equals(Direction.UP)) key = "tooltip.oritech.mode_sided_top";
                
                int horizontalOrdinal = 0;
                if (direction.equals(Direction.EAST)) horizontalOrdinal = 1;
                if (direction.equals(Direction.SOUTH)) horizontalOrdinal = 2;
                if (direction.equals(Direction.WEST)) horizontalOrdinal = 3;
                var inputSlotIndex = assignment.inputStart() + horizontalOrdinal % assignment.inputCount();
                
                tooltip = tooltip.append(
                    Component.translatable("tooltip.oritech.input_dir." + direction)
                        .append(Component.translatable(key, inputSlotIndex)));
            }
            cycleInputButton.withTooltip(tooltip);
        } else {
            cycleInputButton.withTooltip(Component.translatable("tooltip.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName)));
        }
        
        cycleInputButton.setLabel(Component.translatable("button.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName)).withColor(TEXT_COLOR));
    }
    
    // equipment panel
    private void buildEquipmentPanel() {
        int slotCount = menu.armorSlots.size();
        int slotX = -20;
        
        for (int i = 0; i < slotCount; i++) {
            int y = i * 19;
            addComponent(new ItemSlotWidget(slotX, y));
            
            var bgTexture = getEquipmentSlotTexture(i);
            if (bgTexture != null) {
                var bg = new TextureWidget(slotX + 1, y + 1, 16, 16,
                    bgTexture, 0, 0, 16, 16, 16, 16);
                bg.withZIndex(-1);
                addComponent(bg);
            }
        }
        
        equipmentBounds = new Rect2i(slotX - 6, -6, 30, (slotCount - 1) * 19 + 18 + 12);
        
        var equipBg = new SurfaceWidget(equipmentBounds.getX(), equipmentBounds.getY(),
            equipmentBounds.getWidth(), equipmentBounds.getHeight());
        equipBg.withSurface(OritechSurface.PANEL);
        equipBg.withZIndex(-10);
        addComponent(equipBg);
    }
    
    protected ResourceLocation getEquipmentSlotTexture(int armorSlot) {
        return switch (armorSlot) {
            case 0 -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/empty_armor_slot_boots.png");
            case 1 -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/empty_armor_slot_leggings.png");
            case 2 -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/empty_armor_slot_chestplate.png");
            case 3 -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/empty_armor_slot_helmet.png");
            case 4 -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/empty_slot_axe.png");
            default -> null;
        };
    }
    
    // oracle help button
    private void buildOracleButton() {
        if (!OritechClientConfig.enableHelpButton.get()) return;
        
        var hasOracleLib = Platform.isModLoaded("oracle_index");
        Optional<ResourceLocation> linkTarget = hasOracleLib ? getHelpBookLink() : Optional.empty();
        if (hasOracleLib && linkTarget.isEmpty()) return;
        
        var button = ButtonWidget.darkPanel(-10, imageHeight - 13, 14, 14,
            Component.literal("?"),
            btn -> onOracleButtonClick(hasOracleLib, linkTarget))
            .withTextColor(ColorHelper.argb(0.9f, 0.9f, 0.9f))
            .withSurfacePadding(new Insets(2, 1, 3, 1));
        
        button.withTooltip(hasOracleLib
            ? Component.translatable("tooltip.oritech.oracle_available")
            : Component.translatable("tooltip.oritech.oracle_missing"));
        addComponent(button);
    }
    
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void onOracleButtonClick(boolean enabled, Optional<ResourceLocation> target) {
        if (!enabled || target.isEmpty()) {
            Oritech.LOGGER.info("Oracle Index mod is missing. Install it here: https://www.curseforge.com/minecraft/mc-mods/oracle-index (or from modrinth)");
            return;
        }
        OracleClient.openScreen("oritech", target.get(), this);
    }
    
    private Optional<ResourceLocation> getHelpBookLink() {
        if (menu.screenData.getWikiLink().isPresent())
            return Optional.of(ResourceLocation.fromNamespaceAndPath(Oracle.MOD_ID, "books/oritech/" + menu.screenData.getWikiLink().get() + ".mdx"));
        
        var blockItem = menu.machineBlock.getBlock().asItem();
        var itemId = BuiltInRegistries.ITEM.getKey(blockItem);
        
        if (OracleClient.ITEM_LINKS.containsKey(itemId))
            return Optional.of(OracleClient.ITEM_LINKS.get(itemId).linkTarget());
        
        return Optional.empty();
    }
    
    // tick
    @Override
    protected void containerTick() {
        super.containerTick();
        
        for (var component : components)
            component.tick();
        
        tickExtra();
    }
    
    // rendering
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderComponents(graphics, mouseX, mouseY, partialTick);
    }
    
    protected void renderComponents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var sorted = new ArrayList<>(components);
        sorted.sort(Comparator.comparingInt(UIComponent::getZIndex));
        
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        
        var lastZ = Integer.MIN_VALUE;
        
        for (var component : sorted) {
            if (!component.isVisible() || component instanceof OverlayWidget) continue;
            if (lastZ < 0 && component.getZIndex() >= 0)
                graphics.blit(getBackground(), leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos, topPos, 0);
            component.render(graphics, relX, relY, partialTick);
            graphics.pose().popPose();
            lastZ = component.getZIndex();
        }
    }
    
    protected void renderOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        
        for (var component : components) {
            if (component instanceof OverlayWidget && component.isVisible()) {
                graphics.pose().pushPose();
                graphics.pose().translate(leftPos, topPos, 400); // match tooltip-depth so overlays always sit above slot items
                component.render(graphics, relX, relY, partialTick);
                graphics.pose().popPose();
            }
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
    
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
        
        // If an overlay is active, only check its children for tooltips
        for (var c : components) {
            if (c instanceof OverlayWidget overlay && c.isVisible()) {
                var hovered = overlay.getTopmostHovered(relX, relY);
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
        
        if (topHovered != null)
            graphics.renderComponentTooltip(Minecraft.getInstance().font, topHovered.getTooltip(), mouseX, mouseY);
    }
    
    // mouse event dispatch to components
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        
        // Dispatch in reverse z-order (highest z-index first)
        var sorted = new ArrayList<>(components);
        sorted.sort(Comparator.comparingInt(UIComponent::getZIndex).reversed());
        
        for (var c : sorted) {
            if (c.isVisible() && c.isMouseOver(relX, relY) && c.handleClick(relX, relY, button))
                return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int relMX = (int) mouseX - leftPos;
        int relMY = (int) mouseY - topPos;
        
        for (var c : components) {
            if (c.isVisible() && c.handleMouseScroll(relMX, relMY, scrollY))
                return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    // exclusion zones for recipe viewer compat (EMI, REI, JEI)
    public List<Rect2i> getExclusionZones() {
        var zones = new ArrayList<Rect2i>();
        if (extensionBounds != null) {
            zones.add(new Rect2i(
                leftPos + extensionBounds.getX(),
                topPos + extensionBounds.getY(),
                extensionBounds.getWidth(),
                extensionBounds.getHeight()));
        }
        if (equipmentBounds != null) {
            zones.add(new Rect2i(
                leftPos + equipmentBounds.getX(),
                topPos + equipmentBounds.getY(),
                equipmentBounds.getWidth(),
                equipmentBounds.getHeight()));
        }
        return zones;
    }
    
    // utilities
    protected void addComponent(UIComponent component) {
        components.add(component);
    }
    
    protected void removeComponent(UIComponent component) {
        components.remove(component);
    }
    
    public ResourceLocation getBackground() { return BACKGROUND; }
    public ResourceLocation getGuiComponents() { return GUI_COMPONENTS; }
}
