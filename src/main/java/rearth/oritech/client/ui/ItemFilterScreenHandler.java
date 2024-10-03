package rearth.oritech.client.ui;

import io.wispforest.owo.client.screens.SlotGenerator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.network.NetworkContent;

public class ItemFilterScreenHandler extends ScreenHandler {
    
    @NotNull
    protected final BlockPos blockPos;
    @NotNull
    protected final ItemFilterBlockEntity blockEntity;
    
    public ItemFilterScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(ModScreens.ITEM_FILTER_SCREEN, syncId);
        
        this.blockPos = blockEntity.getPos();
        this.blockEntity = (ItemFilterBlockEntity) blockEntity;
        
        SlotGenerator.begin(this::addSlot, 8, 84)
          .playerInventory(playerInventory);
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // slots are 0-27 for inventory, 28-35 for hotbar
        // but player inventory is 0-8 for hotbar, 9-35 for inventory
        var slotStack = player.getInventory().getStack((slot + 9) % 36);
        if (slotStack.isEmpty()) return ItemStack.EMPTY;

        var displayStack = new ItemStack(slotStack.getItem(), 1);

        var data = blockEntity.getFilterSettings();
        for (var item : data.items().values()) {
            // don't add item to filter if it's already in filter
            if (item.isOf(displayStack.getItem())) return ItemStack.EMPTY;
        }
        var newItems = new HashMap<Integer, ItemStack>(data.items());
        for (int i = 0; i < 8; i++) {
            if (!newItems.containsKey(i)) {
                newItems.put(i, displayStack);
                break;
            }
        }
        var newData = new ItemFilterBlockEntity.FilterData(data.useNbt(), data.useWhitelist(), newItems);
        blockEntity.setFilterSettings(newData);
        if (player instanceof ClientPlayerEntity clientPlayer && clientPlayer.client.currentScreen instanceof ItemFilterScreen filterScreen) {
            filterScreen.updateItemFilters();
        }

        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
