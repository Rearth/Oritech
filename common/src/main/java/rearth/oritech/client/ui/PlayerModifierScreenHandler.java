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
import rearth.oritech.block.entity.interaction.PlayerModifierTestEntity;
import rearth.oritech.client.init.ModScreens;

public class PlayerModifierScreenHandler extends ScreenHandler {
    
    @NotNull
    protected final PlayerInventory playerInventory;
    
    @NotNull
    protected final BlockPos blockPos;
    
    public final PlayerEntity player;
    
    protected BlockState machineBlock;
    public PlayerModifierTestEntity blockEntity;
    
    public PlayerModifierScreenHandler(int syncId, PlayerInventory inventory, ModScreens.BasicData setupData) {
        this(syncId, inventory, inventory.player.getWorld().getBlockEntity(setupData.pos()));
    }
    
    // on server, also called from client constructor
    public PlayerModifierScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(ModScreens.MODIFIER_SCREEN, syncId);
        
        this.blockPos = blockEntity.getPos();
        this.playerInventory = playerInventory;
        this.player = playerInventory.player;
        
        this.machineBlock = blockEntity.getCachedState();
        this.blockEntity = (PlayerModifierTestEntity) blockEntity;
        
        buildItemSlots();
    }
    
    private void buildItemSlots() {
        
//        SlotGenerator.begin(this::addSlot, 8, 84)
//          .playerInventory(playerInventory);
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
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
