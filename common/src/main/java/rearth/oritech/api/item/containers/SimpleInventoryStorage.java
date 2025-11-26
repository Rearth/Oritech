package rearth.oritech.api.item.containers;

import io.netty.buffer.ByteBuf;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SimpleInventoryStorage implements Container, ItemApi.InventoryStorage, UpdatableField<Void, List<ItemStack>> {
    
    private final int size;
    public final NonNullList<ItemStack> heldStacks;
    
    private final Runnable onUpdate;
    
    public SimpleInventoryStorage(int size, Runnable onUpdate) {
        this.size = size;
        this.onUpdate = onUpdate;
        this.heldStacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }
    
    @Override
    public int insert(ItemStack toInsert, boolean simulate) {
        var remaining = toInsert.getCount();
        for (var slot = 0; slot < getContainerSize() && remaining > 0; slot++) {
            remaining -= insertToSlot(toInsert.copyWithCount(remaining), slot, simulate);
        }
        
        return toInsert.getCount() - remaining;
    }
    
    @Override
    public int insertToSlot(ItemStack addedStack, int slot, boolean simulate) {
        var slotStack = getItem(slot);
        var slotLimit = Math.min(getSlotLimit(slot), addedStack.getMaxStackSize());
        
        if (slotStack.isEmpty()) {
            var toInsert = Math.min(slotLimit, addedStack.getCount());
            if (!simulate) setItem(slot, addedStack.copyWithCount(toInsert));
            return toInsert;
        }
        
        if (ItemStack.isSameItemSameComponents(slotStack, addedStack)) {
            var available = slotLimit - slotStack.getCount();
            var toInsert = Math.min(available, addedStack.getCount());
            if (toInsert > 0) {
                if (!simulate) slotStack.grow(toInsert);
                return toInsert;
            }
        }
        
        return 0;
    }
    
    @Override
    public int extract(ItemStack toExtract, boolean simulate) {
        var remaining = toExtract.getCount();
        for (var slot = 0; slot < getContainerSize() && remaining > 0; slot++) {
            remaining -= extractFromSlot(toExtract.copyWithCount(remaining), slot, simulate);
        }
        return toExtract.getCount() - remaining;
    }
    
    @Override
    public int extractFromSlot(ItemStack extracted, int slot, boolean simulate) {
        var slotStack = getItem(slot);
        if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, extracted))
            return 0;
        
        var toExtract = Math.min(slotStack.getCount(), extracted.getCount());
        if (!simulate) slotStack.shrink(toExtract);
        return toExtract;
    }
    
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        this.setItem(slot, stack);
    }
    
    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.getItem(slot);
    }
    
    @Override
    public int getSlotCount() {
        return this.getContainerSize();
    }
    
    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }
    
    @Override
    public void update() {
        onUpdate.run();
    }
    
    
    // these are mostly a copy of SimpleInventory, with minor changes and only essential things included to avoid confusion
    @Override
    public int getContainerSize() {
        return size;
    }
    
    @Override
    public boolean isEmpty() {
        for (var itemStack : this.heldStacks) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public ItemStack getItem(int slot) {
        return heldStacks.get(slot);
    }
    
    @Override
    public ItemStack removeItem(int slot, int amount) {
        var itemStack = ContainerHelper.removeItem(this.heldStacks, slot, amount);
        if (!itemStack.isEmpty()) {
            this.setChanged();
        }
        
        return itemStack;
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var itemStack = this.heldStacks.get(slot);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.heldStacks.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        this.heldStacks.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }
    
    @Override
    public void setChanged() {
        this.update();
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    @Override
    public void clearContent() {
        this.heldStacks.clear();
        this.setChanged();
    }
    
    public NonNullList<ItemStack> getHeldStacks() {
        return heldStacks;
    }
    
    @Override
    public List<ItemStack> getDeltaData() {
        return heldStacks;
    }
    
    @Override
    public Void getFullData() {
        return null;
    }
    
    @Override
    public StreamCodec<? extends ByteBuf, List<ItemStack>> getDeltaCodec() {
        return ItemStack.OPTIONAL_LIST_STREAM_CODEC;
    }
    
    @Override
    public StreamCodec<? extends ByteBuf, Void> getFullCodec() {
        return null;
    }
    
    @Override
    public boolean useDeltaOnly(SyncType type) {
        return true;
    }
    
    @Override
    public void handleFullUpdate(Void updatedData) {
    
    }
    
    @Override
    public void handleDeltaUpdate(List<ItemStack> updatedData) {
        this.heldStacks.clear();
        
        for (int i = 0; i < updatedData.size(); i++) {
            var added = updatedData.get(i);
            this.heldStacks.set(i, added);
        }
        
    }
}
