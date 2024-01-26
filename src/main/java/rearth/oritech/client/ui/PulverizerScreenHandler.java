package rearth.oritech.client.ui;

import io.wispforest.owo.client.screens.SlotGenerator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.client.init.ModScreenHandlers;
import rearth.oritech.util.EnergyProvider;
import rearth.oritech.util.ScreenProvider;
import team.reborn.energy.api.EnergyStorage;

public class PulverizerScreenHandler extends ScreenHandler {

    @NotNull
    protected final PlayerInventory playerInventory;
    @NotNull
    protected final Inventory inventory;
    @NotNull
    protected final EnergyStorage energyStorage;

    @NotNull
    protected final ScreenProvider screenData;

    public PulverizerScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, inventory.player.getWorld().getBlockEntity(buf.readBlockPos()));
    }

    public PulverizerScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(ModScreenHandlers.PULVERIZER_SCREEN, syncId);

        this.inventory = ((Inventory) blockEntity);
        inventory.onOpen(playerInventory.player);
        this.playerInventory = playerInventory;

        if (blockEntity instanceof EnergyProvider energyProvider) {
            energyStorage = energyProvider.getStorage();
        } else {
            Oritech.LOGGER.error("Opened oritech block interface without any energy data at " + blockEntity);
            energyStorage = null;
        }

        if (blockEntity instanceof ScreenProvider screenProvider) {
            screenData = screenProvider;
        } else {
            Oritech.LOGGER.error("Opened oritech block interface without any screen data at " + blockEntity);
            screenData = null;
        }

        buildItemSlots();

    }

    private void buildItemSlots() {

        for (var slot : screenData.getActiveSlots()) {
            addMachineSlot(slot.index(), slot.x(), slot.y());
        }

        SlotGenerator.begin(this::addSlot, 8, 84)
                .playerInventory(playerInventory);
    }

    private void addMachineSlot(int inventorySlot, int x, int y) {
        this.addSlot(new Slot(inventory, inventorySlot, x, y));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
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

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
