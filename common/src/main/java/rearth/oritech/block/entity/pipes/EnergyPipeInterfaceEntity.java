package rearth.oritech.block.entity.pipes;

import earth.terrarium.common_storage_lib.energy.EnergyApi;
import earth.terrarium.common_storage_lib.energy.EnergyProvider;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import earth.terrarium.common_storage_lib.storage.util.TransferUtil;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.SimpleEnergyStorage;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EnergyPipeInterfaceEntity extends GenericPipeInterfaceEntity implements EnergyProvider.BlockEntity {
    
    private final SimpleEnergyStorage energyStorage;
    private final boolean isSuperConductor;
    
    private List<ValueStorage> cachedTargets;
    private int cacheHash;
    
    public EnergyPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENERGY_PIPE_ENTITY, pos, state);
        
        isSuperConductor = state.getBlock().equals(BlockContent.SUPERCONDUCTOR_CONNECTION);
        
        if (isSuperConductor) {
            energyStorage = new SimpleEnergyStorage(Oritech.CONFIG.superConductorTransferRate(), Oritech.CONFIG.superConductorTransferRate(), Oritech.CONFIG.superConductorTransferRate());
        } else {
            energyStorage = new SimpleEnergyStorage(Oritech.CONFIG.energyPipeTransferRate(), Oritech.CONFIG.energyPipeTransferRate(), Oritech.CONFIG.energyPipeTransferRate());
        }
        
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putLong("energy", energyStorage.getStoredAmount());
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        energyStorage.set(nbt.getLong("energy"));
    }
    
    @Override
    public ValueStorage getEnergy(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        // if energy is available
        // gather all connection targets supporting insertion
        // shuffle em
        // insert until no more energy is available
        
        if (world.isClient || energyStorage.getStoredAmount() <= 0) return;
        
        var dataSource = isSuperConductor ? SuperConductorBlock.SUPERCONDUCTOR_DATA : EnergyPipeBlock.ENERGY_PIPE_DATA;
        
        var data = dataSource.getOrDefault(world.getRegistryKey().getValue(), new PipeNetworkData());
        var targets = findNetworkTargets(pos, data);
        
        if (targets == null) return;    // this should never happen
        
        var targetHash = targets.hashCode();
        
        List<ValueStorage> energyStorages;
        
        if (this.cacheHash == targetHash) {
            energyStorages = this.cachedTargets;
        } else {
            energyStorages = targets.stream()
                               .map(target -> EnergyApi.BLOCK.find(world, target.getLeft(), target.getRight()))
                               .filter(obj -> Objects.nonNull(obj) && obj.allowsInsertion())
                               .collect(Collectors.toList());
            this.cachedTargets = energyStorages;
            this.cacheHash = targetHash;
        }
        
        Collections.shuffle(energyStorages);
        
        for (var targetStorage : energyStorages) {
            if (energyStorage.getStoredAmount() <= 0) break;
            TransferUtil.moveValue(energyStorage, targetStorage, Long.MAX_VALUE, false);
        }
        
    }
    
    @Override
    public void markDirty() {
        if (this.world != null)
            world.markDirty(pos);
    }
}
