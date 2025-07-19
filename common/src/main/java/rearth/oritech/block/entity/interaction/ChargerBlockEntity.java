package rearth.oritech.block.entity.interaction;

import dev.architectury.hooks.fluid.FluidStackHooks;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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

public class ChargerBlockEntity extends NetworkedBlockEntity implements FluidApi.BlockProvider, EnergyApi.BlockProvider, ItemApi.BlockProvider,
                                                                 ScreenProvider, ExtendedMenuProvider {
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(Oritech.CONFIG.charger.energyCapacity(), Oritech.CONFIG.charger.maxEnergyInsertion(), Oritech.CONFIG.charger.maxEnergyExtraction(), this::markDirty);
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    private final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(16 * FluidStackHooks.bucketAmount(), this::markDirty);
    
    // 0 = bucket/item to be charged/filled, 1 = empty bucket/charged/fill item
    public final InOutInventoryStorage inventory = new InOutInventoryStorage(2, this::markDirty, new InventorySlotAssignment(0, 1, 1, 1));
    
    
    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.CHARGER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (world.isClient) return;
        
        // stop if no input is given, or it's a stackable item
        if (inventory.getStack(0).isEmpty() || inventory.getStack(0).getCount() > 1) return;
        
        var isFull = true;
        var startEnergy = energyStorage.amount;
        var startFluid = fluidStorage.getAmount();
        
        // try charge item
        if (!chargeItems()) isFull = false;
        
        // try filling item
        if (!fillItems()) isFull = false;
        
        // move charged and/or filled item to right
        if (isFull) {
            var outSlot = inventory.getStack(1);
            if (outSlot.isEmpty()) {
                inventory.setStack(1, inventory.getStack(0));
                inventory.setStack(0, ItemStack.EMPTY);
            }
        }
        
        if (fluidStorage.getAmount() != startFluid || energyStorage.amount != startEnergy) {
            ParticleContent.ASSEMBLER_WORKING.spawn(world, pos.toCenterPos().add(0.1, 0.1, 0), 1);
        }
        
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        fluidStorage.writeNbt(nbt, "");
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("energy_stored", energyStorage.amount);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        fluidStorage.readNbt(nbt, "");
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
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
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        this.sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(pos);
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
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
    public Inventory getDisplayedInventory() {
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
