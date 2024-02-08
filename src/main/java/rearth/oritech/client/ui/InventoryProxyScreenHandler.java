package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.InventoryProxyAddonBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.util.ScreenProvider;

import java.util.Objects;

public class InventoryProxyScreenHandler extends ScreenHandler {
    
    @NotNull
    protected final BlockPos blockPos;
    @NotNull
    protected final InventoryProxyAddonBlockEntity addonEntity;
    
    protected int selectedSlot = 0;

    @NotNull
    protected final ScreenProvider controllerScreen;

    // on client, receiving data from writeScreenOpeningData
    public InventoryProxyScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId,
          inventory,
          Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())),
          (ScreenProvider) inventory.player.getWorld().getBlockEntity(buf.readBlockPos()),
          buf.readInt());
    }

    // on server, also called from client constructor
    public InventoryProxyScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, @NotNull ScreenProvider controllerScreen, int selectedSlot) {
        super(ModScreens.INVENTORY_PROXY_SCREEN, syncId);
        
        this.blockPos = blockEntity.getPos();
        this.controllerScreen = controllerScreen;
        this.selectedSlot = selectedSlot;
        this.addonEntity = (InventoryProxyAddonBlockEntity) blockEntity;
    }

    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
