package rearth.oritech.block.entity.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.block.blocks.pipes.EnergyPipeBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.EnergyProvider;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class EnergyPipeInterfaceEntity extends GenericPipeInterfaceEntity implements EnergyProvider {
    
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(50000, 50000, 50000);
    private final HashMap<BlockPos, BlockApiCache<EnergyStorage, Direction>> lookupCache = new HashMap<>();
    
    public EnergyPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENERGY_PIPE_ENTITY, pos, state);
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("energy", energyStorage.getAmount());
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        energyStorage.amount = nbt.getLong("energy");
    }
    
    @Override
    public EnergyStorage getStorage() {
        return energyStorage;
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        // if energy is available
        // gather all connection targets supporting insertion
        // shuffle em
        // insert until no more energy is available
        
        if (world.isClient || energyStorage.getAmount() <= 0) return;
        
        var data = EnergyPipeBlock.ENERGY_PIPE_DATA.getOrDefault(world.getRegistryKey().getValue(), new PipeNetworkData());
        var targets = findNetworkTargets(pos, data);
        
        var energyStorages = targets.stream()
                               .map(target -> findFromCache(world, target.getLeft(), target.getRight()))
                               .filter(obj -> Objects.nonNull(obj) && obj.supportsInsertion())
                               .collect(Collectors.toList());
        
        Collections.shuffle(energyStorages);
        
        var availableEnergy = energyStorage.getAmount();
        try (var tx = Transaction.openOuter()) {
            for (var targetStorage : energyStorages) {
                var transferred = targetStorage.insert(availableEnergy, tx);
                energyStorage.extract(transferred, tx);
                availableEnergy -= transferred;
                
                if (availableEnergy <= 0) break;
            }
            
            tx.commit();
        }
        
        markDirty();
        
    }
    
    private EnergyStorage findFromCache(World world, BlockPos pos, Direction direction) {
        var cacheRes = lookupCache.computeIfAbsent(pos, elem -> BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos));
        return cacheRes.find(direction);
    }
}
