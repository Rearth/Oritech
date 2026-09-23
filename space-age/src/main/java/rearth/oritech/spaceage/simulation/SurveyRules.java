package rearth.oritech.spaceage.simulation;

/** Shared geometry for fixed-range survey actions. */
public final class SurveyRules {
    private SurveyRules() { }

    public static double navigationRadius(SpaceSimulation.SpaceObjectData target) {
        return target.radius() * (target.type() == SpaceObjects.ObjectType.SURVEY_REGION ? 0.72 : 1);
    }

    public static double surveyDistance(SpaceSimulation.SpaceObjectData target, double x, double y) {
        var centerDistance = Math.hypot(target.x() - x, target.y() - y);
        if (target.type() == SpaceObjects.ObjectType.SURVEY_REGION) {
            return Math.max(0, centerDistance - target.radius());
        }
        return centerDistance;
    }
}
