package rearth.oritech.client.ui;

import io.wispforest.owo.client.screens.SlotGenerator;
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
import rearth.oritech.util.ScreenProvider.GuiSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

public class BasicMachineScreenHandler extends AbstractContainerMenu {
    
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
        
        var newStack = ItemStack.EMPTY;
        
        var slot = this.slots.get(invSlot);
        
        if (slot.hasItem()) {
            var originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.getContainerSize() || invSlot >= this.inventory.getContainerSize() + 36) {  // second condition is for machines adding extra slots afterwards, which are treated as part of the machine
                if (!this.moveItemStackTo(originalStack, getPlayerInvStartSlot(newStack), getPlayerInvEndSlot(newStack), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, getMachineInvStartSlot(newStack), getMachineInvEndSlot(newStack), false)) {
                return ItemStack.EMPTY;
            }
            
            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return newStack;
    }
    
    // order is:
    // machine inv slots
    // player inv slots
    // player equipment slots
    
    public int getPlayerInvStartSlot(ItemStack stack) {
        return this.inventory.getContainerSize();
    }
    
    public int getPlayerInvEndSlot(ItemStack stack) {
        
        if (screenData.showArmor()) {
            return this.slots.size() - 1;   // don't include offhand slot
        }
        return this.slots.size();
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
