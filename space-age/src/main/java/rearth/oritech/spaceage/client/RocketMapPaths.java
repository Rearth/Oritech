package rearth.oritech.spaceage.client;

import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Builds the map's decorative paths without changing simulated positions, times or fuel use. */
final class RocketMapPaths {

    // Branch links let detached craft start exactly on their parent's displayed path.
    private final SpaceSimulation.FlightPlan plan;
    // Numerical samples and events are the source of truth for timing and tooltips.
    private final RocketFlightPathCalculator.FlightPath flightPath;
    // Lookups avoid searching the full plan for each sample run.
    private final Map<UUID, SpaceSimulation.FlightPlanAction> actionsById = new HashMap<>();
    // Parent paths may need building before a child, regardless of their order in the plan.
    private final Map<UUID, RocketFlightPathCalculator.CraftPath> pathsByBranch = new HashMap<>();
    // Cached world geometry is shared by drawing, hover tests and event markers.
    private final Map<UUID, List<RenderedPathSegment>> renderedPathsByBranch = new HashMap<>();

    RocketMapPaths(SpaceSimulation.FlightPlan plan, RocketFlightPathCalculator.FlightPath flightPath) {
        this.plan = plan;
        this.flightPath = flightPath;
        plan.branches().forEach(branch -> branch.actions().forEach(action -> actionsById.put(action.id(), action)));
        flightPath.paths().forEach(path -> pathsByBranch.put(path.branchId(), path));
        flightPath.paths().forEach(this::renderedPathSegments);
    }

    List<RenderedPathSegment> renderedPathSegments(RocketFlightPathCalculator.CraftPath path) {
        var cached = renderedPathsByBranch.get(path.branchId());
        if (cached != null) return cached;
        var samples = path.samples();
        var segments = new ArrayList<RenderedPathSegment>();
        Point runOrigin = renderedBranchOrigin(path.branchId());
        UUID abortedTarget = null;
        RocketMapCurve abortedCurve = null;
        int runStart = 1;
        while (runStart < samples.size()) {
            // Samples describe the interval ending at them. Start each card at the preceding sample.
            UUID actionId = samples.get(runStart).actionId();
            UUID targetId = samples.get(runStart).targetId();
            int runEnd = runStart;
            while (runEnd + 1 < samples.size() && samples.get(runEnd + 1).actionId().equals(actionId)) runEnd++;
            var action = actionsById.get(actionId);
            boolean navigation = action != null && action.type() == SpaceSimulation.ActionType.NAVIGATE_TO;
            Point curveDestination = navigation ? abortDestination(path.branchId(), actionId) : null;
            // Instant cards keep the stopped curve; a new destination starts a fresh one.
            var inherited = !navigation || targetId.equals(abortedTarget) ? abortedCurve : null;
            var runSamples = samples.subList(runStart - 1, runEnd + 1);
            var points = runSamples.stream().map(sample -> new Point(sample.x(), sample.y())).toArray(Point[]::new);
            if (inherited == null) RocketMapCurve.alignOrigin(points, runOrigin);
            var curve = inherited != null ? inherited : RocketMapCurve.create(points[0],
                    curveDestination == null ? points[points.length - 1] : curveDestination, navigation);
            curve.appendSegments(segments, points, runSamples);
            runOrigin = curve.apply(points[points.length - 1]);
            if (navigation) {
                abortedTarget = curveDestination == null ? null : targetId;
                abortedCurve = curveDestination == null ? null : curve;
            }
            runStart = runEnd + 1;
        }
        var result = List.copyOf(segments);
        renderedPathsByBranch.put(path.branchId(), result);
        return result;
    }

    private Point abortDestination(UUID branchId, UUID actionId) {
        return flightPath.navigationAborts().stream()
                .filter(abort -> abort.branchId().equals(branchId) && abort.actionId().equals(actionId))
                .findFirst().map(abort -> new Point(abort.destinationX(), abort.destinationY())).orElse(null);
    }

    List<RenderedAsteroidSegment> renderedAsteroidPathSegments(
            RocketFlightPathCalculator.AsteroidPath path) {
        var samples = path.samples();
        if (samples.size() < 2) return List.of();
        var points = new Point[samples.size()];
        for (int index = 0; index < samples.size(); index++) {
            points[index] = new Point(samples.get(index).x(), samples.get(index).y());
        }
        var incoming = incomingAsteroidSegment(path.asteroidId(), samples.getFirst().timeSeconds());
        RocketMapCurve.alignOrigin(points, incoming == null ? null : incoming.to);
        // Released asteroids coast. Only fade out the release offset so they stay joined to the rocket path.
        var segments = new ArrayList<RenderedAsteroidSegment>();
        for (int index = 1; index < points.length; index++) {
            segments.add(new RenderedAsteroidSegment(points[index - 1], points[index]));
        }
        return segments;
    }

