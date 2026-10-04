package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import java.util.*;

/** Precise asteroid contacts collected by a craft and merged only when their results are transmitted or recovered. */
public final class SurveyKnowledge {
    public record Measurement(SpaceSimulation.SpaceObjectData data, long tick) {
        public static final Codec<Measurement> CODEC = RecordCodecBuilder.create(i -> i.group(
                SpaceSimulation.SpaceObjectData.CODEC.fieldOf("data").forGetter(Measurement::data),
                Codec.LONG.fieldOf("tick").forGetter(Measurement::tick)
        ).apply(i, Measurement::new));
    }
    public static final Codec<SurveyKnowledge> CODEC = Measurement.CODEC.listOf().fieldOf("measurements")
            .xmap(SurveyKnowledge::new, knowledge -> List.copyOf(knowledge.measurements.values())).codec();
    private final Map<UUID, Measurement> measurements = new HashMap<>();
    public SurveyKnowledge() { }
    private SurveyKnowledge(List<Measurement> measurements) {
        measurements.forEach(m -> this.measurements.put(m.data.id(), m));
    }
    public SurveyKnowledge copy() { return new SurveyKnowledge(List.copyOf(measurements.values())); }
    public void forget(UUID target) { measurements.remove(target); }
    public void merge(SurveyKnowledge other) {
        other.measurements.forEach((id, measurement) -> measurements.merge(id, measurement, (a, b) -> a.tick >= b.tick ? a : b));
    }
    public void scan(List<SpaceSimulation.SpaceObjectData> truth, long tick, double x, double y, double range) {
        for (var target : truth) {
            if (target.type() != SpaceObjects.ObjectType.ASTEROID) continue;
            var distance = Math.hypot(target.x() - x, target.y() - y);
            if (distance > range) continue;
            measurements.put(target.id(), new Measurement(target, tick));
        }
    }

    /** Reveal every asteroid represented by a survey region in one sweep. */
    public void scanRegion(List<SpaceSimulation.SpaceObjectData> truth, SpaceSimulation.SpaceObjectData region,
                           long tick, double x, double y, double range) {
        var distance = SurveyRules.surveyDistance(region, x, y);
        if (distance > range) return;
        for (var target : truth) {
            if (target.type() != SpaceObjects.ObjectType.ASTEROID
                    || Math.hypot(target.x() - region.x(), target.y() - region.y()) > region.radius()) continue;
            measurements.put(target.id(), new Measurement(target, tick));
        }
    }
    public boolean precise(UUID target) { return measurements.containsKey(target); }
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
        return new SpaceSimulation.SpaceObjectData(truth.id(), truth.type(), truth.x(), truth.y(),
                truth.velocityX(), truth.velocityY(), truth.radius(), truth.surfaceGravity(), truth.mass(),
                SpaceObjects.DetectionState.PRECISE, truth.name(), truth.materials());
    }

    public SpaceSimulation.SpaceObjectData contact(UUID target) {
        var measurement = measurements.get(target);
        return measurement == null ? null : contact(measurement.data);
    }
}
