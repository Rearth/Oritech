package rearth.oritech.spaceage.client.hologram;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import rearth.oritech.spaceage.block.hologram.HoloSettings;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Point;
import rearth.oritech.spaceage.block.hologram.HoloView;
import rearth.oritech.spaceage.block.hologram.HoloView.Frame;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Bounded, stable 3D fog samples; reveals region geometry, never hidden asteroid positions. */
final class HoloClouds {

    private HoloClouds() {
    }

    record Puff(Vec3 center, double radius) {
    }

    static List<Puff> build(HoloSnapshot snapshot, Frame frame) {

        if (!snapshot.settings().shows(HoloSettings.SURVEYS)) return List.of();
        var regions = snapshot.objects().stream().filter(o -> o.type() == ObjectType.SURVEY_REGION).toList();
        if (regions.isEmpty()) return List.of();
        var result = new ArrayList<Puff>();
        var width = snapshot.surface().width();
        var depth = snapshot.surface().depth();
        var step = Math.max(width, depth) / 24d;
        var random = new Random(42);
        for (var x = step / 2; x < width; x += step) {
            for (var z = step / 2; z < depth; z += step) {
                var px = x + (random.nextDouble() - .5) * step * .4;
                var pz = z + (random.nextDouble() - .5) * step * .4;
                var height = random.nextDouble();
                var point = HoloView.unproject(new Point(px, pz), frame, snapshot);
                if (regions.stream().noneMatch(region -> SurveyRules.contains(region, point.x(), point.y()))) continue;
                add(result, px, pz, step * .9, width, depth, height);
            }
        }
        // Small fields must remain visible even when they fall between grid samples.
        for (var region : regions) {
            var radius = region.radius() * Math.max(.1, Math.min(width, depth) - .5) / frame.coverage();
            if (radius >= step) continue;
            var centers = SurveyRules.beltSegment(region) ? SurveyRules.fogCenters(region) : List.of(new SurveyRules.Point(region.x(), region.y()));
            for (var point : centers) {
                var center = HoloView.project(point.x(), point.y(), frame, snapshot);
                if (HoloView.inside(center, snapshot.surface())) add(result, center.x(), center.y(), Math.max(.05, radius), width, depth, random.nextDouble());
            }
        }
        return List.copyOf(result);
    }

    private static void add(List<Puff> result, double x, double z, double radius, int width, int depth, double height) {

        var edge = Math.min(Math.min(x - .08, width - .08 - x), Math.min(z - .08, depth - .08 - z));
        if (edge <= .01) return;
        result.add(new Puff(new Vec3(x, HoloView.PLANE_HEIGHT + (height - .5) * .26, z), Math.min(radius, edge)));
    }

    static Vec3 edge(Puff puff, Vector3f right, Vector3f up, double angle) {

        var a = Math.cos(angle) * puff.radius;
        var b = Math.sin(angle) * puff.radius;
        var center = puff.center;
        return new Vec3(center.x + right.x * a + up.x * b,
                Math.clamp(center.y + right.y * a + up.y * b, .12, .88),
                center.z + right.z * a + up.z * b);
    }
}
