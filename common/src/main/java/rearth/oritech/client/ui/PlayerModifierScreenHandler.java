package rearth.oritech.client.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockContent;

public class PlayerModifierScreenHandler extends AbstractContainerMenu {
    
    @NotNull
    protected final BlockPos blockPos;
    
    public final Player player;
    
    protected final BlockState machineBlock;
    public final AugmentApplicationEntity blockEntity;
    
    public PlayerModifierScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }
    
    // on server, also called from client constructor
    public PlayerModifierScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModScreens.MODIFIER_SCREEN, syncId);
        
        if (blockEntity == null) {
            blockPos = BlockPos.ZERO;
            player = playerInventory.player;
            machineBlock = BlockContent.AUGMENT_APPLICATION_BLOCK.defaultBlockState();
            this.blockEntity = null;
            return;
        }
        
        this.blockPos = blockEntity.getBlockPos();
        this.player = playerInventory.player;
        
        this.machineBlock = blockEntity.getBlockState();
        this.blockEntity = (AugmentApplicationEntity) blockEntity;
        
        if (blockEntity.getLevel().isClientSide)
            this.blockEntity.loadAvailableStations(this.player);    // this should yield the same result on the client, so instead of syncing them we just call it on the client again
        
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        return ItemStack.EMPTY;
    }
    
    public boolean stillValid(Player player) {
        return true;
    }
    
    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        blockEntity.sendUpdate(SyncType.GUI_TICK);
    }
}
