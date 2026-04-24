package rearth.oritech.client.ui;

import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.block.entity.processing.FragmentForgeBlockEntity;
import rearth.oritech.block.entity.processing.PulverizerBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.util.ColorHelper;
import rearth.oritech.util.TooltipHelper;

import java.util.List;

public class UpgradableOritechScreen<T extends UpgradableOritechScreenHandler> extends OritechMachineScreen<T> {
    
    private static final ResourceLocation MACHINE_CORE_CENTER = Oritech.id("textures/gui/modular/machine_core/center.png");
    
    private static final int SPEED_COLOR = ColorHelper.argb(33 / 255f, 158 / 255f, 188 / 255f);
    private static final int EFFICIENCY_COLOR = ColorHelper.argb(142 / 255f, 202 / 255f, 230 / 255f);
    private static final int CAPACITY_COLOR = ColorHelper.argb(2 / 255f, 48 / 255f, 71 / 255f);
    private static final int THROUGHPUT_COLOR = ColorHelper.argb(1f, 183 / 255f, 3 / 255f);
    
    public LabelWidget speedLabel;
    public LabelWidget efficiencyLabel;
    public LabelWidget burstLabel;
    public LabelWidget steamProductionLabel;
    public OverlayWidget addonOverlay;
    
    public UpgradableOritechScreen(T handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected void addExtensionContent(List<UIComponent> content) {
        super.addExtensionContent(content);
        
        if (menu.addonController == null) return;
        
        var baseData = menu.addonController.getBaseAddonData();
        
        var speed = (int) (1f / baseData.speed() * 100);
        var efficiency = baseData.efficiency();
        var extraChambers = baseData.extraChambers();
        
        var burstKey = getBurstStatusKey();
        
        var efficiencyText = "100";
        if (efficiency > 1.03) {
            efficiency = (efficiency - 1) * 100;
            efficiency = Math.round(efficiency / 5f) * 5;
            efficiencyText = "-" + (int) efficiency;
        } else if (efficiency < 0.97) {
            efficiency = ((1f / efficiency) - 1) * 100;
            efficiency = Math.round(efficiency / 5f) * 5;
            efficiencyText = "+" + (int) efficiency;
        }
        
        speed = Math.round(speed / 5f) * 5;
        
        // Separator
        content.add(BoxWidget.filled(0, 0, 60, 1, SEPARATOR_COLOR));
        
        // Speed label
        speedLabel = new LabelWidget(0, 0, 60, 10,
          Component.translatable("title.oritech.machine_speed", speed));
        speedLabel.withTooltip(Component.translatable("tooltip.oritech.machine_speed"));
        speedLabel.withAlignment(LabelWidget.Alignment.CENTER);
        content.add(speedLabel);
        
        // Efficiency label
        efficiencyLabel = new LabelWidget(0, 0, 60, 10,
          Component.translatable("title.oritech.machine_efficiency", efficiencyText));
        efficiencyLabel.withTooltip(Component.translatable("tooltip.oritech.machine_efficiency"));
        efficiencyLabel.withAlignment(LabelWidget.Alignment.CENTER);
        content.add(efficiencyLabel);
        
        // Burst label
        if (!burstKey.isBlank()) {
            burstLabel = new LabelWidget(0, 0, 60, 10,
              Component.translatable("title.oritech." + burstKey));
            burstLabel.withTooltip(Component.translatable("title.oritech." + burstKey + ".tooltip", 0));
            burstLabel.withAlignment(LabelWidget.Alignment.CENTER);
            content.add(burstLabel);
        }
        
        // Extra chambers
        if (extraChambers > 0) {
            var chambersLabel = new LabelWidget(0, 0, 60, 10,
              Component.translatable("title.oritech.chambers", extraChambers));
            chambersLabel.withTooltip(Component.translatable("tooltip.oritech.chambers"));
            chambersLabel.withAlignment(LabelWidget.Alignment.CENTER);
            content.add(chambersLabel);
        }
        
        // Pulverizer/FragmentForge dust combine note
        if (menu.blockEntity instanceof PulverizerBlockEntity || menu.blockEntity instanceof FragmentForgeBlockEntity) {
            var dustLabel = new LabelWidget(0, 0, 60, 10,
              Component.translatable("title.oritech.machine_option_enabled"));
            dustLabel.withTooltip(Component.translatable("tooltip.oritech.pulverizer_dust_combine"));
            dustLabel.withAlignment(LabelWidget.Alignment.CENTER);
            content.add(dustLabel);
        }
        
        // Steam label
        if (menu.blockEntity instanceof UpgradableGeneratorBlockEntity generatorBlock && generatorBlock.isProducingSteam) {
            steamProductionLabel = new LabelWidget(0, 0, 60, 10, Component.translatable("title.oritech.steam_production", generatorBlock.getDisplayedEnergyUsage()));
            steamProductionLabel.withTooltip(Component.translatable("tooltip.oritech.steam_production"));
            steamProductionLabel.withAlignment(LabelWidget.Alignment.CENTER);
            content.add(steamProductionLabel);
        }
        
        // content.add(BoxWidget.filled(0, 0, 60, 1, SEPARATOR_COLOR));
        var addonBtn = ButtonWidget.panel(5, 0, 50, 14,
            Component.translatable("button.oritech.machine.addons").withColor(LabelWidget.DARK_TEXT),
            btn -> toggleAddonOverlay())
                         .withSurfacePadding(Insets.of(2, 0, 2, 0))
                         .withTextColor(LabelWidget.DARK_TEXT);
        content.add(addonBtn);
    }
    
    @Override
    protected void addExtraComponents() {
        addCoreQualityOverlay();
    }
    
    // addon preview
    private void toggleAddonOverlay() {
        if (addonOverlay != null) {
            removeComponent(addonOverlay);
            addonOverlay = null;
            return;
        }
        
        addonOverlay = new OverlayWidget(width, height);
        addonOverlay.setPosition(-leftPos, -topPos);
        addonOverlay.withBackgroundColor(ColorHelper.argb(0f, 0f, 0f, 0.5f));
        addonOverlay.withDismissHandler(() -> {
            removeComponent(addonOverlay);
            addonOverlay = null;
        });
        
        int centerX = -leftPos + width / 2;
        int centerY = -topPos + height / 2;
        
        // 3D block preview
        var preview = new BlockPreviewWidget(centerX - 186 / 2, centerY - 125, 186, 100);
        preview.withSurface(OritechSurface.PANEL);
        preview.withPadding(Insets.of(4));
        preview.withRotationSpeed(0.2f);
        
        var facing = menu.machineBlock.getValue(menu.screenData.getBlockFacingProperty());
        
        // Connected addons
        for (var addonBlockPos : menu.addonController.getConnectedAddons()) {
            var addonState = menu.worldAccess.getBlockState(addonBlockPos);
            var addonEntity = menu.worldAccess.getBlockEntity(addonBlockPos);
            var relativePos = MultiblockMachineEntity.worldToRelativePos(menu.blockPos, addonBlockPos, facing);
            preview.addBlock(addonState, addonEntity, relativePos);
        }
        
        // Open addon slots (ghost indicator)
        for (var openPos : menu.addonController.getOpenAddonSlots()) {
            var relativePos = MultiblockMachineEntity.worldToRelativePos(menu.blockPos, openPos, facing);
            preview.addBlock(BlockContent.ADDON_INDICATOR_BLOCK.defaultBlockState(), null, relativePos);
        }
        
        // Machine itself at center
        preview.addBlock(menu.machineBlock, menu.blockEntity, new Vec3i(0, 0, 0));
        
        addonOverlay.addChild(preview);
        
        // Scrollable addon details list
        var scroll = new ScrollWidget(centerX - 200 / 2, centerY - 10, 200, 130);
        scroll.withSurface(OritechSurface.PANEL);
        scroll.withPadding(Insets.of(6));
        
        var addonBlocks = menu.addonController.getConnectedAddons();
        int yOffset = 0;
        
        for (var addonBlockPos : addonBlocks) {
            var addonState = menu.worldAccess.getBlockState(addonBlockPos);
            if (!(addonState.getBlock() instanceof MachineAddonBlock addonBlock)) continue;
            
            var settings = addonBlock.getAddonSettings();
            var blockName = addonState.getBlock().getName();
            
            var icon = new ItemWidget(3, yOffset + 3, 20, new ItemStack(addonState.getBlock()));
            icon.withShowOverlay(false);
            icon.withTooltipFromStack(false);
            scroll.addChild(icon);
            
            var nameLabel = new LabelWidget(28, yOffset + 4, 140, 10, blockName);
            scroll.addChild(nameLabel);
            
            int statsY = yOffset + 17;
            int statsX = 28;
            
            var speed = (1 - settings.speedMultiplier()) * 100;
            var efficiency = (1 - settings.efficiencyMultiplier()) * 100;
            
            if (speed != 0) {
                var label = new LabelWidget(statsX, statsY, 50, 10,
                  Component.translatable("title.oritech.machine_speed", (int) speed));
                label.withColor(SPEED_COLOR);
                label.withTooltip(Component.translatable("tooltip.oritech.machine_speed"));
                scroll.addChild(label);
                statsX += 42;
            }
            if (efficiency != 0) {
                var label = new LabelWidget(statsX, statsY, 50, 10,
                  Component.translatable("title.oritech.machine_efficiency", (int) efficiency));
                label.withColor(EFFICIENCY_COLOR);
                label.withTooltip(Component.translatable("tooltip.oritech.machine_efficiency"));
                scroll.addChild(label);
                statsX += 42;
            }
            if (settings.addedCapacity() > 0) {
                var label = new LabelWidget(statsX, statsY, 60, 10,
                  Component.translatable("title.oritech.machine.capacitor_added_capacity", TooltipHelper.getEnergyText(settings.addedCapacity())));
                label.withColor(CAPACITY_COLOR);
                scroll.addChild(label);
                statsX += 62;
            }
            if (settings.addedInsert() > 0) {
                var label = new LabelWidget(statsX, statsY, 60, 10,
                  Component.translatable("title.oritech.machine.capacitor_added_throughput", TooltipHelper.getEnergyText(settings.addedInsert())));
                label.withColor(THROUGHPUT_COLOR);
                scroll.addChild(label);
            }
            
            yOffset += 28;
            
            scroll.addChild(BoxWidget.filled(0, yOffset, 192, 1, SEPARATOR_COLOR));
            
            yOffset += 1;
            
        }
        
        if (addonBlocks.isEmpty()) {
            scroll.addChild(new LabelWidget(12, 0, 190, 10,
              Component.translatable("title.oritech.machine.no_addons")));
            yOffset = 15;
        }
        
        var background = new SurfaceWidget(0, 0, 194, yOffset);
        background.setSurface(OritechSurface.PANEL_INSET);
        scroll.addChild(background.withZIndex(-1));
        
        scroll.setContentDimensions(164, yOffset);
        addonOverlay.addChild(scroll);
        addComponent(addonOverlay);
    }
    
    private void addCoreQualityOverlay() {
        if (menu.addonController == null) return;
        
        var offsetX = -5;
        var offsetY = -23;
        
        var x = menu.screenData.getEnergyConfiguration().x() + offsetX;
        var y = menu.screenData.getEnergyConfiguration().y() + offsetY;
        
        var size = 25;
        
        var level = menu.addonController.getCoreQuality();
        var upgradeCount = (int) level - 1;
        
        // The 6th upgrade ring renders behind the center
        if (upgradeCount == 6) {
            addComponent(new TextureWidget(x, y, size, size,
              getRingIdentifier(6), 0, 0, 64, 64, 64, 64));
            upgradeCount = 5;
        }
        
        // Center core
        var center = new TextureWidget(x, y, size, size,
          MACHINE_CORE_CENTER, 0, 0, 64, 64, 64, 64);
        center.withTooltip(getQualityTooltip());
        addComponent(center);
        
        // Ring overlays
        for (int i = 1; i <= upgradeCount; i++) {
            addComponent(new TextureWidget(x, y, size, size,
              getRingIdentifier(i), 0, 0, 64, 64, 64, 64));
        }
    }
    
    @Override
    protected void tickExtra() {
        if (burstLabel != null) {
            var burstKey = getBurstStatusKey();
            if (!burstKey.isBlank()) {
                var burstTicks = 0;
                if (menu.addonController instanceof UpgradableMachineBlockEntity upgradableMachineBlock)
                    burstTicks = upgradableMachineBlock.remainingBurstTicks;
                
                burstLabel.setText(Component.translatable("title.oritech." + burstKey));
                burstLabel.setTooltip(List.of(Component.translatable("title.oritech." + burstKey + ".tooltip", burstTicks)));
            }
        }
        
        if (steamProductionLabel != null && menu.blockEntity instanceof UpgradableGeneratorBlockEntity generatorBlock) {
            var productionRate = menu.screenData.getDisplayedEnergyUsage() * OritechConfig.generators.steamEngineData.rfToSteamRatio.get();
            productionRate = Math.min(productionRate, generatorBlock.boilerStorage.getInputContainer().getStack().getAmount());
            steamProductionLabel.setText(Component.translatable("title.oritech.steam_production", String.format("%.0f", productionRate)));
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
        if (menu.addonController instanceof UpgradableMachineBlockEntity upgradableMachineBlock) {
            var isWorking = upgradableMachineBlock.isActivelyWorking();
            var canBurst = upgradableMachineBlock.isBurstAvailable();
            var isThrottled = upgradableMachineBlock.isBurstThrottled();
            
            if (isThrottled) return "burst.throttled";
            else if (isWorking && canBurst) return "burst.active";
            else if (canBurst) return "burst.ready";
        }
        return "";
    }
}
