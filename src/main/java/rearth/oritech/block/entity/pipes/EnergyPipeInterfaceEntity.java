package rearth.oritech.block.entity.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EnergyPipeInterfaceEntity extends GenericPipeInterfaceEntity implements EnergyApi.BlockProvider {

    private final SimpleEnergyStorage energyStorage;
    private final boolean isSuperConductor;

    private List<ExtractablePipeInterfaceEntity.CachedTarget<DynamicEnergyStorage>> cachedTargets = new ArrayList<>();
    private int cacheHash;

    public EnergyPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENERGY_PIPE_ENTITY.get(), pos, state);

        isSuperConductor = state.getBlock().equals(BlockContent.SUPERCONDUCTOR_CONNECTION) || state.getBlock().equals(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION);

        if (isSuperConductor) {
            energyStorage = new SimpleEnergyStorage(OritechConfig.superConductorTransferRate.get(), OritechConfig.superConductorTransferRate.get(), OritechConfig.superConductorTransferRate.get());
        } else {
            energyStorage = new SimpleEnergyStorage(OritechConfig.energyPipeTransferRate.get(), OritechConfig.energyPipeTransferRate.get(), OritechConfig.energyPipeTransferRate.get());
        }

    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getAmount());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.setAmount(input.getLongOr("energy", 0));
    }

    @Override
    public DynamicEnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        // if energy is available
        // gather all connection targets supporting insertion
        // shuffle em
        // insert until no more energy is available

        if (level.isClientSide() || energyStorage.getAmount() <= 0) return;

        var dataSource = isSuperConductor ? SuperConductorBlock.SUPERCONDUCTOR_DATA : EnergyPipeBlock.ENERGY_PIPE_DATA;

        var data = dataSource.getOrDefault(level.dimension().location(), new PipeNetworkData());
        var targets = findNetworkTargets(pos, data);

        if (targets == null) return;    // this should never happen

        var targetHash = targets.hashCode();

        if (this.cacheHash != targetHash) {
            this.cachedTargets = targets.stream()
                    .map(target -> new ExtractablePipeInterfaceEntity.CachedTarget<>(target.getA(), target.getB(), EnergyApi.BLOCK.createCache(level, target.getA(), target.getB())))
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
