package rearth.oritech.block.entity.storage;

import dev.architectury.hooks.fluid.FluidStackHooks;
import dev.architectury.registry.menu.ExtendedMenuProvider;
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
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.InOutInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.blocks.storage.SmallFluidTank;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.*;

import java.util.List;
import java.util.Objects;

public class SmallTankEntity extends NetworkedBlockEntity implements FluidApi.BlockProvider, ItemApi.BlockProvider, ComparatorOutputProvider,
                                                                       ScreenProvider, ExtendedMenuProvider {
    
    private int lastComparatorOutput = 0;
    public final boolean isCreative;
    
    private ApiLookupCache<FluidApi.FluidStorage> downLookupCache;
    
    public final InOutInventoryStorage inventory = new InOutInventoryStorage(3, this::setChanged, new InventorySlotAssignment(0, 2, 2, 1));
    
    @SyncField({SyncType.TICK, SyncType.INITIAL})
    public final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(Oritech.CONFIG.portableTankCapacityBuckets() * FluidStackHooks.bucketAmount(), this::setChanged);
    
    public SmallTankEntity(BlockPos pos, BlockState state, boolean isCreative) {
        super(isCreative ? BlockEntitiesContent.CREATIVE_TANK_ENTITY : BlockEntitiesContent.SMALL_TANK_ENTITY, pos, state);
        this.isCreative = isCreative;
    }
    
    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
    }
    
    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        fluidStorage.readNbt(nbt, "");
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        setChanged();
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        // fills/drains buckets
        
        // in creative, set tank fill level
        if (isCreative) {
            if (fluidStorage.getFluid() != Fluids.EMPTY) {
                fluidStorage.setAmount(fluidStorage.getCapacity() - FluidStackHooks.bucketAmount() * 8);  //leave space to insert a bit
            } else {
                fluidStorage.setAmount(0);
            }
        }
        
        processInput();
        processOutput();
        
        if (fluidStorage.getAmount() > 0)
            outputToBelow();
        
        updateComparators(world, pos, state);
    }
    
    private void outputToBelow() {
        if (isCreative) return;
        
        if (downLookupCache == null) {
            downLookupCache = ApiLookupCache.create(
              worldPosition.below(),
              Direction.UP, Objects.requireNonNull(level),
              ((world1, targetPos, targetState, targetEntity, direction) -> FluidApi.BLOCK.find(world1, targetPos, targetState, targetEntity, direction)));
            
        }
        
        var tankCandidate = downLookupCache.lookup();
        
        if (!(tankCandidate instanceof SimpleFluidStorage belowTank)) return;
        var ownTank = this.fluidStorage;
        
        SimpleFluidStorage.transfer(ownTank, belowTank, ownTank.getCapacity(), false);
    }
    
    private void updateComparators(Level world, BlockPos pos, BlockState state) {
        var previous = lastComparatorOutput;
        lastComparatorOutput = getComparatorOutput();
        
        if (previous != lastComparatorOutput) {
            world.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }
    
    // from block entity to item
    private void processInput() {
        var inStack = inventory.getItem(0);
        var canFill = this.fluidStorage.getAmount() > 0;
        
        if (!canFill || inStack.isEmpty() || inStack.getCount() > 1) return;
        
        var stackRef = new StackContext(inStack, updated -> inventory.setItem(0, updated));
        var candidate = FluidApi.ITEM.find(stackRef);
        if (candidate == null || !candidate.supportsInsertion()) return;
        
        var moved = FluidApi.transferFirst(fluidStorage, candidate, FluidStackHooks.bucketAmount() * 64, false);
        
        if (moved == 0) {
            // move stack to out slot
            var outStack = inventory.getItem(2);
            if (outStack.isEmpty()) {
                inventory.setItem(2, stackRef.getValue());
                inventory.setItem(0, ItemStack.EMPTY);
            } else if (outStack.getItem().equals(stackRef.getValue().getItem()) && outStack.getCount() < outStack.getMaxStackSize()) {
                outStack.grow(1);
                inventory.setItem(0, ItemStack.EMPTY);
            }
        }
    }
    
    // from item to fluid storage
    private void processOutput() {
        var inStack = inventory.getItem(1);
        var canFill = this.fluidStorage.getAmount() < this.fluidStorage.getCapacity();
        
        if (!canFill || inStack.isEmpty() || inStack.getCount() > 1) return;
        
        var stackRef = new StackContext(inStack, updated -> inventory.setItem(1, updated));
        var candidate = FluidApi.ITEM.find(stackRef);
        if (candidate == null || !candidate.supportsExtraction()) return;
        
        var moved = FluidApi.transferFirst(candidate, fluidStorage, FluidStackHooks.bucketAmount() * 64, false);
        
        if (moved == 0) {
            // move stack
            var outStack = inventory.getItem(2);
            if (outStack.isEmpty()) {
                inventory.setItem(2, stackRef.getValue());
                inventory.setItem(1, ItemStack.EMPTY);
            } else if (outStack.getItem().equals(stackRef.getValue().getItem()) && outStack.getCount() < outStack.getMaxStackSize()) {
                outStack.grow(1);
                inventory.setItem(1, ItemStack.EMPTY);
            }
        }
    }
    
    @Override
    public int getComparatorOutput() {
        if (fluidStorage.getFluid().equals(Fluids.EMPTY)) return 0;
        
        var fillPercentage = fluidStorage.getAmount() / (float) fluidStorage.getCapacity();
        return (int) (1 + fillPercentage * 14);
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !this.isRemoved() && getBlockState().getValue(SmallFluidTank.LIT) != isGlowingFluid()) {
            level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(SmallFluidTank.LIT, isGlowingFluid()));
        }
    }
    
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 50, 19), new GuiSlot(1, 50, 61), new GuiSlot(2, 130, 42, true));
    }
    
    @Override
    public BarConfiguration getFluidConfiguration() {
        return new BarConfiguration(70, 18, 21, 60);
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
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
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.TANK_SCREEN;
    }
    
    public boolean isGlowingFluid() {
        return fluidStorage.getAmount() > 0 && FluidStackHooks.getLuminosity(fluidStorage.getFluid(), null, null) > 0;
    }
    
    @Override
    public boolean showEnergy() {
        return false;
    }
    
    @Override
    public ArrowConfiguration getIndicatorConfiguration() {
        return new ArrowConfiguration(
          Oritech.id("textures/gui/modular/arrow_empty.png"),
          Oritech.id("textures/gui/modular/arrow_full.png"),
          95, 40, 29, 16, true);
    }
    
    @Override
    public boolean showExpansionPanel() {
        return false;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public FluidApi.SingleSlotStorage getFluidStorage(@Nullable Direction direction) {
        return fluidStorage;
    }
}
