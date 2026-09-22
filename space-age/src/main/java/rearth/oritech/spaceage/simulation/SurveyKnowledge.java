package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import java.util.*;

/** Observations merge by source and time window: copying or retransmitting never creates exposure. */
public final class SurveyKnowledge {
    public record Observation(UUID target, UUID source, long window, int viewpoint, double exposure, double precision) {
        public static final Codec<Observation> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("target").forGetter(Observation::target),
                UUIDUtil.STRING_CODEC.fieldOf("source").forGetter(Observation::source),
                Codec.LONG.fieldOf("window").forGetter(Observation::window),
                Codec.INT.fieldOf("viewpoint").forGetter(Observation::viewpoint),
                Codec.DOUBLE.fieldOf("exposure").forGetter(Observation::exposure),
                Codec.DOUBLE.fieldOf("precision").forGetter(Observation::precision)
        ).apply(i, Observation::new));
        String key() { return target + ":" + source + ":" + window + ":" + viewpoint; }
    }
    public record Measurement(SpaceSimulation.SpaceObjectData data, long tick) {
        public static final Codec<Measurement> CODEC = RecordCodecBuilder.create(i -> i.group(
                SpaceSimulation.SpaceObjectData.CODEC.fieldOf("data").forGetter(Measurement::data),
                Codec.LONG.fieldOf("tick").forGetter(Measurement::tick)
        ).apply(i, Measurement::new));
    }
    public static final Codec<SurveyKnowledge> CODEC = RecordCodecBuilder.create(i -> i.group(
            Observation.CODEC.listOf().fieldOf("observations").forGetter(k -> List.copyOf(k.records.values())),
            Measurement.CODEC.listOf().fieldOf("measurements").forGetter(k -> List.copyOf(k.measurements.values()))
    ).apply(i, SurveyKnowledge::new));
    private final Map<String, Observation> records = new HashMap<>();
    private final Map<UUID, Measurement> measurements = new HashMap<>();
    public SurveyKnowledge() { }
    private SurveyKnowledge(List<Observation> observations, List<Measurement> measurements) {
        observations.forEach(o -> records.put(o.key(), o));
        measurements.forEach(m -> this.measurements.put(m.data.id(), m));
    }
    public SurveyKnowledge copy() { return new SurveyKnowledge(List.copyOf(records.values()), List.copyOf(measurements.values())); }
    public void merge(SurveyKnowledge other) {
        other.measurements.forEach((id, measurement) -> measurements.merge(id, measurement, (a, b) -> a.tick >= b.tick ? a : b));
        other.records.forEach((key, o) -> records.merge(key, o, (a, b) -> new Observation(a.target, a.source,
                a.window, a.viewpoint, Math.max(a.exposure, b.exposure), Math.max(a.precision, b.precision))));
    }
    public void scan(List<SpaceSimulation.SpaceObjectData> truth, UUID source, long tick, int viewpoint,
                     double x, double y, int scanners, int ticks, double range) {
        for (var target : truth) {
            if (target.type() != SpaceObjects.ObjectType.ASTEROID) continue;
            var distance = Math.hypot(target.x() - x, target.y() - y);
            if (distance > range) continue;
            measurements.put(target.id(), new Measurement(target, tick));
            var exposure = scanners * ticks * SurveyRules.exposurePerScannerTick(distance, range);
            var precision = exposure * SurveyRules.precisionPerExposure(distance, range);
            var observation = new Observation(target.id(), source, tick / 200, viewpoint, exposure, precision);
            records.merge(observation.key(), observation, (a, b) -> new Observation(a.target, a.source,
                    a.window, a.viewpoint, a.exposure + b.exposure, a.precision + b.precision));
        }
    }
    public double exposure(UUID target) {
        return records.values().stream().filter(o -> o.target.equals(target)).mapToDouble(Observation::exposure).sum();
    }
    public double precision(UUID target) {
        var windows = new HashMap<Long, List<Observation>>();
        records.values().stream().filter(o -> o.target.equals(target))
                .forEach(o -> windows.computeIfAbsent(o.window, ignored -> new ArrayList<>()).add(o));
        return windows.values().stream().mapToDouble(observations -> {
            var views = observations.stream().map(Observation::viewpoint).filter(v -> v >= 0).distinct().count();
            return observations.stream().mapToDouble(Observation::precision).sum() * (1 + Math.min(3, Math.max(0, views - 1)) * 0.5);
        }).sum();
    }
    public boolean precise(UUID target) { return exposure(target) >= 1 && precision(target) >= 16; }
    public List<SpaceSimulation.SpaceObjectData> contacts() {
        return measurements.values().stream().map(this::contact).filter(Objects::nonNull).toList();
    }
    public SpaceSimulation.SpaceObjectData contact(SpaceSimulation.SpaceObjectData truth) {
        var measured = measurements.get(truth.id());
        if (measured == null) return null;
        return contact(measured);
    }

    private SpaceSimulation.SpaceObjectData contact(Measurement measured) {
        var truth = measured.data;
        var work = exposure(truth.id());
        if (work < 1) return null;
        var precision = precision(truth.id());
        var exact = precision >= 16;
        var error = exact ? 0 : 20_000 / Math.max(1, precision);
        var angle = (truth.id().hashCode() & 0xffff) / 65536.0 * Math.PI * 2;
        var composition = work < 2 ? List.<SpaceObjects.AsteroidMaterial>of() : work >= 8 ? truth.materials()
                : truth.materials().stream().map(m -> new SpaceObjects.AsteroidMaterial(m.block(), 0)).toList();
        return new SpaceSimulation.SpaceObjectData(truth.id(), truth.type(), (float) (truth.x() + Math.cos(angle) * error),
                (float) (truth.y() + Math.sin(angle) * error), truth.velocityX(), truth.velocityY(), exact ? truth.radius() : 2_500,
                exact ? truth.surfaceGravity() : 0, work >= 8 ? truth.mass() : 0,
                exact ? SpaceObjects.DetectionState.PRECISE : SpaceObjects.DetectionState.ROUGH, truth.name(), composition, (float) error, (float) Math.min(1, work / 8));
    }

    public SpaceSimulation.SpaceObjectData contact(UUID target) {
        var measurement = measurements.get(target);
        return measurement == null ? null : contact(measurement.data);
    }
}
