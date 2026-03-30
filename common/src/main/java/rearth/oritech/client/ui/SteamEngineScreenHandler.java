package rearth.oritech.client.ui;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.util.ScreenProvider;

public class SteamEngineScreenHandler extends UpgradableOritechScreenHandler {

    public SteamEngineScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }

    public SteamEngineScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
    }

    @Override
    public void addFluidDisplay() {
        if (!(blockEntity instanceof SteamEngineEntity steamEngine)) {
            throw new IllegalStateException("Opened steam engine screen on non-steam-engine block, this should never happen");
        }

        getDataDisplays().add(DisplayDataSource.CreateFluid(
            steamEngine.boilerStorage.getOutputContainer(),
            steamEngine.getFluidConfiguration(),
            screenData));
        getDataDisplays().add(DisplayDataSource.CreateFluid(
            steamEngine.boilerStorage.getInputContainer(),
            getBoilerOutConfig(),
            screenData));
    }

    private ScreenProvider.BarConfiguration getBoilerOutConfig() {
        var config = screenData.getFluidConfiguration();
        return new ScreenProvider.BarConfiguration(config.x() - config.width() - 8, config.y(), config.width(), config.height());
    }
}