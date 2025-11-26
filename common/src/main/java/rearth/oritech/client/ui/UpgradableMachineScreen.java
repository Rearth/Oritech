package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.block.entity.processing.FragmentForgeBlockEntity;
import rearth.oritech.block.entity.processing.PulverizerBlockEntity;
import rearth.oritech.client.ui.components.BlockPreviewComponent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.MachineAddonController;


public class UpgradableMachineScreen<S extends UpgradableMachineScreenHandler> extends BasicMachineScreen<S> {
    
    private static final Color SPEED_COLOR = Color.ofRgb(0x219ebc);
    private static final Color EFFICIENCY_COLOR = Color.ofRgb(0x8ecae6);
    private static final Color CAPACITY_COLOR = Color.ofRgb(0x023047);
    private static final Color THROUGHPUT_COLOR = Color.ofRgb(0xffb703);
    
    private static final float rotationSpeed = 0.2f;
    
    private static final ResourceLocation MACHINE_CORE_CENTER = Oritech.id("textures/gui/modular/machine_core/center.png");
    
    protected LabelComponent speedLabel;
    protected LabelComponent efficiencyLabel;
    protected LabelComponent burstLabel;
    
    public UpgradableMachineScreen(S handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    public void addExtensionComponents(FlowLayout container) {
        super.addExtensionComponents(container);
        
        if (menu.addonController == null) return;
        
        var baseData = menu.addonController.getBaseAddonData();
        
        var speed = (int) (1f / baseData.speed() * 100);
        var efficiency = baseData.efficiency();
        var extraChambers = baseData.extraChambers();
        
        var burstKey = getBurstStatusKey();
        
        var efficiencyText = "100";
        
        // transform the efficiency chang
        if (efficiency > 1.03) {    // bad efficiency
            efficiency = (efficiency - 1) * 100;
            efficiency = Math.round(efficiency / 5f) * 5;
            efficiencyText = "-" + (int) efficiency;
        } else if (efficiency < 0.97) {
            efficiency = ((1f / efficiency) - 1) * 100;
            efficiency = Math.round(efficiency / 5f) * 5;
            efficiencyText = "+" + (int) efficiency;
        }
        
        // round to nearest 5
        speed = Math.round(speed / 5f) * 5;
        
        
        speedLabel = Components.label(Component.translatable("title.oritech.machine_speed", speed));
        efficiencyLabel = Components.label(Component.translatable("title.oritech.machine_efficiency", efficiencyText));
        burstLabel = Components.label(Component.translatable("title.oritech." + burstKey));
        burstLabel.tooltip(Component.translatable("title.oritech." + burstKey + ".tooltip", 0));
        
        container.child(Components.box(Sizing.fixed(73), Sizing.fixed(1)).color(new Color(0.8f, 0.8f, 0.8f)));
        container.child(speedLabel.tooltip(Component.translatable("tooltip.oritech.machine_speed")).margins(Insets.of(3)));
        container.child(efficiencyLabel.tooltip(Component.translatable("tooltip.oritech.machine_efficiency")).margins(Insets.of(3)));
        
        if (!burstKey.isBlank()) {
            container.child(burstLabel.margins(Insets.of(3)));
        }
        
        if (extraChambers > 0) {
            container.child(Components.label(Component.translatable("title.oritech.chambers", extraChambers)).tooltip(Component.translatable("tooltip.oritech.chambers")).margins(Insets.of(3)));
        }
        
        if (steamProductionLabel != null)
            container.child(steamProductionLabel.margins(Insets.of(3)));
        
        if (menu.blockEntity instanceof PulverizerBlockEntity || menu.blockEntity instanceof FragmentForgeBlockEntity) {
            container.child(Components.label(Component.translatable("title.oritech.machine_option_enabled")).tooltip(Component.translatable("tooltip.oritech.pulverizer_dust_combine")).margins(Insets.of(3)));
        }
        
        if (!((MachineAddonController) menu.blockEntity).getAddonSlots().isEmpty())
            addMachinePreview(container);
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        var burstKey = getBurstStatusKey();
        if (!burstKey.isBlank()) {
            
            var burstTicks = 0;
            if (this.menu.addonController instanceof UpgradableMachineBlockEntity upgradableMachineBlock)
                burstTicks = upgradableMachineBlock.remainingBurstTicks;
            
            burstLabel.text(Component.translatable("title.oritech." + burstKey));
            burstLabel.tooltip(Component.translatable("title.oritech." + burstKey + ".tooltip", burstTicks));
        }
        
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        var offsetX = -5;
        var offsetY = -23;
        
        var x = menu.screenData.getEnergyConfiguration().x() + offsetX;
        var y = menu.screenData.getEnergyConfiguration().y() + offsetY;
        
        var size = 25;
        
        if (menu.addonController == null) return;
        
        var level = menu.addonController.getCoreQuality();
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
    
    private Component getQualityTooltip() {
        var quality = String.format("%.2f", menu.addonController.getCoreQuality());
        var effectiveQuality = (int) menu.addonController.getCoreQuality();
        return Component.translatable("tooltip.oritech.machine.quality", effectiveQuality, quality);
    }
    
    private ResourceLocation getRingIdentifier(int level) {
        return Oritech.id("textures/gui/modular/machine_core/ring_" + level + ".png");
    }
    
    private String getBurstStatusKey() {
        
        if (this.menu.addonController instanceof UpgradableMachineBlockEntity upgradableMachineBlock) {
            var isWorking = upgradableMachineBlock.isActivelyWorking();
            var canBurst = upgradableMachineBlock.isBurstAvailable();
            var isThrottled = upgradableMachineBlock.isBurstThrottled();
            
            if (isThrottled) {
                return "burst.throttled";
            } else if (isWorking && canBurst) {
                return "burst.active";
            } else if (canBurst) {
                return "burst.ready";
            }
            
        }
        
        return "";
    }
    
    public void addMachinePreview(FlowLayout sidePanel) {
        
        var floatingContent = Containers.verticalFlow(Sizing.content(), Sizing.content());
        
        var holoPreviewContainer = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(96));
        holoPreviewContainer.surface(ORITECH_PANEL);
        holoPreviewContainer.margins(Insets.of(2));
        
        var detailsScrollPane = Containers.verticalFlow(Sizing.content(2), Sizing.content(2));
        detailsScrollPane.padding(Insets.of(2));
        var detailsContainer = Containers.verticalScroll(Sizing.fixed(176), Sizing.fixed(110), detailsScrollPane);
        detailsContainer.surface(ORITECH_PANEL);
        detailsContainer.margins(Insets.of(2));
        detailsContainer.padding(Insets.of(4));
        
        floatingContent.child(holoPreviewContainer);
        floatingContent.child(detailsContainer);
        
        var slotCount = this.menu.slots.size();
        var floatingPanel = new OverlayContainer<>(floatingContent) {
            @Override
            public void remove() {
                super.remove();
                for (int i = 0; i < slotCount; i++) {
                    UpgradableMachineScreen.this.enableSlot(i);
                }
            }
        };
        
        floatingPanel.zIndex(9000);    // so it renders in front of itemslots
        
        floatingPanel
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        // create block preview renderers
        var previewX = 176 / 2 - 10;
        var previewY = 96 / 2 - 7;
        
        var addonBlocks = menu.addonController.getConnectedAddons();
        
        for (var addonBlockPos : addonBlocks) {
            var addonBlock = menu.worldAccess.getBlockState(addonBlockPos);
            var addonBlockEntity = menu.worldAccess.getBlockEntity(addonBlockPos);
            
            var facing = menu.machineBlock.getValue(menu.screenData.getBlockFacingProperty());
            var relativePos = MultiblockMachineEntity.worldToRelativePos(menu.blockPos, addonBlockPos, facing);
            
            holoPreviewContainer.child(
              new BlockPreviewComponent(addonBlock, addonBlockEntity, relativePos, rotationSpeed)
                .sizing(Sizing.fixed(20))
                .positioning(Positioning.absolute(previewX, previewY))
            );
            
            addonBlock = addonBlock.getBlock().defaultBlockState();
            
            if (addonBlock.hasProperty(MachineAddonBlock.ADDON_USED)) {
                addonBlock = addonBlock.setValue(MachineAddonBlock.ADDON_USED, true);
            }
            
            // detailed list element
            var addonBlockType = (MachineAddonBlock) addonBlock.getBlock();
            var addonSettings = addonBlockType.getAddonSettings();
            var speed = (1 - addonSettings.speedMultiplier()) * 100;
            var efficiency = (1 - addonSettings.efficiencyMultiplier()) * 100;
            
            var blockSize = 23;
            
            var detailPane = Containers.horizontalFlow(Sizing.fill(100), Sizing.content(2))
                               .child(Components.block(addonBlock).sizing(Sizing.fixed(blockSize)).margins(Insets.of(4)))
                               .child(Components.label(addonBlock.getBlock().getName()).margins(Insets.of(5, 2, 4, 2)).verticalSizing(Sizing.fixed(15)));
            
            detailPane.surface(Surface.PANEL_INSET);
            
            var bottomPanel = Containers.horizontalFlow(Sizing.content(2), Sizing.content(2));
            
            if (speed != 0) {
                bottomPanel.child(Components.label(Component.translatable("title.oritech.machine_speed", (int) speed)).color(SPEED_COLOR).tooltip(Component.translatable("tooltip.oritech.machine_speed")));
            }
            if (efficiency != 0) {
                bottomPanel.child(Components.label(Component.translatable("title.oritech.machine_efficiency", (int) efficiency)).color(EFFICIENCY_COLOR).tooltip(Component.translatable("tooltip.oritech.machine_efficiency")));
            }
            
            if (addonBlockType.getAddonSettings().addedCapacity() > 0)
                bottomPanel.child(Components.label(Component.translatable("title.oritech.machine.capacitor_added_capacity", addonSettings.addedCapacity())).color(CAPACITY_COLOR).tooltip(Component.translatable("tooltip.oritech.machine.capacitor_added_capacity")));
            if (addonBlockType.getAddonSettings().addedInsert() > 0)
                bottomPanel.child(Components.label(Component.translatable("title.oritech.machine.capacitor_added_throughput", addonSettings.addedInsert())).color(THROUGHPUT_COLOR).tooltip(Component.translatable("tooltip.oritech.machine.capacitor_added_throughput")));
            
            detailPane.child(bottomPanel.positioning(Positioning.absolute(34, 18)));
            
            detailsScrollPane.child(detailPane);
            
        }
        
        for (var openPos : menu.addonController.getOpenAddonSlots()) {
            
            var relativePos = MultiblockMachineEntity.worldToRelativePos(menu.blockPos, openPos, menu.machineBlock.getValue(menu.screenData.getBlockFacingProperty()));
            var dummyBlock = BlockContent.ADDON_INDICATOR_BLOCK.defaultBlockState();
            
            holoPreviewContainer.child(
              new BlockPreviewComponent(dummyBlock, null, relativePos, rotationSpeed)
                .sizing(Sizing.fixed(20))
                .positioning(Positioning.absolute(previewX, previewY))
            );
        }
        
        if (addonBlocks.isEmpty()) {
            detailsScrollPane.child(Components.label(Component.translatable("title.oritech.machine.no_addons")));
        }
        
        // machine itself
        holoPreviewContainer.child(
          new BlockPreviewComponent(menu.machineBlock, menu.blockEntity, new Vec3i(0, 0, 0), rotationSpeed)
            .sizing(Sizing.fixed(20))
            .positioning(Positioning.absolute(previewX, previewY))
        );
        
        var openAddonsButton = Components.button(Component.translatable("button.oritech.machine.addons").withColor(BasicMachineScreen.GRAY_TEXT_COLOR), button -> {
            root.child(floatingPanel);
            for (int i = 0; i < this.menu.slots.size(); i++) {
                this.disableSlot(i);
            }
        });
        openAddonsButton.renderer(ORITECH_BUTTON);
        openAddonsButton.textShadow(false);
        
        sidePanel.child(openAddonsButton);
    }
}
