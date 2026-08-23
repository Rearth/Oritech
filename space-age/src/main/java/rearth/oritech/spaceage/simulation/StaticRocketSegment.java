package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// everything is in local space relative to the origin of the entire rocket
// this data does not change during the flight of a rocket segment
public record StaticRocketSegment(UUID segmentId, Set<BlockData> blocks, Map<UUID, Set<CouplingData>> originalCouplings,
                                  long staticWeight, int engineCount) {

    private static final Codec<Set<BlockData>> BLOCKS_CODEC = BlockData.CODEC.listOf()
            .xmap(Set::copyOf, List::copyOf);
    private static final Codec<Set<CouplingData>> COUPLINGS_CODEC = CouplingData.CODEC.listOf()
            .xmap(Set::copyOf, List::copyOf);
    private static final Codec<Map<UUID, Set<CouplingData>>> COUPLING_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, COUPLINGS_CODEC);

    public static final Codec<StaticRocketSegment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("segment_id").forGetter(StaticRocketSegment::segmentId),
            BLOCKS_CODEC.fieldOf("blocks").forGetter(StaticRocketSegment::blocks),
            COUPLING_MAP_CODEC.fieldOf("original_couplings").forGetter(StaticRocketSegment::originalCouplings),
            Codec.LONG.fieldOf("static_weight").forGetter(StaticRocketSegment::staticWeight),
            Codec.INT.fieldOf("engine_count").forGetter(StaticRocketSegment::engineCount)
    ).apply(instance, StaticRocketSegment::new));

    public StaticRocketSegment {
        blocks = Set.copyOf(blocks);

        var copiedCouplings = new HashMap<UUID, Set<CouplingData>>();
        originalCouplings.forEach((connectedSegment, couplings) -> copiedCouplings.put(connectedSegment, Set.copyOf(couplings)));
        originalCouplings = Map.copyOf(copiedCouplings);
    }

    public Set<UUID> getConnectedSegments() {
        return originalCouplings.keySet();
    }

    public Set<CouplingData> getCouplingsToSegment(UUID targetSegmentId) {
        return originalCouplings.get(targetSegmentId);
    }

    public record BlockData(BlockPos relativePos, BlockState state) {

        private static final Codec<BlockData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("position").forGetter(BlockData::relativePos),
                BlockState.CODEC.fieldOf("state").forGetter(BlockData::state)
        ).apply(instance, BlockData::new));
    }

    public record CouplingData(BlockPos relativePos, BlockPos oppositeSide) {

        private static final Codec<CouplingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("position").forGetter(CouplingData::relativePos),
                BlockPos.CODEC.fieldOf("opposite_position").forGetter(CouplingData::oppositeSide)
        ).apply(instance, CouplingData::new));
    }
}
