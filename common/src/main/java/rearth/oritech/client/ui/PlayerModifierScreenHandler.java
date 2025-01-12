package rearth.oritech.client.ui;

import io.wispforest.owo.client.screens.SlotGenerator;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.augmenter.PlayerModifierTestEntity;
import rearth.oritech.client.init.ModScreens;

public class PlayerModifierScreenHandler extends ScreenHandler {
    
    @NotNull
    protected final BlockPos blockPos;
    
    public final PlayerEntity player;
    
    protected BlockState machineBlock;
    public PlayerModifierTestEntity blockEntity;
    
    public PlayerModifierScreenHandler(int syncId, PlayerInventory inventory, ModScreens.BasicData setupData) {
        this(syncId, inventory, inventory.player.getWorld().getBlockEntity(setupData.pos()));
    }
    
    // on server, also called from client constructor
    public PlayerModifierScreenHandler(int syncId, PlayerInventory playerInventory, @NotNull BlockEntity blockEntity) {
        super(ModScreens.MODIFIER_SCREEN, syncId);
        
        
        this.blockPos = blockEntity.getPos();
        this.player = playerInventory.player;
        
        this.machineBlock = blockEntity.getCachedState();
        this.blockEntity = (PlayerModifierTestEntity) blockEntity;
        this.blockEntity.inventory.onOpen(player);
        
        if (blockEntity.getWorld().isClient)
            this.blockEntity.loadAvailableStations(this.player);    // this should yield the same result on the client, so instead of syncing them we just call it on the client again
        
        
        SlotGenerator.begin(this::addSlot, 80, 84)
          .playerInventory(playerInventory);
        
        for (int i = 0; i < this.blockEntity.inventory.size(); i++) {
            this.addSlot(new Slot(this.blockEntity.inventory, i, 0, 0));
        }
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        
        var newStack = ItemStack.EMPTY;
        
        var slot = this.slots.get(invSlot);
        var inventory = this.blockEntity.inventory;
        
        if (slot.hasStack()) {
            var originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot >= 36) {
                if (!this.insertItem(originalStack, getPlayerInvStartSlot(newStack), getPlayerInvEndSlot(newStack), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, getMachineInvStartSlot(newStack), getMachineInvEndSlot(newStack), false)) {
                return ItemStack.EMPTY;
            }
            
            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        
        return newStack;
    }
    
    
    
    public int getPlayerInvStartSlot(ItemStack stack) {
        return 0;
    }
    
    public int getPlayerInvEndSlot(ItemStack stack) {
        return 36;
    }
    
    public int getMachineInvStartSlot(ItemStack stack) {
        return 36;
    }
    
    public int getMachineInvEndSlot(ItemStack stack) {
        return 36 + 4;
    }
    
    public boolean canUse(PlayerEntity player) {
        return true;
    }
    
    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }
    
    public static class HandlerFactory implements ExtendedScreenHandlerType.ExtendedFactory<PlayerModifierScreenHandler, ModScreens.BasicData> {
        @Override
        public PlayerModifierScreenHandler create(int syncId, PlayerInventory inventory, ModScreens.BasicData data) {
            return new PlayerModifierScreenHandler(syncId, inventory, data);
        }
    }
    
}
