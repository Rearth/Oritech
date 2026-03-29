package rearth.oritech.block.entity.pipes;

import rearth.oritech.init.OritechConfig;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    
    private List<ExtractablePipeInterfaceEntity.CachedTarget<EnergyApi.EnergyStorage>> cachedTargets = new ArrayList<>();
    private int cacheHash;
    
    public EnergyPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENERGY_PIPE_ENTITY, pos, state);
        
        isSuperConductor = state.getBlock().equals(BlockContent.SUPERCONDUCTOR_CONNECTION) || state.getBlock().equals(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION);
        
        if (isSuperConductor) {
            energyStorage = new SimpleEnergyStorage(OritechConfig.superConductorTransferRate.get(), OritechConfig.superConductorTransferRate.get(), OritechConfig.superConductorTransferRate.get());
        } else {
            energyStorage = new SimpleEnergyStorage(OritechConfig.energyPipeTransferRate.get(), OritechConfig.energyPipeTransferRate.get(), OritechConfig.energyPipeTransferRate.get());
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

        if (this.cacheHash != targetHash) {
            this.cachedTargets = targets.stream()
                                   .map(target -> new ExtractablePipeInterfaceEntity.CachedTarget<>(target.getA(), target.getB(), EnergyApi.BLOCK.createCache(world, target.getA(), target.getB())))
                                   .collect(Collectors.toList());
            this.cacheHash = targetHash;
        }
        
        Collections.shuffle(this.cachedTargets);
        
        for (var cachedTarget : this.cachedTargets) {
            if (energyStorage.getAmount() <= 0) break;
            var targetStorage = cachedTarget.lookup().find();
            if (targetStorage == null || !targetStorage.supportsInsertion()) continue;
            EnergyApi.transfer(energyStorage, targetStorage, Long.MAX_VALUE, false);
        }
        
    }
    
    @Override
    public void setChanged() {
        if (this.level != null)
            level.blockEntityChanged(worldPosition);
    }
}
