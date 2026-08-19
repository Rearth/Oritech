package rearth.oritech.spaceage.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// everything is in local space relative to the ORIGIN of the entire rocket (e.g. the position of where the rocket touched the pad)
// this contains the relevant data of the rocket after assembling, and assume that this information is never changed during the entire flight.
public record StaticRocketSegment(UUID segmentId, Set<BlockData> blocks, Map<UUID, Set<CouplingData>> originalCouplings, long staticWeight, int engineCount) {

    public StaticRocketSegment {
        blocks = Set.copyOf(blocks);

        var copiedCouplings = new HashMap<UUID, Set<CouplingData>>();
        originalCouplings.forEach((connectedSegment, couplings) -> copiedCouplings.put(connectedSegment, Set.copyOf(couplings)));
        originalCouplings = Map.copyOf(copiedCouplings);
    }

    public record BlockData(BlockPos relativePos, BlockState state) {}

    public record CouplingData(BlockPos relativePos, BlockPos oppositeSide){}

    public Set<UUID> getConnectedSegments() {
        return this.originalCouplings.keySet();
    }

    public Set<CouplingData> getCouplingsToSegment(UUID targetSegmentId) {
        return this.originalCouplings.get(targetSegmentId);
    }

}
