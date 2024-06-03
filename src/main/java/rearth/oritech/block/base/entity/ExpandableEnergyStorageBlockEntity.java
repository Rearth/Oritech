package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.machines.storage.SmallStorageBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.DelegatingEnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public abstract class ExpandableEnergyStorageBlockEntity extends BlockEntity implements EnergyProvider, InventoryProvider, MachineAddonController, ScreenProvider, ExtendedScreenHandlerFactory, BlockEntityTicker<ExpandableEnergyStorageBlockEntity> {
    
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    private final List<BlockPos> openSlots = new ArrayList<>();
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    private HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> directionCaches;
    
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
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), getDefaultExtractionRate()) {
        @Override
        public void onFinalCommit() {
            super.onFinalCommit();
            ExpandableEnergyStorageBlockEntity.this.markDirty();
        }
    };
    
    private final EnergyStorage outputStorage = new DelegatingEnergyStorage(energyStorage, null) {
        @Override
        public boolean supportsInsertion() {
            return false;
        }
    };
    
    private final EnergyStorage inputStorage = new DelegatingEnergyStorage(energyStorage, null) {
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
        
        if (networkDirty) sendNetworkEntry();
    }
    
    private void outputEnergy() {
        if (energyStorage.amount <= 0) return;
        
        chargeItems();
        
        var availableOutput = Math.min(energyStorage.amount, energyStorage.maxExtract);
        var totalInserted = 0L;
        
        if (directionCaches == null) directionCaches = getNeighborCaches(pos, world);
        
        try (var tx = Transaction.openOuter()) {
            for (var entry : directionCaches.entrySet()) {
                var insertDirection = entry.getKey().getOpposite();
                var targetCandidate = entry.getValue().find(insertDirection);
                if (targetCandidate == null) continue;
                var inserted = targetCandidate.insert(availableOutput, tx);
                availableOutput -= inserted;
                totalInserted += inserted;
                if (availableOutput <= 0) break;
            }
            energyStorage.extract(totalInserted, tx);
            tx.commit();
        }
        
        // can be skipped, since the change to the energy storage will already mark this as dirty
        //this.markDirty();
    }
    
    private void chargeItems() {
        EnergyStorageUtil.move(this.energyStorage,
          ContainerItemContext.ofSingleSlot(getInventory(null).getSlots().get(0)).find(EnergyStorage.ITEM),
          Long.MAX_VALUE,
          null);
    }
    
    // defaults only to front block
    protected HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> getNeighborCaches(BlockPos pos, World world) {
        
        var res = new HashMap<Direction, BlockApiCache<EnergyStorage, Direction>>(6);
        var facing = getFacing();
        var blockInFront = (BlockPos) Geometry.offsetToWorldPosition(facing, new Vec3i(-1, 0, 0), pos);
        var worldOffset = blockInFront.subtract(pos);
        
        var frontCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, blockInFront);
        res.put(Direction.fromVector(worldOffset.getX(), worldOffset.getY(), worldOffset.getZ()), frontCache);
        
        return res;
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        writeAddonToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
        nbt.put("inventory", inventory.toNbtList());
        nbt.putBoolean("redstone", redstonePowered);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        loadAddonNbtData(nbt);
        updateEnergyContainer();
        energyStorage.amount = nbt.getLong("energy_stored");
        inventory.readNbtList(nbt.getList("inventory", NbtElement.COMPOUND_TYPE));
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
    public EnergyStorage getStorage(Direction direction) {
        
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
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.getPos());
        buf.write(ADDON_UI_ENDEC, getUiData());
        buf.writeFloat(getCoreQuality());
        sendNetworkEntry();
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
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
        return Text.literal("invalid");
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
