package rearth.oritech.block.entity.interaction;

import dev.architectury.hooks.fluid.FluidStackHooks;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.InOutInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;

import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.StackContext;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ChargerBlockEntity extends NetworkedBlockEntity implements FluidApi.BlockProvider, EnergyApi.BlockProvider, ItemApi.BlockProvider,
                                                                 ScreenProvider, ExtendedMenuProvider {
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(Oritech.CONFIG.charger.energyCapacity(), Oritech.CONFIG.charger.maxEnergyInsertion(), Oritech.CONFIG.charger.maxEnergyExtraction(), this::setChanged);
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    private final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(16 * FluidStackHooks.bucketAmount(), this::setChanged);
    
    // 0 = bucket/item to be charged/filled, 1 = empty bucket/charged/fill item
    public final InOutInventoryStorage inventory = new InOutInventoryStorage(2, this::setChanged, new InventorySlotAssignment(0, 1, 1, 1));
    
    
    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.CHARGER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (world.isClientSide) return;
        
        // stop if no input is given, or it's a stackable item
        if (inventory.getItem(0).isEmpty() || inventory.getItem(0).getCount() > 1) return;
        
        var isFull = true;
        var startEnergy = energyStorage.amount;
        var startFluid = fluidStorage.getAmount();
        
        // try charge item
        if (!chargeItems()) isFull = false;
        
        // try filling item
        if (!fillItems()) isFull = false;
        
        // move charged and/or filled item to right
        if (isFull) {
            var outSlot = inventory.getItem(1);
            if (outSlot.isEmpty()) {
                inventory.setItem(1, inventory.getItem(0));
                inventory.setItem(0, ItemStack.EMPTY);
            }
        }
        
        if (fluidStorage.getAmount() != startFluid || energyStorage.amount != startEnergy) {
            ParticleContent.ASSEMBLER_WORKING.spawn(world, pos.getCenter().add(0.1, 0.1, 0), 1);
        }
        
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("energy_stored", energyStorage.amount);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        fluidStorage.readNbt(nbt, "");
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        energyStorage.amount = nbt.getLong("energy_stored");
    }
    
    // return true if nothing is left to charge/fill
    private boolean chargeItems() {
        var heldStack = inventory.heldStacks.get(0);
        
        var stackRef = new StackContext(heldStack, updated -> inventory.heldStacks.set(0, updated));
        var slotEnergyContainer = EnergyApi.ITEM.find(stackRef);
        if (slotEnergyContainer != null) {
            EnergyApi.transfer(energyStorage, slotEnergyContainer, Long.MAX_VALUE, false);
            return slotEnergyContainer.getAmount() >= slotEnergyContainer.getCapacity();
        } else {
            return true;
        }
    }
    
    // return true if nothing is left to fill
    private boolean fillItems() {
        
        var heldStack = inventory.heldStacks.get(0);
        
        var stackRef = new StackContext(heldStack, updated -> inventory.heldStacks.set(0, updated));
        var slotFluidContainer = FluidApi.ITEM.find(stackRef);
        if (slotFluidContainer != null) {
            var moved = FluidApi.transferFirst(fluidStorage, slotFluidContainer, (long) (FluidStackHooks.bucketAmount() * 0.1f), false);
            return fluidStorage.getAmount() > 0 && moved == 0;
        } else {
            return true;
        }
        
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        this.sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.CHARGER_SCREEN;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 56, 38), new GuiSlot(1, 117, 38));
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return 1024;
    }
    
    @Override
    public float getProgress() {
        return 0;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public boolean showArmor() {
        return true;
    }
    
    @Override
    public boolean showExpansionPanel() {
        return false;
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(@Nullable Direction direction) {
        return fluidStorage;
    }
}
