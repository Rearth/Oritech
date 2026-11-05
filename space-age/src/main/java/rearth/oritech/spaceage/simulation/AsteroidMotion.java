package rearth.oritech.spaceage.simulation;

/** Shared drag integration for released asteroids and flight forecasts. */
public final class AsteroidMotion {

    private AsteroidMotion() {
    }

    public static double stopTime(double speed) {

        return speed <= SpaceBalance.ASTEROID_STOP_SPEED ? 0
                : Math.log(speed / SpaceBalance.ASTEROID_STOP_SPEED) / SpaceBalance.SPACE_DRAG_PER_SECOND;
    }

    public static double distanceFactor(double speed, double seconds) {

        var movingTime = Math.min(Math.max(0, seconds), stopTime(speed));
        return -Math.expm1(-SpaceBalance.SPACE_DRAG_PER_SECOND * movingTime) / SpaceBalance.SPACE_DRAG_PER_SECOND;
    }

    public static double velocityFactor(double speed, double seconds) {

        return seconds >= stopTime(speed) ? 0 : Math.exp(-SpaceBalance.SPACE_DRAG_PER_SECOND * Math.max(0, seconds));
    }

    public static double timeAtFraction(double speed, double seconds, double fraction) {

        return -Math.log1p(-SpaceBalance.SPACE_DRAG_PER_SECOND * distanceFactor(speed, seconds) * fraction)
                / SpaceBalance.SPACE_DRAG_PER_SECOND;
    }
}
