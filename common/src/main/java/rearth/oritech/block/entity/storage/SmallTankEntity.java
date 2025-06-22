package rearth.oritech.block.entity.storage;

import dev.architectury.hooks.fluid.FluidStackHooks;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;

import java.util.List;
import java.util.Objects;

public class SmallTankEntity extends NetworkedBlockEntity implements FluidApi.BlockProvider, ItemApi.BlockProvider, ComparatorOutputProvider,
                                                                       ScreenProvider, ExtendedMenuProvider {
    
    private int lastComparatorOutput = 0;
    public final boolean isCreative;
    
    private ApiLookupCache<FluidApi.FluidStorage> downLookupCache;
    
    public final InOutInventoryStorage inventory = new InOutInventoryStorage(3, this::markDirty, new InventorySlotAssignment(0, 2, 2, 1));
    
    @SyncField({SyncType.TICK, SyncType.INITIAL})
    public final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(Oritech.CONFIG.portableTankCapacityBuckets() * FluidStackHooks.bucketAmount(), this::markDirty);
    
    public SmallTankEntity(BlockPos pos, BlockState state, boolean isCreative) {
        super(isCreative ? BlockEntitiesContent.CREATIVE_TANK_ENTITY : BlockEntitiesContent.SMALL_TANK_ENTITY, pos, state);
        this.isCreative = isCreative;
    }
    
    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
    }
    
    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        fluidStorage.readNbt(nbt, "");
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        markDirty();
    }
    
    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
    }
    
    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
    }
    
    @Override
    public void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
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
              pos.down(),
              Direction.UP, Objects.requireNonNull(world),
              ((world1, targetPos, targetState, targetEntity, direction) -> FluidApi.BLOCK.find(world1, targetPos, targetState, targetEntity, direction)));
            
        }
        
        var tankCandidate = downLookupCache.lookup();
        
        if (!(tankCandidate instanceof SimpleFluidStorage belowTank)) return;
        var ownTank = this.fluidStorage;
        
        SimpleFluidStorage.transfer(ownTank, belowTank, ownTank.getCapacity(), false);
    }
    
    private void updateComparators(World world, BlockPos pos, BlockState state) {
        var previous = lastComparatorOutput;
        lastComparatorOutput = getComparatorOutput();
        
        if (previous != lastComparatorOutput) {
            world.updateComparators(pos, state.getBlock());
        }
    }
    
    // from block entity to item
    private void processInput() {
        var inStack = inventory.getStack(0);
        var canFill = this.fluidStorage.getAmount() > 0;
        
        if (!canFill || inStack.isEmpty() || inStack.getCount() > 1) return;
        
        var stackRef = new StackContext(inStack, updated -> inventory.setStack(0, updated));
        var candidate = FluidApi.ITEM.find(stackRef);
        if (candidate == null || !candidate.supportsInsertion()) return;
        
        var moved = FluidApi.transferFirst(fluidStorage, candidate, FluidStackHooks.bucketAmount() * 64, false);
        
        if (moved == 0) {
            // move stack to out slot
            var outStack = inventory.getStack(2);
            if (outStack.isEmpty()) {
                inventory.setStack(2, stackRef.getValue());
                inventory.setStack(0, ItemStack.EMPTY);
            } else if (outStack.getItem().equals(stackRef.getValue().getItem()) && outStack.getCount() < outStack.getMaxCount()) {
                outStack.increment(1);
                inventory.setStack(0, ItemStack.EMPTY);
            }
        }
    }
    
    // from item to fluid storage
    private void processOutput() {
        var inStack = inventory.getStack(1);
        var canFill = this.fluidStorage.getAmount() < this.fluidStorage.getCapacity();
        
        if (!canFill || inStack.isEmpty() || inStack.getCount() > 1) return;
        
        var stackRef = new StackContext(inStack, updated -> inventory.setStack(1, updated));
        var candidate = FluidApi.ITEM.find(stackRef);
        if (candidate == null || !candidate.supportsExtraction()) return;
        
        var moved = FluidApi.transferFirst(candidate, fluidStorage, FluidStackHooks.bucketAmount() * 64, false);
        
        if (moved == 0) {
            // move stack
            var outStack = inventory.getStack(2);
            if (outStack.isEmpty()) {
                inventory.setStack(2, stackRef.getValue());
                inventory.setStack(1, ItemStack.EMPTY);
            } else if (outStack.getItem().equals(stackRef.getValue().getItem()) && outStack.getCount() < outStack.getMaxCount()) {
                outStack.increment(1);
                inventory.setStack(1, ItemStack.EMPTY);
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
    public void markDirty() {
        super.markDirty();
        if (world != null && getCachedState().get(SmallFluidTank.LIT) != isGlowingFluid()) {
            world.setBlockState(getPos(), getCachedState().with(SmallFluidTank.LIT, isGlowingFluid()));
        }
    }
    
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
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
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
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
