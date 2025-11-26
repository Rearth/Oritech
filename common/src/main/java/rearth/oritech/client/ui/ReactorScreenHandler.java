package rearth.oritech.client.ui;

import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.entity.reactor.ReactorControllerBlockEntity;
import rearth.oritech.client.init.ModScreens;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ReactorScreenHandler extends AbstractContainerMenu {
    
    public final ReactorControllerBlockEntity reactorEntity;
    public final Level world;
    
    // this calls the second version
    public ReactorScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    // on server, also called from client constructor
    public ReactorScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModScreens.REACTOR_SCREEN, syncId);
        
        reactorEntity = (ReactorControllerBlockEntity) blockEntity;
        world = blockEntity.getLevel();
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
    public boolean stillValid(Player player) {
        return true;
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        reactorEntity.sendUpdate(SyncType.GUI_TICK);
    }
}
