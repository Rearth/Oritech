package rearth.oritech.block.entity.pipes;

import rearth.oritech.init.OritechConfig;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.AggregatingEnergyStorage;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyPipeInterfaceEntity extends GenericPipeInterfaceEntity implements EnergyApi.BlockProvider {

    // PipeNetworkData is level-scoped but shared by every interface in that pipe type. Keeping the
    // dispatcher here makes its per-tick state transient instead of adding it to saved topology.
    private static final Map<PipeNetworkData, PipeDataRuntime> PIPE_DATA_RUNTIMES = new IdentityHashMap<>();
    
    private final SimpleEnergyStorage energyStorage;
    private final boolean isSuperConductor;
    
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
        if (world.isClientSide) return;
        
        var dataSource = isSuperConductor ? SuperConductorBlock.SUPERCONDUCTOR_DATA : EnergyPipeBlock.ENERGY_PIPE_DATA;
        var data = dataSource.get(world.dimension().location());
        if (data == null) return;

        var pipeRuntime = PIPE_DATA_RUNTIMES.computeIfAbsent(data, ignored -> new PipeDataRuntime());
        pipeRuntime.removeStaleNetworks(data);

        if (energyStorage.getAmount() <= 0) return;

        var networkId = data.pipeNetworkLinks.getOrDefault(pos, -1);
        if (networkId == -1) return;

        var networkRuntime = pipeRuntime.networks.computeIfAbsent(networkId, ignored -> new EnergyNetworkRuntime());
        var gameTime = world.getGameTime();
        if (networkRuntime.lastProcessedTick == gameTime) return;

        // Every interface still ticks, but only the first energized interface dispatches for the
        // whole network. This changes the hot path from N sources walking M targets to one O(N + M)
        // pass while retaining the combined extraction limit of every source interface.
        networkRuntime.lastProcessedTick = gameTime;
        networkRuntime.rebuildIfNeeded(world, data, networkId);
        networkRuntime.distribute(world, this);
    }

    public static void clearRuntimeData() {
        PIPE_DATA_RUNTIMES.values().forEach(PipeDataRuntime::clear);
        PIPE_DATA_RUNTIMES.clear();
    }
    
    @Override
    public void setChanged() {
        if (this.level != null)
            level.blockEntityChanged(worldPosition);
    }

    private static final class PipeDataRuntime {
        private final Map<Integer, EnergyNetworkRuntime> networks = new HashMap<>();
        private long topologyRevision = Long.MIN_VALUE;

        private void removeStaleNetworks(PipeNetworkData data) {
            if (topologyRevision == data.getTopologyRevision()) return;

            var iterator = networks.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                var networkId = entry.getKey();
                var network = entry.getValue();

                if (!data.pipeNetworks.containsKey(networkId)) {
                    network.invalidateTopology();
                    iterator.remove();
                } else if (network.topologyRevision != data.getNetworkRevision(networkId)) {
                    network.invalidateTopology();
                }
            }
            topologyRevision = data.getTopologyRevision();
        }

        private void clear() {
            networks.values().forEach(EnergyNetworkRuntime::invalidateTopology);
            networks.clear();
        }
    }

    private static final class EnergyNetworkRuntime {
        private long topologyRevision = Long.MIN_VALUE;
        private long lastProcessedTick = Long.MIN_VALUE;
        private int nextSourceIndex;
        private int nextTargetIndex;
        private List<BlockPos> sourcePositions = List.of();
        private List<ExtractablePipeInterfaceEntity.CachedTarget<EnergyApi.EnergyStorage>> targets = List.of();

        private void invalidateTopology() {
            targets.forEach(target -> target.lookup().invalidate());
            targets = List.of();
            sourcePositions = List.of();
            topologyRevision = Long.MIN_VALUE;
        }

        private void rebuildIfNeeded(Level world, PipeNetworkData data, int networkId) {
            var currentRevision = data.getNetworkRevision(networkId);
            if (topologyRevision == currentRevision) return;

            sourcePositions = data.pipeNetworks.getOrDefault(networkId, Set.of()).stream()
                                .filter(data.machineInterfaces::containsKey)
                                .sorted(Comparator.comparingLong(BlockPos::asLong))
                                .toList();
            var sourcePositionSet = new HashSet<>(sourcePositions);

            targets = data.pipeNetworkInterfaces.getOrDefault(networkId, Set.of()).stream()
                          // A stale/corrupt topology entry must never feed an interface buffer back
                          // into the same network and create an energy ping-pong loop.
                          .filter(target -> !sourcePositionSet.contains(target.getA()))
                          .sorted(Comparator.comparingLong((net.minecraft.util.Tuple<BlockPos, Direction> target) -> target.getA().asLong())
                                            .thenComparingInt(target -> target.getB().ordinal()))
                          .map(target -> new ExtractablePipeInterfaceEntity.CachedTarget<>(
                              target.getA(), target.getB(), EnergyApi.BLOCK.createCache(world, target.getA(), target.getB())))
                          .toList();

            if (targets.isEmpty()) nextTargetIndex = 0;
            else nextTargetIndex = Math.floorMod(nextTargetIndex, targets.size());
            if (sourcePositions.isEmpty()) nextSourceIndex = 0;
            else nextSourceIndex = Math.floorMod(nextSourceIndex, sourcePositions.size());
            topologyRevision = currentRevision;
        }

        private void distribute(Level world, EnergyPipeInterfaceEntity triggeringInterface) {
            var sources = new ArrayList<EnergyApi.EnergyStorage>(sourcePositions.size());
            var includedTrigger = false;
            var sourceCount = sourcePositions.size();
            var sourceStartIndex = sourceCount == 0 ? 0 : Math.floorMod(nextSourceIndex, sourceCount);

            for (var offset = 0; offset < sourceCount; offset++) {
                var sourcePos = sourcePositions.get((sourceStartIndex + offset) % sourceCount);
                if (!world.hasChunkAt(sourcePos)) continue;

                var sourceEntity = world.getBlockEntity(sourcePos);
                if (!(sourceEntity instanceof EnergyPipeInterfaceEntity source) || source.isRemoved()) continue;
                if (source.isSuperConductor != triggeringInterface.isSuperConductor) continue;

                if (source == triggeringInterface) includedTrigger = true;
                if (source.energyStorage.getAmount() > 0) sources.add(source.energyStorage);
            }

            if (sourceCount > 0) nextSourceIndex = (sourceStartIndex + 1) % sourceCount;

            // Be defensive against topology data that is still being repaired during block updates.
            // The triggering interface is known to belong to this network and must not be stranded.
            if (!includedTrigger && triggeringInterface.energyStorage.getAmount() > 0) {
                sources.add(triggeringInterface.energyStorage);
            }

            if (sources.isEmpty() || targets.isEmpty()) return;

            var aggregate = new AggregatingEnergyStorage(sources);
            var targetCount = targets.size();
            var startIndex = Math.floorMod(nextTargetIndex, targetCount);

            for (var offset = 0; offset < targetCount && aggregate.extract(Long.MAX_VALUE, true) > 0; offset++) {
                var target = targets.get((startIndex + offset) % targetCount);
                var targetStorage = target.lookup().find();
                if (targetStorage == null || !targetStorage.supportsInsertion()) continue;

                EnergyApi.transfer(aggregate, targetStorage, Long.MAX_VALUE, false);
            }

            // All targets may be visited when energy is plentiful, so advancing by one rather than
            // by the visit count ensures a different destination receives first chance next tick.
            nextTargetIndex = (startIndex + 1) % targetCount;
        }
    }
}
