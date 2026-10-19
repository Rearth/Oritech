package rearth.oritech.spaceage.robot.client;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rearth.oritech.spaceage.robot.RobotArmReach;

import java.util.Arrays;

/** Curved-chain IK in GeckoLib model pixels; pivots come from robot_arm.geo.json. */
public final class RobotArmIk {

    public static final String[] BONES = {"rotY", "rotZ2", "rotZ3", "rotZ4", "rotY2", "rotZ5", "rotZ6", "rotZ8", "rotZ9", "rotX", "rotX2"};
    public static final char[] AXES = {'y', 'z', 'z', 'z', 'y', 'z', 'z', 'z', 'z', 'x', 'x'};
    private static final float[] HEIGHTS = {29, 34.5f, 48.5f, 56.6f, 64.5f, 71.5f, 79.5f, 98.5f, 104.5f, 107.5f, 115.5f};
    private final float[] angles = new float[BONES.length];
    private final Vector3f[] positions = new Vector3f[BONES.length];
    private final Vector3f tip = new Vector3f();
    public static final float MAX_EXTENSION = RobotArmReach.MAX_EXTENSION;
    private float extension;
    private float renderedYaw;
    private boolean constrained;

    public RobotArmIk() {

        for (int i = 0; i < BONES.length; i++) {
            positions[i] = new Vector3f();
        }
        forward();
    }

    public Pose solve(Vector3f target, float seconds) {

        return solve(target, null, seconds);
    }

    public Pose solve(Vector3f target, Quaternionf rotation, float seconds) {

        constrained = rotation != null;
        if (constrained) target = new Vector3f(target).sub(rotation.transform(new Vector3f(0, 15.5f, 0)));

        var distance = target.distance(0, 34.5f, 8);
        // Bend the ordinary links first. The telescope only supplies the missing reach,
        // with extra clearance for work beneath the shoulder.
        var desiredExtension = Math.clamp(distance - (constrained ? 68.5f : 84) + Math.max(0, -target.y - 32) * .6f, 0, MAX_EXTENSION);
        extension = desiredExtension;
        var fold = 1 - Math.clamp((distance - 55) / (RobotArmReach.LENGTH + MAX_EXTENSION - 55), 0, 1);
        Arrays.fill(angles, 0);
        angles[2] = -.28f * fold;
        angles[3] = -.24f * fold;
        angles[5] = -.24f * fold;
        angles[7] = -.38f * fold;
        angles[8] = -.32f * fold;
        forward();
        // Each curved half has an equivalent endpoint vector. Solve those vectors,
        // rather than straightening the redundant hinges into two rigid rods.
        var upper = new Vector3f(positions[6]).sub(positions[1]);
        var upperTurn = angles[2] + angles[3] + angles[5];
        var lower = new Vector3f(tip).sub(positions[6]).rotateZ(-upperTurn);
        var upperLength = upper.length();
        var lowerLength = lower.length();
        var radial = (float) Math.hypot(target.x, target.z - 8);
        var vertical = target.y - 34.5f;
        var reach = Math.clamp(distance, Math.abs(upperLength - lowerLength) + .001f, upperLength + lowerLength - .001f);
        var shoulder = (float) Math.atan2(radial, vertical)
                - (float) Math.acos(Math.clamp((upperLength * upperLength + reach * reach - lowerLength * lowerLength) / (2 * upperLength * reach), -1, 1));
        var elbow = (float) Math.acos(Math.clamp((reach * reach - upperLength * upperLength - lowerLength * lowerLength) / (2 * upperLength * lowerLength), -1, 1));
        angles[1] = -shoulder + (float) Math.atan2(upper.x, upper.y);
        angles[6] = -elbow - upperTurn - (float) Math.atan2(upper.x, upper.y) + (float) Math.atan2(lower.x, lower.y);
        var yaw = (float) Math.atan2(8 - target.z, target.x);
        if (radial > .5f) {
            var delta = (float) Math.atan2(Math.sin(yaw - renderedYaw), Math.cos(yaw - renderedYaw));
            renderedYaw += delta * (1 - (float) Math.exp(-seconds * 18));
        }
        angles[0] = yaw;
        // A small spatial wrist curl uses the secondary axes without random motion.
        var twist = fold * Math.clamp(radial / 24, 0, 1);
        angles[4] = twist * (.12f + .45f * (float) Math.sin(yaw) * Math.min(1, radial * radial / (48 * 48)));
        angles[9] = .18f * twist;
        angles[10] = -.24f * twist;
        var reachable = new Vector3f(target).sub(0, 34.5f, 8).mul(Math.min(1, (upperLength + lowerLength - .15f) / Math.max(distance, .001f))).add(0, 34.5f, 8);
        refine(reachable);
        if (!constrained) angles[0] += renderedYaw - yaw;
        forward();
        var wrist = new Vector3f();
        if (constrained) {
            angles[10] = 0;
            var parent = orientation(new Pose(angles, extension, tip, null));
            new Quaternionf(parent).invert().mul(rotation).normalize().getEulerAnglesZYX(wrist);
            tip.add(rotation.transform(new Vector3f(0, 15.5f, 0)));
        }
        return new Pose(angles.clone(), extension, new Vector3f(tip), constrained ? wrist : null);
    }

    public static Pose interpolate(Pose previous, Pose next, float seconds) {

        if (previous == null) return next;
        var blend = 1 - (float) Math.exp(-seconds * 32);
        var rendered = next.angles().clone();
        for (int i = 0; i < rendered.length - 1; i++) {
            var delta = rendered[i] - previous.angles()[i];
            rendered[i] = previous.angles()[i] + (float) Math.atan2(Math.sin(delta), Math.cos(delta)) * blend;
        }
        var wrist = wristRotation(previous).slerp(wristRotation(next), blend).normalize().getEulerAnglesZYX(new Vector3f());
        rendered[rendered.length - 1] = 0;
        return new Pose(rendered, previous.extension() + (next.extension() - previous.extension()) * blend,
                new Vector3f(previous.tip()).lerp(next.tip(), blend), wrist);
    }

