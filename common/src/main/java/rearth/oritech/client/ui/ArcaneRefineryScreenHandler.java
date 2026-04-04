package rearth.oritech.client.ui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.block.entity.processing.ArcaneRefineryBlockEntity;
import rearth.oritech.util.ScreenProvider;

import java.util.Objects;

public class ArcaneRefineryScreenHandler extends OritechScreenHandler {
    
    private static final ScreenProvider.BarConfiguration OUTPUT_A_CONFIG = new ScreenProvider.BarConfiguration(92, 6, 21, 74);
    
    public ArcaneRefineryScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public ArcaneRefineryScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
    }
    
    @Override
    protected void addEnergyDisplay() {
        // super.addEnergyDisplay();
        // todo
    }
    
    @Override
    public void addFluidDisplay() {
        if (!(blockEntity instanceof ArcaneRefineryBlockEntity refinery)) {
            throw new IllegalStateException("Opened arcane refinery screen on non-refinery block, this should never happen");
        }

        getDataDisplays().add(DisplayDataSource.CreateFluid(
            refinery.ownStorage.getInputContainer(),
            this.screenData.getFluidConfiguration(),
            this.screenData));
        getDataDisplays().add(DisplayDataSource.CreateFluid(
            refinery.ownStorage.getOutputContainer(),
          new ScreenProvider.BarConfiguration(65, 6, 18, 52),
            this.screenData));
    }
}
