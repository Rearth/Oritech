package rearth.oritech.client.ui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;
import rearth.oritech.util.ScreenProvider;

import java.util.Objects;

public class CentrifugeScreenHandler extends UpgradableOritechScreenHandler {
    
    public CentrifugeScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public CentrifugeScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
    }
    
    @Override
    public void addFluidDisplay() {
        if (!(blockEntity instanceof CentrifugeBlockEntity centrifugeEntity) || !centrifugeEntity.hasFluidAddon) return;
        
        getDataDisplays().add(DisplayDataSource.CreateFluid(
          centrifugeEntity.fluidContainer.getInputContainer(),
          new ScreenProvider.BarConfiguration(28, 6, 21, 74),
          this.screenData));
        
        getDataDisplays().add(DisplayDataSource.CreateFluid(
          centrifugeEntity.fluidContainer.getOutputContainer(),
          this.screenData.getFluidConfiguration(),
          this.screenData));
    }
}
