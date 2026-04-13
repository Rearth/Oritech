package rearth.oritech.client.ui;

import dev.architectury.platform.Platform;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oracle.Oracle;
import rearth.oracle.OracleClient;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.data.*;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.util.ColorHelper;
import rearth.oritech.util.InventoryInputMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class OritechMachineScreen<T extends OritechScreenHandler> extends OritechWidgetScreen<T> {

    public static final ResourceLocation BACKGROUND = Oritech.id("textures/gui/modular/gui_base.png");
    public static final ResourceLocation BACKGROUND_TALL = Oritech.id("textures/gui/modular/gui_base_tall.png");
    public static final ResourceLocation GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components.png");

    protected ButtonWidget cycleInputButton;

    protected Rect2i extensionBounds;
    protected Rect2i extensionInsetBounds;
    protected Rect2i equipmentBounds;

    public OritechMachineScreen(T handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166, BACKGROUND);
    }

    @Override
    protected void rebuildComponents() {
        cycleInputButton = null;
        extensionBounds = null;
        extensionInsetBounds = null;
        equipmentBounds = null;
        super.rebuildComponents();
    }

    @Override
    protected void buildComponents() {
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
                addComponent(new FluidDisplayWidget(fluidSource, menu.blockPos));
            } else if (source instanceof DisplayDataSource.SoulDataSource soulSource) {
                addComponent(new SoulDisplayWidget(soulSource));
            } else if (source instanceof DisplayDataSource.ProgressDataSource progressSource) {
                addComponent(new ProgressDisplayWidget(progressSource));
            }
        }
    }

    protected void addExtraComponents() {}
    protected void tickExtra() {}

    protected void addExtensionContent(List<UIComponent> content) {
        content.add(new LabelWidget(0, 0, 60, 10, Component.translatable("title.oritech.details")).withAlignment(LabelWidget.Alignment.CENTER));

        var inputSlots = menu.screenData.getGuiSlots().stream().filter(slot -> !slot.output()).count();
        if (menu.screenData.inputOptionsEnabled() && inputSlots > 1) {
            cycleInputButton = ButtonWidget.panel(1, 0, 58, 14,
                Component.translatable("button.oritech.input_mode_fill_matching_recipe").withColor(LabelWidget.DARK_TEXT),
                btn -> NetworkManager.sendToServer(new MachineBlockEntity.InventoryInputModeSelectorPacket(menu.blockPos)))
                .withTextColor(LabelWidget.DARK_TEXT);
            cycleInputButton.withSurfacePadding(Insets.of(2, 1, 3, 1));
            content.add(cycleInputButton);
        }

        for (var label : menu.screenData.getExtraExtensionLabels()) {
            var widget = new LabelWidget(0, 0, 60, 10, label.getA());
            widget.withAlignment(LabelWidget.Alignment.CENTER);
            widget.withTooltip(label.getB());
            content.add(widget);
        }

        if (menu.showRedstoneAddon()) {
            var torchOn = menu.screenData.receivedRedstoneSignal() > 0;
            content.add(BoxWidget.filled(0, 0, 63, 1, SEPARATOR_COLOR));
            content.add(new BlockWidget(-3, -7, 20,
                Blocks.REDSTONE_TORCH.defaultBlockState().setValue(RedstoneTorchBlock.LIT, torchOn)).withPadding(Insets.of(-1)));
            content.add(new LabelWidget(10, 1, 50, 10,
                Component.translatable("text.oritech.redstone_power", menu.screenData.receivedRedstoneSignal())).withAlignment(LabelWidget.Alignment.CENTER));

            if (!menu.screenData.currentRedstoneEffect().isEmpty()) {
                var effectLabel = new LabelWidget(0, 0, 60, 10,
                    Component.translatable(menu.screenData.currentRedstoneEffect()));
                effectLabel.withTooltip(Component.translatable(menu.screenData.currentRedstoneEffect() + ".tooltip"));
                content.add(effectLabel.withAlignment(LabelWidget.Alignment.CENTER));
            }
        }
    }

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
        int spacing = 4;

        int cx = panelX + outerPad + innerPad + 3;
        int cy = panelY + outerPad + innerPad;
        
        for (var component : content) {
            cy += component.getPadding().top();
            component.setPosition(component.getX() + cx, component.getY() + cy);
            addComponent(component);
            
            if (component.getPadding().top() >= 0)
                cy += component.getHeight() + component.getPadding().bottom() + spacing;
            
        }

        int contentH = Math.max(0, cy - (panelY + outerPad + innerPad) - spacing);
        int insetH = contentH + innerPad * 2;
        int panelH = insetH + outerPad * 2;

        extensionBounds = new Rect2i(panelX, panelY, panelW, panelH);
        extensionInsetBounds = new Rect2i(panelX + outerPad, panelY + outerPad, panelW - outerPad * 2, insetH);

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

        cycleInputButton.setLabel(Component.translatable("button.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName)).withColor(LabelWidget.DARK_TEXT));
    }

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

    @Override
    protected void containerTick() {
        super.containerTick();

        for (var component : components)
            component.tick();

        tickExtra();
        updateSettingsButtons();
    }
    
    @Override
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

    public ResourceLocation getBackground() {
        return BACKGROUND;
    }

    public ResourceLocation getGuiComponents() {
        return GUI_COMPONENTS;
    }
    
    @Override
    public BlockState getTitleState() {
        return menu.machineBlock;
    }
}