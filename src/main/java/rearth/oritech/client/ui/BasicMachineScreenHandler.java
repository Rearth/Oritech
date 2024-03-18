package rearth.oritech.client.ui;

import io.wispforest.owo.client.screens.SlotGenerator;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.util.EnergyProvider;
import rearth.oritech.util.FluidProvider;
import rearth.oritech.util.ScreenProvider;
import team.reborn.energy.api.EnergyStorage;

import java.util.Objects;

public class BasicMachineScreenHandler extends ScreenHandler {

    @NotNull
    protected final PlayerInventory playerInventory;
    @NotNull
    protected final Inventory inventory;
    @NotNull
    protected final EnergyStorage energyStorage;

    @NotNull
    protected final BlockPos blockPos;

    @NotNull
    protected final ScreenProvider screenData;
    
    protected final FluidProvider fluidProvider;
    
    protected BlockState machineBlock;
    protected BlockEntity blockEntity;

    // on client, receiving data from writeScreenOpeningData
    public BasicMachineScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())));
    }

    // on server, also called from client constructor
    public BasicMachineScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(((ScreenProvider) blockEntity).getScreenHandlerType(), syncId);
        
        this.screenData = (ScreenProvider) blockEntity;
        this.blockPos = blockEntity.getPos();
        this.inventory = screenData.getDisplayedInventory();
        inventory.onOpen(playerInventory.player);
        this.playerInventory = playerInventory;

        if (blockEntity instanceof EnergyProvider energyProvider) {
            energyStorage = energyProvider.getStorage();
        } else {
            Oritech.LOGGER.error("Opened oritech block interface without any energy data at " + blockEntity);
            energyStorage = null;
        }
        
        if (blockEntity instanceof FluidProvider blockFluidProvider && blockFluidProvider.getForDirectFluidAccess() != null) {
            var fluidIterator = blockFluidProvider.getFluidStorage(null).iterator();
            if (fluidIterator.hasNext()) {
                this.fluidProvider = blockFluidProvider;
            } else {
                this.fluidProvider = null;
            }
        } else {
            fluidProvider = null;
        }
        
        this.machineBlock = blockEntity.getCachedState();
        this.blockEntity = blockEntity;
        
        
        buildItemSlots();
    }
    
    private void buildItemSlots() {

        for (var slot : screenData.getGuiSlots()) {
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

    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }
}
