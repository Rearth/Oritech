package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.api.screen.data.FluidDisplayWidget;
import rearth.oritech.api.screen.widgets.BoxWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

public class SteamEngineScreen extends UpgradableOritechScreen<UpgradableOritechScreenHandler> {
    
    protected LabelWidget productionLabel;
    protected LabelWidget steamConsumptionLabel;
    
    public SteamEngineScreen(UpgradableOritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected void addExtensionContent(List<UIComponent> content) {
        super.addExtensionContent(content);

        var steamEntity = ((SteamEngineEntity) menu.blockEntity);
        var data = steamEntity.clientStats;
        if (data == null) return;
        var workerCount = data.slaves();
        
        content.add(BoxWidget.filled(0, 0, 60, 1, SEPARATOR_COLOR).withPadding(Insets.of(2, 0, 2, 0)));

        productionLabel = new LabelWidget(0, 0, 60, 10,
            Component.translatable("title.oritech.steam_energy_production", 0));
        productionLabel.withTooltip(Component.translatable("tooltip.oritech.steam_energy_production"));
        productionLabel.withAlignment(LabelWidget.Alignment.CENTER);
        content.add(productionLabel);

        steamConsumptionLabel = new LabelWidget(0, 0, 60, 10,
            Component.translatable("title.oritech.steam_consumption", 0));
        steamConsumptionLabel.withTooltip(Component.translatable("tooltip.oritech.steam_consumption", 0));
        steamConsumptionLabel.withAlignment(LabelWidget.Alignment.CENTER);
        content.add(steamConsumptionLabel);

        if (workerCount > 0) {
            var workerLabel = new LabelWidget(0, 0, 60, 10,
                Component.translatable("title.oritech.chambers", workerCount));
            workerLabel.withTooltip(Component.translatable("tooltip.oritech.steam_workers"));
            workerLabel.withAlignment(LabelWidget.Alignment.CENTER);
            content.add(workerLabel);
        }
    }
    
    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        var steamEntity = (SteamEngineEntity) menu.blockEntity;
        addComponent(new FluidDisplayWidget(DisplayDataSource.CreateFluid(
            steamEntity.boilerStorage.getOutputContainer(),
            getBoilerInConfig(),
            menu.screenData)));
        addComponent(new FluidDisplayWidget(DisplayDataSource.CreateFluid(
            steamEntity.boilerStorage.getInputContainer(),
            getBoilerOutConfig(),
            menu.screenData)));
    }

    @Override
    protected void tickExtra() {
        super.tickExtra();

        var steamEntity = ((SteamEngineEntity) menu.blockEntity);
        var data = steamEntity.clientStats;
        if (data == null || productionLabel == null) return;

        var rfProduced = data.energyProduced();
        var steamUsed = data.steamConsumed();

        var speed = String.format("%.0f", data.speed() * 100);
        var efficiency = String.format("%.0f", data.efficiency() * 100);
        var totalSteamUsage = String.format("%.0f", (float) steamUsed);

        if (speedLabel != null)
            speedLabel.setText(Component.translatable("title.oritech.machine_speed", speed));
        if (efficiencyLabel != null)
            efficiencyLabel.setText(Component.translatable("title.oritech.machine_efficiency", efficiency));
        productionLabel.setText(Component.translatable("title.oritech.machine_energy_production", rfProduced));
        steamConsumptionLabel.setText(Component.translatable("title.oritech.steam_consumption", totalSteamUsage));
    }
    
    public ScreenProvider.BarConfiguration getBoilerInConfig() {
        return menu.screenData.getFluidConfiguration();
    }
    
    public ScreenProvider.BarConfiguration getBoilerOutConfig() {
        var config = getBoilerInConfig();
        return new ScreenProvider.BarConfiguration(config.x() - config.width() - 8, config.y(), config.width(), config.height());
    }
}
