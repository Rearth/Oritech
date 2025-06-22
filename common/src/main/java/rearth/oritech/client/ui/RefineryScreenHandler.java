package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

public class RefineryScreenHandler extends UpgradableMachineScreenHandler {
    
    protected final RefineryBlockEntity refinery;
    protected FluidApi.SingleSlotStorage outputAContainer;
    protected FluidApi.SingleSlotStorage outputBContainer;
    protected FluidApi.SingleSlotStorage outputCContainer;
    
    public RefineryScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, ModScreens.UpgradableData.PACKET_CODEC.decode(buf));
    }
    
    public RefineryScreenHandler(int syncId, PlayerInventory inventory, ModScreens.UpgradableData data) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(data.pos())), data.addonUiData(), data.coreQuality());
    }
    
    public RefineryScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, MachineAddonController.AddonUiData addonUiData, float coreQuality) {
        super(syncId, playerInventory, blockEntity, addonUiData, coreQuality);
        
        if (!(blockEntity instanceof RefineryBlockEntity refineryEntity)) {
            throw new IllegalStateException("Opened centrifuge screen on non-centrifuge block, this should never happen");
        }
        
        this.refinery = refineryEntity;
        
        this.mainFluidContainer = refineryEntity.ownStorage.getInputContainer();
        this.outputAContainer = refineryEntity.ownStorage.getOutputContainer();
        this.outputBContainer = refineryEntity.nodeA;
        this.outputCContainer = refineryEntity.nodeB;
        
    }
}
