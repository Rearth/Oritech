package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.util.EnergyProvider;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.HashMap;

public abstract class PassiveGeneratorBlockEntity extends BlockEntity implements EnergyProvider, BlockEntityTicker<PassiveGeneratorBlockEntity> {
    
    protected final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(200000, 0, 1000);
    private HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> directionCaches;
    
    public PassiveGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, PassiveGeneratorBlockEntity blockEntity) {
        if (world.isClient || !isProducing()) return;
        
        var producedAmount = getProductionRate();
        energyStorage.amount = Math.min(energyStorage.amount + producedAmount, energyStorage.capacity);
        this.markDirty();
        
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
    
    public abstract int getProductionRate();
    
    public abstract boolean isProducing();
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        energyStorage.amount = nbt.getLong("energy");
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("energy", energyStorage.getAmount());
    }
    
    @Override
    public EnergyStorage getStorage() {
        return energyStorage;
    }
    
    protected HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> getNeighborCaches(BlockPos pos, World world) {
        
        var res = new HashMap<Direction, BlockApiCache<EnergyStorage, Direction>>(6);
        
        var topCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.up());
        res.put(Direction.DOWN, topCache);
        var botCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.down());
        res.put(Direction.UP, botCache);
        var northCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.north());
        res.put(Direction.SOUTH, northCache);
        var eastCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.east());
        res.put(Direction.WEST, eastCache);
        var southCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.south());
        res.put(Direction.NORTH, southCache);
        var westCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.west());
        res.put(Direction.EAST, westCache);
        
        return res;
    }
}
