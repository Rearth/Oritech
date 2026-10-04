package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// this data may change during flight and is specific to a single rocket segment
public class DynamicRocketSegment {

    private static final Codec<Set<UUID>> CONNECTIONS_CODEC = UUIDUtil.STRING_CODEC.listOf()
            .xmap(Set::copyOf, List::copyOf);

    public static final Codec<DynamicRocketSegment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("fuel_burn_ticks").forGetter(segment -> segment.availableFuelBurnTimeTicks),
            Codec.LONG.fieldOf("available_rf").forGetter(segment -> segment.availableRF),
            Codec.LONG.fieldOf("fuel_weight").forGetter(segment -> segment.currentFuelWeight),
            CONNECTIONS_CODEC.fieldOf("connected_segments").forGetter(DynamicRocketSegment::getConnectedSegments)
    ).apply(instance, DynamicRocketSegment::new));

    public long availableFuelBurnTimeTicks;
    public long availableRF;
    public long currentFuelWeight;

    // if a segment is connected, all couplings to it are still coupled/connected, and vice versa
    private final Set<UUID> connectedSegments = new HashSet<>();

    public DynamicRocketSegment(long availableFuelBurnTimeTicks, long availableRF, long currentFuelWeight,
                                Set<UUID> connectedSegments) {
        this.availableFuelBurnTimeTicks = availableFuelBurnTimeTicks;
        this.availableRF = availableRF;
        this.currentFuelWeight = currentFuelWeight;
        this.connectedSegments.addAll(connectedSegments);
    }

    public Set<UUID> getConnectedSegments() {
        return connectedSegments;
    }

    public boolean isSegmentConnected(UUID segmentId) {
        return connectedSegments.contains(segmentId);
    }

    public void removeConnection(UUID segmentId) {
        connectedSegments.remove(segmentId);
    }
}