    private static Quaternionf wristRotation(Pose pose) {

        return pose.wrist() == null ? new Quaternionf().rotationX(pose.angles()[BONES.length - 1])
                : new Quaternionf().rotationZYX(pose.wrist().z, pose.wrist().y, pose.wrist().x);
    }

    private void refine(Vector3f target) {

        // Correct the three primary angles after adding the spatial curl. Damped
        // least squares stays well behaved near a fully stretched configuration.
        var joints = new int[]{0, 1, 6};
        for (int pass = 0; pass < 24; pass++) {
            forward();
            var error = new Vector3f(target).sub(tip);
            if (error.lengthSquared() < .0001f) break;
            var origin = new Vector3f(tip);
            var jacobian = new Matrix3f().zero();
            for (int column = 0; column < joints.length; column++) {
                angles[joints[column]] += .001f;
                forward();
                jacobian.setColumn(column, new Vector3f(tip).sub(origin).mul(1000));
                angles[joints[column]] -= .001f;
            }
            var transpose = new Matrix3f(jacobian).transpose();
            var normal = new Matrix3f(transpose).mul(jacobian);
            normal.m00(normal.m00() + 2f).m11(normal.m11() + 2f).m22(normal.m22() + 2f);
            var delta = normal.invert().transform(transpose.transform(error));
            for (int i = 0; i < joints.length; i++) angles[joints[i]] += Math.clamp(delta.get(i), -.15f, .15f);
        }
    }

    private float clearancePenalty() {

        var penalty = 0f;
        // The mount is intentionally inside the housing. Check every moving link above it.
        for (int i = 2; i <= positions.length; i++) {
            var a = positions[i - 1];
            var b = i == positions.length ? tip : positions[i];
            for (int sample = 0; sample <= 8; sample++) {
                var t = sample / 8f;
                var x = a.x + (b.x - a.x) * t;
                var y = a.y + (b.y - a.y) * t;
                var z = a.z + (b.z - a.z) * t;
                var depth = Math.min(29 - y, y + 3);
                depth = Math.min(depth, Math.min(x + 11, 27 - x));
                depth = Math.min(depth, Math.min(z + 11, 27 - z));
                if (depth > 0) penalty += depth * depth;
            }
        }
        return penalty;
    }

    public boolean clearsBase() {

        return clearancePenalty() < .01f;
    }

    private void forward() {

        var orientation = new Quaternionf();
        var position = new Vector3f(0, HEIGHTS[0], 8);
        for (int i = 0; i < BONES.length; i++) {
            if (i > 0) position.add(orientation.transform(new Vector3f(0, HEIGHTS[i] - HEIGHTS[i - 1] + (i == 7 ? extension : 0), 0)));
            positions[i].set(position);
            switch (AXES[i]) {
                case 'x' -> orientation.rotateX(angles[i]);
                case 'y' -> orientation.rotateY(angles[i]);
                default -> orientation.rotateZ(angles[i]);
            }
        }
        tip.set(position).add(orientation.transform(new Vector3f(0, constrained ? 0 : 131 - HEIGHTS[HEIGHTS.length - 1], 0)));
    }

    public static Quaternionf orientation(Pose pose) {

        var rotation = new Quaternionf();
        for (int i = 0; i < BONES.length; i++) {
            if (i == BONES.length - 1 && pose.wrist() != null) {
                rotation.rotateZYX(pose.wrist().z, pose.wrist().y, pose.wrist().x);
                continue;
            }
            switch (AXES[i]) {
                case 'x' -> rotation.rotateX(pose.angles()[i]);
                case 'y' -> rotation.rotateY(pose.angles()[i]);
                default -> rotation.rotateZ(pose.angles()[i]);
            }
        }
        return rotation;
    }

    /** Lift over the housing before traversing, then lower at the destination. */
    public static Vector3f travel(Vector3f start, Vector3f end, float progress) {

        var t = Math.clamp(progress, 0, 1);
        var clearance = 64 + Math.min(32, start.distance(end) * .25f);
        var height = Math.max(clearance, Math.max(start.y, end.y) + 16);
        var outerStart = outsideBase(start);
        var outerEnd = outsideBase(end);
        var raisedStart = new Vector3f(outerStart.x, height, outerStart.z);
        var raisedEnd = new Vector3f(outerEnd.x, height, outerEnd.z);
        if (t < .1f) return new Vector3f(start).lerp(outerStart, ease(t * 10));
        if (t > .9f) return outerEnd.lerp(end, ease((t - .9f) * 10));
        // A cubic arc avoids the stop-and-turn corners of separate lift/traverse legs.
        var u = ease((t - .1f) / .8f);
        var v = 1 - u;
        return outerStart.mul(v * v * v).add(raisedStart.mul(3 * v * v * u))
                .add(raisedEnd.mul(3 * v * u * u)).add(outerEnd.mul(u * u * u));
    }

    private static Vector3f outsideBase(Vector3f point) {

        var result = new Vector3f(point);
        if (point.y < 30 && point.x > -40 && point.x < 48 && point.z > -40 && point.z < 48) {
            // Descend alongside the housing before reaching underneath it.
            result.x = point.x < 8 ? -40 : 48;
        }
        return result;
    }

    public static float ease(float value) {

        var t = Math.clamp(value, 0, 1);
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    public record Pose(float[] angles, float extension, Vector3f tip, Vector3f wrist) {}
}
