package rearth.oritech.spaceage.simulation.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Constant resource rates during one navigation event, in burn ticks and RF per second.
 */
public record FlightResourceRate(double fuel, double rf) {

    public static final Codec<FlightResourceRate> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("fuel").forGetter(FlightResourceRate::fuel),
            Codec.DOUBLE.fieldOf("rf").forGetter(FlightResourceRate::rf)
    ).apply(i, FlightResourceRate::new));

    public static final Codec<Map<SegmentRef, FlightResourceRate>> MAP_CODEC = Entry.CODEC.listOf().xmap(
            entries -> entries.stream().collect(Collectors.toMap(Entry::segment, Entry::rate)),
            rates -> rates.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList());

    public void apply(DynamicRocketSegment resources, double seconds) {

        resources.consumeFuel(fuel * seconds);
        if (rf >= 0) resources.spendRF(rf * seconds);
        else resources.receiveRF(-rf * seconds);
    }

    private record Entry(SegmentRef segment, FlightResourceRate rate) {

        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                SegmentRef.CODEC.fieldOf("segment").forGetter(Entry::segment),
                FlightResourceRate.CODEC.fieldOf("rate").forGetter(Entry::rate)
        ).apply(i, Entry::new));
    }

}
