package rearth.oritech.api.item.containers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import rearth.oritech.api.item.ItemApi;

public class SimpleInventoryStorage implements Inventory, ItemApi.InventoryStorage {
    
    private final int size;
    public final DefaultedList<ItemStack> heldStacks;
    
    private final Runnable onUpdate;
    
    public SimpleInventoryStorage(int size, Runnable onUpdate) {
        this.size = size;
        this.onUpdate = onUpdate;
        this.heldStacks = DefaultedList.ofSize(size, ItemStack.EMPTY);
    }
    
    @Override
    public int insert(ItemStack toInsert, boolean simulate) {
        var remaining = toInsert.getCount();
        for (var slot = 0; slot < size() && remaining > 0; slot++) {
            remaining -= insertToSlot(toInsert.copyWithCount(remaining), slot, simulate);
        }
        
        return toInsert.getCount() - remaining;
    }
    
    @Override
    public int insertToSlot(ItemStack addedStack, int slot, boolean simulate) {
        var slotStack = getStack(slot);
        var slotLimit = Math.min(getSlotLimit(slot), addedStack.getMaxCount());
        
        if (slotStack.isEmpty()) {
            var toInsert = Math.min(slotLimit, addedStack.getCount());
            if (!simulate) setStack(slot, addedStack.copyWithCount(toInsert));
            return toInsert;
        }
        
        if (ItemStack.areItemsAndComponentsEqual(slotStack, addedStack)) {
            var available = slotLimit - slotStack.getCount();
            var toInsert = Math.min(available, addedStack.getCount());
            if (toInsert > 0) {
                if (!simulate) slotStack.increment(toInsert);
                return toInsert;
            }
        }
        
        return 0;
    }
    
    @Override
    public int extract(ItemStack toExtract, boolean simulate) {
        var remaining = toExtract.getCount();
        for (var slot = 0; slot < size() && remaining > 0; slot++) {
            remaining -= extractFromSlot(toExtract.copyWithCount(remaining), slot, simulate);
        }
        return toExtract.getCount() - remaining;
    }
    
    @Override
    public int extractFromSlot(ItemStack extracted, int slot, boolean simulate) {
        var slotStack = getStack(slot);
        if (slotStack.isEmpty() || !ItemStack.areItemsAndComponentsEqual(slotStack, extracted))
            return 0;
        
        var toExtract = Math.min(slotStack.getCount(), extracted.getCount());
        if (!simulate) slotStack.decrement(toExtract);
        return toExtract;
    }
    
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        this.setStack(slot, stack);
    }
    
    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.getStack(slot);
    }
    
    @Override
    public int getSlotCount() {
        return this.size();
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
    public int size() {
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
    public ItemStack getStack(int slot) {
        return heldStacks.get(slot);
    }
    
    @Override
    public ItemStack removeStack(int slot, int amount) {
        var itemStack = Inventories.splitStack(this.heldStacks, slot, amount);
        if (!itemStack.isEmpty()) {
            this.markDirty();
        }
        
        return itemStack;
    }
    
    @Override
    public ItemStack removeStack(int slot) {
        var itemStack = this.heldStacks.get(slot);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.heldStacks.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
    }
    
    @Override
    public void setStack(int slot, ItemStack stack) {
        this.heldStacks.set(slot, stack);
        stack.capCount(this.getMaxCount(stack));
        this.markDirty();
    }
    
    @Override
    public void markDirty() {
        this.update();
    }
    
    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }
    
    @Override
    public void clear() {
        this.heldStacks.clear();
        this.markDirty();
    }
    
    public DefaultedList<ItemStack> getHeldStacks() {
        return heldStacks;
    }
}
