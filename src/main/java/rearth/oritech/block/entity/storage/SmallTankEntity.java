package rearth.oritech.block.entity.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.api.transfer.fluid.SimpleFluidStorage;
import rearth.oritech.api.transfer.item.InOutInventoryStorage;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.block.blocks.storage.SmallFluidTank;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ComparatorOutputProvider;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

public class SmallTankEntity extends NetworkedBlockEntity implements FluidProvider, ItemProvider, ComparatorOutputProvider,
                                                                       ScreenProvider, MenuProvider {
    
    private int lastComparatorOutput = 0;
    public final boolean isCreative;
    
    private BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> cachedOutputTarget;
    
    public final InOutInventoryStorage inventory = new InOutInventoryStorage(3, this::setChanged, new ContainerSlotAssignment(0, 2, 2, 1));
    
    @SyncField({SyncType.TICK, SyncType.INITIAL})
    public final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(OritechConfig.portableTankCapacityBuckets.get() * 1000, this::setChanged);
    
    public SmallTankEntity(BlockPos pos, BlockState state, boolean isCreative) {
        super((isCreative ? BlockEntitiesContent.CREATIVE_TANK_ENTITY : BlockEntitiesContent.SMALL_TANK_ENTITY).get(), pos, state);
        this.isCreative = isCreative;
    }
    
    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fluidStorage.serialize(output);
        inventory.serialize(output);
    }
    
    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluidStorage.deserialize(input);
        inventory.deserialize(input);
        setChanged();
    }

    public boolean hasStoredFluidForDrops() {
        return fluidStorage.getAmount() > 0;
    }

    public FluidStack getStoredFluidForDrops() {
        return fluidStorage.getContent();
    }
    
    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        // fills/drains buckets
        
        // in creative, set tank fill level
        if (isCreative) {
            if (fluidStorage.getFluid() != Fluids.EMPTY) {
                fluidStorage.set(0, FluidResource.of(fluidStorage.getContent()), fluidStorage.getCapacity() - 1000 * 8);  //leave space to insert a bit
            } else {
                fluidStorage.set(0, FluidResource.of(fluidStorage.getContent()), 0);
            }
        }
        
        processInput();
        processOutput();
        
        if (fluidStorage.getAmount() > 0)
            outputToBelow();
        
        updateComparators(level, pos, state);
    }
    
    private void outputToBelow() {
        if (isCreative) return;

        if (cachedOutputTarget == null) {
            if (!(level instanceof ServerLevel serverLevel)) return;
            cachedOutputTarget = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, serverLevel, worldPosition.below(), Direction.UP);
        }

        var tankCandidate = cachedOutputTarget.getCapability();
        if (tankCandidate == null) return;

        var resource = fluidStorage.getResource(0);
        if (resource.isEmpty()) return;

        try (var transaction = Transaction.openRoot()) {
            var inserted = tankCandidate.insert(resource, fluidStorage.getAmount(), transaction);
            if (inserted <= 0) return;

            var extracted = fluidStorage.extract(0, resource, inserted, transaction);
            if (extracted == inserted) {
                transaction.commit();
            }
        }
    }
    
    private void updateComparators(Level level, BlockPos pos, BlockState state) {
        var previous = lastComparatorOutput;
        lastComparatorOutput = getComparatorOutput();
        
        if (previous != lastComparatorOutput) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }
    
    // from block entity to item
    private void processInput() {
        var canFill = this.fluidStorage.getAmount() > 0;

        if (!canFill) return;

        var inputStorage = inventory.getInputContainer();
        var inResource = inputStorage.getResource(0);
        if (inResource.isEmpty()) return;

        var inStack = inResource.toStack();
        if (inStack.getCount() > 1) return;

        var candidate = inStack.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forHandlerIndexStrict(inputStorage, 0));
        if (candidate == null) return;

        var resource = fluidStorage.getResource(0);
        if (resource.isEmpty()) return;

        try (var transaction = Transaction.openRoot()) {
            var inserted = candidate.insert(resource, fluidStorage.getAmount(), transaction);
            if (inserted > 0) {
                var extracted = fluidStorage.extract(0, resource, inserted, transaction);
                if (extracted == inserted) {
                    transaction.commit();
                    return;
                }
            }
        }

        moveInputToOutput(0);
    }

    // from item to fluid storage
    private void processOutput() {
        var canFill = this.fluidStorage.getAmount() < this.fluidStorage.getCapacity();

        if (!canFill) return;

        var inputStorage = inventory.getInputContainer();
        var inResource = inputStorage.getResource(1);
        if (inResource.isEmpty()) return;

        var inStack = inResource.toStack();
        if (inStack.getCount() > 1) return;

        var candidate = inStack.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forHandlerIndexStrict(inputStorage, 1));
        if (candidate == null) return;

        var resource = candidate.getResource(0);
        if (resource.isEmpty()) {
            moveInputToOutput(1);
            return;
        }

        try (var transaction = Transaction.openRoot()) {
            var maxTaken = Math.min(candidate.getAmountAsLong(0), fluidStorage.getCapacity() - fluidStorage.getAmount());
            var taken = candidate.extract(0, resource, (int) maxTaken, transaction);
            if (taken > 0) {
                var inserted = fluidStorage.insert(resource, taken, transaction);
                if (inserted == taken) {
                    transaction.commit();
                    return;
                }
            }
        }

        moveInputToOutput(1);
    }

    private void moveInputToOutput(int inputSlot) {
        var inputStorage = inventory.getInputContainer();
        var inResource = inputStorage.getResource(inputSlot);
        if (inResource.isEmpty()) return;

        try (var transaction = Transaction.openRoot()) {
            var inserted = inventory.getOutputContainer().insert(inResource, 1, transaction);
            if (inserted != 1) return;

            var extracted = inputStorage.extract(inputSlot, inResource, 1, transaction);
            if (extracted == 1) {
                transaction.commit();
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
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(getBlockPos());
        sendUpdate(SyncType.GUI_OPEN);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new OritechScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
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
    public StacksResourceHandler<ItemStack, ItemResource> getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.TANK_SCREEN.get();
    }
    
    public boolean isGlowingFluid() {
        return fluidStorage.getAmount() > 0 && fluidStorage.getContent().getFluid().defaultFluidState().createLegacyBlock().getLightEmission() > 0;
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
    public ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        return fluidStorage;
    }
}
