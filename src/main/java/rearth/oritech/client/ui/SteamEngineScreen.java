package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import rearth.oritech.block.entity.machines.generators.SteamEngineEntity;
import rearth.oritech.util.ScreenProvider;

public class SteamEngineScreen extends UpgradableMachineScreen<SteamEngineScreenHandler> {
    
    private final FluidDisplay waterDisplay;
    protected LabelComponent productionLabel;
    protected LabelComponent steamUsageLabel;
    
    public SteamEngineScreen(SteamEngineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        
        var config = handler.screenData.getFluidConfiguration();
        var offset = -config.width() - 8;
        var configWater = new ScreenProvider.BarConfiguration(config.x() + offset, config.y(), config.width(), config.height());
        var container = handler.engineWaterStorage;
        waterDisplay = initFluidDisplay(container, configWater);
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        addFluidDisplay(overlay, waterDisplay);
        updateFluidDisplay(waterDisplay);
    }
    
    @Override
    public void addExtensionComponents(FlowLayout container) {
        super.addExtensionComponents(container);
        
        productionLabel = Components.label(Text.translatable("title.oritech.steam_energy_production", 0));
        container.child(productionLabel.tooltip(Text.translatable("tooltip.oritech.steam_energy_production")).margins(Insets.of(3)));
        
        steamUsageLabel = Components.label(Text.translatable("title.oritech.steam_consumption", 0));
        container.child(steamUsageLabel.tooltip(Text.translatable("tooltip.oritech.steam_consumption")).margins(Insets.of(3)));
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        updateFluidDisplay(waterDisplay);
        
        var steamEntity = ((SteamEngineEntity) handler.blockEntity);
        var data = steamEntity.getBaseAddonData();
        
        var speed = String.format("%.0f", 1 / data.speed() * 100);
        var efficiency = String.format("%.0f", 1 / data.efficiency() * 100);
        var totalProduction = steamEntity.energyProducedTick;
        var totalSteamUsage = String.format("%.0f", steamEntity.energyProducedTick * data.efficiency() / steamEntity.getEnergyPerTick());
        
        speedLabel.text(Text.translatable("title.oritech.machine_speed", speed));
        efficiencyLabel.text(Text.translatable("title.oritech.machine_efficiency", efficiency));
        productionLabel.text(Text.translatable("title.oritech.steam_production", totalProduction));
        steamUsageLabel.text(Text.translatable("title.oritech.steam_consumption", totalSteamUsage));
    }
}
