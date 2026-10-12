package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// this data may change during flight and is specific to a single rocket segment
public class DynamicRocketSegment {

    private static final Codec<Set<UUID>> CONNECTIONS_CODEC = UUIDUtil.STRING_CODEC.listOf()
            .xmap(Set::copyOf, List::copyOf);
    private static final Codec<Map<BlockPos, RocketControllerState>> CRAFTING_CODEC = CargoEntry.CODEC.listOf().xmap(
            entries -> entries.stream().collect(Collectors.toMap(CargoEntry::pos, CargoEntry::cargo)),
            map -> map.entrySet().stream().map(e -> new CargoEntry(e.getKey(), e.getValue())).toList());
    private static final Codec<Map<BlockPos, BlockState>> LAYOUT_CODEC =
            StaticRocketSegment.BlockData.CODEC.listOf().xmap(
                    blocks -> blocks.stream().collect(Collectors.toMap(StaticRocketSegment.BlockData::relativePos, StaticRocketSegment.BlockData::state)),
                    map -> map.entrySet().stream().map(e -> new StaticRocketSegment.BlockData(e.getKey(), e.getValue())).toList());
    public static final Codec<DynamicRocketSegment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("fuel_burn_ticks").forGetter(segment -> segment.availableFuelBurnTimeTicks),
            Codec.LONG.fieldOf("available_rf").forGetter(segment -> segment.availableRF),
            Codec.DOUBLE.fieldOf("fuel_weight").forGetter(segment -> segment.currentFuelWeight),
            CONNECTIONS_CODEC.fieldOf("connected_segments").forGetter(DynamicRocketSegment::getConnectedSegments),
            CRAFTING_CODEC.fieldOf("crafters").forGetter(segment -> segment.crafters),
            Codec.LONG.fieldOf("fuel_capacity").forGetter(segment -> segment.fuelCapacity),
            Codec.LONG.fieldOf("rf_capacity").forGetter(segment -> segment.rfCapacity),
            LAYOUT_CODEC.fieldOf("layout").forGetter(segment -> segment.layout),
            LAYOUT_CODEC.fieldOf("reserved").forGetter(segment -> segment.reserved),
            CraftPass.CODEC.optionalFieldOf("craft_pass").forGetter(segment -> Optional.ofNullable(segment.pass)),
            RocketCargoTransfer.Result.CODEC.optionalFieldOf("exchange_report").forGetter(segment -> Optional.ofNullable(segment.lastExchange))
    ).apply(instance, DynamicRocketSegment::new));
    // Only changed cells are stored here; unchanged cells come from the launch structure.
    public final Map<BlockPos, BlockState> layout = new HashMap<>();
    // Inputs remain owned by these cells while a recipe runs, even when the layout shows air.
    public final Map<BlockPos, BlockState> reserved = new HashMap<>();
    // Cargo controllers share names with crafters, but never own a processing job.
    public final Map<BlockPos, RocketControllerState> crafters = new HashMap<>();
    // if a segment is connected, all couplings to it are still coupled/connected, and vice versa
    private final Set<UUID> connectedSegments = new HashSet<>();
    public CraftPass pass;
    public RocketCargoTransfer.Result lastExchange;
    public long fuelCapacity;
    public long rfCapacity;
    public long availableFuelBurnTimeTicks;
    public long availableRF;
    public double currentFuelWeight;

    public DynamicRocketSegment(long fuel, long rf, double mass, Set<UUID> connections, Map<BlockPos, RocketControllerState> crafters,
                                long fuelCapacity, long rfCapacity, Map<BlockPos, BlockState> layout,
                                Map<BlockPos, BlockState> reserved, Optional<CraftPass> pass, Optional<RocketCargoTransfer.Result> lastExchange) {

        this(fuel, rf, mass, connections, crafters, fuelCapacity, rfCapacity);
        this.layout.putAll(layout);
        this.reserved.putAll(reserved);
        this.pass = pass.map(CraftPass::copy).orElse(null);
        this.lastExchange = lastExchange.orElse(null);
    }

    public DynamicRocketSegment(long fuel, long rf, double mass, Set<UUID> connections, Map<BlockPos, RocketControllerState> crafters, long fuelCapacity, long rfCapacity) {

        this(fuel, rf, mass, connections, crafters);
        this.fuelCapacity = fuelCapacity;
        this.rfCapacity = rfCapacity;
    }

    public DynamicRocketSegment(long fuel, long rf, double mass, Set<UUID> connections, Map<BlockPos, RocketControllerState> crafters) {

        this(fuel, rf, mass, connections);
        crafters.forEach((pos, cargo) -> this.crafters.put(pos.immutable(), cargo.copy()));
    }

    public DynamicRocketSegment(long availableFuelBurnTimeTicks, long availableRF, double currentFuelWeight,
                                Set<UUID> connectedSegments) {

        this.availableFuelBurnTimeTicks = availableFuelBurnTimeTicks;
        this.fuelCapacity = availableFuelBurnTimeTicks;
        this.rfCapacity = availableRF;
        this.availableRF = availableRF;
        this.currentFuelWeight = currentFuelWeight;
        this.connectedSegments.addAll(connectedSegments);
    }

    public DynamicRocketSegment copy() {

        // Forecasts and detached craft must not share mutable passes, controllers or cell maps.
        return new DynamicRocketSegment(availableFuelBurnTimeTicks, availableRF, currentFuelWeight, connectedSegments, crafters,
                fuelCapacity, rfCapacity, layout, reserved, Optional.ofNullable(pass), Optional.ofNullable(lastExchange));
    }

    /**
     * Mixed fuels use the same remaining fraction for mass and chemical blast energy.
     */
    public void consumeFuel(long ticks) {

        var previous = availableFuelBurnTimeTicks;
        availableFuelBurnTimeTicks = Math.max(0, previous - ticks);
        if (previous > 0) currentFuelWeight *= availableFuelBurnTimeTicks / (double) previous;
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

    public record CargoEntry(BlockPos pos, RocketControllerState cargo) {

        public static final Codec<CargoEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("position").forGetter(CargoEntry::pos),
                RocketControllerState.CODEC.fieldOf("cargo").forGetter(CargoEntry::cargo)
        ).apply(i, CargoEntry::new));
    }
}
