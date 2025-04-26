package rearth.oritech.client.ui;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockContent;

import java.util.Objects;

public class PlayerModifierScreenHandler extends ScreenHandler {
    
    @NotNull
    protected final BlockPos blockPos;
    
    public final PlayerEntity player;
    
    protected final BlockState machineBlock;
    public final AugmentApplicationEntity blockEntity;
    
    public PlayerModifierScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, inventory.player.getWorld().getBlockEntity(buf.readBlockPos()));
    }
    
    // on server, also called from client constructor
    public PlayerModifierScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(ModScreens.MODIFIER_SCREEN, syncId);
        
        if (blockEntity == null) {
            blockPos = BlockPos.ORIGIN;
            player = playerInventory.player;
            machineBlock = BlockContent.AUGMENT_APPLICATION_BLOCK.getDefaultState();
            this.blockEntity = null;
            return;
        }
        
        this.blockPos = blockEntity.getPos();
        this.player = playerInventory.player;
        
        this.machineBlock = blockEntity.getCachedState();
        this.blockEntity = (AugmentApplicationEntity) blockEntity;
        
        if (blockEntity.getWorld().isClient)
            this.blockEntity.loadAvailableStations(this.player);    // this should yield the same result on the client, so instead of syncing them we just call it on the client again
        
        // add dummy slot positions to allow inv sync
//        for (int i = 0; i < 5; i++) {
//            this.addSlot(new Slot(this.blockEntity.inventory, i, -500, -500));
//        }
        
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        return ItemStack.EMPTY;
    }
    
    public boolean canUse(PlayerEntity player) {
        return true;
    }
    
    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }
    
}
