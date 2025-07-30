package rearth.oritech.block.entity.pipes;

import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.EnergyApi.EnergyStorage;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyPipeInterfaceEntity extends GenericPipeInterfaceEntity implements EnergyApi.BlockProvider {
    
    private final SimpleEnergyStorage energyStorage;
    private final boolean isSuperConductor;
    
    private List<EnergyApi.EnergyStorage> cachedTargets = List.of();
    private int cacheHash;
    
    public EnergyPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENERGY_PIPE_ENTITY, pos, state);
        
        isSuperConductor = state.getBlock().equals(BlockContent.SUPERCONDUCTOR_CONNECTION) || state.getBlock().equals(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION);
        
        if (isSuperConductor) {
            energyStorage = new SimpleEnergyStorage(Oritech.CONFIG.superConductorTransferRate(), Oritech.CONFIG.superConductorTransferRate(), Oritech.CONFIG.superConductorTransferRate());
        } else {
            energyStorage = new SimpleEnergyStorage(Oritech.CONFIG.energyPipeTransferRate(), Oritech.CONFIG.energyPipeTransferRate(), Oritech.CONFIG.energyPipeTransferRate());
        }
        
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putLong("energy", energyStorage.getAmount());
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        energyStorage.setAmount(nbt.getLong("energy"));
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        // if energy is available
        // gather all connection targets supporting insertion
        // shuffle em
        // insert until no more energy is available
        
        if (world.isClientSide || energyStorage.getAmount() <= 0) return;
        
        var dataSource = isSuperConductor ? SuperConductorBlock.SUPERCONDUCTOR_DATA : EnergyPipeBlock.ENERGY_PIPE_DATA;
        
        var data = dataSource.getOrDefault(world.dimension().location(), new PipeNetworkData());
        var targets = findNetworkTargets(pos, data);
        
        if (targets == null) return;    // this should never happen
        
        var targetHash = targets.hashCode();
        
        List<EnergyApi.EnergyStorage> energyStorages;
        
        if (this.cacheHash == targetHash) {
            energyStorages = this.cachedTargets;
        } else {
            energyStorages = targets.stream()
                               .map(target -> EnergyApi.BLOCK.find(world, target.getA(), target.getB()))
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
    public void setChanged() {
        if (this.level != null)
            level.blockEntityChanged(worldPosition);
    }
}
