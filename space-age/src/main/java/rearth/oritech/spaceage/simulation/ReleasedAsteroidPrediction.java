package rearth.oritech.spaceage.simulation;

import static rearth.oritech.spaceage.simulation.RocketFlightPathState.BURN_TOLERANCE;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.AsteroidPath;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.MotionSample;

import java.util.ArrayList;
import java.util.List;

/** Predicts where an asteroid coasts after its rocket releases it. */
final class ReleasedAsteroidPrediction {

    private ReleasedAsteroidPrediction() {
    }

    static AsteroidPath predict(Craft craft,
                                                            Context context) {
        var asteroid = craft.attachedAsteroid;
        var earth = context.objects.get(SpaceObjects.EARTH_ID);
        var samples = new ArrayList<MotionSample>();
        var x = craft.x;
        var y = craft.y;
        var velocityX = craft.velocityX;
        var velocityY = craft.velocityY;
        var time = craft.time;
        var uncertainty = earth == null ? 100_000 : AsteroidImpactRules.landingUncertaintyBlocks(
                Math.max(0, Math.hypot(x - earth.xAt(time), y - earth.yAt(time)) - earth.radius()));
        samples.add(new MotionSample(time, x, y, Math.hypot(velocityX, velocityY)));
        AsteroidImpactRules.ImpactPrediction impact = null;
        if (earth != null && Math.hypot(x - earth.xAt(time), y - earth.yAt(time)) <= earth.radius()) {
            impact = impact(craft, earth, velocityX, velocityY);
            return new AsteroidPath(asteroid.id(), List.copyOf(samples), impact, uncertainty);
        }

        // This is only a map preview, so a coarse fixed step keeps the result quick and readable.
        for (var step = 0; earth != null && step < 480; step++) {
            var seconds = 2_500.0;
            var previousX = x;
            var previousY = y;
            var previousTime = time;
            var previousVelocityX = velocityX;
            var previousVelocityY = velocityY;
            var drag = Math.exp(-AsteroidImpactRules.SPACE_DRAG_PER_SECOND * seconds);
            x += velocityX * seconds;
            y += velocityY * seconds;
            velocityX *= drag;
            velocityY *= drag;
            time += seconds;
            var hitFraction = hitFraction(previousX - earth.xAt(previousTime), previousY - earth.yAt(previousTime),
                    x - earth.xAt(time), y - earth.yAt(time), earth.radius());
            if (Double.isFinite(hitFraction)) {
                x = previousX + (x - previousX) * hitFraction;
                y = previousY + (y - previousY) * hitFraction;
                time = previousTime + seconds * hitFraction;
                var hitDrag = Math.exp(-AsteroidImpactRules.SPACE_DRAG_PER_SECOND * seconds * hitFraction);
                velocityX = previousVelocityX * hitDrag;
                velocityY = previousVelocityY * hitDrag;
                samples.add(new MotionSample(time, x, y, Math.hypot(velocityX, velocityY)));
                impact = impact(craft, earth, velocityX, velocityY);
                break;
            }
            samples.add(new MotionSample(time, x, y, Math.hypot(velocityX, velocityY)));
            if (Math.hypot(velocityX, velocityY) < 0.01) break;
        }
        return new AsteroidPath(asteroid.id(), List.copyOf(samples), impact, uncertainty);
    }

    private static AsteroidImpactRules.ImpactPrediction impact(Craft craft,
                                                                SpaceSimulation.SpaceObjectData earth,
                                                                double velocityX, double velocityY) {
        // The landing card defines the impact settings even after the rocket has gone away.
        var landing = craft.lastEarthSurfaceAction == null
                ? SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(earth.id()).withOrbit(SpaceSimulation.OrbitBand.SURFACE)
                : craft.lastEarthSurfaceAction;
        var relativeSpeed = Math.hypot(velocityX - earth.velocityX(), velocityY - earth.velocityY());
        return AsteroidImpactRules.predictArrival(0, earth, relativeSpeed, craft.attachedAsteroid, landing);
    }

    private static double hitFraction(double startX, double startY, double endX, double endY, double radius) {
        var deltaX = endX - startX;
        var deltaY = endY - startY;
        var a = deltaX * deltaX + deltaY * deltaY;
        var c = startX * startX + startY * startY - radius * radius;
        if (c <= 0) return 0;
        if (a <= BURN_TOLERANCE) return Double.NaN;
        var b = 2 * (startX * deltaX + startY * deltaY);
        var discriminant = b * b - 4 * a * c;
        if (discriminant < 0) return Double.NaN;
        var fraction = (-b - Math.sqrt(discriminant)) / (2 * a);
        return fraction >= 0 && fraction <= 1 ? fraction : Double.NaN;
    }
}
