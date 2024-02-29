package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import rearth.oritech.util.InventoryProvider;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;
import java.util.Objects;

public abstract class ItemEnergyFrameInteractionBlockEntity extends FrameInteractionBlockEntity implements InventoryProvider, EnergyProvider, ExtendedScreenHandlerFactory, ScreenProvider {
    
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(5000, 100, 0) {
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            ItemEnergyFrameInteractionBlockEntity.this.markDirty();
        }
    };
    
    protected final SimpleInventory inventory = new SimpleInventory(3) {
        @Override
        public void markDirty() {
            ItemEnergyFrameInteractionBlockEntity.this.markDirty();
        }
    };
    
    public final InventoryStorage inventoryWrapper = InventoryStorage.of(inventory, null);
    
    public ItemEnergyFrameInteractionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public abstract int getMoveEnergyUsage();
    public abstract int getOperationEnergyUsage();
    
    @Override
    protected boolean canProgress() {
        return energyStorage.amount >= getMoveEnergyUsage() && energyStorage.amount >= getOperationEnergyUsage();
    }
    
    @Override
    protected void doProgress() {
        energyStorage.amount -= getMoveEnergyUsage();
        this.markDirty();
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        energyStorage.amount -= getOperationEnergyUsage();
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        inventory.readNbtList(nbt.getList("inventory", NbtElement.COMPOUND_TYPE));
        energyStorage.amount = nbt.getLong("energy_stored");
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("inventory", inventory.toNbtList());
        nbt.putLong("energy_stored", energyStorage.amount);
    }
    
    @Override
    public InventoryStorage getInventory() {
        return inventoryWrapper;
    }
    
    @Override
    public EnergyStorage getStorage() {
        return energyStorage;
    }
    
    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.getPos());
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("Invalid");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public List<ScreenProvider.GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 50, 11),
          new GuiSlot(1, 68, 11),
          new GuiSlot(2, 86, 11));
    }
    
    @Override
    public float getProgress() {
        return (float) getCurrentProgress() / this.getWorkTime();
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return getOperationEnergyUsage();
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, FrameInteractionBlockEntity blockEntity) {
        super.tick(world, pos, state, blockEntity);
        
        if (!world.isClient && isActivelyViewed()) {
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.MachineFrameGuiPacket(pos, energyStorage.amount, energyStorage.capacity, getCurrentProgress()));
        }
    }
    
    private boolean isActivelyViewed() {
        var closestPlayer = Objects.requireNonNull(world).getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 5, false);
        return closestPlayer != null && closestPlayer.currentScreenHandler instanceof BasicMachineScreenHandler handler && getPos().equals(handler.getBlockPos());
    }
    
    public DynamicEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
}
