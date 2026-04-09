package rearth.oritech.client.ui;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.client.init.ModScreens;

import java.util.HashMap;
import java.util.Objects;

public class ItemFilterScreenHandler extends AbstractContainerMenu {
    
    @NotNull
    protected final BlockPos blockPos;
    @NotNull
    protected final ItemFilterBlockEntity blockEntity;
    
    public ItemFilterScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public ItemFilterScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModScreens.ITEM_FILTER_SCREEN, syncId);
        
        this.blockPos = blockEntity.getBlockPos();
        this.blockEntity = (ItemFilterBlockEntity) blockEntity;
        
        addPlayerInventory(playerInventory, 8, 84);
    }
    
    /**
     * Adds the standard player inventory slots (27 main + 9 hotbar).
     */
    protected void addPlayerInventory(Inventory playerInventory, int startX, int startY) {
        // Main inventory (3 rows of 9, slot indices 9-35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        // Hotbar (1 row of 9, slot indices 0-8)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY + 58));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        // slots are 0-27 for inventory, 28-35 for hotbar
        // but player inventory is 0-8 for hotbar, 9-35 for inventory
        var slotStack = player.getInventory().getItem((slot + 9) % 36);
        if (slotStack.isEmpty()) return ItemStack.EMPTY;

        var displayStack = new ItemStack(slotStack.getItem(), 1);
        displayStack.applyComponents(slotStack.getComponents());

        var data = blockEntity.getFilterSettings();
        for (var item : data.items().values()) {
            // don't add item to filter if it's already in filter
            if (item.is(displayStack.getItem())) return ItemStack.EMPTY;
        }
        var newItems = new HashMap<>(data.items());
        for (int i = 0; i < 12; i++) {
            if (!newItems.containsKey(i)) {
                newItems.put(i, displayStack);
                break;
            }
        }
        
        var newData = new ItemFilterBlockEntity.FilterData(data.useNbt(), data.useWhitelist(), data.useComponents(), newItems);
        blockEntity.setFilterSettings(newData);
        if (Objects.requireNonNull(blockEntity.getLevel()).isClientSide) {
            if (player instanceof LocalPlayer clientPlayer && clientPlayer.minecraft.screen instanceof ItemFilterScreen filterScreen) {
                filterScreen.updateItemFilters();
            }
        }

        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
