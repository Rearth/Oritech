package rearth.oritech.spaceage.simulation.mission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import rearth.oritech.spaceage.simulation.flight.FlightMotion;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Small display-only routes, independent of guidance samples and resource accounting. */
public final class MissionJourney {

    private MissionJourney() {
    }

    public record Point(double x, double y) {

        public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Point::x), Codec.DOUBLE.fieldOf("y").forGetter(Point::y)
        ).apply(i, Point::new));
    }

    public record Leg(UUID action, List<Point> points) {

        public static final Codec<Leg> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("action").forGetter(Leg::action),
                Point.CODEC.listOf().fieldOf("points").forGetter(Leg::points)
        ).apply(i, Leg::new));
    }

    public static List<Point> sample(List<PathSample> samples, double start, double end, int count) {

        if (samples.isEmpty() || end <= start) return List.of();
        var points = new ArrayList<Point>();
        for (int i = 0; i < count; i++) {
            var state = FlightMotion.sample(samples, start + (end - start) * i / (count - 1));
            points.add(new Point(state.x(), state.y()));
        }
        return List.copyOf(points);
    }

    public static List<Leg> legs(CraftPath path) {

        var result = new ArrayList<Leg>();
        var samples = path.samples();
        for (int start = 0; start + 1 < samples.size();) {
            var action = samples.get(start + 1).actionId();
            var end = start + 1;
            while (end + 1 < samples.size() && samples.get(end + 1).actionId().equals(action)) end++;
            var points = sample(samples, samples.get(start).timeSeconds(), samples.get(end).timeSeconds(), 16);
            if (!points.isEmpty()) result.add(new Leg(action, points));
            start = end;
        }
        return List.copyOf(result);
    }

    static void remember(MissionState state) {

        if (state.transfer == null || state.transfer.path == null || state.transfer.ticks <= 0) return;
        var root = state.transfer.path.paths().stream().filter(p -> p.branchId().equals(state.transfer.start.plan().root().id())).findFirst();
        if (root.isEmpty() || root.get().samples().isEmpty()) return;
        var samples = root.get().samples();
        var points = sample(samples, samples.getFirst().timeSeconds(), Math.min(root.get().durationSeconds(), state.transfer.ticks / 20d), 8);
        if (points.isEmpty()) return;
        var action = state.transfer.start.plan().currentAction();
        if (action != null) state.journeyHistory.add(new Leg(action.id(), points));
        if (state.journeyHistory.size() > 64) state.journeyHistory.removeFirst();
    }
}
