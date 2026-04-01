package rearth.oritech.client.ui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;
import rearth.oritech.util.ScreenProvider;

import java.util.Objects;

public class RefineryScreenHandler extends OritechScreenHandler {
    
    private static final ScreenProvider.BarConfiguration OUTPUT_A_CONFIG = new ScreenProvider.BarConfiguration(92, 6, 21, 74);
    private static final ScreenProvider.BarConfiguration OUTPUT_B_CONFIG = new ScreenProvider.BarConfiguration(92 + 27, 6, 21, 74);
    private static final ScreenProvider.BarConfiguration OUTPUT_C_CONFIG = new ScreenProvider.BarConfiguration(92 + 27 * 2, 6, 21, 74);
    
    public RefineryScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public RefineryScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
    }

    @Override
    public void addFluidDisplay() {
        if (!(blockEntity instanceof RefineryBlockEntity refinery)) {
            throw new IllegalStateException("Opened refinery screen on non-refinery block, this should never happen");
        }

        getDataDisplays().add(DisplayDataSource.CreateFluid(
            refinery.ownStorage.getInputContainer(),
            this.screenData.getFluidConfiguration(),
            this.screenData));
        getDataDisplays().add(DisplayDataSource.CreateFluid(
            refinery.ownStorage.getOutputContainer(),
            OUTPUT_A_CONFIG,
            this.screenData));
        getDataDisplays().add(DisplayDataSource.CreateFluid(
            refinery.nodeA,
            OUTPUT_B_CONFIG,
            this.screenData));
        getDataDisplays().add(DisplayDataSource.CreateFluid(
            refinery.nodeB,
            OUTPUT_C_CONFIG,
            this.screenData));
    }
}
