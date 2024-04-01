package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.blocks.machines.storage.SmallStorageBlock;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.util.*;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.DelegatingEnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public abstract class ExpandableEnergyStorageBlockEntity extends BlockEntity implements EnergyProvider, MachineAddonController, BlockEntityTicker<ExpandableEnergyStorageBlockEntity> {
    
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    private final List<BlockPos> openSlots = new ArrayList<>();
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    private HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> directionCaches;
    
    
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
        
        outputEnergy();
    }
    
    private void outputEnergy() {
        if (energyStorage.amount <= 0) return;
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
        
        this.markDirty();
    }
    
    // defaults only to front block
    protected HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> getNeighborCaches(BlockPos pos, World world) {
        
        var res = new HashMap<Direction, BlockApiCache<EnergyStorage, Direction>>(6);
        var facing = getFacing();
        var blockInFront = (BlockPos) Geometry.offsetToWorldPosition(facing, new Vec3i(-1, 0, 0), pos);
        
        System.out.println(blockInFront.toCenterPos());
        ParticleContent.HIGHLIGHT_BLOCK.spawn(world, Vec3d.of(blockInFront));
        
        var frontCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, blockInFront);
        res.put(Direction.DOWN, frontCache);
        
        return res;
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        writeAddonToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        loadAddonNbtData(nbt);
        updateEnergyContainer();
        energyStorage.amount = nbt.getLong("energy_stored");
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
}
