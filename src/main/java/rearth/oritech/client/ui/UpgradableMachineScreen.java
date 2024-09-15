package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.blocks.machines.addons.MachineAddonBlock;
import rearth.oritech.block.entity.machines.processing.FragmentForgeBlockEntity;
import rearth.oritech.block.entity.machines.processing.PulverizerBlockEntity;
import rearth.oritech.client.ui.components.BlockPreviewComponent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.MachineAddonController;


public class UpgradableMachineScreen<S extends UpgradableMachineScreenHandler> extends BasicMachineScreen<S> {
    
    private static final Color SPEED_COLOR = Color.ofRgb(0x219ebc);
    private static final Color EFFICIENCY_COLOR = Color.ofRgb(0x8ecae6);
    private static final Color CAPACITY_COLOR = Color.ofRgb(0x023047);
    private static final Color THROUGHPUT_COLOR = Color.ofRgb(0xffb703);
    
    private static final float rotationSpeed = 0.2f;
    
    private static final Identifier MACHINE_CORE_CENTER = Oritech.id("textures/gui/modular/machine_core/center.png");
    
    protected LabelComponent speedLabel;
    protected LabelComponent efficiencyLabel;
    
    public UpgradableMachineScreen(S handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    public void addExtensionComponents(FlowLayout container) {
        super.addExtensionComponents(container);
        
        var speed = 1 / handler.addonUiData.speed() * 100;
        var efficiency = 1 / handler.addonUiData.efficiency() * 100;
        
        speedLabel = Components.label(Text.translatable("title.oritech.machine_speed", (int) speed));
        efficiencyLabel = Components.label(Text.translatable("title.oritech.machine_efficiency", (int) efficiency));
        
        container.child(Components.box(Sizing.fixed(73), Sizing.fixed(1)).color(new Color(0.8f, 0.8f, 0.8f)));
        container.child(speedLabel.tooltip(Text.translatable("tooltip.oritech.machine_speed")).margins(Insets.of(3)));
        container.child(efficiencyLabel.tooltip(Text.translatable("tooltip.oritech.machine_efficiency")).margins(Insets.of(3)));
        
        if (steamProductionLabel != null)
            container.child(steamProductionLabel.margins(Insets.of(3)));
        
        if (handler.blockEntity instanceof PulverizerBlockEntity || handler.blockEntity instanceof FragmentForgeBlockEntity) {
            container.child(Components.label(Text.translatable("title.oritech.machine_option_enabled")).tooltip(Text.translatable("tooltip.oritech.pulverizer_dust_combine")).margins(Insets.of(3)));
        }
        
        if (!((MachineAddonController) handler.blockEntity).getAddonSlots().isEmpty())
            addMachinePreview(container);
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        var offsetX = -5;
        var offsetY = -23;
        
        var x = handler.screenData.getEnergyConfiguration().x() + offsetX;
        var y = handler.screenData.getEnergyConfiguration().y() + offsetY;
        
        var size = 25;
        
        var level = handler.quality;
        var upgradeCount = level - 1;
        
        // the 6th upgrade needs to be rendered behind
        if (upgradeCount == 6) {
            overlay.child(
              Components.texture(getRingIdentifier(6), 64, 64, 64, 64, 64, 64)
                .sizing(Sizing.fixed(size))
                .positioning(Positioning.absolute(x, y))
            );
            
            upgradeCount = 5;
        }
        
        overlay.child(
          Components.texture(MACHINE_CORE_CENTER, 64, 64, 64, 64, 64, 64)
            .sizing(Sizing.fixed(size))
            .positioning(Positioning.absolute(x, y))
            .tooltip(getQualityTooltip())
        );
        
        for (int i = 1; i <= upgradeCount; i++) {
             overlay.child(
              Components.texture(getRingIdentifier(i), 64, 64, 64, 64, 64, 64)
                .sizing(Sizing.fixed(size))
                .positioning(Positioning.absolute(x, y))
            );
        }
    }
    
    private Text getQualityTooltip() {
        var quality = String.format("%.2f", handler.quality);
        var effectiveQuality = (int) handler.quality;
        return Text.translatable("tooltip.oritech.machine.quality", effectiveQuality, quality);
    }
    
    private Identifier getRingIdentifier(int level) {
        return Oritech.id("textures/gui/modular/machine_core/ring_" + level + ".png");
    }
    
    public void addMachinePreview(FlowLayout sidePanel) {
        
        var floatingContent = Containers.verticalFlow(Sizing.content(), Sizing.content());
        
        var holoPreviewContainer = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(96));
        holoPreviewContainer.surface(Surface.PANEL);
        holoPreviewContainer.margins(Insets.of(2));
        
        var detailsScrollPane = Containers.verticalFlow(Sizing.content(2), Sizing.content(2));
        detailsScrollPane.padding(Insets.of(2));
        var detailsContainer = Containers.verticalScroll(Sizing.fixed(176), Sizing.fixed(110), detailsScrollPane);
        detailsContainer.surface(Surface.PANEL);
        detailsContainer.margins(Insets.of(2));
        detailsContainer.padding(Insets.of(4));
        
        floatingContent.child(holoPreviewContainer);
        floatingContent.child(detailsContainer);
        
        var floatingPanel = new OverlayContainer<>(floatingContent) {
            @Override
            public void remove() {
                super.remove();
                //handler.showSlots();
            }
        };
        
        floatingPanel.zIndex(9000);    // so it renders in front of itemslots
        
        floatingPanel
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        // create block preview renderers
        var previewX = 176 / 2 - 10;
        var previewY = 96 / 2 - 7;
        
        for (var addonBlockPos : handler.addonUiData.positions()) {
            var addonBlock = handler.worldAccess.getBlockState(addonBlockPos);
            var addonBlockEntity = handler.worldAccess.getBlockEntity(addonBlockPos);
            
            var facing  = handler.machineBlock.get(handler.screenData.getBlockFacingProperty());
            var relativePos = MultiblockMachineEntity.worldToRelativePos(handler.blockPos, addonBlockPos, facing);
            
            holoPreviewContainer.child(
              new BlockPreviewComponent(addonBlock, addonBlockEntity, relativePos, rotationSpeed)
                .sizing(Sizing.fixed(20))
                .positioning(Positioning.absolute(previewX, previewY))
            );
            
            addonBlock = addonBlock.getBlock().getDefaultState();
            
            // detailed list element
            var addonBlockType = (MachineAddonBlock) addonBlock.getBlock();
            var addonSettings = addonBlockType.getAddonSettings();
            var speed = (1 - addonSettings.speedMultiplier()) * 100;
            var efficiency = (1 - addonSettings.efficiencyMultiplier()) * 100;
            
            var blockSize = addonSettings.extender() ? 15 : 23;
            
            var detailPane = Containers.horizontalFlow(Sizing.fill(100), Sizing.content(2))
                               .child(Components.block(addonBlock).sizing(Sizing.fixed(blockSize)).margins(Insets.of(4)))
                               .child(Components.label(addonBlock.getBlock().getName()).margins(Insets.of(5, 2, 4, 2)).verticalSizing(Sizing.fixed(15)));
            
            detailPane.surface(Surface.PANEL_INSET);
            
            var bottomPanel = Containers.horizontalFlow(Sizing.content(2), Sizing.content(2));
            
            if (speed != 0) {
                bottomPanel.child(Components.label(Text.translatable("title.oritech.machine_speed", (int) speed)).color(SPEED_COLOR).tooltip(Text.translatable("tooltip.oritech.machine_speed")));
            }
            if (efficiency != 0) {
                bottomPanel.child(Components.label(Text.translatable("title.oritech.machine_efficiency", (int) efficiency)).color(EFFICIENCY_COLOR).tooltip(Text.translatable("tooltip.oritech.machine_efficiency")));
            }
            
            if (addonBlockType.getAddonSettings().addedCapacity() > 0)
                bottomPanel.child(Components.label(Text.translatable("title.oritech.machine.capacitor_added_capacity", addonSettings.addedCapacity())).color(CAPACITY_COLOR).tooltip(Text.translatable("tooltip.oritech.machine.capacitor_added_capacity")));
            if (addonBlockType.getAddonSettings().addedInsert() > 0)
                bottomPanel.child(Components.label(Text.translatable("title.oritech.machine.capacitor_added_throughput", addonSettings.addedInsert())).color(THROUGHPUT_COLOR).tooltip(Text.translatable("tooltip.oritech.machine.capacitor_added_throughput")));
            
            detailPane.child(bottomPanel.positioning(Positioning.absolute(34, 18)));
            
            detailsScrollPane.child(detailPane);
            
        }
        
        for (var openPos : handler.addonUiData.openSlots()) {
            
            var relativePos = MultiblockMachineEntity.worldToRelativePos(handler.blockPos, openPos, handler.machineBlock.get(handler.screenData.getBlockFacingProperty()));
            var dummyBlock = BlockContent.ADDON_INDICATOR_BLOCK.getDefaultState();
            
            holoPreviewContainer.child(
              new BlockPreviewComponent(dummyBlock, null, relativePos, rotationSpeed)
                .sizing(Sizing.fixed(20))
                .positioning(Positioning.absolute(previewX, previewY))
            );
        }
        
        if (handler.addonUiData.positions().isEmpty()) {
            detailsScrollPane.child(Components.label(Text.translatable("title.oritech.machine.no_addons")));
        }
        
        // machine itself
        holoPreviewContainer.child(
          new BlockPreviewComponent(handler.machineBlock, handler.blockEntity, new Vec3i(0, 0, 0), rotationSpeed)
            .sizing(Sizing.fixed(20))
            .positioning(Positioning.absolute(previewX, previewY))
        );
        
        var openAddonsButton = Components.button(Text.translatable("button.oritech.machine.addons"), button -> {
            root.child(floatingPanel);
            // handler.hideSlots();
        });
        
        sidePanel.child(openAddonsButton);
    }
}
