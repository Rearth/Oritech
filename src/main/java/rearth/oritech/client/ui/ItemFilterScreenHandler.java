package rearth.oritech.client.ui;

import io.wispforest.owo.client.screens.SlotGenerator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.client.init.ModScreens;

public class ItemFilterScreenHandler extends ScreenHandler {
    
    @NotNull
    protected final BlockPos blockPos;
    @NotNull
    protected final ItemFilterBlockEntity blockEntity;
    
    public ItemFilterScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, (ItemFilterBlockEntity) inventory.player.getWorld().getBlockEntity(buf.readBlockPos()));
    }
    
    public ItemFilterScreenHandler(int syncId, PlayerInventory playerInventory, ItemFilterBlockEntity blockEntity) {
        super(ModScreens.ITEM_FILTER_SCREEN, syncId);
        
        this.blockPos = blockEntity.getPos();
        this.blockEntity = blockEntity;
        
        SlotGenerator.begin(this::addSlot, 8, 84)
          .playerInventory(playerInventory);
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
