package rearth.oritech.block.entity.pipes;

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
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.SimpleEnergyStorage;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EnergyPipeInterfaceEntity extends GenericPipeInterfaceEntity implements EnergyApi.BlockProvider {
    
    private final SimpleEnergyStorage energyStorage;
    private final boolean isSuperConductor;
    
    private List<EnergyApi.EnergyContainer> cachedTargets;
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
        nbt.putLong("energy", energyStorage.getAmount());
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        energyStorage.setAmount(nbt.getLong("energy"));
    }
    
    @Override
    public EnergyApi.EnergyContainer getStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        // if energy is available
        // gather all connection targets supporting insertion
        // shuffle em
        // insert until no more energy is available
        
        if (world.isClient || energyStorage.getAmount() <= 0) return;
        
        var dataSource = isSuperConductor ? SuperConductorBlock.SUPERCONDUCTOR_DATA : EnergyPipeBlock.ENERGY_PIPE_DATA;
        
        var data = dataSource.getOrDefault(world.getRegistryKey().getValue(), new PipeNetworkData());
        var targets = findNetworkTargets(pos, data);
        
        if (targets == null) return;    // this should never happen
        
        var targetHash = targets.hashCode();
        
        List<EnergyApi.EnergyContainer> energyStorages;
        
        if (this.cacheHash == targetHash) {
            energyStorages = this.cachedTargets;
        } else {
            energyStorages = targets.stream()
                               .map(target -> EnergyApi.BLOCK.find(world, target.getLeft(), target.getRight()))
                               .filter(obj -> Objects.nonNull(obj) && obj.supportsInsertion())
                               .collect(Collectors.toList());
            this.cachedTargets = energyStorages;
            this.cacheHash = targetHash;
        }
        
        Collections.shuffle(energyStorages);
        
        for (var targetStorage : energyStorages) {
            if (energyStorage.getAmount() <= 0) break;
            EnergyApi.transfer(energyStorage, targetStorage, Long.MAX_VALUE, false);
        }
        
    }
    
    @Override
    public void markDirty() {
        if (this.world != null)
            world.markDirty(pos);
    }
}