    private RenderedPathSegment incomingAsteroidSegment(UUID asteroidId, double releaseTime) {
        RenderedPathSegment closest = null;
        double closestTime = Double.NEGATIVE_INFINITY;
        for (var segments : renderedPathsByBranch.values()) {
            for (var segment : segments) {
                boolean carriesAsteroid = segment.firstSample.attachedAsteroidId().equals(asteroidId)
                        || segment.secondSample.attachedAsteroidId().equals(asteroidId);
                double segmentTime = segment.secondSample.timeSeconds();
                if (carriesAsteroid && segmentTime <= releaseTime + 0.01 && segmentTime >= closestTime
                        && Math.hypot(segment.to.x - segment.from.x,
                        segment.to.y - segment.from.y) > 0.001) {
                    closest = segment;
                    closestTime = segmentTime;
                }
            }
        }
        return closest;
    }

    private Point renderedBranchOrigin(UUID branchId) {
        for (var event : flightPath.boosterEvents()) {
            if (!event.childBranchId().equals(branchId)) continue;
            var parent = pathsByBranch.get(event.branchId());
            if (parent != null) renderedPathSegments(parent);
            return renderedWorldPosition(event.branchId(), event.timeSeconds(), event.x(), event.y());
        }
        var branch = plan.branches().stream().filter(item -> item.id().equals(branchId))
                .findFirst().orElse(null);
        if (branch == null || branch.isRoot()) return null;
        for (var parent : flightPath.paths()) {
            var moment = parent.actionMoments().stream()
                    .filter(item -> item.actionId().equals(branch.parentSeparationAction()))
                    .findFirst().orElse(null);
            if (moment == null) continue;
            renderedPathSegments(parent);
            return renderedWorldPosition(parent.branchId(), moment.timeSeconds(), moment.x(), moment.y());
        }
        return null;
    }

    Point renderedWorldPosition(UUID branchId, double timeSeconds, double worldX, double worldY) {
        var segments = renderedPathsByBranch.get(branchId);
        if (segments == null || segments.isEmpty()) {
            var origin = renderedBranchOrigin(branchId);
            return origin == null ? new Point(worldX, worldY) : origin;
        }
        // Markers use the very same line pieces as hover tests, including their subdivision fractions.
        for (var segment : segments) {
            double firstTime = segment.firstSample.timeSeconds();
            double secondTime = segment.secondSample.timeSeconds();
            if (timeSeconds < firstTime - 0.01 || timeSeconds > secondTime + 0.01) continue;
            double timeProgress = secondTime - firstTime <= 0.0001 ? 1
                    : Math.clamp((timeSeconds - firstTime) / (secondTime - firstTime), 0, 1);
            if (timeProgress < segment.sampleProgressFrom - 0.0001
                    || timeProgress > segment.sampleProgressTo + 0.0001) continue;
            double segmentProgress = segment.sampleProgressTo - segment.sampleProgressFrom <= 0.0001 ? 1
                    : Math.clamp((timeProgress - segment.sampleProgressFrom)
                    / (segment.sampleProgressTo - segment.sampleProgressFrom), 0, 1);
            return new Point(segment.from.x + (segment.to.x - segment.from.x) * segmentProgress,
                    segment.from.y + (segment.to.y - segment.from.y) * segmentProgress);
        }
        return new Point(worldX, worldY);
    }

    /**
     * @param from Start of this line piece.
     * @param to End of this line piece.
     * @param firstSample Simulation state before this piece.
     * @param secondSample State after this piece, including phase and firing engines.
     * @param sampleProgressFrom Fraction of the original sample interval where this piece starts.
     * @param sampleProgressTo Fraction of the original sample interval where this piece ends.
     */
    record RenderedPathSegment(Point from, Point to,
                                       RocketFlightPathCalculator.PathSample firstSample,
                                       RocketFlightPathCalculator.PathSample secondSample,
                                       double sampleProgressFrom, double sampleProgressTo) {
    }

    /**
     * @param from Start of this line piece.
     * @param to End of this line piece.
     */
    record RenderedAsteroidSegment(Point from, Point to) {
    }

    /**
     * @param x X position in the simulation plane.
     * @param y Y position in the simulation plane.
     */
    record Point(double x, double y) {
        Point lerp(Point other, double progress) {
            return new Point(x + (other.x - x) * progress, y + (other.y - y) * progress);
        }
    }

}
