package rearth.oritech.client.ui;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
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
        
        if (blockEntity.getWorld().isClient)
            this.blockEntity.loadAvailableStations(this.player);    // this should yield the same result on the client, so instead of syncing them we just call it on the client again
        
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
    
    public static class HandlerFactory implements ExtendedScreenHandlerType.ExtendedFactory<PlayerModifierScreenHandler, ModScreens.BasicData> {
        @Override
        public PlayerModifierScreenHandler create(int syncId, PlayerInventory inventory, ModScreens.BasicData data) {
            return new PlayerModifierScreenHandler(syncId, inventory, data);
        }
    }
    
}
