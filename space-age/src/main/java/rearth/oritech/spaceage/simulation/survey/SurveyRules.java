package rearth.oritech.spaceage.simulation.survey;

import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared geometry for fixed-range survey actions.
 */
public final class SurveyRules {

    private SurveyRules() {
    }

    public static double navigationRadius(SpaceSimulation.SpaceObjectData target) {

        return target.radius() * (target.type() == SpaceObjects.ObjectType.SURVEY_REGION ? 0.72 : 1);
    }

    public static double orbitRadius(SpaceSimulation.SpaceObjectData target, SpaceSimulation.OrbitBand orbit) {

        // Asteroids need a navigable stand-off envelope despite their small physical size.
        if (target.type() == SpaceObjects.ObjectType.ASTEROID && orbit == SpaceSimulation.OrbitBand.TIGHT)
            return Math.max(5_000, target.radius() * 100 + orbit.altitude());
        return navigationRadius(target) + orbit.altitude();
    }

    public static boolean beltSegment(SpaceSimulation.SpaceObjectData target) {

        return target.type() == SpaceObjects.ObjectType.SURVEY_REGION && target.surveyHalfAngle() > 0;
    }

    public static boolean contains(SpaceSimulation.SpaceObjectData target, double x, double y) {

        return surveyDistance(target, x, y) < 1;
    }

    public static double boundingRadius(SpaceSimulation.SpaceObjectData target) {

        return target.radius() + (beltSegment(target)
                ? 2 * Math.hypot(target.x(), target.y()) * Math.sin(target.surveyHalfAngle() / 2) : 0);
    }

    public static Point beltPoint(SpaceSimulation.SpaceObjectData target, double angleOffset, double radialOffset) {

        var angle = Math.atan2(target.y(), target.x()) + angleOffset;
        var radius = Math.hypot(target.x(), target.y()) + radialOffset;
        return new Point(Math.cos(angle) * radius, Math.sin(angle) * radius);
    }

    public static List<Point> fogCenters(SpaceSimulation.SpaceObjectData target) {

        var count = Math.max(1, (int) Math.ceil(Math.hypot(target.x(), target.y()) * target.surveyHalfAngle() * 2 / (target.radius() * .7)));
        var result = new ArrayList<Point>();
        for (var i = 0; i <= count; i++)
            result.add(beltPoint(target, target.surveyHalfAngle() * (2.0 * i / count - 1), 0));
        return result;
    }

    public static List<Point> outline(SpaceSimulation.SpaceObjectData target) {

        var result = new ArrayList<Point>();
        for (var i = 0; i <= 24; i++)
            result.add(beltPoint(target, target.surveyHalfAngle() * (i / 12.0 - 1), target.radius()));
        for (var i = 24; i >= 0; i--)
            result.add(beltPoint(target, target.surveyHalfAngle() * (i / 12.0 - 1), -target.radius()));
        return result;
    }

    public static Point clampToRegion(SpaceSimulation.SpaceObjectData target, double x, double y) {

        if (beltSegment(target)) {
            var middle = Math.atan2(target.y(), target.x());
            var delta = Math.atan2(Math.sin(Math.atan2(y, x) - middle), Math.cos(Math.atan2(y, x) - middle));
            var angle = middle + Math.clamp(delta, -target.surveyHalfAngle(), target.surveyHalfAngle());
            var radius = Math.hypot(target.x(), target.y());
            var projected = Math.clamp(x * Math.cos(angle) + y * Math.sin(angle), radius - target.radius(), radius + target.radius());
            return new Point(Math.cos(angle) * projected, Math.sin(angle) * projected);
        }
        var dx = x - target.x();
        var dy = y - target.y();
        var scale = Math.min(1, target.radius() / Math.max(1e-9, Math.hypot(dx, dy)));
        return new Point(target.x() + dx * scale, target.y() + dy * scale);
    }

    public static double surveyDistance(SpaceSimulation.SpaceObjectData target, double x, double y) {

        if (beltSegment(target)) {
            var middle = Math.atan2(target.y(), target.x());
            var delta = Math.atan2(Math.sin(Math.atan2(y, x) - middle), Math.cos(Math.atan2(y, x) - middle));
            var angle = middle + Math.clamp(delta, -target.surveyHalfAngle(), target.surveyHalfAngle());
            var radius = Math.hypot(target.x(), target.y());
            var projected = Math.clamp(x * Math.cos(angle) + y * Math.sin(angle), radius - target.radius(), radius + target.radius());
            return Math.hypot(x - Math.cos(angle) * projected, y - Math.sin(angle) * projected);
        }
        var centerDistance = Math.hypot(target.x() - x, target.y() - y);
        if (target.type() == SpaceObjects.ObjectType.SURVEY_REGION) {
            return Math.max(0, centerDistance - target.radius());
        }
        return centerDistance;
    }

    public record Point(double x, double y) {
    }
}
