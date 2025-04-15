package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.util.ScreenProvider;

public class SteamEngineScreen extends UpgradableMachineScreen<UpgradableMachineScreenHandler> {
    
    protected LabelComponent productionLabel;
    
    public SteamEngineScreen(UpgradableMachineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    public void addExtensionComponents(FlowLayout container) {
        super.addExtensionComponents(container);
        
        var steamEntity = ((SteamEngineEntity) handler.blockEntity);
        var data = steamEntity.clientStats;
        if (data == null) return;
        var workerCount = data.slaves();
        
        productionLabel = Components.label(Text.translatable("title.oritech.steam_energy_production", 0));
        container.child(productionLabel.tooltip(Text.translatable("tooltip.oritech.steam_energy_production")).margins(Insets.of(3)));
        
        steamProductionLabel.text(Text.translatable("title.oritech.steam_consumption", 0));
        steamProductionLabel.tooltip(Text.translatable("tooltip.oritech.steam_consumption", 0));
        
        if (workerCount > 0) {
            container.child(Components.label(Text.translatable("title.oritech.chambers", workerCount)).tooltip(Text.translatable("tooltip.oritech.steam_workers")).margins(Insets.of(3)));
        }
        
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        updateEnergyBar();
        
        var steamEntity = ((SteamEngineEntity) handler.blockEntity);
        var data = steamEntity.clientStats;
        if (data == null) return;
        
        var rfProduced = data.energyProduced();
        var steamUsed = data.steamConsumed();
        
        var speed = String.format("%.0f", data.speed() * 100);
        var efficiency = String.format("%.0f", data.efficiency() * 100);
        var totalSteamUsage = String.format("%.0f", (float) steamUsed);
        
        speedLabel.text(Text.translatable("title.oritech.machine_speed", speed));
        efficiencyLabel.text(Text.translatable("title.oritech.machine_efficiency", efficiency));
        productionLabel.text(Text.translatable("title.oritech.machine_energy_production", rfProduced));
        steamProductionLabel.text(Text.translatable("title.oritech.steam_consumption", totalSteamUsage));
    }
    
    @Override
    public ScreenProvider.BarConfiguration getBoilerInConfig() {
        return handler.screenData.getFluidConfiguration();
    }
    
    @Override
    public ScreenProvider.BarConfiguration getBoilerOutConfig() {
        var config = getBoilerInConfig();
        return new ScreenProvider.BarConfiguration(config.x() - config.width() - 8, config.y(), config.width(), config.height());
    }
}
