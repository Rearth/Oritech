package rearth.oritech.spaceage.simulation;

/** Shared scan rates and conservative survey estimates, without exposing hidden asteroid data. */
public final class SurveyRules {
    private SurveyRules() { }

    public static double exposurePerScannerTick(double distance, double range) {
        return (2 - distance / range) / SpaceBalance.DAY;
    }

    public static double precisionPerExposure(double distance, double range) {
        return 1 + 31 * Math.pow(1 - distance / range, 2);
    }

    public static double navigationRadius(SpaceSimulation.SpaceObjectData target) {
        return target.radius() * (target.type() == SpaceObjects.ObjectType.SURVEY_REGION ? 0.72 : 1);
    }

    public static double surveyDistance(SpaceSimulation.SpaceObjectData target, double x, double y) {
        return Math.hypot(target.x() - x, target.y() - y)
                + (target.type() == SpaceObjects.ObjectType.SURVEY_REGION ? target.radius() : target.uncertainty());
    }
}
