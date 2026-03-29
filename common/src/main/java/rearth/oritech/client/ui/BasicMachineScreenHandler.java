package rearth.oritech.client.ui;

import io.wispforest.owo.client.screens.SlotGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BasicMachineScreenHandler extends AbstractContainerMenu implements MachineMenuHandler {
    
    @NotNull
    protected final Inventory playerInventory;
    @NotNull
    protected final Container inventory;
    @NotNull
    protected final EnergyApi.EnergyStorage energyStorage;
    
    @NotNull
    protected final BlockPos blockPos;
    
    @NotNull
    public final ScreenProvider screenData;
    
    @Nullable
    protected final FluidApi.SingleSlotStorage steamStorage;
    @Nullable
    protected final FluidApi.SingleSlotStorage waterStorage;
    @Nullable
    protected FluidApi.SingleSlotStorage mainFluidContainer;
    
    protected BlockState machineBlock;
    public BlockEntity blockEntity;
    protected List<Integer> armorSlots;
    
    public BasicMachineScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    // on server, also called from client constructor
    public BasicMachineScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(((ScreenProvider) blockEntity).getScreenHandlerType(), syncId);
        
        this.screenData = (ScreenProvider) blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.inventory = screenData.getDisplayedInventory();
        if (inventory != null)
            inventory.startOpen(playerInventory.player);
        this.playerInventory = playerInventory;
        
        if (blockEntity instanceof EnergyApi.BlockProvider energyProvider) {
            energyStorage = energyProvider.getEnergyStorage(null);
        } else {
            energyStorage = null;
        }
        
        if (blockEntity instanceof FluidApi.BlockProvider blockProvider && blockProvider.getFluidStorage(null) instanceof SimpleFluidStorage container) {
            this.mainFluidContainer = container;
        } else {
            mainFluidContainer = null;
        }
        
        this.machineBlock = blockEntity.getBlockState();
        this.blockEntity = blockEntity;
        
        if (this.blockEntity instanceof UpgradableGeneratorBlockEntity generatorEntity && generatorEntity.isProducingSteam) {
            waterStorage = generatorEntity.boilerStorage.getInputContainer();
            steamStorage = generatorEntity.boilerStorage.getOutputContainer();
        } else if (this.blockEntity instanceof SteamEngineEntity steamEngineEntity) {
            waterStorage = steamEngineEntity.boilerStorage.getOutputContainer();
            steamStorage = steamEngineEntity.boilerStorage.getInputContainer();
        } else {
            steamStorage = null;
            waterStorage = null;
        }
        
        buildItemSlots();
    }
    
    private void buildItemSlots() {
        
        for (var slot : screenData.getGuiSlots()) {
            addMachineSlot(slot.index(), slot.x(), slot.y(), slot.output());
        }
        
        SlotGenerator.begin(this::addSlot, 8, 84)
          .playerInventory(playerInventory);
        
        if (screenData.showArmor()) {
            armorSlots = new ArrayList<>(5);
            for (int i = 0; i < playerInventory.armor.size() + 1; i++) {
                final var iteration = i;
                var index = this.addSlot(new Slot(playerInventory, 36 + i, -20, i * 19) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        if (iteration == 4) return super.mayPlace(stack);  // offhand, to prevent dupes
                        
                        if (stack.getItem() instanceof ArmorItem armorItem) {
                            return super.mayPlace(stack) && armorItem.getEquipmentSlot().getIndex() == iteration;
                        }
                        return false;
                    }
                });
                armorSlots.add(index.index);
            }
        }
    }
    
    public void addMachineSlot(int inventorySlot, int x, int y, boolean output) {
        if (output) {
            this.addSlot(new BasicMachineOutputSlot(inventory, inventorySlot, x, y));
        } else {
            this.addSlot(new Slot(inventory, inventorySlot, x, y));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        var slot = this.slots.get(invSlot);
        
        if (slot.hasItem()) {
            var originalStack = slot.getItem();
            var newStack = originalStack.copy();
            
            int machineSize = this.inventory.getContainerSize();
            int playerInvStart = getPlayerInvStartSlot(newStack);
            int playerInvEnd = getPlayerInvEndSlot(newStack);
            int totalSize = this.slots.size();   // Includes armor/offhand if enabled
            
            // case 1: Slot is inside the Machine (Output/Input)
            if (invSlot < machineSize) {
                // Move to Player Main Inventory
                if (!this.moveItemStackTo(originalStack, playerInvStart, playerInvEnd, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // case 2: Slot is inside Player Main Inventory (Storage + Hotbar)
            else if (invSlot >= playerInvStart && invSlot < playerInvEnd) {
                // Move to Machine Inventory
                if (!this.moveItemStackTo(originalStack, 0, machineSize, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // case 3: Slot is an Armor or Offhand slot
            else if (invSlot >= playerInvEnd && invSlot < totalSize) {
                // Move to Player Main Inventory ONLY.
                if (!this.moveItemStackTo(originalStack, playerInvStart, playerInvEnd, true)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            return newStack;
        }
        
        return ItemStack.EMPTY;
    }
    
    // order is:
    // machine inv slots
    // player inv slots
    // player equipment slots
    
    public int getPlayerInvStartSlot(ItemStack stack) {
        return this.inventory.getContainerSize();
    }
    
    public int getPlayerInvEndSlot(ItemStack stack) {
        
        return getPlayerInvStartSlot(stack) + 36;
    }
    
    public int getMachineInvStartSlot(ItemStack stack) {
        return 0;
    }
    
    public int getMachineInvEndSlot(ItemStack stack) {
        return this.inventory.getContainerSize();
    }
    
    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
    
    @Override
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }
    
    public boolean showRedstoneAddon() {
        return screenData.hasRedstoneControlAvailable();
    }
    
    @Override
    public void broadcastChanges() {
        
        if (blockEntity instanceof NetworkedBlockEntity networkedBlockEntity)
            networkedBlockEntity.sendUpdate(SyncType.GUI_TICK, (ServerPlayer) this.player());
        
        super.broadcastChanges();
    }
}
