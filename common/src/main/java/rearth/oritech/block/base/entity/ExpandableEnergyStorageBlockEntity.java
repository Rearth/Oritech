package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.machines.storage.SmallStorageBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.init.ItemContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ExpandableEnergyStorageBlockEntity extends BlockEntity implements EnergyApi.BlockEnergyApi.EnergyProvider, InventoryProvider, MachineAddonController, ScreenProvider, ExtendedScreenHandlerFactory, BlockEntityTicker<ExpandableEnergyStorageBlockEntity> {
    
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    private final List<BlockPos> openSlots = new ArrayList<>();
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    
    private boolean networkDirty = false;
    private boolean redstonePowered;
    
    public final SimpleInventory inventory = new SimpleInventory(1) {
        @Override
        public void markDirty() {
            ExpandableEnergyStorageBlockEntity.this.markDirty();
        }
    };
    
    protected final InventoryStorage inventoryStorage = InventoryStorage.of(inventory, null);
    
    //own storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), getDefaultExtractionRate(), this::markDirty);
    
    private final EnergyApi.EnergyContainer outputStorage = new DelegatingEnergyStorage(energyStorage, null) {
        @Override
        public boolean supportsInsertion() {
            return false;
        }
    };
    
    private final EnergyApi.EnergyContainer inputStorage = new DelegatingEnergyStorage(energyStorage, null) {
        @Override
        public boolean supportsExtraction() {
            return false;
        }
    };
    
    public ExpandableEnergyStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, ExpandableEnergyStorageBlockEntity blockEntity) {
        if (world.isClient) return;
        
        if (!redstonePowered)
            outputEnergy();
        
        inputFromCrystal();
        
        if (networkDirty) sendNetworkEntry();
    }
    
    private void inputFromCrystal() {
        if (energyStorage.amount >= energyStorage.capacity || inventory.isEmpty()) return;
        
        if (!inventory.getStack(0).getItem().equals(ItemContent.OVERCHARGED_CRYSTAL)) return;
        
        energyStorage.amount = Math.min(energyStorage.capacity, energyStorage.amount + Oritech.CONFIG.overchargedCrystalChargeRate());
    }
    
    private void outputEnergy() {
        if (energyStorage.amount <= 0) return;
        
        chargeItems();
        
        // todo caching for targets? Used to be BlockApiCache.create()
        var target = getOutputPosition(pos, getFacing());
        var candidate = EnergyApi.BLOCK.find(world, target.getRight(), target.getLeft());
        if (candidate != null) {
            EnergyApi.transfer(energyStorage, candidate, Long.MAX_VALUE, false);
        }
    }
    
    private void chargeItems() {
        
        var heldStack = inventory.heldStacks.get(0);
        if (heldStack.isEmpty()) return;
        
        var slot = ContainerItemContext.ofSingleSlot(getInventory(null).getSlot(0));
        var slotEnergyContainer = EnergyApi.ITEM.find(heldStack, slot);
        if (slotEnergyContainer != null) {
            EnergyApi.transfer(energyStorage, slotEnergyContainer, Long.MAX_VALUE, false);
        }
    }
    
    public static Pair<Direction, BlockPos> getOutputPosition(BlockPos pos, Direction facing) {
        var blockInFront = (BlockPos) Geometry.offsetToWorldPosition(facing, new Vec3i(-1, 0, 0), pos);
        var worldOffset = blockInFront.subtract(pos);
        var direction = Direction.fromVector(worldOffset.getX(), worldOffset.getY(), worldOffset.getZ());
        
        return new Pair<>(direction, blockInFront);
    }
    
    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        writeAddonToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putBoolean("redstone", redstonePowered);
    }
    
    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        loadAddonNbtData(nbt);
        updateEnergyContainer();
        energyStorage.amount = nbt.getLong("energy_stored");
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        redstonePowered = nbt.getBoolean("redstone");
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return inventoryStorage;
    }
    
    public Direction getFacing() {
        return getCachedState().get(SmallStorageBlock.TARGET_DIR);
    }
    
    
    @Override
    public EnergyApi.EnergyContainer getStorage(Direction direction) {
        
        if (direction == null)
            return energyStorage;
        
        if (direction.equals(getFacing())) {
            return outputStorage;
        } else {
            return inputStorage;
        }
    }
    
    @Override
    public List<BlockPos> getConnectedAddons() {
        return connectedAddons;
    }
    
    @Override
    public List<BlockPos> getOpenSlots() {
        return openSlots;
    }
    
    @Override
    public Direction getFacingForAddon() {
        var facing = Objects.requireNonNull(world).getBlockState(getPos()).get(SmallStorageBlock.TARGET_DIR);
        
        if (facing.equals(Direction.UP) || facing.equals(Direction.DOWN))
            return Direction.NORTH;
        
        return facing;
    }
    
    @Override
    public DynamicEnergyStorage getStorageForAddon() {
        return energyStorage;
    }
    
    @Override
    public SimpleInventory getInventoryForAddon() {
        return null;
    }
    
    @Override
    public ScreenProvider getScreenProvider() {
        return null;
    }
    
    @Override
    public BaseAddonData getBaseAddonData() {
        return addonData;
    }
    
    @Override
    public void setBaseAddonData(BaseAddonData data) {
        this.addonData = data;
    }
    
    public abstract long getDefaultExtractionRate();
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        sendNetworkEntry();
        return new ModScreens.UpgradableData(pos, getUiData(), getCoreQuality());
    }
    
    @Override
    public void markDirty() {
        if (world != null)
            world.markDirty(pos);
        networkDirty = true;
    }
    
    private boolean isActivelyViewed() {
        var closestPlayer = Objects.requireNonNull(world).getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 5, false);
        return closestPlayer != null && closestPlayer.currentScreenHandler instanceof BasicMachineScreenHandler handler && getPos().equals(handler.getBlockPos());
    }
    
    protected void sendNetworkEntry() {
        
        if (world.getTime() % 5 != 0 && !isActivelyViewed()) return;
        
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GenericEnergySyncPacket(pos, energyStorage.amount, energyStorage.capacity));
        networkDirty = false;
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this, getUiData(), getCoreQuality());
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 80, 35));
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
    public BlockPos getMachinePos() {
        return getPos();
    }
    
    @Override
    public World getMachineWorld() {
        return getWorld();
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.STORAGE_SCREEN;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    @Override
    public Property<Direction> getBlockFacingProperty() {
        return SmallStorageBlock.TARGET_DIR;
    }
    
    public void setRedstonePowered(boolean isPowered) {
        this.redstonePowered = isPowered;
    }
}
