package rearth.oritech.client.ui;

import io.wispforest.owo.client.screens.SlotGenerator;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.FluidProvider;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.List;

public class BasicMachineScreenHandler extends ScreenHandler {
    
    @NotNull
    protected final PlayerInventory playerInventory;
    @NotNull
    protected final Inventory inventory;
    @NotNull
    protected final EnergyApi.EnergyContainer energyStorage;
    
    @NotNull
    protected final BlockPos blockPos;
    
    @NotNull
    protected final ScreenProvider screenData;
    
    @Nullable
    protected final SingleVariantStorage<FluidVariant> steamStorage;
    @Nullable
    protected final SingleVariantStorage<FluidVariant> waterStorage;
    
    
    protected final FluidProvider fluidProvider;
    
    protected BlockState machineBlock;
    protected BlockEntity blockEntity;
    protected List<Integer> armorSlots;
    
    public BasicMachineScreenHandler(int syncId, PlayerInventory inventory, ModScreens.BasicData setupData) {
        this(syncId, inventory, inventory.player.getWorld().getBlockEntity(setupData.pos()));
    }
    
    // on server, also called from client constructor
    public BasicMachineScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(((ScreenProvider) blockEntity).getScreenHandlerType(), syncId);
        
        this.screenData = (ScreenProvider) blockEntity;
        this.blockPos = blockEntity.getPos();
        this.inventory = screenData.getDisplayedInventory();
        inventory.onOpen(playerInventory.player);
        this.playerInventory = playerInventory;
        
        if (blockEntity instanceof EnergyApi.BlockProvider energyProvider) {
            energyStorage = energyProvider.getStorage(null);
        } else {
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
        
        if (this.blockEntity instanceof UpgradableGeneratorBlockEntity generatorEntity && generatorEntity.isProducingSteam) {
            steamStorage = generatorEntity.getSteamStorage();
            waterStorage = generatorEntity.getWaterStorage();
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
                    public boolean canInsert(ItemStack stack) {
                        if (iteration == 4) return super.canInsert(stack);  // offhand, to prevent dupes
                        
                        if (stack.getItem() instanceof ArmorItem armorItem) {
                            return super.canInsert(stack) && armorItem.getSlotType().getEntitySlotId() == iteration;
                        }
                        return false;
                    }
                });
                armorSlots.add(index.id);
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
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        
        var newStack = ItemStack.EMPTY;
        
        var slot = this.slots.get(invSlot);

        if (slot.hasStack()) {
            var originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size() || invSlot >= this.inventory.size() + 36) {  // second condition is for machines adding extra slots afterwards, which are treated as part of the machine
                if (!this.insertItem(originalStack, getPlayerInvStartSlot(newStack), getPlayerInvEndSlot(newStack), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, getMachineInvStartSlot(newStack), getMachineInvEndSlot(newStack), false)) {
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
    
    // order is:
    // machine inv slots
    // player inv slots
    // player equipment slots
    
    public int getPlayerInvStartSlot(ItemStack stack) {
        return this.inventory.size();
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
        return this.inventory.size();
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
    
    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }
    
    public boolean showRedstoneAddon() {
        return screenData.hasRedstoneControlAvailable();
    }
    
}
